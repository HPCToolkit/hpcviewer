package edu.rice.cs.hpc.data.experiment.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import edu.rice.cs.hpc.data.experiment.metric.AbstractCombineMetric;
import edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.visitors.CallersViewScopeVisitor;


/****************************************************************************
 * special class for caller view's call site scope
 * 
 *
 ****************************************************************************/
public class CallSiteScopeCallerView extends CallSiteScope implements IMergedScope {

	private boolean flag_scope_has_child;

	private Scope scopeCCT; // store the orignal CCT scope
	private Scope scopeCost; // the original CCT cost scope. In caller view, a caller scope needs 2 pointers: the cct and the scope
	
	private ArrayList<CallSiteScopeCallerView> listOfmerged;

	static final private IncrementalCombineMetricUsingCopy combine_with_dupl = new IncrementalCombineMetricUsingCopy();
	static final private CombineMetricUsingCopyNoCondition combine_without_cond = new CombineMetricUsingCopyNoCondition();
	
	/**
	 * 
	 * @param scope
	 * @param scope2
	 * @param csst
	 * @param id
	 * @param cct
	 */
	public CallSiteScopeCallerView(LineScope scope, ProcedureScope scope2,
			CallSiteScopeType csst, int id, Scope cct, Scope s_cost) {
		super(scope, scope2, csst, id, cct.getFlatIndex());

		this.scopeCCT = cct;
		this.scopeCost = s_cost;
		this.flag_scope_has_child = false;
	}

	/***
	 * retrieve the CCT scope of this scope
	 * @return
	 */
	public Scope getScopeCCT() {
		return this.scopeCCT;
	}
	
	
	/***
	 * add merged scope into the list
	 * @param status
	 * @param scope
	 */
	public void merge(IMergedScope.MergingStatus status, CallSiteScopeCallerView scope, int counter_to_assign) {
		if (listOfmerged == null) 
			listOfmerged = new ArrayList<CallSiteScopeCallerView>();
		
		//-----------------------------------------
		// During the initialization phase (the first time a caller tree is created,
		//	the counter of the merged scope is equivalent to the counter of the existing scope.
		//	This counter would be then used in the future (in the second phase)
		//
		// In the second phase (incremental), the counter of the child of the merged scope is given from 
		//	the counter of the parent of this child. Since here we don't know who is the "real" parent,
		//	we need to pass it from the parameter
		//-----------------------------------------
		switch (status) {
		case INIT:
			scope.setCounter(this.getCounter());
			break;

		case INCREMENTAL:
			scope.setCounter(counter_to_assign);
			break;
		}
		listOfmerged.add(scope);	// include the new scope to merge
	}
	

	/*****
	 * Mark that this scope has a child. The number of children is still unknown though
	 * 	and has to computed dynamically
	 */
	public void markScopeHasChildren() {
		this.flag_scope_has_child = true;
	}
	
	/***
	 * check if the scope has a child or not
	 * @return
	 */
	public boolean hasScopeChildren() {
		return this.flag_scope_has_child;
	}

