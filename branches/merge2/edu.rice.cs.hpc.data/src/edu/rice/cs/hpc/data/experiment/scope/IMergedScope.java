package edu.rice.cs.hpc.data.experiment.scope;

import edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.visitors.AbstractFinalizeMetricVisitor;
import edu.rice.cs.hpc.data.experiment.scope.visitors.PercentScopeVisitor;

public interface IMergedScope {
	public Object[] getAllChildren(AbstractFinalizeMetricVisitor finalizeVisitor,
			PercentScopeVisitor percentVisitor, 
			MetricValuePropagationFilter inclusiveOnly, 
			MetricValuePropagationFilter exclusiveOnly );
	
}
