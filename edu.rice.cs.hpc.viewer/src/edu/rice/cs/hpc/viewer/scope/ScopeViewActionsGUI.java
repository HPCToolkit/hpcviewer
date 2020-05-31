/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.*;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.OSValidator;
import edu.rice.cs.hpc.viewer.metric.MetricColumnDialog;
import edu.rice.cs.hpc.viewer.provider.TableMetricState;
import edu.rice.cs.hpc.viewer.resources.Icons;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.viewer.util.FilterDataItem;
import edu.rice.cs.hpc.viewer.util.Utilities;
import edu.rice.cs.hpc.viewer.window.Database;
import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.IMetricManager;


/**
 * General actions GUI for basic scope views like caller view and calling context view
 * This GUI includes tool bar for zooms, add derived metrics, show/hide columns, and hot call path 
 *
 */
public class ScopeViewActionsGUI implements IScopeActionsGUI {

    // ----------------------------------- CONSTANTS
	final protected Color clrGREEN, clrYELLOW, clrRED, clrNORMAL;
	
    //======================================================
	// ------ DATA ----------------------------------------
    //======================================================
	// GUI STUFFs
    protected ScopeTreeViewer 	treeViewer;		  	// tree for the caller and callees
	protected ScopeViewActions objViewActions;
	protected Shell shell;
	protected IWorkbenchWindow objWindow;

    // variable declaration uniquely for coolbar
	protected ToolItem tiZoomin;		// zoom-in button
	protected ToolItem tiZoomout ;	// zoom-out button
	protected ToolItem tiColumns ;	// show/hide button
	protected ToolItem tiHotCallPath;
	protected ToolItem tiAddExtMetric;
	protected Label lblMessage;
	
	//------------------------------------DATA
	protected Scope nodeTopParent; // the current node which is on the top of the table (used as the aggregate node)
	protected Database 	database;		// experiment data	
	
	private boolean affectOtherViews;

	/**
     * Constructor initializing the data
     * @param shellGUI
     * @param objViewer
     * @param fontMetricColumn
     * @param objActions
     */
	public ScopeViewActionsGUI(Shell objShell, IWorkbenchWindow window, Composite parent, 
			ScopeViewActions objActions) {
		this(objShell, window, parent, objActions, true);
	}
	
	/****
	 * Constructor to create a GUI part of the actions
	 * 
	 * @param objShell : parent shell
	 * @param window : active window
	 * @param parent : parent component
	 * @param objActions : action
	 * @param affectOtherViews : boolean true if an action should trigger other views
	 */
	public ScopeViewActionsGUI(Shell objShell, IWorkbenchWindow window, Composite parent, 
			ScopeViewActions objActions, boolean affectOtherViews) {

		this.objViewActions = objActions;
		this.shell = objShell;
		this.objWindow = window;
		
		this.clrNORMAL = shell.getBackground();
		final Display display = shell.getDisplay();
		
		this.clrYELLOW = display.getSystemColor(SWT.COLOR_YELLOW);
		this.clrRED = display.getSystemColor(SWT.COLOR_RED);
		this.clrGREEN = display.getSystemColor(SWT.COLOR_GREEN);
		
		this.affectOtherViews = affectOtherViews; 
	}

