package edu.rice.cs.hpc.data.experiment.scope.visitors;

import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.AlienScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.FileScope;
import edu.rice.cs.hpc.data.experiment.scope.GroupScope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.LoadModuleScope;
import edu.rice.cs.hpc.data.experiment.scope.LoopScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.ScopeVisitType;
import edu.rice.cs.hpc.data.experiment.scope.StatementRangeScope;

public class PercentScopeVisitor implements IScopeVisitor {
	RootScope root;
	int nMetrics;

	public PercentScopeVisitor(int metricCount, RootScope r) {
		nMetrics = metricCount;
		root = r;
	}
	//----------------------------------------------------
	// visitor pattern instantiations for each Scope type
	//----------------------------------------------------

	public void visit(Scope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(RootScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(LoadModuleScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(FileScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(ProcedureScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(AlienScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(LoopScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(StatementRangeScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(CallSiteScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(LineScope scope, ScopeVisitType vt) { calc(scope, vt); }
	public void visit(GroupScope scope, ScopeVisitType vt) { calc(scope, vt); }

	//----------------------------------------------------
	// propagate a child's metric values to its parent
	//----------------------------------------------------

	protected void calc(Scope scope, ScopeVisitType vt) {
		if (vt == ScopeVisitType.PostVisit) {
			setPercentValue(scope, root, this.nMetrics);
		}
	}
	
	
	/***
	 * Compute and set the percent of a scope
	 * @param scope: scope in which a percent needs to be counted
	 * @param root: the root scope
	 * @param num_metrics: number of metrics
	 */
	static public void setPercentValue(Scope scope, RootScope root, int num_metrics) {
		for (int i = 0; i < num_metrics; i++) {
			MetricValue m = scope.getMetricValue(i);
			MetricValue root_value = root.getMetricValue(i);
			if (m != MetricValue.NONE && root_value != MetricValue.NONE) {
				double myValue = MetricValue.getValue(m);
				double total = MetricValue.getValue(root_value);
				if (total != 0.0) MetricValue.setPercentValue(m, myValue/total);
			}

		}

	}
}
