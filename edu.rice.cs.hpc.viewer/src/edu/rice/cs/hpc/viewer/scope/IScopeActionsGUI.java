/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/**
 * @author laksonoadhianto
 *
 */
public interface IScopeActionsGUI {

	/**
	 * Method to start to build the GUI for the actions
	 * @param parent
	 * @return
	 */
	public Composite buildGUI(Composite parent, CoolBar coolbar);
	
	
	/**
	 * IMPORTANT: need to call this method once the content of tree is changed !
	 * Warning: call only this method when the tree has been populated !
	 * @param exp
	 * @param scope
	 * @param columns
	 */
	public void updateContent(Experiment exp, RootScope scope);

	public void finalizeContent(RootScope root);
	
	/**
	 * Set the new tree viewer
	 * @param tree
	 */
	public void setTreeViewer(ScopeTreeViewer tree);

	/**
	 * Show a warning message (with yellow background).
	 * The caller has to remove the message and restore it to the original state
	 * by calling restoreMessage() method
	 */
	public void showWarningMessagge(String sMsg);
	
	/**
	 * Restore the message bar into the original state
	 */
	public void restoreMessage();
	
	/**
	 * Show an error message on the message bar. It is the caller responsibility to 
	 * remove the message
	 * @param sMsg
	 */
	public void showErrorMessage(String sMsg);
	
	/**
	 * Show the information on the message bar
	 * @param sMsg
	 */
	public void showInfoMessage(String sMsg);
	/**
     * Check zoom buttons (zoom out and zoom in)
     * @param node: the current selected node
     */
    //public void checkZoomButtons(Scope.Node node);
	//public void updateButtons (boolean bZoomIn, boolean bZoomOut );
	public void enableZoomIn (boolean enabled);
	public void enableZoomOut (boolean enabled);
	public void enableHotCallPath (boolean enabled);
    /**
     * Disable actions that need a selected node
     */
    public void disableNodeButtons();
	
	/**
	 * Reset the button and actions into disabled state
	 */
	public void resetActions();

	/**
	 * Enable the some actions (resize and column properties) actions for this view
	 */
	public void enableActions();
	
    /**
     * Inserting a "node header" on the top of the table to display
     * either aggregate metrics or "parent" node (due to zoom-in)
     * TODO: we need to shift to the left a little bit
     * @param nodeParent
     */
    public void insertParentNode(Scope nodeParent);

	public void restoreParentNode();

	
	public void setColumnsStatus(boolean []status);	
	
}
