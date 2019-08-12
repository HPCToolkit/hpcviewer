package edu.rice.cs.hpc.viewer.scope.thread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.swtchart.IAxisSet;
import org.swtchart.ILineSeries;
import org.swtchart.ISeriesSet;
import org.swtchart.LineStyle;
import org.swtchart.ISeries.SeriesType;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricRaw;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.viewer.editor.IViewerEditor;
import edu.rice.cs.hpc.viewer.graph.GraphChart;
import edu.rice.cs.hpc.viewer.graph.IChartSelectionListener;
import edu.rice.cs.hpc.viewer.graph.UserSelectionData;
import edu.rice.cs.hpc.viewer.util.WindowTitle;
import edu.rice.cs.hpc.viewer.window.Database;

/**************************************************************************************************
 * 
 * Class to display the relationship between all ranks and cct nodes
 *
 **************************************************************************************************/
public class ThreadEditor extends EditorPart implements IViewerEditor
{
	static final public String ID = "edu.rice.cs.hpc.viewer.scope.thread.ThreadEditor";
	static final private double VALUE_INVALID = Double.NaN;
	private Database database;
	private MetricRaw metric;
	
	private GraphChart chart;
	
	@Override
	public void doSave(IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		this.setSite(site);
		this.setInput(input);
		
		if (input instanceof ThreadEditorInput) {
			database = ((ThreadEditorInput)input).getDatabase();
			metric	   = ((ThreadEditorInput)input).getMetric();
			if (metric == null) {
				Experiment experiment = database.getExperiment();
				BaseMetric []metrics  = experiment.getMetricRaw();
				if (metrics != null) {
					metric = (MetricRaw) metrics[0];
				}
			}
		} else {
			throw new PartInitException("Input is not a known class: " + input.getClass());
		}
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {

		//----------------------------------------------
		// chart creation
		//----------------------------------------------
		chart = new GraphChart(parent, SWT.NONE);

		// turn off the legend
		chart.getLegend().setVisible(false);
		try {
			setMenu(chart);
			createPlot(chart, database);
		} catch (IOException e) {
			chart.getTitle().setText(e.getMessage());
			e.printStackTrace();
		}	
	}
	
	private void setMenu(final GraphChart chart) throws IOException
	{
		final IWorkbenchWindow window = getSite().getWorkbenchWindow();

		final MenuManager menuManager = new MenuManager("Show-view");
		chart.setChartSelectionListener(new IChartSelectionListener() {
			
			@Override
			public void selection(UserSelectionData data) {
				menuManager.removeAll();
				menuManager.createContextMenu(chart);

				// set the thread to be viewed in the thread view
				String id 	   = data.serie.getId();
				final int rank = Integer.valueOf(id);
				final ArrayList<Integer> threads = new ArrayList<Integer>(1);
				threads.add(rank);
				
				// show the menu with the real thread label
				final double thread_label = data.valueY;
				
				menuManager.add(new Action("Show thread " + thread_label) {
					public void run() {
						// display the view
						final Experiment experiment = getExperiment();
						final RootScope rootScope   = experiment.getRootScope(RootScopeType.CallingContextTree);
						ThreadViewFactory.build(window, rootScope, threads);
					}
				});
				final Menu menu = menuManager.getMenu();
				// adjust the appearance of the menu, make it closer to the cursor
				// but not to distract the user
				final Point point = chart.toDisplay(new Point(data.event.x+40, data.event.y+10));
				menu.setLocation(point);
				menu.setVisible(true);
			}
		});
	}
	
	/***
	 * Create the plot map using a background thread, and a UI thread
	 * to paint the graph.
	 * 
	 * @param chart : the current chart, it mustn't be null
	 * @param database : the current database, it mustn't be null
	 */
	private void createPlot(GraphChart chart, Database database)
	{
		setPartName(getEditorPartName());

		// set the window title with a possible db number
		WindowTitle wt = new WindowTitle();
		final IWorkbenchWindow window = getEditorSite().getWorkbenchWindow(); 
		wt.setEditorTitle(window, this); 
		
		IThreadDataCollection threadData = database.getThreadDataCollection();
		if (threadData == null)
			return;
		
		// schedule a background job to gather data from the file
		GatheringDataJob job = new GatheringDataJob(getSite().getShell(), database, metric);
		job.setUser(true);
		// paint the chart if the job has terminated
		job.addJobChangeListener(new JobDone(chart, metric));
		
		job.schedule();
		
		// temporary message on the chart
		chart.getTitle().setText("... gathering data.... please wait...");
		chart.getAxisSet().getXAxes()[0].getTitle().setText("");
		chart.getAxisSet().getYAxes()[0].getTitle().setText("");		
	}

	@Override
	public void setFocus() {
	}

	/************************************************
	 * 
	 * Listener class to wait until a job has completed
	 *
	 ************************************************/
	static private class JobDone implements IJobChangeListener
	{
		final private GraphChart chart;
		final private MetricRaw metric;
		
		JobDone(GraphChart chart, MetricRaw metric) {
			this.chart 	= chart;
			this.metric = metric;
		}
		
		@Override
		public void done(IJobChangeEvent event) {
			final GatheringDataJob job = (GatheringDataJob) event.getJob();
			
			final RankValue []rankValues = job.rankValues;

			final ISeriesSet seriesSet = chart.getSeriesSet();
			final Display display 	   = chart.getDisplay();
			
			display.asyncExec(new Runnable() {
				
				@Override
				public void run() {
					if (rankValues == null) {
						chart.getTitle().setText("Fail to read data");
					}
					for(int i=0; i<rankValues.length; i++) {
						// we "encode" the ID of this serie as the same as the index of the rank
						// this index will be used later on to display with the thread view
						ILineSeries scatterSeries = (ILineSeries) seriesSet.
								createSeries(SeriesType.LINE, String.valueOf(i) );
						scatterSeries.setLineStyle(LineStyle.NONE);
						scatterSeries.setSymbolSize(3);
						scatterSeries.setXSeries(job.listCCT);
						scatterSeries.setYSeries(rankValues[i].values);
					}
					
					chart.getTitle().setText(metric.getDisplayName());
					chart.getAxisSet().getXAxes()[0].getTitle().setText("CCT Node ID");
					chart.getAxisSet().getYAxes()[0].getTitle().setText("Rank");		

					// -----------------------------------------------------------------
					// set the values x and y to the plot
					// -----------------------------------------------------------------
					IAxisSet axisSet = chart.getAxisSet();
					axisSet.adjustRange();
/*					IAxis yaxs  = axisSet.getYAxes()[0];
					Range range = yaxs.getRange();
					range.lower = -1.0;
					yaxs.setRange(range);*/
				}
			});
		}
		
		@Override
		public void sleeping(IJobChangeEvent event) {}
		
		@Override
		public void scheduled(IJobChangeEvent event) {}
		
		@Override
		public void running(IJobChangeEvent event) {}
				
		@Override
		public void awake(IJobChangeEvent event) {}
		
		@Override
		public void aboutToRun(IJobChangeEvent event) {}
	}
	
	/************************************************
	 * 
	 * Active class to gathering data from the file
	 *
	 ************************************************/
	static private class GatheringDataJob extends Job
	{
		final private Database  database;
		final private MetricRaw metric;
		final private Shell	    shell;
		private RankValue []	rankValues;
		private double []		listCCT;

		GatheringDataJob(Shell shell, Database database, MetricRaw metric) {
			super("Gathering data");
			this.shell    = shell;
			this.database = database;
			this.metric	  = metric;
		}
		
 		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Experiment experiment = database.getExperiment();

			int numCCTs = experiment.getMaxCCTID()-experiment.getMinCCTID();

			IThreadDataCollection threadData = database.getThreadDataCollection();
			if (threadData == null)
				return null;

			try {
				rankValues = getValues(experiment, threadData, numCCTs, monitor);
			} catch (IOException e) {
				MessageDialog.openError(shell, "Fail to read data", e.getMessage());
				e.printStackTrace();
				return Status.CANCEL_STATUS;
			}
			return Status.OK_STATUS;
		}
		
 		/***
 		 * read the value from the file and return the ranks and its values
 		 * 
 		 * @param experiment
 		 * @param threadData
 		 * @param numCCTs
 		 * @param monitor
 		 * @return
 		 * @throws IOException
 		 */
		private RankValue []getValues(Experiment experiment, IThreadDataCollection threadData,
				int numCCTs, IProgressMonitor monitor) throws IOException {
			
			monitor.beginTask(getName(), numCCTs);
			
			double[] ranks 		   = threadData.getEvenlySparseRankLabels();
			RankValue []rankValues = new RankValue[ranks.length];

			for (int i=0; i<ranks.length; i++) {
				rankValues[i] = new RankValue(ranks[i], numCCTs);
			}
			BaseMetric []metrics = experiment.getMetricRaw();
			int numMetric 		 = metrics.length;
			listCCT 			 = new double[numCCTs];
			int cctMin			 = experiment.getMinCCTID();
			
			for(int i=0; i<numCCTs; i++) {
				listCCT[i] = experiment.getMinCCTID() + i;
				double []vals = threadData.getMetrics(i + cctMin, metric.getRawID(), numMetric);
				for(int j=0; j<vals.length; j++) {
					if (vals[j] > 0.0) {
						rankValues[j].values[i] = rankValues[j].rank;
					}
				}
				monitor.worked(1);
			}
			monitor.done();
			return rankValues;
		}
	}