	/*****************
	 * retrieve the child scopes of this node. 
	 * If a node has merged siblings, then we need to reconstruct the children of the merged scopes
	 * @param finalizeVisitor: visitor traversal for finalization phase
	 * @param percentVisitor: visitor traversal to compute the percentage
	 * @param inclusiveOnly: filter for inclusive metrics
	 * @param exclusiveOnly: filter for exclusive metrics 
	 */
	@Override
	public Object[] getAllChildren(
			MetricValuePropagationFilter inclusiveOnly, 
			MetricValuePropagationFilter exclusiveOnly ) {

		Object children[] = this.getChildren();

		if (children != null && children.length>0) {
			
			//-------------------------------------------------------------------------
			// this scope has already computed children, we do nothing, just return them
			//-------------------------------------------------------------------------
			return this.getChildren();
			
		} else {
			
			//-------------------------------------------------------------------------
			// construct my own child
			//-------------------------------------------------------------------------
			
			LinkedList<CallSiteScopeCallerView> listOfChain = CallerScopeBuilder.createCallChain
				(root, scopeCCT, scopeCost, combine_without_cond, inclusiveOnly, exclusiveOnly);

			if (!listOfChain.isEmpty())
			{
				CallSiteScopeCallerView first = listOfChain.removeFirst();
				CallersViewScopeVisitor.addNewPathIntoTree(this, first, listOfChain);
			}
		}
		
		//----------------------------------------------------------------
		// get the list of children from the merged siblings
		//----------------------------------------------------------------

		if (this.listOfmerged != null) {
			for(Iterator<CallSiteScopeCallerView> iter = this.listOfmerged.iterator(); iter.hasNext(); ) {
				
				CallSiteScopeCallerView scope = iter.next();

				try {
					Scope scope_cct = scope.scopeCCT;

					//-------------------------------------------------------------------------
					// construct the child of this merged scope
					//-------------------------------------------------------------------------
					LinkedList<CallSiteScopeCallerView> listOfChain = CallersViewScopeVisitor.createCallChain
						(root, scope_cct, scope, combine_without_cond, inclusiveOnly, exclusiveOnly);
					
					//-------------------------------------------------------------------------
					// For recursive function where the counter is more than 1, the counter to 
					//	assign to the child scope is the counter of the scope minus 1
					// For normal function it has to be zero
					//-------------------------------------------------------------------------
					int counter_to_assign = scope.getCounter() - 1;
					if (counter_to_assign<0)
						counter_to_assign = 0;
					
					//-------------------------------------------------------------------------
					// merge (if possible) the path of this new created merged scope
					//-------------------------------------------------------------------------
					CallersViewScopeVisitor.mergeCallerPath(IMergedScope.MergingStatus.INCREMENTAL, counter_to_assign,
							this, listOfChain, combine_with_dupl, inclusiveOnly, exclusiveOnly);

				} catch (java.lang.ClassCastException e) {
					
					//-------------------------------------------------------------------------
					// theoretically it is impossible to merge two main procedures 
					// however thanks to "partial call path", the CCT can have two main procedures !
					//-------------------------------------------------------------------------

					System.err.println("Warning: dynamically merging procedure scope: " + scope.scopeCCT +
							" ["+scope.scopeCCT.flat_node_index+"]");
				}
			}
		}
		return this.getChildren();
	}
	

	public int getNumMergedScopes() {
		if (listOfmerged == null) return 0;
		return listOfmerged.size();
	}

	
	/************************
	 * combination class to combine two metrics
	 * This class is specifically designed for combining merged nodes in incremental caller view
	 *************************/
	static private class IncrementalCombineMetricUsingCopy extends AbstractCombineMetric {

		/*
		 * (non-Javadoc)
		 * @see edu.rice.cs.hpc.data.experiment.metric.AbstractCombineMetric#combine(edu.rice.cs.hpc.data.experiment.scope.Scope, edu.rice.cs.hpc.data.experiment.scope.Scope, edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter, edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter)
		 */
		public void combine(Scope target, Scope source,
				MetricValuePropagationFilter inclusiveOnly,
				MetricValuePropagationFilter exclusiveOnly) {

			
			if (target instanceof CallSiteScopeCallerView) {

				//-----------------------------------------------------------
				// only combine the outermost "node" of incremental callsite
				//-----------------------------------------------------------
				if (inclusiveOnly != null && source.isCounterZero()) {
					target.safeCombine(source, inclusiveOnly);
				} 
										
				if (exclusiveOnly != null)
					target.combine(source, exclusiveOnly);
				
			} else {
				System.err.println("ERROR-ICMUC: the target combine is incorrect: " + target + " -> " + target.getClass() );
			}
			
		}
	}


	/************************
	 * combination class specific for the creation of incremental call site
	 * in this phase, we need to store the information of counter from the source
	 ************************/
	static private class CombineMetricUsingCopyNoCondition extends AbstractCombineMetric {

		/*
		 * (non-Javadoc)
		 * @see edu.rice.cs.hpc.data.experiment.metric.AbstractCombineMetric#combine(edu.rice.cs.hpc.data.experiment.scope.Scope, 
		 * edu.rice.cs.hpc.data.experiment.scope.Scope, edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter, 
		 * edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter)
		 */
		public void combine(Scope target, Scope source,
				MetricValuePropagationFilter inclusiveOnly,
				MetricValuePropagationFilter exclusiveOnly) {

			if (target instanceof CallSiteScopeCallerView) {
				
				if (inclusiveOnly != null) {
					target.safeCombine(source, inclusiveOnly);
				}
				if (exclusiveOnly != null)
					target.combine(source, exclusiveOnly);
				
				target.setCounter(source.getCounter());
				
			} else {
				System.err.println("ERROR-CMUCNC: the target combine is incorrect: " + target + " -> " + target.getClass() );
			}
			
		}
	}

}
