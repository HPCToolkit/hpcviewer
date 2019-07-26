/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

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
		TreeViewerColumn colDerived = treeViewer.addTreeColumn(objMetric,  false);
		
		// update the viewer, to refresh its content and invoke the provider
		// bug SWT https://bugs.eclipse.org/bugs/show_bug.cgi?id=199811
		// we need to hold the UI to draw until all the data is available
		// 2012.09.21: do not refresh. It crashes on linux/gtk/ppc
		//objTreeViewer.refresh();	// we refresh to update the data model of the table
		
		// notify the GUI that we have added a new column
    	int width 			    = (int) colDerived.getColumn().getData(ScopeTreeViewer.COLUMN_DATA_WIDTH);
    	
    	colDerived.getColumn().setWidth(width);
    	
    	ColumnPixelData colData = new ColumnPixelData(width, true);
    	TreeColumnLayout layout = (TreeColumnLayout) treeViewer.getTree().getParent().getLayout();
    	
    	layout.setColumnData(colDerived.getColumn(), colData);
		
    	objActionsGUI.restoreParentNode();
    	
		//this.objActionsGUI.addMetricColumns(colDerived); 
    	treeViewer.getTree().setRedraw(true);
		
    	// adjust the column width 
    	treeViewer.getTree().pack();
		
		// instead of refresh, we use update which will reset the input and
		//	reinitialize the table. It isn't elegant, but works in all platforms
		view.updateDisplay();
	}
	

}
