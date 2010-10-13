/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import com.graphbuilder.math.func.Function;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;

/**
 * @author laksonoadhianto
 *
 */
public class AggregateFunction implements Function {

	private BaseMetric []arrMetrics;
	private RootScope rootscope;
	
	/**
	 * Retrieve the aggregate value of a metric
	 * @param metrics: a list of metrics
	 */
	public AggregateFunction(BaseMetric []metrics, RootScope scope) {
		this.arrMetrics = metrics;
		this.rootscope = scope;
	}

	/* (non-Javadoc)
	 * @see com.graphbuilder.math.func.Function#acceptNumParam(int)
	 */
	public boolean acceptNumParam(int numParam) {
		// TODO Auto-generated method stub
		return (numParam == 1);
	}

	/* (non-Javadoc)
	 * @see com.graphbuilder.math.func.Function#of(double[], int)
	 */
	public double of(double[] param, int numParam) {
		// TODO Auto-generated method stub
		int index = (int) param[0];
		if(index > this.arrMetrics.length || index<0)
			throw new java.lang.ArrayIndexOutOfBoundsException("Aggregate(x): the value of x is out of range.");
		BaseMetric metric = this.arrMetrics[index];
		return metric.getValue(this.rootscope).getValue();
	}

	public String toString() {
		return "aggregate(&x)";
	}

}