	/**
	 * Method to start to build the GUI for the actions
	 * @param parent
	 * @return toolbar composite
	 */
	public Composite buildGUI(Composite parent, CoolBar coolbar) {
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
	public void updateContent(Experiment exp, RootScope scope) {
		// save the new data and properties
		String sFilename = exp.getDefaultDirectory().getAbsolutePath();
		ViewerWindow vWin = ViewerWindowManager.getViewerWindow(this.objWindow);
		if (vWin == null) {
			System.out.printf("ScopeViewActionsGUI.updateContent: ViewerWindow class not found\n");
			return;
		}
		database = vWin.getDb(sFilename);
	}
	
	
	public void finalizeContent(RootScope root) {
		
		// actions needed when a new experiment is loaded
		//this.resizeTableColumns();	// we assume the data has been populated
        this.enableActions();
        // since we have a new content of experiment, we need to display 
        // the aggregate metrics
    	insertParentNode(root);
	}
	
    //======================================================
    public void setTreeViewer(ScopeTreeViewer tree) {
    	this.treeViewer = tree;
    }

    /**
     * Inserting a "node header" on the top of the table to display
     * either aggregate metrics or "parent" node (due to zoom-in)
     * TODO: we need to shift to the left a little bit
     * @param nodeParent
     */
    public void insertParentNode(Scope nodeParent) {
    	Scope scope = nodeParent;
    	
    	// Bug fix: avoid using list of columns from the experiment
    	// formerly: .. = this.myExperiment.getMetricCount() + 1;
    	TreeColumn []columns = treeViewer.getTree().getColumns();
    	int nbColumns = columns.length; 	// columns in base metrics
    	String []sText = new String[nbColumns];
    	sText[0] = new String(scope.getName());
    	
    	// --- prepare text for base metrics
    	// get the metrics for all columns
    	for (int i=1; i< nbColumns; i++) {
    		// we assume the column is not null
    		Object o = columns[i].getData();
    		if(o instanceof BaseMetric) {
    			BaseMetric metric = (BaseMetric) o;
    			// ask the metric for the value of this scope
    			// if it's a thread-level metric, we will read metric-db file
    			sText[i] = metric.getMetricTextValue(scope);
    		}
    	}
    	
    	// draw the root node item
    	Utilities.insertTopRow(treeViewer, Utilities.getScopeNavButton(scope), sText);
    	this.nodeTopParent = nodeParent;
    }
    
    /**
     * Restoring the "node header" in case of refresh method in the viewer
     */
    @Override
    public void restoreParentNode() {
    	if(this.nodeTopParent != null) {
    		this.insertParentNode(this.nodeTopParent);
    	}
    }
	

	//======================================================
    // ................ GUI and LAYOUT ....................
    //======================================================
	
	/**
	 * Show a message with information style (with green background)
	 */
	public void showInfoMessage(String sMsg) {
		this.lblMessage.setBackground(this.clrGREEN);
		this.lblMessage.setText(sMsg);
	}
	
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
		if(this.lblMessage != null && !lblMessage.isDisposed()) {
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
     * Show column properties (hidden, visible ...)
     */
    protected void showColumnsProperties() {

    	TreeColumn []columns     = treeViewer.getTree().getColumns();    	
		List<BaseMetric> metrics = objViewActions.getMetricManager().getVisibleMetrics();
		if (metrics == null)
			return;
		
		List<FilterDataItem> arrayOfItems = new ArrayList<FilterDataItem>(metrics.size());
		
		for(BaseMetric metric: metrics) {
			
			FilterDataItem item = new FilterDataItem(metric.getDisplayName(), false, false);
			
			// looking for associated metric in the column
			// a metric may not exit in table viewer because
			// it has no metric value (empty metric)
			
			for(TreeColumn column: columns) {
				Object data = column.getData();
				
				if (data != null) {
					BaseMetric m = (BaseMetric) data;
					if (m.equalIndex(metric)) {
						item.enabled = true;
						item.checked = column.getWidth() > 1;
						item.setData(column);
						
						break;
					}
				}
			}
			arrayOfItems.add(item);
		}
				
    	MetricColumnDialog dialog = new MetricColumnDialog(shell, arrayOfItems);
    	dialog.enableAllViewOption(affectOtherViews);
    	if (dialog.open() == Dialog.OK) {
    		boolean isAppliedToAllViews = dialog.isAppliedToAllViews();
    		arrayOfItems = dialog.getResult();
    		
    		boolean []checked = new boolean[arrayOfItems.size()];
    		int i = 0;
    		for (FilterDataItem item : arrayOfItems) {
				checked[i] = item.checked && item.enabled;
				i++;
    		}
    		
    		if (isAppliedToAllViews) {
    			
    			// send message to all registered views, that there is a change of column properties
    			// we don't verify if there's a change or not. Let the view decides what they want to do 
    			
    			final ISourceProviderService service = (ISourceProviderService) objWindow.getService(ISourceProviderService.class);
    			TableMetricState metricStateProvider = (TableMetricState) service.getSourceProvider(TableMetricState.METRIC_COLUMNS_VISIBLE);
    			metricStateProvider.notifyColumnChange(database.getExperiment(), checked);
    			
    		} else {
    			setColumnsStatus(checked);
    		}
    	}
    }
    
    
    
    /**
     * Change the column status (hide/show) in this view only
     * @param status : array of boolean column status based on metrics (not on column).
     *  The number of items in status has to be the same as the number of metrics<br>
     * 	true means the column is shown, hidden otherwise.
     */
    public void setColumnsStatus(boolean []status) {
    	if (treeViewer.getTree().isDisposed())
    		return;
		
		// the number of table columns have to be bigger than the number of status
		// since the table also contains tree scope column
		
		assert status.length == objViewActions.getMetricManager().getMetricCount();

		treeViewer.getTree().setRedraw(false);
    	
		TreeColumn []columns = treeViewer.getTree().getColumns();

		boolean []toShow = new boolean[columns.length];
		int numColumn = 0;
		
		// list of metrics and list of columns are not the same
		// columns only has "enabled" metrics (i.e. metrics that are not null)
		// hence the number of column is always <= number of metrics
		//
		// here we try to correspond between metrics to show and the columns
		
		IMetricManager metricMgr = objViewActions.getMetricManager();
		List<BaseMetric> metrics = metricMgr.getVisibleMetrics();
		int numMetrics = metrics.size();
		
		for (TreeColumn column: columns) {

			Object metric = column.getData();
			if (metric == null || !(metric instanceof BaseMetric))
				continue; // not a metric column
			
			int i=0;			
			for (i=0; i<numMetrics && !metrics.get(i).equalIndex((BaseMetric)metric); i++);
			
			if (i<numMetrics && metrics.get(i).equalIndex((BaseMetric) metric)) {
				toShow[numColumn] = status[i];
				numColumn++;
			}
		}
		TreeColumnLayout layout = (TreeColumnLayout) treeViewer.getTree().getParent().getLayout();
		
		int i = -1; // index of the column
		
		for (TreeColumn column : columns) {
			
			if (column.getData() == null) continue; // not a metric column 
			
			i++;

			int iWidth = 0;
			if (toShow[i]) {
				// display column
				// bug #78: we should keep the original width
				if (column.getWidth() > 1)
					continue; // it's already shown

				if (iWidth <= 0) {
	       			// Laks: bug no 131: we need to have special key for storing the column width
	        		Object o = column.getData(ScopeTreeViewer.COLUMN_DATA_WIDTH);
	       			if((o != null) && (o instanceof Integer) ) {
	       				iWidth = ((Integer)o).intValue();
	       			} else {
		        		iWidth = ScopeTreeViewer.COLUMN_DEFAULT_WIDTH;
	       			}
				}
				// Specific fix for Linux+gtk+ppcle64: need to set the layout here
				// to avoid SWT/GTK to remove the last column
				
				layout.setColumnData(column, new ColumnPixelData(iWidth, true));
			} else {
				// hide column					
				if (column.getWidth() <= 0) 
					continue; // it's already hidden
				
	   			Integer objWidth = Integer.valueOf( column.getWidth() );
	   			
	   			// Laks: bug no 131: we need to have special key for storing the column width
	   			column.setData(ScopeTreeViewer.COLUMN_DATA_WIDTH, objWidth);
	   			
				// need a special treatment for Linux/GTK platform:
				// Explicitly set column pixel into zero due to SWT/GTK implementation that
				// inhibit changes for the last column
				if (OSValidator.isUnix())
					layout.setColumnData(column, new ColumnPixelData(0, false));
			}
			// for other OS other than Linux, we need to set the width explicitly
			// the layout will not take affect until users move or resize columns in the table
			// eclipse bug: forcing to refresh the table has no effect either
			
			column.setWidth(iWidth);
		}
		treeViewer.getTree().setRedraw(true);
    }
    
    

    //======================================================
    // ................ BUTTON ............................
    //======================================================

    /**
     * Disable actions that need a selected node
     */
    public void disableNodeButtons() {
    	this.tiZoomin.setEnabled(false);
    	this.tiZoomout.setEnabled(false);
    	this.tiHotCallPath.setEnabled(false);
    }

    /*
     * (non-Javadoc)
     * @see edu.rice.cs.hpc.viewer.scope.IScopeActionsGUI#enableHotCallPath(boolean)
     */
	public void enableHotCallPath(boolean enabled) {
		this.tiHotCallPath.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.IScopeActionsGUI#enableZoomIn(boolean)
	 */
	public void enableZoomIn(boolean enabled) {
		this.tiZoomin.setEnabled(enabled);
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.IScopeActionsGUI#enableZoomOut(boolean)
	 */
	public void enableZoomOut(boolean enabled) {
		this.tiZoomout.setEnabled(enabled);
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
    
    /*
     * 
     */
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
    protected Composite addTooBarAction(CoolBar coolbar) {
    	// prepare the toolbar
    	ToolBar toolbar = new ToolBar(coolbar, SWT.FLAT);
    	    	
    	// zoom in
    	tiZoomin = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomin.setToolTipText("Zoom-in the selected node");
    	tiZoomin.setImage(Icons.getImage(Icons.Image_ZoomIn));
    	tiZoomin.addSelectionListener(new SelectionAdapter() {
      	  	public void widgetSelected(SelectionEvent e) {
      	  	objViewActions.zoomIn();
      	  	}
      	});
    	
    	// zoom out
    	tiZoomout = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomout.setToolTipText("Zoom-out the selected node");
    	tiZoomout.setImage(Icons.getImage(Icons.Image_ZoomOut));
    	tiZoomout.addSelectionListener(new SelectionAdapter() {
    	  public void widgetSelected(SelectionEvent e) {
    		  objViewActions.zoomOut();
    	  }
    	});
    	
    	new ToolItem(toolbar, SWT.SEPARATOR);
    	// hot call path
    	this.tiHotCallPath= new ToolItem(toolbar, SWT.PUSH);
    	tiHotCallPath.setToolTipText("Expand the hot path below the selected node");
    	tiHotCallPath.setImage(Icons.getImage(Icons.Image_FlameIcon));
    	tiHotCallPath.addSelectionListener(new SelectionAdapter() {
    	  public void widgetSelected(SelectionEvent e) {
    		  objViewActions.showHotCallPath();
    	  }
    	});
    	
    	this.tiAddExtMetric = new ToolItem(toolbar, SWT.PUSH);
    	tiAddExtMetric.setImage(Icons.getImage(Icons.Image_FnMetric));
    	tiAddExtMetric.setToolTipText("Add a new derived metric");
    	tiAddExtMetric.addSelectionListener(new SelectionAdapter(){
    		public void widgetSelected(SelectionEvent e) {
    			objViewActions.addExtNewMetric();
    		}
    	});

    	new ToolItem(toolbar, SWT.SEPARATOR);
    	
    	this.tiColumns = new ToolItem(toolbar, SWT.PUSH);
    	tiColumns.setImage(Icons.getImage(Icons.Image_CheckColumns));
    	tiColumns.setToolTipText("Hide/show columns");
    	tiColumns.addSelectionListener(new SelectionAdapter() {
        	  public void widgetSelected(SelectionEvent e) {
        		  showColumnsProperties();
        	  }
        	});
    	new ToolItem(toolbar, SWT.SEPARATOR);
    	
    	// ------------------------------- export CSV ------
    	ToolItem tiCSV = new ToolItem(toolbar, SWT.PUSH);
    	tiCSV.setImage( Icons.getImage(Icons.Image_SaveCSV) );
    	tiCSV.setToolTipText( "Export the current view into a comma separated value file" );
    	tiCSV.addSelectionListener( new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			exportCSV();
    		}
    	});
    	
    	// ------------ Text fonts
    	// bigger font
    	ToolItem tiFontBigger = new ToolItem (toolbar, SWT.PUSH);
    	tiFontBigger.setImage(Icons.getImage(Icons.Image_FontBigger));
    	tiFontBigger.setToolTipText("Increase font size");
    	tiFontBigger.addSelectionListener( new SelectionAdapter() {
      	  public void widgetSelected(SelectionEvent e) {
      		  Utilities.increaseFont(objWindow);
    	  }
    	});

    	// smaller font
    	ToolItem tiFontSmaller = new ToolItem (toolbar, SWT.PUSH);
    	tiFontSmaller.setImage(Icons.getImage(Icons.Image_FontSmaller));
    	tiFontSmaller.setToolTipText("Decrease font size");
    	tiFontSmaller.addSelectionListener( new SelectionAdapter() {
      	  public void widgetSelected(SelectionEvent e) {
      		  Utilities.DecreaseFont(objWindow);
    	  }
    	});
    	
    	// set the coolitem
    	this.createCoolItem(coolbar, toolbar);
     	return toolbar;
    }

    
    /**
     * Constant comma separator
     */
    final private String COMMA_SEPARATOR = ",";
    
    /**
     * Method to export the displayed items in the current view into a CSV format file
     */
	protected void exportCSV() {
		
		Experiment experiment = database.getExperiment();
		
		FileDialog fileDlg = new FileDialog(this.shell, SWT.SAVE);
		fileDlg.setFileName(experiment.getName() + ".csv");
		fileDlg.setFilterExtensions(new String [] {"*.csv", "*.*"});
		fileDlg.setText("Save the data in the table to a file (CSV format)");
		final String sFilename = fileDlg.open();
		if ( (sFilename != null) && (sFilename.length()>0) ) {
			try {
				this.shell.getDisplay().asyncExec( new Runnable() {

					public void run() {
						try {
							// -----------------------------------------------------------------------
							// Check if the status of the file
							// -----------------------------------------------------------------------
							File objFile = new File( sFilename );
							if ( objFile.exists() ) {
								if ( !MessageDialog.openConfirm( shell, "File already exists" , 
									sFilename + ": file already exist. Do you want to replace it ?") )
									return;
							}
							// WARNING: java.io.File seems always fail to verify writable status on Linux !
							/*
							if ( !objFile.canWrite() ) {
								MessageDialog.openError( shell, "Error: Unable to write the file", 
										sFilename + ": File is not writable ! Please check if you have right to write in the directory." );
								return;
							} */

							// -----------------------------------------------------------------------
							// prepare the file
							// -----------------------------------------------------------------------
							showInfoMessage( "Writing to file: "+sFilename);
							FileWriter objWriter = new FileWriter( objFile );
							BufferedWriter objBuffer = new BufferedWriter (objWriter);
							
							// -----------------------------------------------------------------------
							// writing to the file
							// -----------------------------------------------------------------------
							
							// write the title
							String sTitle = treeViewer.getColumnTitle(0, COMMA_SEPARATOR);
							objBuffer.write(sTitle + Utilities.NEW_LINE);

							// write the top row items
							Object root = treeViewer.getInput();
							if (root instanceof Scope) {
								StringBuffer sb = new StringBuffer();
								objViewActions.saveContent((Scope)root, COMMA_SEPARATOR, sb);
								objBuffer.write(sb.toString());
								objBuffer.write(Utilities.NEW_LINE);
								
							} else {
								String sTopRow[] = Utilities.getTopRowItems(treeViewer);
								// tricky: add '"' for uniting the text in the spreadsheet
								sTopRow[0] = "\"" + sTopRow[0] + "\"";	
								sTitle = treeViewer.getTextBasedOnColumnStatus(sTopRow, COMMA_SEPARATOR, 0, 0);
								objBuffer.write(sTitle + Utilities.NEW_LINE);
							}

							// write the content text
							ArrayList<TreeItem> items = new ArrayList<TreeItem>();
							internalCollectExpandedItems(items, treeViewer.getTree().getItems());
							String sText = objViewActions.getContent( items.toArray(new TreeItem[items.size()]), 
									COMMA_SEPARATOR);
							objBuffer.write(sText);
							
							// -----------------------------------------------------------------------
							// End of the process
							// -----------------------------------------------------------------------							
							objBuffer.close();
							restoreMessage();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				});
			} catch ( SWTException e ) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method is a modified version of AbstractViewer.internalCollectExpandedItems()
	 * @param result
	 * @param items
	 */
	private void internalCollectExpandedItems(List<TreeItem> result, TreeItem []items) {
		if (items != null)
			for (int i = 0; i < items.length; i++) {
				TreeItem itemChild = items[i];
				if (itemChild.getData() instanceof Scope)
					result.add(itemChild);
				if (itemChild.getExpanded())
					internalCollectExpandedItems(result, itemChild.getItems());
			}
	}
	
}
