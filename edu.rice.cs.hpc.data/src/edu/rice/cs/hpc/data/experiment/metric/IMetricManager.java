package edu.rice.cs.hpc.data.experiment.metric;

public interface IMetricManager 
{
	public BaseMetric   getMetric(String ID);
	public BaseMetric 	getMetric(int index);
	
	public BaseMetric   getMetricFromOrder(int order);
	
	public int 		    getMetricCount();
	public BaseMetric[] getMetrics();
	public void addDerivedMetric(DerivedMetric objMetric);
}
