package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.CoolBar;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.viewer.framework.Activator;
import edu.rice.cs.hpc.viewer.metric.*;
import edu.rice.cs.hpc.viewer.util.PreferenceConstants;
import edu.rice.cs.hpc.viewer.util.Utilities;

/**
 * Class to manage the actions of the tree view such as zooms, flattening,
 * resize the columns, etc. This class will add additional toolbar on the top
 * of the tree. Therefore, it is necessary to instantiate this class before
 * the creation of the tree, then call the method updateContent() to associate
 * the action with the tree (once the tree is created). 
 * This looks somewhat stupid, but this is the fastest thing in my mind :-(
 * 
 * @author laksono
 *
 */
public abstract class ScopeViewActions /*extends ScopeActions /* implements IToolbarManager*/ 
{
	// constants
	static public double fTHRESHOLD = PreferenceConstants.P_THRESHOLD_DEFAULT; 
	static final private int MESSAGE_TIMEOUT = 8000; // time out when showing a message

	//-------------- DATA
    protected ScopeTreeViewer 	treeViewer;		  	// tree 
    protected RootScope 		myRootScope;		// the root scope of this view

    // laksono 2009.04.07
    protected ScopeZoom objZoom = null;
    
    public interface IActionType {};
    public enum ActionType implements IActionType {ZoomIn, ZoomOut} ;
	
	protected IWorkbenchWindow 	objWindow;
	protected IScopeActionsGUI 	objActionsGUI;
    protected Shell				objShell;
	
    /**
     * Constructor: create actions and the GUI (which is a coolbar)
     * @param viewSite the site of the view (used for retrieving shell, display, ...)
     * @param parent composite
     */
    public ScopeViewActions(Shell shell, IWorkbenchWindow window, Composite parent, CoolBar coolbar) {

    	objShell = shell;
    	this.objWindow  = window;
    	createGUI(parent, coolbar);
		// need to instantiate the zoom class after the creation of GUIs
		objZoom = new ScopeZoom(treeViewer, (ScopeViewActionsGUI) this.objActionsGUI);
		ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
		fTHRESHOLD = objPref.getFloat(PreferenceConstants.P_THRESHOLD);
    }

    /**
     * Each class has its own typical GUI creation
     */
	abstract protected  Composite createGUI(Composite parent, CoolBar coolbar);

    /**
     * The tree has been updated or has new content. This object needs to refresh
     * the data and variable initialization too.
     * @param exp
     * @param scope
     * @param columns
     */
	public void updateContent(Experiment exp, RootScope scope) {
    	this.myRootScope = scope;
    	this.objActionsGUI.updateContent(exp, scope);
    }
	
	public void finalizeContent(RootScope root) {
		objActionsGUI.finalizeContent(root);
	}
	
    /**
     * Update the content of tree viewer
     * @param tree
     */
    public void setTreeViewer(ScopeTreeViewer tree) {
    	this.treeViewer = tree;
    	this.objActionsGUI.setTreeViewer(tree);
    	this.objZoom.setViewer(tree);
    }

	public void setColumnStatus(boolean []status) {
		this.objActionsGUI.setColumnsStatus(status);
	}

    /**
	 * find the hot call path
	 * @param Scope scope
	 * @param BaseMetric metric
	 * @param int iLevel
	 * @param TreePath path
	 * @param HotCallPath objHotPath (caller has to allocate it)
	 */
	private boolean getHotCallPath(Scope scope, BaseMetric metric, int iLevel, TreePath path, HotCallPath objHotPath) {
		if(scope == null || metric == null )
			return false;

		AbstractContentProvider content = (AbstractContentProvider)treeViewer.getContentProvider();
		Object []children = content.getSortedChildren(scope);
		
		if (objHotPath == null) objHotPath = new HotCallPath();
		
		objHotPath.node = scope;
		objHotPath.path = path;
		
		// singly depth first search
		// bug fix: we only drill once !
		if (children != null && children.length > 0) {
			Object o = children[0];
			if(o instanceof Scope) {
				// get the child node
				Scope scopeChild = (Scope) o;
				
				// let's move deeper down the tree
				// this cause java null pointer
				try {
					treeViewer.expandToLevel(path, 1);					
				} catch (Exception e) {
					System.out.println("Cannot expand path: " + path);
					return false;
				}

				// compare the value of the parent and the child
				// if the ratio is significant, we stop 
				MetricValue mvParent = metric.getValue(scope);
				MetricValue mvChild  = metric.getValue(scopeChild);
				
				double dParent = MetricValue.getValue(mvParent);
				double dChild  = MetricValue.getValue(mvChild);
				
				// simple comparison: if the child has "significant" difference compared to its parent
				// then we consider it as hot path node.
				if(dChild < (ScopeViewActions.fTHRESHOLD * dParent)) {
					objHotPath.node     = scopeChild;
					
					return true;
				} else {

					TreePath childPath = path.createChildPath(scopeChild);
					return getHotCallPath(scopeChild, metric, iLevel+ 1, childPath, objHotPath);
				}
			}
		}
		// if we reach at this statement, then there is no hot call path !
		return false;
	}

