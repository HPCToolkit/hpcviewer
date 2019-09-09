/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.provider.TableMetricState;

/**
 * 
 * Basic class to implement ScopeViewActions
 *
 */
public class BaseScopeViewActions extends ScopeViewActions {

	public BaseScopeViewActions(Shell shell, IWorkbenchWindow window,
			Composite parent, CoolBar coolbar) {
		super(shell, window, parent, coolbar);
	}

	public void checkStates(Scope nodeSelected) {
    	boolean bCanZoomIn = objZoom.canZoomIn(nodeSelected);
		objActionsGUI.enableZoomIn( bCanZoomIn );
		objActionsGUI.enableHotCallPath( bCanZoomIn );
		objActionsGUI.enableZoomOut( objZoom.canZoomOut() );
	}


    /**
     * Each class has its own typical GUI creation
     */
	protected  Composite createGUI(Composite parent, CoolBar coolbar) {
    	this.objActionsGUI = new ScopeViewActionsGUI(this.objShell, this.objWindow, parent, this);
    	return objActionsGUI.buildGUI(parent, coolbar);
	}


	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.ScopeViewActions#actionZoom(edu.rice.cs.hpc.viewer.scope.ScopeViewActions.ZoomType)
	 */
	protected void registerAction(IActionType type) {	}

	@Override
	protected IMetricManager getMetricManager() {
		return (Experiment) myRootScope.getExperiment();
	}

	
	/***
	 * Add a new metric column into a view's table
	 * @param view : the view that contains metric table
	 * @param objMetric : the new metric 
	 */
	public void addMetricColumn(AbstractBaseScopeView view, DerivedMetric objMetric) {
		
		if (treeViewer.getTree().isDisposed())
			return;
		
		treeViewer.getTree().setRedraw(false);
		
		treeViewer.addTreeColumn(objMetric,  false);
    	
    	objActionsGUI.restoreParentNode();
    	
    	treeViewer.getTree().setRedraw(true);
		
    	// adjust the column width 
    	// treeViewer.getTree().pack();
		
		// instead of refresh, we use update which will reset the input and
		//	reinitialize the table. It isn't elegant, but works in all platforms
		view.updateDisplay();
	}

	@Override
	public void notifyNewDerivedMetrics(DerivedMetric objMetric) {
		
		final ISourceProviderService service = (ISourceProviderService) objWindow.getService(ISourceProviderService.class);
		TableMetricState metricStateProvider = (TableMetricState) service.getSourceProvider(TableMetricState.METRIC_COLUMNS_VISIBLE);

		metricStateProvider.notifyMetricAdd(myRootScope.getExperiment(), objMetric);		
	}
}
