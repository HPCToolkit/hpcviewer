/**
 * 
 */
package edu.rice.cs.hpc.viewer.metric;

import com.graphbuilder.math.VarMap;
import edu.rice.cs.hpc.data.experiment.metric.ExtDerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.Metric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/**
 * @author la5
 *
 */
public class MetricVarMap extends VarMap {

	private Metric metrics[] = new Metric[2];
	private Scope scope;
	/**
	 * 
	 */
	public MetricVarMap() {
		super(false);
	}

	public MetricVarMap(Scope s) {
		super(false);
		this.scope = s;
	}
	
	public MetricVarMap(Metric m[]) {
		super(false);
		this.metrics = m;
	}
	
	public MetricVarMap(Scope s, Metric m[]) {
		super(false);
		this.scope = s;
		this.metrics = m;
	}
	/**
	 * @param caseSensitive
	 */
	public MetricVarMap(boolean caseSensitive) {
		super(caseSensitive);
		// TODO Auto-generated constructor stub
	}

	//===========================
	

	/**
	 * set the value for a metric variable (identified as $x) where x is the metric index
	 * @param iMetricID: the index of the metric
	 * @param metric: pointer to the metric
	 */
	public void setMetrics(Metric []arrMetrics) {
		this.metrics = arrMetrics;
	}

	/**
	 * set the current scope which contains metric values
	 * @param s: the scope of node
	 */
	public void setScope(Scope s) {
		this.scope = s;
	}
	
	/**
	 * Overloaded method: a callback to retrieve the value of a variable (or a metric)
	 * If the variable is a normal variable, it will call the parent method.		
	 */
	public double getValue(String varName) {
		this.hasValidValue = true;
		if(varName.startsWith("$")) {
			// Metric variable
			String sIndex = varName.substring(1);
			try {
				int index = Integer.parseInt(sIndex);
				if(index<this.metrics.length) {
					Metric metric = this.metrics[index];
					// TODO: dirty tricks: separate treatment for derived metric.
					// we should use polymorphism  in the future
					if(metric instanceof ExtDerivedMetric) {
						//return scope.getDerivedMetricValue((ExtDerivedMetric)metric, metric.getIndex()).getValue();
						return ((ExtDerivedMetric)metric).computeValue(scope); 
					} else {
						MetricValue mv  = scope.getMetricValue(metric);
						if(mv.isAvailable())
							return this.scope.getMetricValue(metric).getValue();
						// in this case, the value is invalid or the metric has no value
						// it is important to notify not to include the value into the table
						this.hasValidValue = false;
						throw new RuntimeException(varName);

					}
				} else
					throw new RuntimeException("metric index is not valid: " + varName);
			} catch (java.lang.NumberFormatException e) {
				e.printStackTrace();
				return 0;
			}
		} else
			return super.getValue(varName);
	}
	
	private boolean hasValidValue;
	
	/**
	 * check if the expression, the scope and the metric have a valid value.
	 * To some cases, a metric has no value, and any arithmetric operation for
	 * void value is invalid.
	 * @return true if the value of the expression is valid.
	 */
	public boolean isValueValid() {
		return this.hasValidValue;
	}
}
