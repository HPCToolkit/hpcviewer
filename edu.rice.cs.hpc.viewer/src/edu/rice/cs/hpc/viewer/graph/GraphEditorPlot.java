package edu.rice.cs.hpc.viewer.graph;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.swtchart.IAxisSet;
import org.swtchart.IAxisTick;

import edu.rice.cs.hpc.data.experiment.metric.MetricRaw;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/*********************************************************************
 * 
 * Class to handle a plotting graph
 *
 *********************************************************************/
public class GraphEditorPlot extends GraphEditor {

    public static final String ID = "edu.rice.cs.hpc.viewer.graph.GraphEditorPlot";
    
	@Override
	protected double[] getValuesX(Scope scope, MetricRaw metric) 
	throws NumberFormatException, IOException {
		
		return threadData.getEvenlySparseRankLabels();
	}

	@Override
	protected double[] getValuesY(Scope scope, MetricRaw metric) throws IOException {
		{
			double []y_values = threadData.getMetrics(scope.getCCTIndex(), metric.getRawID(), metric.getSize());
			return y_values;
		}
	}


	@Override
	protected String getXAxisTitle() {
		IAxisSet axisSet = this.getChart().getAxisSet();
		IAxisTick xTick  = axisSet.getXAxis(0).getTick();
		String title 	 = "Rank";

		xTick.setFormat(new DecimalFormat("##########"));
		
		try {
			title = threadData.getRankTitle();
			
			if (threadData.getParallelismLevel()>1) 
			{
				xTick.setFormat(new DecimalFormat("######00.00##"));
				return title;
			}
		} catch (IOException e) {			
			e.printStackTrace();
		}
		return title;
	}

	@Override
	protected ArrayList<Integer> translateUserSelection(
			ArrayList<Integer> selections) {
		return selections;
	}
}
