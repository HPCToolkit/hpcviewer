package edu.rice.cs.hpc.data.experiment.metric;

import java.util.List;

public interface IMetricManager 
{
	public BaseMetric   getMetric(String ID);
	public BaseMetric 	getMetric(int index);
	
	public BaseMetric   getMetricFromOrder(int order);
	
	public int 		    getMetricCount();
	
	/** get all metrics associated with this database*/
	public BaseMetric[] getMetrics();
	
	/** get metrics that are NOT invisible (can be hidden, but not invisible) */
	public List<BaseMetric> getVisibleMetrics();

	public void addDerivedMetric(DerivedMetric objMetric);
}