	/************************************************
	 * 
	 * Data class to store the rank and its metric values
	 *
	 ************************************************/
	static private class RankValue 
	{
		public double[]values;
		final public double rank;
		
		RankValue(double rank, int numCCT) {
			values 	  = new double[numCCT];
			// set default values with invalid value
			Arrays.fill(values, VALUE_INVALID);
			this.rank = rank;
		}
		
		public String toString() {
			return values.toString();
		}
	}
	
	/**
	 * Make 256 rainbow color palette - Jean-Luc Pon's algorithm.
	 */
	public static PaletteData makeRainbow1Palette(int nb){
		RGB rainbow[]=new RGB[256];
		RGB colorVal[]=new RGB[5];
		colorVal[0]=new RGB(200,0,250);
		colorVal[1]=new RGB(40,40,255);
		colorVal[2]=new RGB(40,255,40);
		colorVal[3]=new RGB(250,250,0);
		colorVal[4]=new RGB(255,0,0);
		double colorPos[]=new double[5];
		colorPos[0]=0.0;
		colorPos[1]=0.25;
		colorPos[2]=0.50;
		colorPos[3]=0.75;
		colorPos[4]=1.0;
		int colId;
		colId=0;
		for (int i=0; i < nb; i++) {
			double r1, g1, b1;
			double r2, g2, b2;
			double r=(double)i / (double)nb;
			if (colId < (colorPos.length - 2) && r >= colorPos[colId + 1])     colId++;
			r1=(double)colorVal[colId].red;
			g1=(double)colorVal[colId].green;
			b1=(double)colorVal[colId].blue;
			r2=(double)colorVal[colId + 1].red;
			g2=(double)colorVal[colId + 1].green;
			b2=(double)colorVal[colId + 1].blue;
			double rr=(r - colorPos[colId]) / (colorPos[colId + 1] - colorPos[colId]);
			if (rr < 0.0)     rr=0.0;
			if (rr > 1.0)     rr=1.0;
			rainbow[i]=new RGB((int)(r1 + (r2 - r1) * rr),(int)(g1 + (g2 - g1) * rr),(int)(b1 + (b2 - b1) * rr));
		}
		return new PaletteData(rainbow);
	}

	@Override
	public String getEditorPartName() {
		final String name = database.getExperiment().getName() + ": " + metric.getDisplayName();
		return name;
	}

	@Override
	public void setEditorPartName(String title) {
		setPartName(title);
	}

	@Override
	public Experiment getExperiment() {
		return database.getExperiment();
	}
}
