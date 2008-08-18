/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.layout.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.graphics.Color;
//import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewSite;
//import org.eclipse.jface.action.IStatusLineManager;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.resources.Icons;
import edu.rice.cs.hpc.viewer.util.ColumnProperties;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.viewer.util.Utilities;
//import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
//import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;

/**
 * General actions GUI for basic scope views like caller view and calling context view
 * This GUI includes toolbar for zooms, add derived metrics, show/hide columns, and hot call path 
 * @author laksono
 *
 */
public class ScopeViewActionsGUI implements IScopeActionsGUI {

	final static private String COLUMN_DATA_WIDTH = "w"; 
    //======================================================
	// ------ DATA ----------------------------------------
    //======================================================
	// GUI STUFFs
    private TreeViewer 	treeViewer;		  	// tree for the caller and callees
    private ScopeViewActions objViewActions;
    private TreeViewerColumn []colMetrics;	// metric columns
    private Shell shell;

    // variable declaration uniquely for coolbar
	private ToolItem tiZoomin;		// zoom-in button
	private ToolItem tiZoomout ;	// zoom-out button
	private ToolItem tiColumns ;	// show/hide button
	private ToolItem tiHotCallPath;
	private ToolItem tiAddExtMetric;
	private Label lblMessage;
	
	//------------------------------------DATA
	protected Scope.Node nodeTopParent; // the current node which is on the top of the table (used as the aggregate node)
	protected Experiment 	myExperiment;		// experiment data	
	protected RootScope 		myRootScope;		// the root scope of this view

    // ----------------------------------- CONSTANTS
    private Color clrYELLOW, clrRED, clrNORMAL;
    
    /**
     * Constructor initializing the data
     * @param shellGUI
     * @param objViewer
     * @param fontMetricColumn
     * @param objActions
     */
	public ScopeViewActionsGUI(Shell objShell, Composite parent, 
			ScopeViewActions objActions) {

		this.objViewActions = objActions;
		this.shell = objShell;
		//this.statusLine = viewSite.getActionBars().getStatusLineManager();
		
		this.clrNORMAL = this.shell.getBackground();
		this.clrYELLOW = new Color(this.shell.getDisplay(),255,255,0);
		this.clrRED = new Color(this.shell.getDisplay(), 250,128,114);
	}

	/**
	 * Method to start to build the GUI for the actions
	 * @param parent
	 * @return
	 */
	public Composite buildGUI(Composite parent, CoolBar coolbar) {
		//CoolBar coolbar = this.initToolbar(parent);
		Composite newParent = this.addTooBarAction(coolbar);
		this.finalizeToolBar(parent, coolbar);
		return newParent;
	}

	/**
	 * IMPORTANT: need to call this method once the content of tree is changed !
	 * Warning: call only this method when the tree has been populated !
	 * @param exp
	 * @param scope
	 * @param columns
	 */
	public void updateContent(Experiment exp, RootScope scope, TreeViewerColumn []columns) {
		// save the new data and properties
		this.myExperiment = exp;
		this.myRootScope = scope;
		this.colMetrics = columns;
		//this.setLevelText(scope.getTreeNode().iLevel);	// @TODO: initialized with root level
		
		// actions needed when a new experiment is loaded
		this.resizeTableColumns();	// we assume the data has been populated
        this.enableActions();
        // since we have a new content of experiment, we need to display 
        // the aggregate metrics
        this.displayRootExperiment();
	}
	
    //======================================================
    public void setTreeViewer(TreeViewer tree) {
    	this.treeViewer = tree;
    }

    /**
     * Inserting a "node header" on the top of the table to display
     * either aggregate metrics or "parent" node (due to zoom-in)
     * TODO: we need to shift to the left a little bit
     * @param nodeParent
     */
    public void insertParentNode(Scope.Node nodeParent) {
    	Scope scope = nodeParent.getScope();
    	// Bug fix: avoid using list of columns from the experiment
    	// formerly: .. = this.myExperiment.getMetricCount() + 1;
    	int nbColumns = this.colMetrics.length; 	// coloumns in base metrics
    	String []sText = new String[nbColumns+1];
    	sText[0] = new String(scope.getName());
    	// --- prepare text for base metrics
    	// get the metrics for all columns
    	for (int i=0; i< nbColumns; i++) {
    		// we assume the column is not null
    		Object o = this.colMetrics[i].getColumn().getData();
    		if(o instanceof BaseMetric) {
    			BaseMetric metric = (BaseMetric) o;//this.myExperiment.getMetric(i);
    			sText[i+1] = metric.getMetricTextValue(scope);
    		}
    	}
    	
    	// draw the root node item
    	Utilities.insertTopRow(treeViewer, Utilities.getScopeNavButton(scope), sText);
    	this.nodeTopParent = nodeParent;
    }
    