	/**
	 * Get the current input node
	 * @return
	 */
	private Scope getInputNode() {
		Object o = treeViewer.getInput();
		Scope child;
		if (!(o instanceof Scope)) {
				TreeItem []tiObjects = this.treeViewer.getTree().getItems();
				o = tiObjects[0];
				if(o instanceof Scope)
					child = (Scope)tiObjects[0].getData(); //the 0th item can be the aggregate metric
				else if(tiObjects.length>1)
					// in case of the top row is not a node, the second one MUST BE a node
					child = (Scope)tiObjects[1].getData();
				else
					// Otherwise there is something wrong with the data and the tree
					throw (new java.lang.RuntimeException("ScopeViewActions: tree contains unknown objects"));
				// tricky solution when zoom-out the flattened node
				if(child != null)
					child = (Scope)child.getParent();
		} else 
			child = (Scope) o;
		return child;
	}
	
	
	//====================================================================================
	// ----------------------------- ACTIONS ---------------------------------------------
	//====================================================================================

	/**
	 * Class to restoring the background of the message bar by waiting for 5 seconds
	 * TODO: we need to parameterize the timing for the wait
	 * @author la5
	 *
	 */
	private class RestoreMessageThread extends Thread {	
		RestoreMessageThread() {
			super();
		}
         public void run() {
             try{
            	 sleep(MESSAGE_TIMEOUT);
             } catch(InterruptedException e) {
            	 e.printStackTrace();
             }
             // need to run from UI-thread for restoring the background
             // without UI-thread we will get SWTException !!
             if (objShell != null && !objShell.isDisposed()) {
            	 Display display = objShell.getDisplay();
            	 if (display != null && !display.isDisposed()) {
            		 display.asyncExec(new Runnable() {
                    	 public void run() {
                        	 objActionsGUI.restoreMessage();
                    	 }
                     });
            	 }
             }
         }
     }
	
	public void showInfoMessage(String sMsg) {
		this.objActionsGUI.showInfoMessage(sMsg);
		// remove the msg in 5 secs
		RestoreMessageThread thrRestoreMessage = new RestoreMessageThread();
		thrRestoreMessage.start();
	}
	
	/**
	 * Show an error message on the message bar (closed to the toolbar) and
	 * wait for 5 seconds before removing the message
	 * @param strMsg
	 */
	public void showErrorMessage(String strMsg) {
		this.objActionsGUI.showErrorMessage(strMsg);
		// remove the msg in 5 secs
		RestoreMessageThread thrRestoreMessage = new RestoreMessageThread();
		thrRestoreMessage.start();
	}
	
	public void showWarningMessage(String strMsg) {
		objActionsGUI.showWarningMessagge(strMsg);
		// remove the msg in 5 secs
		RestoreMessageThread thrRestoreMessage = new RestoreMessageThread();
		thrRestoreMessage.start();
	}
	
