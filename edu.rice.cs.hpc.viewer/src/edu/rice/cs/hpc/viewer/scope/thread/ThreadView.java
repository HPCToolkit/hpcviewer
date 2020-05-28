package edu.rice.cs.hpc.viewer.scope.thread;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbenchWindow;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.metric.MetricRaw;
import edu.rice.cs.hpc.data.experiment.metric.MetricType;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.metric.MetricRawManager;
import edu.rice.cs.hpc.viewer.graph.GraphMenu;
import edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.ScopeSelectionAdapter;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;
import edu.rice.cs.hpc.viewer.scope.ScopeViewActions;
import edu.rice.cs.hpc.viewer.scope.StyledScopeLabelProvider;
import edu.rice.cs.hpc.viewer.window.Database;

/******************************************************************************************
 * 
 * View part for showing metric database of a certain thread
 *
 ******************************************************************************************/
public class ThreadView extends AbstractBaseScopeView 
{
	static final public String ID = "edu.rice.cs.hpc.viewer.scope.thread.ThreadView";
	static final private int MAX_THREAD_INDEX = 2;
	
	/** the metric manager of the view. DO NOT access this variable directly.
	 *  Instead, we need to query to getMetricManager() */
	private IMetricManager metricManager = null;
	
	private ThreadContentProvider contentProvider = null;
	

	@Override
	public void setInput(Database db, RootScope scope, boolean keepColumnStatus)
	{		
	}
	
	/*****
	 * Customized setInput from {@link edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView}
	 * using list of threads as the additional parameter.
	 * @param db : database
	 * @param scope : the root (should be cct root)
	 */
	public void setInput(Database db, RootScope scope, List<Integer> threads)
	{
		if (database != db && myRootScope != scope) {
	    	database = db;
	    	myRootScope = scope;// try to get the aggregate value

	        // tell the action class that we have built the tree
	        objViewActions.setTreeViewer(treeViewer);
	        
			IMetricManager mm = getMetricManager();

	        ((ThreadScopeViewAction)objViewActions).setMetricManager(mm);			
		}
		        
    	/*****
    	 * add new columns of metrics for a given list of threads<br/>
    	 * If the threads already displayed in the table, we do nothing.
    	 * Otherwise, we'll add new columns for these threads.
    	 * 
    	 */

		List<BaseMetric > metrics = getMetricManager().getVisibleMetrics();
		
		// 1. check if the threads already exist in the view
		boolean col_exist = false;
		if (metrics != null) {
			for (BaseMetric metric : metrics) {
				if (metric instanceof MetricRaw) {
					List<Integer> lt = ((MetricRaw)metric).getThread();
					if (lt.size() == threads.size()) {
						for(Integer i : threads) {
							col_exist = lt.contains(i);
							if (!col_exist) {
								break;
							}
						}
					}
				}
				if (col_exist) 
					break;
			}
		}
		
		// 2. if the column of this thread exist, exit.
		if (col_exist)
			return;

		// 3. add the new metrics into the table
		final Experiment experiment = database.getExperiment();
		initTableColumns(threads, experiment.getMetricRaw());

		// 4. update the table content, including the aggregate experiment
		updateDisplay();
	}
	
	@Override
	public void updateDisplay() {
		// return immediately when there's no database or the view is closed (disposed)
        if (database == null || treeViewer == null || treeViewer.getTree().isDisposed())
        	return;

        final Experiment experiment = getExperiment();
        
		// reassign root scope
        if (myRootScope == null) {
    		RootScope rootCCT = experiment.getRootScope(RootScopeType.CallingContextTree);
    		myRootScope = createRoot(rootCCT);
        }

		if (myRootScope.getChildCount()>0) {
        	treeViewer.setInput(myRootScope);
        	
        	objViewActions.updateContent(experiment, myRootScope);
        	objViewActions.finalizeContent(myRootScope);
        	objViewActions.checkNodeButtons();
        }
		
		// pack the columns, either to fit the title of the header, or 
		// the item in the column
		
		final TreeColumn []columns = treeViewer.getTree().getColumns();
		
		for (final TreeColumn col : columns) {
			if (col.getData() != null) {
				col.pack();
			}
		}
	}
	
	
	@Override
	protected void initTableColumns(boolean keepColumnStatus) {	}

	@Override
	protected ScopeViewActions createActions(Composite parent, CoolBar coolbar) {
    	IWorkbenchWindow window = this.getSite().getWorkbenchWindow();
    	
        return new ThreadScopeViewAction(this, window, parent, coolbar, getMetricManager()) ;
	}

	@Override
	protected void mouseDownEvent(Event event) {
	}

	@Override
	protected void createAdditionalContextMenu(IMenuManager mgr, Scope scope) {
		GraphMenu.createAdditionalContextMenu(getViewSite().getWorkbenchWindow(), mgr, database, scope);
	}

	@Override
	protected AbstractContentProvider getScopeContentProvider() {
		if (contentProvider == null) {
			contentProvider = new ThreadContentProvider(getTreeViewer());
		}
		return contentProvider;
	}

