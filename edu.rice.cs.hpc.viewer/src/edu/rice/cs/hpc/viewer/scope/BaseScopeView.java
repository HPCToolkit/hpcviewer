package edu.rice.cs.hpc.viewer.scope;

import java.util.Map;

import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.common.ui.Util;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.visitors.FilterScopeVisitor;
import edu.rice.cs.hpc.viewer.provider.TableMetricState;


/**
 * 
 *
 */
abstract public class BaseScopeView  extends AbstractBaseScopeView 
{
	
    //======================================================
    // ................ ATTRIBUTES..........................
    //======================================================

	final private ISourceProviderListener listener;

	
    //======================================================
    // ................ METHODS  ..........................
    //======================================================
	
	public BaseScopeView() 
	{
		super();
		final ISourceProviderService service = (ISourceProviderService)Util.getActiveWindow().
				getService(ISourceProviderService.class);
		final ISourceProvider yourProvider   = service.getSourceProvider(TableMetricState.METRIC_COLUMNS_VISIBLE); 
		
		listener = new ISourceProviderListener() {
			
			@Override
			public void sourceChanged(int sourcePriority, String sourceName, Object sourceValue) {
				
				if (sourceName.equals(TableMetricState.METRIC_COLUMNS_VISIBLE) ||
						sourceName.equals(TableMetricState.METRIC_COLUMN_ADD)) {
					
					if (!(sourceValue instanceof TableMetricState.TableMetricData)) 
						return;
					
					TableMetricState.TableMetricData metricState = (TableMetricState.TableMetricData) sourceValue;
					
					// if hpcviewer opens multiple database, we need to make sure that
					// this view only reacts when a message came from within this database
					
					if (getExperiment() != metricState.getExperiment()) 
						return;
					
					if (sourceName.equals(TableMetricState.METRIC_COLUMNS_VISIBLE))
						objViewActions.setColumnStatus((boolean[])metricState.getValue());
					
					else 
						objViewActions.addMetricColumn(BaseScopeView.this, (DerivedMetric)metricState.getValue());
				}
			}
			
			@Override
			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {}
		};
		
		yourProvider.addSourceProviderListener(listener);
	}
	
    /// ---------------------------------------------
    /// filter feature
    /// ---------------------------------------------
    
    /****
     * enable/disable filter
     * 
     * @param isEnabled
     */
	protected void enableFilter(boolean isEnabled)
    {
    	if (treeViewer.getTree().isDisposed())
    		return;
    	
    	Experiment experiment = getExperiment();
    	if (experiment == null || myRootScope == null)
    		return;
    	
		RootScopeType rootType = myRootScope.getType();
		
		// reassign root scope
		myRootScope = experiment.getRootScope(rootType);
		
		// update the content of the view
		refreshTree(myRootScope);
		
        // ------------------------------------------------------------
    	// check the status of filter. 
        // if the filter may incur misleading information, we should warn users
        // ------------------------------------------------------------
        checkFilterStatus(experiment);
    }
    

	
    //======================================================
    // ................ UPDATE ............................
    //======================================================
    
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView#updateDisplay()
	 */
	public void updateDisplay() 
	{
		// return immediately when there's no database or the view is closed (disposed)
        if (database == null || treeViewer == null || treeViewer.getTree().isDisposed())
        	return;
        
        // ------------------------------------------------------------
        // Tell children to update the content with the new database
        // ------------------------------------------------------------
        final Experiment myExperiment = database.getExperiment();
        this.updateDatabase(myExperiment);

        // Update root scope
        if (myRootScope != null && myRootScope.getChildCount() > 0) {
            treeViewer.setInput(myRootScope);
            
            // Try select the first scope
            /*TreeItem objItem = treeViewer.getTree().getItem(1);            
            treeViewer.getTree().showItem(objItem);
            this.treeViewer.getTree().setSelection(objItem);*/
    		
            // Finalize the content of the table: 
    		// - update the root scope of the actions
    		// - insert the parent node (aggregate metric)
    		objViewActions.finalizeContent(myRootScope);

            // reset the button
            this.objViewActions.checkNodeButtons();
            
            // ------------------------------------------------------------
        	// check the status of filter. 
            // if the filter may incur misleading information, we should warn users
            // ------------------------------------------------------------
            checkFilterStatus(myExperiment);
        }
   	}

