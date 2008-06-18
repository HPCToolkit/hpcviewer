/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.viewer.metric.MetricVarMap;

//math expression
import com.graphbuilder.math.*;

/**
 * @author la5
 *
 */
public class ExtDerivedMetric extends Metric {
	//===================================================================================
	// DATA
	//===================================================================================
	
	// a counter to know the number of derived metrics
	static public int Counter = 0;
	// formula expression
	private Expression expression;
	// the total aggregate value
	private double dRootValue = 0.0;
	private MetricValue objAggregateValue = new MetricValue(0.0);
	// the value of the derived metric: null if not computed
	//private double dValue;
	// map function
	private FuncMap fctMap;
	// map variable 
	private MetricVarMap varMap;

	//===================================================================================
	// CONSTRUCTORS
	//===================================================================================
	
	/**
	 * @param experiment
	 * @param shortName
	 * @param nativeName
	 * @param displayName
	 * @param displayed
	 * @param percent
	 * @param sampleperiod
	 * @param metricType
	 * @param partnerIndex
	 */
	public ExtDerivedMetric(Experiment experiment, String shortName,
			String nativeName, String displayName, boolean displayed,
			boolean percent, String sampleperiod, MetricType metricType,
			int partnerIndex) {
		super(experiment, shortName, nativeName, displayName, displayed,
				percent, sampleperiod, metricType, partnerIndex);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 * @param scopeRoot
	 * @param e
	 * @param sName
	 * @param index
	 * @param bPercent
	 */
	public ExtDerivedMetric(RootScope scopeRoot, Expression e, String sName, int index, boolean bPercent) {
		super(scopeRoot.getExperiment(), "EDM"+ExtDerivedMetric.Counter, "ExtDerivedMetric"+ExtDerivedMetric.Counter,
				sName, true, bPercent, ".",MetricType.DERIVED, 0);
		ExtDerivedMetric.Counter++;
		this.setIndex(index);
		this.expression = e;
		this.fctMap = new FuncMap();
		this.fctMap.loadDefaultFunctions(); // initialize it with the default functions
		this.varMap = new MetricVarMap(scopeRoot.getExperiment().getMetrics());
		// compute the aggregate value if necessary
		if(bPercent) {
			MetricValue objValue = this.computeAggregate(scopeRoot, index);//accumulateMetricsFromKids(scopeRoot, scopeRoot, index);
			this.dRootValue = (!objValue.isAvailable()?0.0:objValue.getValue());
			this.objAggregateValue = new MetricValue(this.dRootValue);
		}
	}

	//===================================================================================
	// AGGREGATE VALUE
	//===================================================================================
	
	/**
	 * Computing the aggregate value of a metric
	 */
	private MetricValue computeAggregate(Scope scopeAggregate, int index) {
		MetricValue objValue = new MetricValue();
		int nbKids = scopeAggregate.getSubscopeCount();
		double dTotal = 0.0;
		for(int i=0;i<nbKids;i++) {
			Scope kid = scopeAggregate.getSubscope(i);
			MetricValue objKidValue = this.accumulateMetricsFromKids(kid, index);
			if(objKidValue.isAvailable()) {
				dTotal = dTotal + objKidValue.getValue();
			}
		}
		if(dTotal != 0.0) {
			objValue = new MetricValue(dTotal);
		} //else
			//objValue = MetricValue.NONE;
		return objValue;
	}
	/**
	 * Computing the aggregate values of the children and save it to the original "parent" 
	 * (which is the root scope)
	 * @param parent: the root scope
	 * @param current: current scope
	 * @param index: matrix index
	 * @return the value, MetricValue.NONE if there is no value
	 */
	private MetricValue accumulateMetricsFromKids(Scope current, int index) {
		int nkids = current.getSubscopeCount();
		MetricValue objCurrentValue = this.getValue(current);//current.getDerivedMetricValue(this, index);
		double dTotal = 0.0;
		// ATT: we accumulate only the leaves, and DO NOT add the current value into the total value
		if(objCurrentValue.isAvailable() && (nkids==0)) {
			//dTotal = objCurrentValue.getValue();
			return objCurrentValue;
		}
		for (int i = 0; i < nkids; i++) {
			Scope child = current.getSubscope(i);
			// compute the accumulated value of the children of the child
			MetricValue objTotalChildrenValue = accumulateMetricsFromKids(child, index);
			// compute the total
			if(objTotalChildrenValue.isAvailable()) {
				dTotal = dTotal + objTotalChildrenValue.getValue();// + objChildValue.getValue();
			}
		}
		// we have computed all the kids. If the total is zero, then return none
		// otherwise return the total
		MetricValue objTotalValue;
		if(dTotal != 0.0) {
			objTotalValue = new MetricValue(dTotal);
		} else
			objTotalValue = MetricValue.NONE;
		return objTotalValue;
	}

	public MetricValue getAggregateValue() {
		return this.objAggregateValue;
	}
	
	/**
	 * Computing the value of the derived metric
	 * @param scope: the current scope
	 * @return the object Double if there is a value, null otherwise
	 */
	public Double computeValue(Scope scope) {
		Double objResult = null;
		this.varMap.setScope(scope);
		try {
			double dValue = this.expression.eval(this.varMap, this.fctMap);
			objResult = new Double(dValue);
		} catch(java.lang.Exception e) {
			// should throw an exception
		}
		return objResult;
	}
	
	/**
	 * Overloading method to compute the value of the derived metric
	 */
	public MetricValue getValue(Scope scope) {
		Double dVal = this.computeValue(scope);
		if(dVal == null)
			return MetricValue.NONE;	// the value is not available !
		return new MetricValue(dVal);
	}
	/**
	 * Retrieve the text value of the scope
	 * Notes: we put here instead of Scope class to avoid too much instantiation done in Scope class
	 * @param scope
	 * @return
	 */
	public String getTextValue(Scope scope) {
		MetricValue mv = this.getValue(scope); //scope.getDerivedMetricValue(this, this.index);
		if(!mv.isAvailable()) 
			return null;
		double dVal; // = mv.getValue();
		if(scope instanceof RootScope) {
			dVal = this.dRootValue;
		} else {
			dVal = mv.getValue();
		}
		if(dVal == 0.0)
			return null;
		if(this.percent && this.dRootValue != 0.0) {
			mv = new MetricValue(dVal, (double)dVal/this.dRootValue);
		//} else {
		//	mv = new MetricValue(dVal);
		}
		return this.getDisplayFormat().format(mv);
	}
	
	/**
	 * Compare two derived values from two different scopes
	 * @param scope1
	 * @param scope2
	 * @return zero if the values are identical, <0 if the first is less, >0 otherwise
	 */
	public int compare(Scope scope1, Scope scope2) {
		int iResult = 0;
		MetricValue mv1 = this.getValue(scope1); 	//scope1.getDerivedMetricValue(this);
		MetricValue mv2 = this.getValue(scope2);	//scope2.getDerivedMetricValue(this);
		if(mv1.isAvailable() || mv2.isAvailable()) {
			if(!mv1.isAvailable()) {
				// only mv2 is available
				return 1;
			}
			if(!mv2.isAvailable()) {
				// only mv1 is available
				return -1;
			}
			// compare both values
			double d1 = mv1.getValue();
			double d2 = mv2.getValue();
			// attention: we treat 0.0 as value none !
			if(d1 == d2)
				return 0;
			// if one the value is zero, the other has higher priority, regardless the value
			if(d1 == 0.0 && d2 != 0.0)
				return 1;
			if(d2 == 0.0 && d1 != 0.0)
				return -1;
			// simple comparison.
			return(int) (d2-d1);
			/*if(d2>d1)
				return 1;
			if(d1>d2)
				return -1;
			return 0; */
		}
		return iResult;
	}
}

