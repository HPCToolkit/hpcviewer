package edu.rice.cs.hpc.viewer.graph;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.swtchart.IAxisSet;
import org.swtchart.IAxisTick;
import org.swtchart.Chart;
import org.swtchart.Range;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricRaw;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.editor.IViewerEditor;
import edu.rice.cs.hpc.viewer.scope.thread.ThreadView;
import edu.rice.cs.hpc.viewer.scope.thread.ThreadViewFactory;
import edu.rice.cs.hpc.viewer.util.WindowTitle;
import edu.rice.cs.hpc.viewer.window.Database;


/**************************************************************************************
 * Base class for hpcviewer editor to display graph
 *  <p>
 * The class implements IViewerEditor, so it can be renamed, manipulated and changed
 * 	by the viewer manager</p>
 **************************************************************************************/
public abstract class GraphEditorBase extends EditorPart implements IViewerEditor 
{
	// chart is used to plot graph or histogram on canvas. each editor has its own chart
    private Chart chart;
    protected IThreadDataCollection threadData;
    
    @Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

		this.setSite(site);
		this.setInput(input);
		
		if (input instanceof GraphEditorInput)
		{
			final GraphEditorInput editorInput = (GraphEditorInput) input;
			final Database database 		   = editorInput.getDatabase();
			threadData = database.getThreadDataCollection();
		}
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	/******
	 * Do finalization of the editor
	 * 
	 * Due to SWT Chart bug, we need to adjust the range once the create-part-control
	 * 	finishes its layout.
	 */
	public void editorFinalize() {
		IAxisSet axisSet = this.getChart().getAxisSet();
		axisSet.adjustRange();

		// set the lower range to be zero so that we can see if there is load imbalance or not
		Range range = axisSet.getAxes()[1].getRange();
		if (range.lower > 0) {
			range.lower = 0;
			axisSet.getAxes()[1].setRange(range);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		
		IEditorInput input = this.getEditorInput();
		if (input == null || !(input instanceof GraphEditorInput) )
			throw new RuntimeException("Invalid input for graph editor");
		
		String title = getEditorPartName();
		setEditorPartName( title );

		// set the window title with a possible db number
		WindowTitle wt = new WindowTitle();
		final IWorkbenchWindow window = getEditorSite().getWorkbenchWindow(); 
		wt.setEditorTitle(window, this); //, exp, editorName);

		//----------------------------------------------
		// chart creation
		//----------------------------------------------
		chart = new GraphChart(parent, SWT.NONE);
		chart.getTitle().setText( title );
		final MenuManager menuManager = new MenuManager("Show-view");
		
		((GraphChart)chart).setChartSelectionListener(new IChartSelectionListener() {
			
			@Override
			public void selection(UserSelectionData data) {
				menuManager.removeAll();
				menuManager.createContextMenu(chart);

				try {
					// show the menu with the real thread label
					ArrayList<Integer> threads = new ArrayList<Integer>(1);
					threads.add(data.index);
					
					final ArrayList<Integer> list = translateUserSelection(threads);
					final double []labels = threadData.getRankLabels();
					final double thread_label = labels[list.get(0)];
					
					menuManager.add(new Action("Show thread " + thread_label) {
						public void run() {
							// display the view
							final GraphEditorInput editorInput = (GraphEditorInput) getEditorInput();
							final Scope scope = editorInput.getScope();
							
							RootScope root = null;
							if (scope instanceof RootScope) {
								root = (RootScope) scope;
							} else {
								root = scope.getRootScope();
							}
							ThreadViewFactory.build(window, root, list);
						}
					});
	            	final Menu menu = menuManager.getMenu();
	            	// adjust the appearance of the menu, make it closer to the cursor
	            	// but not to distract the user
					final Point point = chart.toDisplay(new Point(data.event.x+40, data.event.y+10));
					menu.setLocation(point);
	            	menu.setVisible(true);
	            	
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		//----------------------------------------------
		// formatting axis
		//----------------------------------------------
		IAxisSet axisSet = chart.getAxisSet();
		IAxisTick yTick = axisSet.getYAxis(0).getTick();
		yTick.setFormat(new DecimalFormat("0.0##E0##"));

		// turn off the legend
		chart.getLegend().setVisible(false);
		
		//----------------------------------------------
		// plot data
		//----------------------------------------------
		GraphEditorInput editor_input = (GraphEditorInput) input;
		Scope scope = editor_input.getScope();
		BaseMetric metric = editor_input.getMetric();
		
		if (plotData(scope, (MetricRaw) metric) != 0) {
			final String title_error = "*Failed to display the graph*";
			
			setEditorPartName(title_error);
			chart.getTitle().setText(title_error);
		}
	}


	public String getEditorPartName() {
		final GraphEditorInput input = (GraphEditorInput) this.getEditorInput();
		final String name = input.getName();
		return name;
	}

	public void setEditorPartName(String title) {
		this.setPartName(title);
		return;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.editor.IViewerEditor#getExperiment()
	 */
	public Experiment getExperiment() {
		final GraphEditorInput input = (GraphEditorInput) this.getEditorInput();
		return input.getDatabase().getExperiment();
	}

	
	protected Chart getChart() {
		return this.chart;
	}

	/**
	 * method to plot a graph of a specific scope and metric of an experiment
	 * 
	 * @param scope: the scope to plot
	 * @param metric: the raw metric to plot
	 */
	protected abstract int plotData(Scope scope, MetricRaw metric );
	
	/****
	 * Translate a set of thread-index selections into the original set of
	 * thread-index selection.<br/>
	 * It is possible that the child class change the index of x-axis. This
	 * method will then translate from the current selected index to the original
	 * index so that it can be displayed properly by {@link ThreadView}. 
	 *  
	 * @param selections : a set of selected index (usually only one item)
	 * @return the translated set of indexes
	 */
	protected abstract ArrayList<Integer> translateUserSelection(ArrayList<Integer> selections); 

}
