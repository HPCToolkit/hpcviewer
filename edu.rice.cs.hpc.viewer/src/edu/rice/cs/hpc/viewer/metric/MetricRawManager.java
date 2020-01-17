package edu.rice.cs.hpc.viewer.metric;

import java.util.ArrayList;

import org.eclipse.swt.widgets.TreeColumn;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;

/********************************************************
 * 
 * Class to handle metric raw (metric-db from hpcprof) 
 * including adding it with derived metrics
 *
 ********************************************************/
public class MetricRawManager implements IMetricManager 
{
	final private ScopeTreeViewer treeViewer;
	
	public MetricRawManager(ScopeTreeViewer treeViewer) {
		this.treeViewer = treeViewer;
	}
	
	@Override
	public BaseMetric getMetric(String ID) {
		BaseMetric []metrics = getMetrics();
 		for (BaseMetric m : metrics ) {
			if (m.getShortName().equals(ID))
				return m;
		}
		return null;
	}

	@Override
	public BaseMetric getMetric(int index) {
		BaseMetric []metrics = getMetrics();
		if (metrics != null) {
			if (index >= 0 && index < metrics.length)
				return metrics[index];
		}
		return null;
	}

	@Override
	public int getMetricCount() {
		BaseMetric[] metrics = getMetrics();
		if (metrics != null)
			return metrics.length;
		return 0;
	}

	@Override
	public BaseMetric[] getMetrics() {
		TreeColumn []columns = treeViewer.getTree().getColumns();
		if (columns != null && columns.length > 1) {
			ArrayList<BaseMetric> listMetrics = new ArrayList<BaseMetric>(columns.length);
			for(TreeColumn col : columns) {
				Object obj = col.getData();
				if (obj instanceof BaseMetric) {
					listMetrics.add((BaseMetric) obj);
				}
			}
			BaseMetric []metrics = new BaseMetric[listMetrics.size()];
			return listMetrics.toArray(metrics);
		}
		return null;
	}

	@Override
	public void addDerivedMetric(DerivedMetric objMetric) {
	}

	@Override
	public BaseMetric getMetricFromOrder(int order) {

		return getMetric(order);
	}
}