	@Override
	public void dispose() {
		super.dispose();
		
		final ISourceProviderService service = (ISourceProviderService) Util.getActiveWindow().getService(ISourceProviderService.class);
		TableMetricState serviceProvider     = (TableMetricState) service.getSourceProvider(TableMetricState.METRIC_COLUMNS_VISIBLE);
		serviceProvider.removeSourceProviderListener(listener);
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView#initTableColumns()
	 */
	protected void initTableColumns(boolean keepColumnStatus) {
		
		if (treeViewer == null) return;
		
        objViewActions.updateContent(database.getExperiment(), myRootScope);
		
    	Tree tree = treeViewer.getTree();
    	if (tree == null || tree.isDisposed()) return;
    	
		addMetricColumnsToTable(tree, keepColumnStatus);
	}

	/***
	 * check if the filter incurs omitted scopes or not
	 * 
	 * @param myExperiment : the current experiment
	 */
	private void checkFilterStatus(Experiment myExperiment) 
	{
    	if (myExperiment != null) {
    		int filterStatus = myExperiment.getFilterStatus();
    		switch (filterStatus) {
    			case FilterScopeVisitor.STATUS_FAKE_PROCEDURE:
    				objViewActions.showWarningMessage("Warning: the result of filter may incur incorrect information in Callers View and Flat View.");
    				break;
    			case FilterScopeVisitor.STATUS_OK:
    	    		int filtered = myExperiment.getNumberOfFilteredScopes();
    	    		if (filtered>0) {
    	    			// show the information how many scopes matched with the filer
    	    			// this is important to warn users that filtering may hide some scopes 
    	    			// that can be useful for analysis.
        	    		String msg = "At least there ";
        	    		if (filtered == 1) {
        	    			msg += "is one scope";
        	    		}  else {
        	    			msg += "are " + filtered + " scopes";
        	    		}
    	    			objViewActions.showInfoMessage(msg + " matched with the filter.");
    	    		}
	    			break;
    		}
    	}

	}
	
	/******
	 * The same version as {@link BaseScopeView.initTableColumns} but without
	 * 	worrying if the tree has been disposed or not.
	 * 
	 * @param tree
	 * @param keepColumnStatus
	 */
	private void addMetricColumnsToTable(Tree tree, boolean keepColumnStatus) 
	{
        final Experiment myExperiment = database.getExperiment();
        final int numMetric			  = myExperiment.getMetricCount();

        int iColCount = tree.getColumnCount();
        
        /** status: list of booleans to indicate if a column should be shown or not 
         *  The number of items depends of the number of created column. 
         *  If the experiment has empty  metrics, the number of columns is not the same
         *    as the number of metrics since we don't create empty columns to save memory
         *    and processing */ 
        boolean status[] = new boolean[numMetric];
        
        boolean empty[]  = new boolean[numMetric];

        tree.setRedraw(false);
        
        if (!keepColumnStatus) {
        	int i=0;
        	
        	// empty metric: if the root scope has no metric value
        	// displayable: if the metric is to be displayed and not empty
        	for(BaseMetric metric: myExperiment.getMetrics()) {
        		
        		empty[i]  = myRootScope.getMetricValue(metric) == MetricValue.NONE;
    			status[i] = !empty[i] && metric.getDisplayed();
        		i++;
        	}
        }
        else if(iColCount>1) {
        	
        	TreeColumn []columns = tree.getColumns();
        	int i=0;
        	for(BaseMetric metric: myExperiment.getMetrics()) {
        		empty[i]  = myRootScope.getMetricValue(metric) == MetricValue.NONE;
        		
        		int j;
        		for (j=1; j<columns.length && columns[j].getData() != metric; j++) ;
        		
        		if (j<columns.length && columns[j].getData() == metric) {
        			status[i] = columns[j].getWidth() > 1;
        		} else {
        			status[i] = false;
        		}
        		i++;
        	}
        }
    	TreeColumn []columns = tree.getColumns();
    	
    	// remove the metric columns blindly
    	// TODO we need to have a more elegant solution here
    	for(int i=0;i<iColCount;i++) {
    		TreeColumn column = columns[i]; //treeViewer.getTree().getColumn(1);
    		column.dispose();
    	}

        //----------------- create the column tree
        final TreeViewerColumn colTree = createScopeColumn(treeViewer);
        
		ScopeSelectionAdapter selectionAdapter = new ScopeSelectionAdapter(treeViewer, colTree);
		colTree.getColumn().addSelectionListener(selectionAdapter);
        
        boolean alreadySorted = false;
        
        // add table column for each metric
    	for (int i=0; i<numMetric; i++)
    	{
    		final BaseMetric metric = myExperiment.getMetric(i);
    		if (metric != null) {
        		
                boolean toBeSorted = false;
                
        		// A column is sorted if it is the first displayed column            		
        		if (!alreadySorted && !empty[i] && metric.getDisplayed()) {
        			toBeSorted    = true;
        			alreadySorted = true;
        		}
        		// only create metric columns if it has metric values
        		// we don't need to create empty column just to show the metric exists
        		if (!empty[i])
        			treeViewer.addTreeColumn(metric, toBeSorted);
    		}
    	}
    	this.objViewActions.objActionsGUI.setColumnsStatus(status);
    	
        tree.setRedraw(true);
	}
	

    /**
     * Tell children to update the content with the new database
     * @param new_database
     */
    abstract protected void updateDatabase(Experiment new_database);
    
    /***
     * Method to be implemented by the child class.<br/>
     * This method is called when a filter is applied, and the view needs
     * to be refreshed with the new root tree.
     * 
     * @param root : the new root tree
     */
    abstract protected void refreshTree(RootScope root);

}
