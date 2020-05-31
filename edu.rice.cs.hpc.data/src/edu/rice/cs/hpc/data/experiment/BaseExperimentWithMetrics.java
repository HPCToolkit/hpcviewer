package edu.rice.cs.hpc.data.experiment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.metric.MetricType;


/****************************************************************************
 * 
 * abstract base experiment that contains metrics. <br/>
 * This class just load metrics without generating callers view and flat view
 *
 ****************************************************************************/
public abstract class BaseExperimentWithMetrics extends BaseExperiment 
implements IMetricManager
{
	/***** A list of metric descriptor   */
	protected List<BaseMetric> metrics;
	protected ArrayList<BaseMetric> metricsWithOrder;

	//////////////////////////////////////////////////////////////////////////
	//ACCESS TO METRICS													    //
	//////////////////////////////////////////////////////////////////////////

	/*************************************************************************
	 *	Returns the array of metrics in the experiment.
	 ************************************************************************/

	/*****
	 * Set the list of metric descriptors. Ideally this method has to be called
	 * at most once for every creation of experiment
	 * 
	 * @param metricList : a list of metric descriptors
	 */
	public void setMetrics(List<BaseMetric> metricList) {
		metrics = metricList;
		
		metricsWithOrder = new ArrayList<BaseMetric>();
		for(BaseMetric metric:metrics) {
			if (metric.getOrder() >= 0) {
				metricsWithOrder.add(metric);
			}
		}
	}

	/*****
	 * Retrieve the list of metric descriptors
	 * 
	 * @return
	 */
	public BaseMetric[] getMetrics()
	{
		return 	metrics.toArray(new BaseMetric[metrics.size()]);
	}

	
	/*****
	 * Return the list of "visible" metrics. <br/>
	 * Note: visible doesn't mean "SHOW". 
	 * Visible here means that the metrics can be displayed on metric property window
	 * or on list of metrics. 
	 * Perhaps we should change the term to be "displayable" metrics.
	 */
	public List<BaseMetric> getVisibleMetrics() 
	{
		ArrayList<BaseMetric> listMetrics = new ArrayList<>(metrics.size());
		
		for(BaseMetric metric : metrics) {
			switch (metric.getVisibility()) {
			
			case SHOW:
			case HIDE:
				listMetrics.add(metric);
				break;
				
			case SHOW_INCLUSIVE:
				if (metric.getMetricType() == MetricType.INCLUSIVE)
					listMetrics.add(metric);
				break;
				
			case SHOW_EXCLUSIVE:
				if (metric.getMetricType() == MetricType.EXCLUSIVE)
					listMetrics.add(metric);
				break;
			
			default:
				break;
			}
		}
		return listMetrics;
	}

	
	/*************************************************************************
	 *	Returns the number of metrics in the experiment.
	 ************************************************************************/
	public int getMetricCount()
	{
		return this.metrics.size();
	}

	/*************************************************************************
	 *	Returns the metric with a given index.
	 ************************************************************************/
	public BaseMetric getMetric(int index)
	{
		BaseMetric metric;
		// laks 2010.03.03: bug fix when the database contains no metrics
		try {
			metric = this.metrics.get(index);
		} catch (Exception e) {
			// if the metric doesn't exist or the index is out of range, return null
			metric = null;
		}
		return metric;
	}

	/*************************************************************************
	 *	Returns the metric with a given internal name.
	 ************************************************************************/
	public BaseMetric getMetric(String name)
	{
		final int size = metrics.size();

		for (int i=0; i<size; i++) {

			final BaseMetric metric = metrics.get(i);
			if (metric.getShortName().equals(name))
				return metrics.get(i);
		}
		return null;	
	}
	
	@Override
	public BaseMetric getMetricFromOrder(int order)
	{
		if (order >=0 && metricsWithOrder != null && order < metricsWithOrder.size()) {
			return metricsWithOrder.get(order);
		}
		return null;
	}
	
	//////////////////////////////////////////////////////////////////////////
	//Compute Derived Metrics												//
	//////////////////////////////////////////////////////////////////////////

	/**
	 * Create a derived metric based on formula expression
	 * @param objMetric : a new derived metric
	 * @return 
	 */
	@Override
	public void addDerivedMetric(DerivedMetric objMetric) {

		this.metrics.add(objMetric);
	}



	//////////////////////////////////////////////////////////////////////////
	//ACCESS TO SEARCH PATH												    //
	//////////////////////////////////////////////////////////////////////////

	/*************************************************************************
	 *	Returns the number of search paths in the experiment.
	 ************************************************************************/
	public int getSearchPathCount()
	{
		return this.configuration.getSearchPathCount();
	}

	/*************************************************************************
	 *	Returns the search path with a given index.
	 ************************************************************************/

	public File getSearchPath(int index)
	{
		return this.configuration.getSearchPath(index);
	}
}