	@Override
	protected void enableFilter(boolean isEnabled) {
    	if (treeViewer.getTree().isDisposed())
    		return;
    	
    	Experiment experiment = getExperiment();
		
		// reassign root scope
		myRootScope = experiment.getRootScope(RootScopeType.CallingContextTree);
		// update the content of the view
		updateDisplay();
	}
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
    	if(element instanceof Scope)
            return ((Scope) element).hasChildren(); // !((Scope.Node) element).isLeaf();
    	else
    		return false;
    }


	@Override
	protected CellLabelProvider getLabelProvider() {
		return new StyledScopeLabelProvider( getSite().getWorkbenchWindow() ); 
	}

	/****
	 * customized table initialization
	 * @param threads : list of threads
	 * @throws IOException 
	 */
	private void initTableColumns(List<Integer> threads, BaseMetric []mr)  {
		if (mr == null)
		{
			objViewActions.showErrorMessage("The database has no thread-level metrics.");
			objViewActions.disableButtons();
		}
		else {
			IThreadDataCollection threadData = database.getThreadDataCollection();
			String[] labels;
			
			if (treeViewer.getTree().getColumnCount() == 0) {
		        TreeViewerColumn colTree = createScopeColumn(treeViewer);
		        colTree.getColumn().setWidth(ScopeTreeViewer.COLUMN_DEFAULT_WIDTH);
		        
				ScopeSelectionAdapter selectionAdapter = new ScopeSelectionAdapter(treeViewer, colTree);
				colTree.getColumn().addSelectionListener(selectionAdapter);
				
				contentProvider.sort_column(colTree, ScopeSelectionAdapter.SORT_ASCENDING);
			}

			try {
				labels = threadData.getRankStringLabels();
				
				// duplicate "raw metrics" before setting them into the column. The reason for duplication is: 
				// Although metric A in column X is the same as metric A in column Y (they are both metric A),
				// but column X is for displaying the values for threads P while column Y is for displaying
				// for threads Q. 
				boolean sort = getMetricManager().getMetricCount() == 0;
				HashMap<Integer, BaseMetric> listOfDuplicates = new HashMap<Integer, BaseMetric>(mr.length);
				
				for(int j=0; j<mr.length; j++)
				{
					MetricRaw mdup = (MetricRaw) mr[j].duplicate();
					mdup.setThread(threads);
					
					StringBuffer buffer = new StringBuffer();
					buffer.append('[');
					int size = threads.size();
					
					// for the column title: only list the first MAX_THREAD_INDEX of the set of threads
					for(int i=0; i<size && i<=MAX_THREAD_INDEX; i++) {
						final int index;
						if (i<MAX_THREAD_INDEX) {
							index = threads.get(i);
						} else {
							// show the last thread index
							if (size > MAX_THREAD_INDEX+1)
								buffer.append("..");
							index = threads.get(size-1);
						}
						buffer.append(labels[index]);
						if (i < MAX_THREAD_INDEX && i<size-1)
							buffer.append(',');
					}
					buffer.append("]-");
					buffer.append(mdup.getDisplayName());

					mdup.setDisplayName(buffer.toString());
					final String metricID = String.valueOf(treeViewer.getTree().getColumnCount());
					mdup.setShortName(metricID);
					listOfDuplicates.put(mr[j].getIndex(), mdup);
					
/*					if (mdup.getMetricType() == MetricType.INCLUSIVE) {
						MetricValue value = mdup.getValue(myRootScope);
						if (value == MetricValue.NONE || value.getValue() == 0.0)
							continue;
					}
*/					
					treeViewer.addTreeColumn(mdup, sort);
					
					// sort initially the first column metric
					sort = false;
				}
				
				Iterator<Entry<Integer, BaseMetric>> iterator = listOfDuplicates.entrySet().iterator();
				while(iterator.hasNext()) {
					Entry<Integer, BaseMetric> entry = iterator.next();
					BaseMetric metric = entry.getValue();
					int partner 	  = metric.getPartner();
					if (partner >= 0) {
						BaseMetric metricPartner = listOfDuplicates.get(partner);
						((MetricRaw)metric).setMetricPartner((MetricRaw) metricPartner);
					}
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/***
	 * copy CCT root and duplicate its children to create a new 
	 * root scope for this thread view.
	 * 
	 * @param rootCCT : root CCT
	 * 
	 * @return RootScope
	 */
	private RootScope createRoot(RootScope rootCCT)
	{	
		// create and duplicate the configuration
		RootScope rootThread = (RootScope) rootCCT.duplicate();
		rootThread.setRootName("Thread View");
		
		// duplicate the children
		for(int i=0; i<rootCCT.getChildCount(); i++)
		{
			Scope scope = (Scope) rootCCT.getChildAt(i);
			rootThread.addSubscope(scope);
		}
		return rootThread;
	}
	
	
	private IMetricManager getMetricManager() 
	{
		if (metricManager != null)
			return metricManager;
		
		// create a new metric manager for this view
		metricManager = new MetricRawManager(treeViewer);
		return metricManager;
	}
	
	/************************************
	 * 
	 * Specific content provider for thread view
	 *
	 ************************************/
	static class ThreadContentProvider extends AbstractContentProvider
	{
	    public ThreadContentProvider(ScopeTreeViewer viewer) {
			super(viewer);
		}

		/*
	     * (non-Javadoc)
	     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	     */
	    public boolean hasChildren(Object element) {
	    	if(element instanceof Scope)
	            return ((Scope) element).hasChildren(); // !((Scope.Node) element).isLeaf();
	    	else
	    		return false;
	    }
	    
		@Override
		public Object[] getChildren(Object node) {
			if (node instanceof Scope) {
				return ((Scope)node).getChildren();
			}
			return null;
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
			
		}
	}
}