    /**
     * Restoring the "node header" in case of refresh method in the viewer
     */
    private void restoreParentNode() {
    	if(this.nodeTopParent != null) {
    		this.insertParentNode(this.nodeTopParent);
    	}
    }
	/**
	 * Add the aggregate metrics item on the top of the tree
	 */
    protected void displayRootExperiment() {
    	Scope.Node  node = (Scope.Node) this.myRootScope.getTreeNode();
    	this.insertParentNode(node);
    }
	
	/**
	 * Resize the columns automatically
	 * ATT: Please call this method once the data has been populated
	 */
	public void resizeTableColumns() {
        // resize the column according to the data size
		int nbCols = this.colMetrics.length;
        for (int i=0; i<nbCols; i++) {
        	TreeColumn column = this.colMetrics[i].getColumn();
        	// do NOT resize if the column is hidden
        	if(column.getWidth()>1)
        		column.pack();
        }
	}

	//======================================================
    // ................ GUI and LAYOUT ....................
    //======================================================
	
	/**
	 * Show a warning message (with yellow background).
	 * The caller has to remove the message and restore it to the original state
	 * by calling restoreMessage() method
	 */
	public void showWarningMessagge(String sMsg) {
		this.lblMessage.setBackground(this.clrYELLOW);
		this.lblMessage.setText(sMsg);
	}
	
	/**
	 * Show an error message on the message bar. It is the caller responsibility to 
	 * remove the message
	 * @param sMsg
	 */
	public void showErrorMessage(String sMsg) {
		this.lblMessage.setBackground(this.clrRED);
		this.lblMessage.setText(" " + sMsg);
	}

	/**
	 * Restore the message bar into the original state
	 */
	public void restoreMessage() {
		if(this.lblMessage != null) {
			this.lblMessage.setBackground(this.clrNORMAL);
			this.lblMessage.setText("");
		}
	}
	/**
	 * Reset the button and actions into disabled state
	 */
	public void resetActions() {
		this.tiColumns.setEnabled(false);
		this.tiAddExtMetric.setEnabled(false);
		// disable zooms and hot-path buttons
		this.disableNodeButtons();
	}
	
	/**
	 * Enable the some actions (resize and column properties) actions for this view
	 */
	public void enableActions() {
		this.tiColumns.setEnabled(true);
		this.tiAddExtMetric.setEnabled(true);
	}
	    
	/**
	 * Hiding a metric column
	 * @param iColumnPosition: the index of the metric
	 */
	public void hideMetricColumn(int iColumnPosition) {
			int iWidth = this.colMetrics[iColumnPosition].getColumn().getWidth();
   			if(iWidth > 0) {
       			Integer objWidth = Integer.valueOf(iWidth); 
       			// Laks: bug no 131: we need to have special key for storing the column width
       			this.colMetrics[iColumnPosition].getColumn().setData(COLUMN_DATA_WIDTH,objWidth);
       			this.colMetrics[iColumnPosition].getColumn().setWidth(0);
   			}
	}
    /**
     * Show column properties (hidden, visible ...)
     */
    private void showColumnsProperties() {
    	ColumnProperties objProp = new ColumnProperties(this.shell, this.colMetrics);
    	objProp.open();
    	if(objProp.getReturnCode() == org.eclipse.jface.dialogs.IDialogConstants.OK_ID) {
        	boolean result[] = objProp.getResult();
           	for(int i=0;i<result.length;i++) {
           		// hide this column
           		if(!result[i]) {
           			this.hideMetricColumn(i);
           		} else {
           			// display the hidden column
           			// Laks: bug no 131: we need to have special key for storing the column width
            		Object o = this.colMetrics[i].getColumn().getData(COLUMN_DATA_WIDTH);
           			int iWidth = 120;
           			if((o != null) && (o instanceof Integer) ) {
           				iWidth = ((Integer)o).intValue();
               			this.colMetrics[i].getColumn().setWidth(iWidth);
           			}
           		}
           	}
   		}
    }
    
    /**
     * Add a new metric column
     * @param colMetric
     */
    public void addMetricColumns(TreeViewerColumn colMetric) {
    	int nbCols = this.colMetrics.length + 1;
    	TreeViewerColumn arrColumns[] = new TreeViewerColumn[nbCols];
    	for(int i=0;i<nbCols-1;i++)
    		arrColumns[i] = this.colMetrics[i];
    	arrColumns[nbCols-1] = colMetric;
    	this.colMetrics = arrColumns;
    	// when adding a new column, we have to refresh the viewer
    	// and this means we have to recompute again the top row of the table
    	this.restoreParentNode();
    }
    //======================================================
    // ................ BUTTON ............................
    //======================================================
    /**
     * Check zoom buttons (zoom out and zoom in)
     * @param node: the current selected node
     */
    public void checkZoomButtons(Scope.Node node) {
    	tiZoomout.setEnabled(this.shouldZoomOutBeEnabled());
    	boolean b = shouldZoomInBeEnabled(node);
    	tiZoomin.setEnabled(b);
    	this.tiHotCallPath.setEnabled(b);
    }