	/**
	 * show the hot path below the selected node in the tree
	 */
	public void showHotCallPath() {
		// find the selected node
		ISelection sel = treeViewer.getSelection();
		if (!(sel instanceof TreeSelection)) {
			System.err.println("SVA: not a TreeSelecton instance");
			return;
		}
		TreeSelection objSel = (TreeSelection) sel;
		// get the node
		Object o = objSel.getFirstElement();
		if (!(o instanceof Scope)) {
			showErrorMessage("Please select a scope node.");
			return;
		}
		Scope current = (Scope) o;
		// get the item
		TreeItem item = this.treeViewer.getTree().getSelection()[0];
		// get the selected metric
		TreeColumn colSelected = this.treeViewer.getTree().getSortColumn();
		if((colSelected == null) || colSelected.getWidth() == 0) {
			// the column is hidden or there is no column sorted
			this.showErrorMessage("Please select a column to sort before using this feature.");
			return;
		}
		// get the metric data
		Object data = colSelected.getData();
		if(data instanceof BaseMetric && item != null) {
			BaseMetric metric = (BaseMetric) data;
			// find the hot call path
			int iLevel = 0;
			TreePath []path = objSel.getPaths();
			
			HotCallPath objHotPath = new HotCallPath();
			
			boolean is_found = getHotCallPath(current, metric, iLevel, path[0], objHotPath);

			// whether we find it or not, we should reveal the tree path to the last scope
			
			treeViewer.setSelection(new StructuredSelection(objHotPath.path), true);

			if(!is_found) {
				this.showErrorMessage("No hot child.");
			}
		} else {
			// It is almost impossible for the jvm to reach this part of branch.
			// but if it is the case, it should be a BUG !!
			if(data !=null )
				System.err.println("SVA BUG: data="+data.getClass()+" item= " + (item==null? 0 : item.getItemCount()));
			else
				this.showErrorMessage("Please select a metric column !");
		}
	}
	
	/**
	 * Retrieve the selected node
	 * @return null if there is no selected node
	 */
	public Scope getSelectedNode() {
		ISelection sel = treeViewer.getSelection();
		if (!(sel instanceof TreeSelection))
			return null;
		Object o = ((TreeSelection)sel).getFirstElement();
		if (!(o instanceof Scope)) {
			return null;
		}
		return (Scope) o;
	}
	/**
	 * Zoom-in the children
	 */
	public void zoomIn() {
		// set the new view based on the selected node
		Scope current = this.getSelectedNode();
		if(current == null)
			return;
		
		// ---------------------- save the current view
		Scope objInputNode = this.getInputNode();
		objZoom.zoomIn(current, objInputNode);
		Scope nodeSelected = this.getSelectedNode();
		
		registerAction(ActionType.ZoomIn);
		
		checkStates(nodeSelected);
	}
	
	/**
	 * Zoom-out the node
	 */
	public void zoomOut() {
		try {
			objZoom.zoomOut();
			// funny behavior on Windows: they still keep the track of the previously selected item !!
			// therefore we need to check again the state of the buttons
			Scope nodeSelected = this.getSelectedNode();
			
			registerAction(ActionType.ZoomOut);

			this.checkStates(nodeSelected);
		} catch (Exception e) {
			// just ignore if there's somwthing wrong with the zoom out
			MessageDialog.openError(objShell, "Error", e.getMessage());
		}
	}
	
	/**
	 * retrieve the class scope zoom of this object
	 * @return
	 */
	public ScopeZoom getScopeZoom () {
		return this.objZoom;
	}
	
	
	/**
	 * create a new metric based on a free expression
	 */
	public void addExtNewMetric() {
		
		if (this.myRootScope == null)
			return;
		
		// prepare the dialog box
		ExtDerivedMetricDlg dlg = new ExtDerivedMetricDlg(this.objShell, 
				getMetricManager(), myRootScope);

		// display the dialog box
		if(dlg.open() == Dialog.OK) {

			final DerivedMetric objMetric = dlg.getMetric();
			
			getMetricManager().addDerivedMetric(objMetric);
			
			notifyNewDerivedMetrics(objMetric);
		}
	}

	/**
	 * Resize the columns
	 */
	public void resizeColumns() {
		this.objActionsGUI.resizeTableColumns();
	}


	/**
	 * Retrieve the content of the table into a string
	 * @param items (list of items to be exported)
	 * @param sSeparator (separator)
	 * @return String: content of the table
	 */
	public String getContent(TreeItem []items, String sSeparator) {
    	StringBuffer sbText = new StringBuffer();
    	
    	// get all selected items
    	for (int i=0; i< items.length; i++) {
    		TreeItem objItem = items[i];
    		Object o = objItem.getData();
    		// let get the metrics if the selected item is a scope node
    		if (o instanceof Scope) {
    			Scope objScope = (Scope) o;
    			this.saveContent(objScope, sSeparator, sbText);
    		} else {
    			// in case user click the first row, we need a special treatment
    			// first row of the table is supposed to be a sub-header, but at the moment we allow user
    			//		to do anything s/he wants.
    			String sElements[] = (String []) o; 
    			sbText.append( "\"" + sElements[0] + "\"" );
    			sbText.append( sSeparator ); // separate the node title and the metrics
    			sbText.append( this.treeViewer.getTextBasedOnColumnStatus(sElements, sSeparator, 1, 0) );
    		}
    		sbText.append(Utilities.NEW_LINE);
    	}
    	return sbText.toString();
	}

