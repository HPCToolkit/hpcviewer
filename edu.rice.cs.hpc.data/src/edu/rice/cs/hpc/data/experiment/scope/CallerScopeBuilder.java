package edu.rice.cs.hpc.data.experiment.scope;

import java.util.LinkedList;

import edu.rice.cs.hpc.data.experiment.metric.AbstractCombineMetric;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope.ProcedureType;
import edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter;

public class CallerScopeBuilder {
	
	// number of maximum "descendants" in callers tree
	static private final int MAX_DESC = 2;

	
	/******
	 * create caller tree, return the list of call path of this scope_cct
	 * @param scope_cct: scope in CCT
	 * @param scope_cost: the scope for cost reference. All nodes will be assigned with this cost
	 * @param combine: the combine method
	 * @param inclusiveOnly
	 * @param exclusiveOnly
	 * 
	 * @return list of call path
	 */
	static public LinkedList<CallSiteScopeCallerView> createCallChain(RootScope root_caller_tree,
			Scope scope_cct, Scope scope_cost, AbstractCombineMetric combine, 
			MetricValuePropagationFilter inclusiveOnly, MetricValuePropagationFilter exclusiveOnly ) {
		//-----------------------------------------------------------------------
		// compute callPath: a chain of my callers
		//
		// we build a chain of callers  by tracing the path from a procedure
		// up to the root of the calling context tree. notice that 
		// this isn't simply a reversal of the path. in the chain of callers,
		// we associate the call site with the caller. in the calling context
		// tree, the call site is paired with the callee. that's why we
		// work with a pair of CallSiteScopes at a time (innerCS and enclosingCS)
		//-----------------------------------------------------------------------
		Scope innerCS = scope_cct;
		LinkedList<CallSiteScopeCallerView> callPathList = new LinkedList<CallSiteScopeCallerView>();
		Scope next = scope_cct.getParentScope();
		int numKids = 0;
		CallSiteScopeCallerView prev_scope = null;
		while ( (next != null) && !(next instanceof RootScope) && (numKids<MAX_DESC) )
		{
			if ( isCallSiteCandidate(next, innerCS)) {

				Scope enclosingCS = null;
				ProcedureScope mycaller = null;
				
				if (next instanceof ProcedureScope) {
					mycaller = (ProcedureScope) next;
					if (((ProcedureScope)next).isAlien()) {
						// hack for alien procedure: use the current scope
						//	for the enclosing call site
						enclosingCS = next;
					}
					
				}	else if (next instanceof CallSiteScope) {
					enclosingCS = next;
					mycaller = ((CallSiteScope)enclosingCS).getProcedureScope(); 
				}
				
				LineScope lineScope = null;
				
				if (innerCS instanceof CallSiteScope) {
					// normal call site
					lineScope = ((CallSiteScope)innerCS).getLineScope();
				} else {
					// hack for alien procedure: create a new line scope
					lineScope = new LineScope(innerCS.root, innerCS.getSourceFile(),
							innerCS.getFirstLineNumber(), innerCS.getCCTIndex(),
							innerCS.getFlatIndex());
				}
				
				if(lineScope != null) {
					numKids++;
					if (prev_scope != null)
						prev_scope.markScopeHasChildren(); //numChildren = 1;

					//--------------------------------------------------------------
					// creating a new child scope if the path is not too long (< MAX_DESC)
					//--------------------------------------------------------------
					if (numKids<MAX_DESC) {
						// we will reuse CCT ID of the CCT scope as the unique identifier of this
						// call site.
						// the CallSiteScopeCallerView class will also use the flat ID of the
						// CCT scope for its flat ID.
						// While the metric of this new scope is the same as the original 
						//	CCT call site scope (scope_cost)
						CallSiteScopeCallerView callerScope =
							new CallSiteScopeCallerView( lineScope, mycaller,
									CallSiteScopeType.CALL_FROM_PROCEDURE, next.getCCTIndex(), next, scope_cost);

						callerScope.setRootScope(root_caller_tree);
						// set the value of the new scope
						combine.combine(callerScope, scope_cost, inclusiveOnly, exclusiveOnly);
						callPathList.addLast(callerScope);
						
						innerCS = enclosingCS;
						prev_scope = callerScope;
					}
				}
			}
			next = next.getParentScope();
		}

		return callPathList;
	}
	
	
	/****
	 * Merge the same path in the caller path (if exist)
	 * @param callee
	 * @param callerPathList
	 * @param combine
	 * @param inclusiveOnly
	 * @param exclusiveOnly
	 */
	static public void mergeCallerPath(IMergedScope.MergingStatus status, int counter_to_assign, Scope callee, 
			LinkedList<CallSiteScopeCallerView> callerPathList, AbstractCombineMetric combine,
			MetricValuePropagationFilter inclusiveOnly, MetricValuePropagationFilter exclusiveOnly) 
	{
		if (callerPathList.size() == 0) return; // merging an empty path is trivial

		CallSiteScopeCallerView first = callerPathList.removeFirst();

		// -------------------------------------------------------------------------
		// attempt to merge first node on caller path with existing caller of callee  
		//--------------------------------------------------------------------------
		int nCallers = callee.getSubscopeCount();
		for (int i = 0; i < nCallers; i++) {
			CallSiteScopeCallerView existingCaller = (CallSiteScopeCallerView) callee.getSubscope(i);

			//------------------------------------------------------------------------
			// we check if the scope is identical with the existing scope in the path
			// if it is the case, we should merge them
			//------------------------------------------------------------------------
			final ProcedureScope firstProc  = first.getProcedureScope();
			final ProcedureScope callerProc = existingCaller.getProcedureScope();
			
			String firstID  = String.valueOf(firstProc.getFlatIndex());
			String callerID = String.valueOf(callerProc.getFlatIndex());
						
			if (firstID.equals(callerID)) {

				//------------------------------------------------------------------------
				// combine metric values for first to those of existingCaller.
				//------------------------------------------------------------------------
				combine.combine(existingCaller, first, inclusiveOnly, exclusiveOnly);

				//------------------------------------------------------------------------
				// We found the same CCT in the path. let's merge them
				//------------------------------------------------------------------------
				existingCaller.merge(status, first, counter_to_assign);

				//------------------------------------------------------------------------
				// merge rest of call path as a child of existingCaller.
				//------------------------------------------------------------------------
				mergeCallerPath(status, counter_to_assign, existingCaller, callerPathList, combine, inclusiveOnly, exclusiveOnly);

				return; // merged with existing child. nothing left to do.
			}
		}

		//----------------------------------------------
		// no merge possible. add new path into tree.
		//----------------------------------------------
		addNewPathIntoTree(callee, first, callerPathList);
	}

	
	
	/**********
	 * add children 
	 * @param callee: the parent
	 * @param first: the first child
	 * @param callerPathList: list of children (excluding the first child)
	 */
	static public void addNewPathIntoTree(Scope callee, CallSiteScopeCallerView first,
			LinkedList<CallSiteScopeCallerView> callerPathList) {
		
		callee.addSubscope(first);
		first.setParentScope(callee);
		for (Scope prev = first; callerPathList.size() > 0; ) {
			Scope next = callerPathList.removeFirst();
			prev.addSubscope(next);
			next.setParentScope(prev);
			prev = next;
		}
	}
	
	
	/******
	 * Return if a scope is either a callsite or a procedure scope.
	 * The inner scope cannot be empty
	 * 
	 * @param scope
	 * @param innerCS
	 * @return
	 */
	static private boolean isCallSiteCandidate(Scope scope, Scope innerCS) {
		return ( ((scope instanceof CallSiteScope) || 
				// laks 2013.12.2 original code: (scope instanceof ProcedureScope && !((ProcedureScope)scope).isAlien()) )
				(scope instanceof ProcedureScope) )
				&& (innerCS != null) );
	}
}
