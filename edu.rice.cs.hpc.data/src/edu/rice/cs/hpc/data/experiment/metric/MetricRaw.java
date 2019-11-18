package edu.rice.cs.hpc.data.experiment.metric;

import java.io.IOException;
import java.util.List;

import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.scope.IMetricScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/****************************************
 * Raw metric class\n
 * a.k.a thread-level metric
 ****************************************/
public class MetricRaw  extends BaseMetric 
{
	private int ID;			 // the index of this metric as specified in XML
	private String db_glob;  // old format: the glob pattern of the metric-db file
	private int db_id;		 // sequential index of the metric in the XML. Is has to be between 0 to the number of metrics
	private int num_metrics; // number of metrics
	
	private IThreadDataCollection threadData;
	
	/*** list of threads that its metric values have to be computed.<br/> 
	 *   each MetricRaw may have different threads. **/
	private List<Integer> threads = null;
	
	/*** list of scope metric values of a certain threads. The length of the array is the number of cct nodes*/
	private double []thread_values = null;
	
	/*** similar to partner index, but this partner refers directly to the metric partner.**/
	private MetricRaw partner;
	

	/******
	 * creation of a new raw metric
	 * @param id
	 * @param title
	 * @param db_pattern
	 * @param db_num
	 * @param partner_index
	 * @param type
	 * @param metrics
	 */
	public MetricRaw(int id, String title, String db_pattern, int db_num, int partner_index, 
			MetricType type, int metrics) {
		// raw metric has no partner
		// default annotation: percentage, although some metrics may have no percent due to missing value
		// on its root (such as exclusive metric)
		super( String.valueOf(id), title, true, null, AnnotationType.PERCENT, db_num, partner_index, type);
		this.ID 	 = id;
		this.db_glob = db_pattern;
		this.db_id 	 = db_num;
		this.num_metrics = metrics;
	}
	
	public void setThreadData(IThreadDataCollection threadData)
	{
		this.threadData = threadData;
	}
	
	public void setThread(List<Integer> threads)
	{
		this.threads = threads;
	}
	
	public List<Integer> getThread()
	{
		return threads;
	}
	
	/***
	 * return the glob pattern of files of this raw metric
	 * @return
	 */
	public String getGlob() {
		return this.db_glob;
	}
	
	
	/***
	 * retrieve the "local" ID of the raw metric
	 * This ID is unique among raw metrics in the same experiment 
	 * @return
	 */
	public int getRawID() {
		return this.db_id;
	}
	
	
	/***
	 * retrieve the number of raw metrics in this experiment
	 * @return
	 */
	public int getSize() {
		return this.num_metrics;
	}
	
	
	/***
	 * return the ID of the raw metric
	 * The ID is unique for all raw metric across experiments 
	 * @return
	 */
	public int getID() {
		return this.ID;
	}

	/**
	 * set the metric partner of this metric. If this metric is exclusive,
	 * the metric partner should be inclusive.
	 * 
	 * @param partner : the metric partner
	 */
	public void setMetricPartner(MetricRaw partner) {
		this.partner = partner;
	}

	/**
	 * get the metric partner
	 * @return metric partner
	 */
	public MetricRaw getMetricPartner() {
		return partner;
	}
	
	@Override
	public MetricValue getValue(IMetricScope s) {
		MetricValue value = MetricValue.NONE;
		if (threadData != null)
		{
			try {
				if (threads != null)
				{
					value = getValue(s, threads);
					
					// to compute the percentage, we need to have the value of the root
					// If the root has no value, we have to recompute it only for one time
					// Once we have the root's value, we don't have to recompute it
					MetricValue rootValue = MetricValue.NONE;
					
					if (s instanceof RootScope) {
						if (value != MetricValue.NONE)
							rootValue = value;
						else if (metricType != MetricType.EXCLUSIVE)
							rootValue = getValue((RootScope)s, threads);
						else if (partner != null)
							rootValue = partner.getValue((RootScope)s, threads);
					} else {
						RootScope root = ((Scope)s).getRootScope();
						rootValue = getValue(root);//s.getRootMetricValue(this);
					}

					if (rootValue != null && rootValue != MetricValue.NONE) {
						// if the value exist, we compute the percentage
						//setAnnotationType(AnnotationType.PERCENT);
						MetricValue.setAnnotationValue(value, value.getValue() / rootValue.getValue());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return value;
	}


	@Override
	public BaseMetric duplicate() {
		MetricRaw dup = new MetricRaw(ID, displayName, db_glob, db_id, 
				partner_index, metricType, num_metrics);
		// TODO: hack to duplicate also the thread data
		dup.threadData = threadData;
		return dup;
	}
	
	
	/****
	 * Basic method to retrieve the value of a scope for a given set of threads
	 * @param s : scope 
	 * @param threads : a list of threads
	 * @return a metric value
	 * @throws IOException
	 */
	private MetricValue getValue(IMetricScope s, List<Integer> threads) throws IOException  {
		MetricValue value = MetricValue.NONE;
		if (threads != null)
		{
			if (threads.size()>1)
			{
				// by default we return the sum of the the value of the threads
				// we should make this customizable in the future
				// users may like to see the average or the min or the max, ...
				
				value = getSumValue(s, threads);
			} else if (threads.size()==1)
			{
				value = getSpecificValue(s, threads.get(0));
			}
			if (value == MetricValue.NONE && s instanceof RootScope 
					&& metricType == MetricType.EXCLUSIVE) {
				if (partner != null)
					value = partner.getValue(s, threads);
			}
		}
		return value;
	}
	
	public MetricValue getRawValue(IMetricScope s)
	{
		return getValue(s);
	}
	
	/***
	 * compute the sum of the value across the threads
	 * @param s the current scope
	 * @param threads list of threads
	 * @return
	 * @throws IOException
	 */
	private MetricValue getSumValue(IMetricScope s, List<Integer> threads) throws IOException
	{
		double val_sum = 0.0;
		double []values = threadData.getMetrics(((Scope)s).getCCTIndex(), getIndex(), num_metrics);
		for(Integer thread : threads)
		{
			val_sum += (values[thread]);
		}
		MetricValue value = setValue(val_sum); 
		return value;
	}
	

	/****
	 * get the metric value of a give scope on a given thread
	 * 
	 * @param s : the scope
	 * @param thread_id : the thread ID
	 * @return a metric value
	 * @throws IOException
	 */
	private MetricValue getSpecificValue(IMetricScope s, int thread_id) throws IOException
	{
		checkValues(thread_id);
		MetricValue mv = MetricValue.NONE;
		Scope scope = (Scope)s;
		if (thread_values != null) {
			mv = setValue(thread_values[scope.getCCTIndex()-1]);
		} else {
			// there is no API implementation for reading the whold CCT metrics
			// TODO: using the old get metric for the new database
			double []values = threadData.getMetrics(scope.getCCTIndex(), getIndex(), num_metrics);
			double value    = values[thread_id];
			mv  = setValue(value);
		}
		return mv;
	}
	
	
	private MetricValue setValue(double value)
	{
		MetricValue mv = MetricValue.NONE;
		if (Double.compare(value, 0) != 0)
			mv = new MetricValue(value);
		return mv;
	}
	
	private void checkValues(int thread_id) throws IOException
	{
		if (thread_values == null)
			thread_values = threadData.getScopeMetrics(thread_id, ID, num_metrics);
	}
}
