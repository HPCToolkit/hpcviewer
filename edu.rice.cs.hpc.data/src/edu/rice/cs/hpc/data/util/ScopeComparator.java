package edu.rice.cs.hpc.data.util;

import java.util.Comparator;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;


/********
 * 
 * Class to compare two scopes
 * 
 * To use this class, it's required to instantiate first, and then
 * assign the metric and the direction using {@link setMetric} and {@link setDirection}<br/>
 * If the metric is not assigned, it will compare based on the name of the scope.
 * 
 ********/

public class ScopeComparator implements Comparator<Object> 
{
	
	final static public int SORT_DESCENDING = 1;  // from high value to lower value
	final static public int SORT_ASCENDING  = -1; // from low value to higher value

	private BaseMetric metric = null;
	private int direction = 1;
	
	public void setMetric(BaseMetric metric) {
		this.metric = metric;
	}
	
	public void setDirection(int direction) {
		this.direction = direction;
	}

	@Override
	public int compare(Object n1, Object n2) {
		
		Scope node1 = (Scope) n1;
		Scope node2 = (Scope) n2;
		
		// dirty solution: if the column position is 0 then we sort
		// according to its element name
		// otherwise, sort according to the metric
		if(metric == null) {
			return direction * this.doCompare(node1, node2);
		}
		
		MetricValue mv1 = this.metric.getValue(node1); 
		MetricValue mv2 = this.metric.getValue(node2);
		
		int iRet = direction * MetricValue.compareTo(mv2, mv1);
		if(iRet != 0)
			return iRet;

		// if the two values are equal, look at the text of the tree node
		return direction * this.doCompare(node1, node2);
	}


	/**
	 * Compare the names of node 1 and node 2.
	 * However, if both scopes are of type CallSiteScope, compare
	 * the line numbers of the nodes
	 * @param node1
	 * @param node2
	 * @return
	 */
	private int doCompare(Scope node1, Scope node2) {
		if (node1 instanceof CallSiteScope && 
			node2 instanceof CallSiteScope) {
			
			CallSiteScope cs1 = (CallSiteScope) node1;
			CallSiteScope cs2 = (CallSiteScope) node2;
			
			LineScope ls1 = cs1.getLineScope();
			LineScope ls2 = cs2.getLineScope();
			
			int linediff = ls1.getLineNumber() - ls2.getLineNumber();
			
			// if the two nodes have the same line number, we need to compare the name of the nodes
			// sometimes we don't have information of the line number (like non-debug code), and
			// all the line numbers are all zeros
			
			if (ls1 != null && ls2 != null && linediff != 0)
				return linediff;
		} 
		String text1 = node1.getName();
		String text2 = node2.getName();
		return text1.compareTo(text2);
	}
}