	/**
	 * Retrieve the content of the table into a string
	 * @param items (list of items to be exported)
	 * @param sSeparator (separator)
	 * @return String: content of the table
	 */
	public String getDisplayContent(TreeItem []items, String sSeparator) {
    	StringBuffer sbText  = new StringBuffer();
    	TreeColumn columns[] = treeViewer.getTree().getColumns();
    	
    	// get all selected items
    	for (int i=0; i< items.length; i++) {
    		TreeItem objItem = items[i];
    		
    		// get text of the displayed columns
    		// hidden columns will not be copied
    		for(int j=0; j<columns.length; j++) {
    			if (columns[j].getWidth() > 0) {
    				if (j>0)
                		sbText.append(sSeparator);

            		String text = objItem.getText(j);
            		sbText.append(text);
    			}
    		}
    		sbText.append(Utilities.NEW_LINE);
    	}
    	return sbText.toString();
	}
	
	/**
	 * private function to copy a scope node into a buffer string
	 * @param objScope current scope
	 * @param sSeparator string separator between different metric column
	 * @param sbText output saved content
	 */
	public void saveContent( Scope objScope, String sSeparator, StringBuffer sbText ) {

		final TreeColumn []columns = treeViewer.getTree().getColumns();
		sbText.append( "\"" + objScope.getName() + "\"" );
		
		for(int j=1; j<columns.length; j++) 
		{
			if (columns[j].getWidth()>0) {
				Object obj = columns[j].getData();
				if (obj != null && obj instanceof BaseMetric) {
					// the column is not hidden
					BaseMetric metric = (BaseMetric) obj;
					MetricValue value = objScope.getMetricValue(metric);
					if (value == MetricValue.NONE) {
						// no value: write empty space
						sbText.append(sSeparator + " ");
					} else {
						sbText.append(sSeparator + value.getValue());
					}
				}
			}
		}
	}
	
	
	//--------------------------------------------------------------------------
	// BUTTONS CHECK
	//--------------------------------------------------------------------------
	/**
	 * Check if zoom-in button should be enabled
	 * @param node
	 * @return
	 */
    public boolean shouldZoomInBeEnabled(Scope node) {
    	return this.objZoom.canZoomIn(node);
    }
    
    /**
     * In case there is no selected node, we determine the zoom-out button
     * can be enabled only and only if we have at least one item in the stack
     * @return
     */
    public boolean shouldZoomOutBeEnabled() {
    	if (objZoom == null )
    		return false;
    	else
    		return objZoom.canZoomOut();
    }
    

    /**
     * Check if zooms and hot-path button need to be disabled or not
     * This is required to solve bug no 132: 
     * https://outreach.scidac.gov/tracker/index.php?func=detail&aid=132&group_id=22&atid=169
     */
    public void checkNodeButtons() {
    	Scope nodeSelected = this.getSelectedNode();
    	if(nodeSelected == null)
    		this.objActionsGUI.disableNodeButtons();
    	else
    		this.checkStates(nodeSelected);
    }
    
    /**
     * Disable buttons
     */
    public void disableButtons () {
    	objActionsGUI.disableNodeButtons();
    }
    
    public RootScope getRootScope() {
    	return myRootScope;
    }
    
    /**
     * An abstract method to be implemented: check the state of buttons for the selected node
     * Each action (either caller view, calling context view or flat view) may have different
     * implementation for this verification
     * 
     * @param nodeSelected
     */
    public abstract void checkStates ( Scope nodeSelected );
    
    public abstract void addMetricColumn(AbstractBaseScopeView view, DerivedMetric objMetric);
    
    public abstract void notifyNewDerivedMetrics(DerivedMetric objMetric);
    
    protected abstract void registerAction( IActionType type );
        
    protected abstract IMetricManager getMetricManager();
    
    static class HotCallPath 
    {
    	// last node iterated
    	Scope node = null;
    	
    	TreePath path = null;
    }
}
