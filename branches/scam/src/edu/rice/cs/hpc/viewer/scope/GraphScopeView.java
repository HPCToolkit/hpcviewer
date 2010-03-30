package edu.rice.cs.hpc.viewer.scope;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.extdata.ThreadLevelDataFile;
import edu.rice.cs.hpc.data.experiment.extdata.ThreadLevelDataManager;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.Scope;


/*****************************************************************************************
 * view to display a graph
 * @author laksonoadhianto
 *
 *****************************************************************************************/
public class GraphScopeView extends ViewPart {
    public static final String ID = "edu.rice.cs.hpc.viewer.scope.GraphScopeView";
    
    private ChartComposite chartFrame;
    
	@Override
	public void createPartControl(Composite parent) {
		chartFrame = new ChartComposite(parent, SWT.NONE, null, true, true, false, true, true);
	}

	@Override
	public void setFocus() {
	}

	
	/**
	 * Plot a given metrics for a specific scope
	 * @param exp
	 * @param scope
	 * @param metric
	 * @param num_metrics
	 */
	public void plotData(Experiment exp, Scope scope, BaseMetric metric, int num_metrics) {
		
		ThreadLevelDataManager objDataManager = exp.getThreadLevelDataManager();
		
		// adjust the node index: 1=the root, 2=node-0, 3=node-1, .... 
		int node_index = scope.getCCTIndex() - 2;
		// adjust the metric index: start from the first metric
		int metric_index = GraphScopeView.getNormalizedMetricIndex(metric.getIndex() - exp.getMetric(0).getIndex());
		
		String sTitle = getGraphTitle(scope, metric, metric_index);
		this.setPartName(sTitle);
		
		if (!objDataManager.isDataAvailable()) {
			return;
		}
		
		String series[] = objDataManager.getSeriesName();
		XYSeriesCollection table = new XYSeriesCollection();

		for (int i=0; i<series.length; i++) {
			double y_values[];
			try {
				y_values = objDataManager.getMetrics(series[i],node_index, metric_index);
				ArrayList<String> x_values = objDataManager.getProcessIDs(series[i]);				
				table.addSeries(this.setData(series[i], x_values, y_values));
				
			} catch (IOException e) {
				MessageDialog.openError(this.getSite().getShell(), "Error reading file !", e.getMessage());
				System.err.println(e.getMessage());
				e.printStackTrace();
				return;
			}			
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart(sTitle, "Process.Threads", "Metrics", table,
				PlotOrientation.VERTICAL, series.length>1, false, false); 
		Plot plot = chart.getPlot();
		plot.setBackgroundPaint(java.awt.Color.WHITE);
		plot.setOutlinePaint(java.awt.Color.GRAY);
		chart.setBackgroundPaint(java.awt.Color.WHITE);
		chartFrame.setChart(chart);
	}

	
	/***
	 * grab the title of the graph
	 * @param scope 
	 * @param metric
	 * @param metric_index (normalized 0-based index)
	 * @return
	 */
	static public String getGraphTitle(Scope scope, BaseMetric metric, int metric_index) {
		String sTitle = metric.getDisplayName();
		int pos = sTitle.indexOf('-');
		String sMetricStatus = (metric_index % 2 == 0? " (I)" : " (E)");
		return scope.getShortName() + ": " + sTitle.substring(0, pos) + sMetricStatus;
		
	}

	
	static int getNormalizedMetricIndex(int metric_index) {
		return metric_index >> 3;
	}
	
	
	static int getStandardMetricIndex(int normal_metric_index) {
		return normal_metric_index << 3;
	}
	
	/**
	 * primitive plotting of a set of Xs and Ys
	 * @param title
	 * @param x_values
	 * @param y_values
	 */
	private XYSeries setData(String Series, ArrayList<String> x_values, double y_values[]) {
		XYSeries dataset = new XYSeries(Series, true, false);
		int num_data = x_values.size();
		
		if (num_data>y_values.length)
			num_data = y_values.length;

		for (int i=0; i<num_data; i++) {
			dataset.add(Double.valueOf(x_values.get(i)).doubleValue(), y_values[i]);
		}
		return dataset;
	}

}