    /**
     * Disable actions that need a selected node
     */
    public void disableNodeButtons() {
    	this.tiZoomin.setEnabled(false);
    	this.tiZoomout.setEnabled(false);
    	this.tiHotCallPath.setEnabled(false);
    }
    //======================================================
    // ................ ZOOM ............................
    //======================================================
    /**
     * Check if the button Zoom-in should be available given node as 
     * the main node to zoom
     * @param node
     * @return
     */
    static public boolean shouldZoomInBeEnabled(Scope.Node node) {
    	if(node != null)
    		return (node.getChildCount()>0);
    	else
    		return false;
    }
    
    /**
     * Check if the button zoom-out should be enable 
     * @return
     */
    public boolean shouldZoomOutBeEnabled() {
    	// FIXME: this is a spaghetti code: need to call the user object
    	// 		  in order to see if the zoom out can be enabled :-(
    	return this.objViewActions.shouldZoomOutBeEnabled();   	
    }
    //======================================================
    // ................ CREATION ............................
    //======================================================
    /**
     * Creating an item for the existing coolbar
     * @param coolBar
     * @param toolBar
     */
    protected void createCoolItem(CoolBar coolBar, Control toolBar) {
    	CoolItem coolItem = new CoolItem(coolBar, SWT.NULL);
    	coolItem.setControl(toolBar);
    	org.eclipse.swt.graphics.Point size =
    		toolBar.computeSize( SWT.DEFAULT,
    	                           SWT.DEFAULT);
    	org.eclipse.swt.graphics.Point coolSize = coolItem.computeSize (size.x, size.y);
    	coolItem.setSize(coolSize);    	
    }
    
    protected void finalizeToolBar(Composite parent, CoolBar coolBar) {
    	// message text
    	lblMessage = new Label(parent, SWT.NONE);
    	lblMessage.setText("");

    	// but the message label yes
    	GridDataFactory.fillDefaults().grab(true, false).applyTo(lblMessage);
    	// the coolbar part shouldn't be expanded 
    	GridDataFactory.fillDefaults().grab(false, false).applyTo(coolBar);
    	// now the toolbar area should be able to be expanded automatically
    	GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
    	// two kids for toolbar area: coolbar and message label
    	GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(parent);

    }
	/**
     * Create a toolbar region on the top of the view. This toolbar will be used to host some buttons
     * to make actions on the treeview.
     * @param aParent
     * @return Composite of the view. The tree should be based on this composite.
     */
    public Composite addTooBarAction(CoolBar coolbar) {
    	// prepare the toolbar
    	ToolBar toolbar = new ToolBar(coolbar, SWT.FLAT);
    	Icons iconsCollection = Icons.getInstance();
    	    	
    	// zoom in
    	tiZoomin = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomin.setToolTipText("Zoom-in the selected node");
    	tiZoomin.setImage(iconsCollection.imgZoomIn);
    	tiZoomin.addSelectionListener(new SelectionAdapter() {
      	  	public void widgetSelected(SelectionEvent e) {
      	  	objViewActions.zoomIn();
      	  	}
      	});
    	
    	// zoom out
    	tiZoomout = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomout.setToolTipText("Zoom-out the selected node");
    	tiZoomout.setImage(iconsCollection.imgZoomOut);
    	tiZoomout.addSelectionListener(new SelectionAdapter() {
    	  public void widgetSelected(SelectionEvent e) {
    		  objViewActions.zoomOut();
    	  }
    	});
    	
    	new ToolItem(toolbar, SWT.SEPARATOR);
    	// hot call path
    	this.tiHotCallPath= new ToolItem(toolbar, SWT.PUSH);
    	tiHotCallPath.setToolTipText("Expand the hot path below the selected node");
    	tiHotCallPath.setImage(iconsCollection.imgFlame);
    	tiHotCallPath.addSelectionListener(new SelectionAdapter() {
    	  public void widgetSelected(SelectionEvent e) {
    		  objViewActions.showHotCallPath();
    	  }
    	});
    	
    	new ToolItem(toolbar, SWT.SEPARATOR);
    	
    	this.tiAddExtMetric = new ToolItem(toolbar, SWT.PUSH);
    	tiAddExtMetric.setImage(iconsCollection.imgExtAddMetric);
    	tiAddExtMetric.setToolTipText("Add a new derived metric");
    	tiAddExtMetric.addSelectionListener(new SelectionAdapter(){
    		public void widgetSelected(SelectionEvent e) {
    			objViewActions.addExtNewMetric();
    		}
    	});

    	this.tiColumns = new ToolItem(toolbar, SWT.PUSH);
    	tiColumns.setImage(iconsCollection.imgColumns);
    	tiColumns.setToolTipText("Hide/show columns");
    	tiColumns.addSelectionListener(new SelectionAdapter() {
        	  public void widgetSelected(SelectionEvent e) {
        		  showColumnsProperties();
        	  }
        	});
    	new ToolItem(toolbar, SWT.SEPARATOR);
        // set the coolitem
    	this.createCoolItem(coolbar, toolbar);
    	

    	return toolbar;
    }
    
}
