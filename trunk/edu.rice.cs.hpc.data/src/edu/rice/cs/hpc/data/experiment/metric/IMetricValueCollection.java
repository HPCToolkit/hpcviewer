package edu.rice.cs.hpc.data.experiment.metric;

/***********************************************************
 * 
 * The root interface to manage a collection of metric values.
 * The implementation can use either an array or a sparse 
 * array for storing a set of metric values.
 *
 ***********************************************************/
public interface IMetricValueCollection 
{
	/****
	 * get a metric value of a given index
	 * @param index of the metric
	 * @return
	 */
	public MetricValue getValue(int index);
	
	/***
	 * get the annotation of a given metric index
	 * @param index
	 * @return
	 */
	public float getAnnotation(int index);
	
	/****
	 * set a metric value to a certain index
	 * 
	 * @param index
	 * @param value
	 */
	public void setValue(int index, MetricValue value);
	
	/*****
	 * add an additional annotation to the metric value
	 * @param index
	 * @param ann
	 */
	public void setAnnotation(int index, float ann);
	
	/*****
	 * check if a value exist
	 * @param index
	 * @return
	 */
	public boolean isValueAvailable(int index);
	
	/****
	 * check if an annotation exist
	 * @param index
	 * @return
	 */
	public boolean isAnnotationAvailable(int index);
	
	/*****
	 * get the size of metric values
	 * @return
	 */
	public int size();
	
	/*****
	 * dispose the allocated resources
	 */
	public void dispose();
}
