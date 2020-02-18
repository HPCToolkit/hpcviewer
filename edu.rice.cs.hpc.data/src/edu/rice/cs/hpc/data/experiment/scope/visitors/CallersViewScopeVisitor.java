package edu.rice.cs.hpc.data.experiment.scope.visitors;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.scope.filters.ExclusiveOnlyMetricPropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.filters.InclusiveOnlyMetricPropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter;


/*************************
 * A class that manages the initialization phase of the creation of callers tree
 * For the second phase of callers tree (incremental callers path) should be seen in CallSiteScopeCallerView
 * 
 * @author laksonoadhianto
 *
 */
public class CallersViewScopeVisitor extends CallerScopeBuilder implements IScopeVisitor {

	//----------------------------------------------------
	// Constants
	//----------------------------------------------------

	//----------------------------------------------------
	// private data
	//----------------------------------------------------
	private final CombineCallerScopeMetric combinedMetrics;
	
	private final ExclusiveOnlyMetricPropagationFilter exclusiveOnly;
	private final InclusiveOnlyMetricPropagationFilter inclusiveOnly;
	
	final private ListCombinedScopes listCombinedScopes;

	final private Hashtable<String, Scope> calleeht = new Hashtable<String, Scope>();
	
	private RootScope callersViewRootScope;
	
	/****--------------------------------------------------------------------------------****
	 * 
	 * @param experiment
	 * @param cvrs
	 * @param nMetrics
	 * @param dodebug
	 * @param filter
	 ****--------------------------------------------------------------------------------****/
	public CallersViewScopeVisitor(Experiment experiment, RootScope cvrs, 
			int nMetrics, boolean dodebug, MetricValuePropagationFilter filter) {
		this.callersViewRootScope = cvrs;
		exclusiveOnly = new ExclusiveOnlyMetricPropagationFilter(experiment);
		inclusiveOnly = new InclusiveOnlyMetricPropagationFilter(experiment);
		combinedMetrics = new CombineCallerScopeMetric();

		listCombinedScopes = new ListCombinedScopes();

	}

	//----------------------------------------------------
	// visitor pattern instantiations for each Scope type
	//----------------------------------------------------

	public void visit(CallSiteScope scope, ScopeVisitType vt) {
		
		//--------------------------------------------------------------------------------
		// if there are no exclusive costs to attribute from this context, we are done here
		//--------------------------------------------------------------------------------
		if (!scope.hasNonzeroMetrics()) {
			return; 
		}
		
		if (vt == ScopeVisitType.PreVisit) {
			this.listCombinedScopes.push();

			// Find (or add) callee in top-level hashtable
			ProcedureScope callee = this.createProcedureIfNecessary(scope);
			
			prepareCallChain(scope, callee);

		} else if (vt == ScopeVisitType.PostVisit)  {
			
			this.decrementCounter();
		}
	}
	
		 
	public void visit(Scope scope, ScopeVisitType vt) { }
	public void visit(RootScope scope, ScopeVisitType vt) { }
	public void visit(LoadModuleScope scope, ScopeVisitType vt) { }
	public void visit(FileScope scope, ScopeVisitType vt) { }
	
	public void visit(ProcedureScope scope, ScopeVisitType vt) { 
		
		//--------------------------------------------------------------------------------
		// if there are no exclusive costs to attribute from this context, we are done here
		//--------------------------------------------------------------------------------
		if ( !scope.hasNonzeroMetrics() || 
			  scope.isFalseProcedure() ) {
			return; 
		}
		
		if (vt == ScopeVisitType.PreVisit) {
			this.listCombinedScopes.push();
				// Find (or add) callee in top-level hashtable
			ProcedureScope callee = this.createProcedureIfNecessary(scope);
			if (scope.isAlien()) {
				prepareCallChain(scope, callee);
			}

		} else if (vt == ScopeVisitType.PostVisit){
			
			this.decrementCounter();
		}
	}
	
