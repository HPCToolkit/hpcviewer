/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import com.graphbuilder.math.Expression;
import com.graphbuilder.math.ExpressionTree;
import com.graphbuilder.math.FuncMap;
import com.graphbuilder.math.VarMap;
import com.graphbuilder.math.func.Function;

import edu.rice.cs.hpc.data.experiment.scope.IMetricScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;


/**
 * @author la5
 *
 */
public class MetricVarMap extends VarMap {

	private IMetricManager 	metricManager;
	private IMetricScope 	scope;
	private IMetricScope 	root;
	private BaseMetric 		metric = null;

	public MetricVarMap() {
		this(null, null, null);
	}
	
	public MetricVarMap(RootScope root, IMetricManager metricManager) {
		this(root, null, metricManager);
	}
	
	public MetricVarMap(RootScope root, IMetricScope s, IMetricManager metricManager) {
		super(false);
		this.scope = s;
		this.root  = root;
		this.metricManager = metricManager;
	}
	

	//===========================
	

	public void setMetricManager(IMetricManager metricManager) {
		this.metricManager = metricManager;
	}
	
	public void setMetric(BaseMetric metric)
	{
		this.metric = metric;
	}
	
	/**
	 * set the current scope which contains metric values
	 * @param s: the scope of node
	 */
	public void setScope(IMetricScope s) {
		this.scope = s;
	}
	
	public void setRootScope(RootScope root)
	{
		this.root = root;
	}
	
	/**
	 * Overloaded method: a callback to retrieve the value of a variable (or a metric)
	 * If the variable is a normal variable, it will call the parent method.		
	 */
	public double getValue(String varName) {
		assert(metricManager != null);
		
		char firstLetter = varName.charAt(0);
		if (firstLetter == '$' || firstLetter == '@') 
		{
			//---------------------------------------------------------
			// get the value of the scope for this metric
			//---------------------------------------------------------

			// Metric variable
			String sIndex = varName.substring(1);
			BaseMetric metricToQuery = metricManager.getMetric(sIndex);
			if (metricToQuery == null) 
				throw new RuntimeException("metric ID unknown: " + sIndex);
			
			//---------------------------------------------------------
			// 2011.02.08: new interpretation of the symbol "@x" where x is the metric ID
			// @x returns the aggregate value of metric x 
			//---------------------------------------------------------
			final IMetricScope currentScope = (firstLetter == '@' ? root : scope);

			if (currentScope != null) {
				MetricValue value = MetricValue.NONE;
				if (this.metric != null && this.metric == metricToQuery) {
					// avoid recursive call: if the metric queries its own value, we returns
					// the "raw" value 
					value = metricToQuery.getRawValue(currentScope);
				} else {
					value = metricToQuery.getValue(currentScope);
				}
				if(MetricValue.isAvailable(value))
					return value.getValue();
			}
		} else if (firstLetter == '#') {
			String sIndex = varName.substring(1);
			Integer index = Integer.valueOf(sIndex);
			BaseMetric bm = metricManager.getMetricFromOrder(index);
			
			if (bm != null) {
				
				final IMetricScope currentScope = (firstLetter == '@' ? root : scope);
				MetricValue value = MetricValue.NONE;
				
				if (metric != null && metric.getMetricType() == MetricType.EXCLUSIVE) {
					// for exclusive metric, we have to compute the exclusive metric of the source
					// not the inclusive one

					String sPartner = String.valueOf(bm.getPartner());
					BaseMetric pm   = metricManager.getMetric(sPartner);
					
					if (pm != null)
						value = pm.getValue(currentScope);
				} else {
					value = bm.getValue(currentScope);
				}
				
				if (value != MetricValue.NONE)
					return bm.getValue(currentScope).value;
			}
		} else
			//---------------------------------------------------------
			// get directly the value of the variable
			//---------------------------------------------------------
			return super.getValue(varName);

		return 0.0d;
	}
	
	
	/**
	 * Unit test for MetricVarMap
	 * @param args
	 */
	public static void main(String[] args) {
		String s = "@1*r^2";
		Expression x = ExpressionTree.parse(s);

		MetricVarMap vm = new MetricVarMap();
		vm.setValue("r", 5);

		FuncMap fm = new FuncMap(); // no functions in expression
		fm.loadDefaultFunctions();
		System.out.println(x); 
		System.out.println(x.eval(vm, fm)); 

		vm.setValue("r", 10);
		System.out.println(x.eval(vm, fm)); 
		Function []fs = fm.getFunctions();
		for( int i=0; i<fs.length ; i++) {
			System.out.println("\t<tr><td>" + " <code> " + fs[i].toString() + " </code> </td> <td></td> </tr>");
		}
	}

}