	public void visit(AlienScope scope, ScopeVisitType vt) { }
	public void visit(LoopScope scope, ScopeVisitType vt) { }
	public void visit(StatementRangeScope scope, ScopeVisitType vt) { 	}
	public void visit(LineScope scope, ScopeVisitType vt) {  }
	public void visit(GroupScope scope, ScopeVisitType vt) { }

	
	//----------------------------------------------------
	// helper functions  
	//----------------------------------------------------
	
	
	/*****
	 * prepare and initialize call chain of a given scope node and procedure node
	 * 
	 * @param scope: the current scope
	 * @param callee: the callee node (the procedure scope of this call chain)
	 */
	private void prepareCallChain(Scope scope, ProcedureScope callee) {
		LinkedList<CallSiteScopeCallerView> callPathList = createCallChain(callersViewRootScope,
				scope, scope, 
				combinedMetrics, this.inclusiveOnly, this.exclusiveOnly);

		//-------------------------------------------------------
		// ensure my call path is represented among my children.
		//-------------------------------------------------------
		mergeCallerPath(IMergedScope.MergingStatus.INIT, 0, callee, callPathList, 
				combinedMetrics, this.inclusiveOnly, this.exclusiveOnly);
	}
	
	/********
	 * Find caller view's procedure of a given scope. 
	 * If it doesn't exist, create a new one, attach to the tree, and copy the metrics
	 * @param cct_s: either call site or procedure
	 * @return
	 ********/
	private ProcedureScope createProcedureIfNecessary( Scope cct_s ) {
		ProcedureScope cct_proc_s;
		
		if (cct_s instanceof ProcedureScope)
			cct_proc_s = (ProcedureScope) cct_s;
		else
			cct_proc_s = ( (CallSiteScope)cct_s).getProcedureScope();
		
		String objCode = cct_s.getSourceFile().getFileID() + ":" + cct_proc_s.getFlatIndex();

		ProcedureScope caller_proc = (ProcedureScope) calleeht.get(objCode);
		
		if (caller_proc == null) {
			// create a new procedure scope
			caller_proc = (ProcedureScope) cct_proc_s.duplicate();
			caller_proc.setRootScope(callersViewRootScope);
			
			// add to the tree
			callersViewRootScope.addSubscope(caller_proc);
			caller_proc.setParentScope(this.callersViewRootScope);
			
			// add to the dictionary
			calleeht.put(objCode, caller_proc);
		}
		
		// accumulate the metrics
		this.combinedMetrics.combine(caller_proc, cct_s, inclusiveOnly, exclusiveOnly);
		return caller_proc;
	}
		

	
	/********
	 * decrement the counter of a caller scope
	 * @param caller_s
	 ********/
	private void decrementCounter() {
		
		//---------------------------------------------------------------------------
		// decrement all the combined scopes which are computed in this scope
		// 	When a caller view scope is created, it creates also its children and its merged children
		//		the counter of these children are then need to be decremented based on the CCT scope
		//---------------------------------------------------------------------------
		ArrayList<Scope> list = this.listCombinedScopes.pop();
		if (list != null) {
			Iterator<Scope> iter = list.iterator();
			while (iter.hasNext()) {
				Scope combinedScope = iter.next();
				combinedScope.decrementCounter();
			}
		}
	}
	

	
	//----------------------------------------------------
	// helper classes  
	//----------------------------------------------------
	
	/********************************************************************
	 *
	 * class helper to store the list of combined scopes
	 * 
	 ********************************************************************/
	static private class ListCombinedScopes {
		private Stack<ArrayList<Scope>> combinedScopes;
		
		public ArrayList<Scope> push() {
			if (this.combinedScopes == null) {
				this.combinedScopes = new Stack< ArrayList<Scope>>();
			}
			ArrayList<Scope> list = new ArrayList<Scope>();
			this.combinedScopes.push(list);
			return list;
		}
		
		public ArrayList<Scope> pop() {
			return this.combinedScopes.pop();
		}
		
		public void addList(Scope combined) {
			ArrayList<Scope> list = this.combinedScopes.peek();
			list.add(combined);
		}
		
	}
	
	
	/********************************************************************
	 *
	 * class to combine metrics from different scopes
	 *
	 ********************************************************************/
	private class CombineCallerScopeMetric extends AbstractCombineMetric {

		public void combine(Scope target, Scope source,
				MetricValuePropagationFilter inclusiveOnly,
				MetricValuePropagationFilter exclusiveOnly) {

			if (target.isCounterZero() && inclusiveOnly != null) {
				target.safeCombine(source, inclusiveOnly);
			}
			if (exclusiveOnly != null)
				target.combine(source, exclusiveOnly);

			target.incrementCounter();

			listCombinedScopes.addList(target);
			
		}
	}
	
	
}
