package edu.rice.cs.hpc.viewer.scope;

// User interface
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;

// SWT
import org.eclipse.swt.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

// Jface
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewerColumn;

// HPC
import edu.rice.cs.hpc.data.experiment.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.viewer.util.EditorManager;
import org.eclipse.swt.graphics.Font;
import edu.rice.cs.hpc.viewer.util.Utilities;

public class ScopeView extends ViewPart {
    public static final String ID = "edu.rice.cs.hpc.scope.ScopeView";

    private TreeViewer 	treeViewer;		  	// tree for the caller and callees
    private TreeViewerColumn colTree;		// column for the calls tree
    private TreeViewerColumn []colMetrics;	// metric columns
    private Experiment 	myExperiment;		// experiment data	
    private Scope 		myRootScope;		// the root scope of this view
    private ColumnViewerSorter sorterTreeColummn;	// sorter for the tree
    private EditorManager editorSourceCode;	// manager to display the source code
    private ScopeTreeContentProvider treeContentProvider;
	private ScopeViewActions objViewActions;	// actions for this scope view
    
    //======================================================
    // ................ HELPER ............................
    //======================================================
    
    
    /**
     * Display the source code of the node in the editor area
     * @param node the current OR selected node
     */
    private void displayFileEditor(Scope.Node node) {
    	if(editorSourceCode == null) {
    		this.editorSourceCode = new EditorManager(this.getSite());
    	}
    	this.editorSourceCode.displayFileEditor(node);
    }

    //======================================================
    // ................ ACTIONS ............................
    //======================================================

    /**
     * Menu action to zoom-in a node
     */
    private Action acZoomin = new Action("Zoom-in"){
    	public void run() {
    		objViewActions.zoomIn();
    	}
    };
    
    /**
     * Menu action to zoom a node
     */
    private Action acZoomout = new Action("Zoom-out"){
    	public void run() {
    		objViewActions.zoomOut();
    	}
    };

    /**
     * Helper method to know if an item has been selected
     * @return true if an item is selected, false otherwise
     */
    private boolean isItemSelected() {
    	return (this.treeViewer.getTree().getSelectionCount() > 0);
    }
    
    /**
     * Helper method to retrieve the selected item
     * @return
     */
    private Scope.Node getSelectedItem() {
        TreeItem[] selection = this.treeViewer.getTree().getSelection();
        return (Scope.Node)selection[0].getData();
    }
    /**
     * Creating the context submenu for the view
     * TODO Created only the line selected
     * @param mgr
     */
    private void fillContextMenu(IMenuManager mgr) {
    	Scope.Node node = this.getSelectedItem();
    	// ---- zoomin
        mgr.add(acZoomin);
        acZoomin.setEnabled(this.objViewActions.shouldZoomInBeEnabled(node));
        // ---- zoomout
        mgr.add(acZoomout);
        acZoomout.setEnabled(this.objViewActions.shouldZoomOutBeEnabled(node));
        // additional feature
        mgr.add(new Separator());
        mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        Scope scope = node.getScope();
        // show the source code
        if(node.hasSourceCodeFile) {
            // show the editor source code
        	String sMenuTitle ;
        	if(scope instanceof FileScope) {
        		sMenuTitle = "Show " + scope.getSourceFile().getName();
        	} else
        		sMenuTitle= "Show "+scope.getToolTip(); // the tooltip contains the info we need: file and the linenum
            mgr.add(new ScopeViewTreeAction(sMenuTitle, node){
                	public void run() {
                		displayFileEditor(this.nodeSelected);
                	}
            });
        }
        // show the call site in case this one exists
        if(scope instanceof CallSiteScope) {
        	CallSiteScope callSiteScope = (CallSiteScope) scope;
        	LineScope lineScope = (LineScope) callSiteScope.getLineScope();
        	// do not show up in the menu context if the callsite does not exist
        	if(Utilities.isFileReadable(lineScope)) {
            	String sMenuTitle = "Callsite "+lineScope.getToolTip();
                mgr.add(new ScopeViewTreeAction(sMenuTitle, lineScope.getTreeNode()){
                	public void run() {
                		displayFileEditor(this.nodeSelected);
                	}
                });
        	}
        }
    }
    
    /**
     * Creating context menu manager
     */
    private void createContextMenu() {
        // Create menu manager.
    	MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
                public void menuAboutToShow(IMenuManager mgr) {
                    if(isItemSelected())
                    	fillContextMenu(mgr);
                }
        });
        
        // Create menu.
        Menu menu = menuMgr.createContextMenu(this.treeViewer.getControl());
        this.treeViewer.getControl().setMenu(menu);
        
        // Register menu for extension.
        getSite().registerContextMenu(menuMgr, this.treeViewer);
    }
    
    /**
     * Actions/menus for Scope view tree.
     * @author laksono
     *
     */
    private class ScopeViewTreeAction extends Action {
    	protected Scope.Node nodeSelected;
    	public ScopeViewTreeAction(String sTitle, Scope.Node nodeCurrent) {
    		super(sTitle);
    		this.nodeSelected = nodeCurrent;
    	}
    	public void setScopeNode(Scope.Node node) {
    		this.nodeSelected = node;
    	}
    }
    
    //===================================================================
    // ---------- VIEW CREATION -----------------------------------------
    //===================================================================
    /**
     * Create the content of the view
     */
    public void createPartControl(Composite aParent) {
		// prepare the font for metric columns: it is supposed to be fixed font
		Display display = Display.getCurrent();
		Utilities.setFontMetric(display);
		
		// Create the actions (flatten, unflatten,...) and the tollbar on top of the table
        this.objViewActions = new ScopeViewActions(this.getViewSite(),
        		aParent); //actions of the tree
        
		// -----
    	treeViewer = new TreeViewer(aParent,SWT.BORDER|SWT.FULL_SELECTION);
    	// set the attributes
    	this.treeContentProvider = new ScopeTreeContentProvider(); 
    	treeViewer.setContentProvider(this.treeContentProvider);
        treeViewer.getTree().setHeaderVisible(true);
        treeViewer.getTree().setLinesVisible(true);

        // tell the action class that we have built the tree
        this.objViewActions.setTreeViewer(treeViewer);
        
        //----------------- create the column tree
        this.colTree = new TreeViewerColumn(treeViewer,SWT.LEFT, 0);
        this.colTree.getColumn().setText("Scope");
        this.colTree.getColumn().setWidth(200); //TODO dynamic size
        this.colTree.setLabelProvider(new ScopeLabelProvider(this.getSite().getWorkbenchWindow())); // laks addendum
        sorterTreeColummn = new ColumnViewerSorter(this.treeViewer, this.colTree.getColumn(), null,0); 

        //-----------------
        // Laks 11.11.07: need this to expand the tree for all view
        GridData data = new GridData(GridData.FILL_BOTH);
        treeViewer.getTree().setLayoutData(data);
        //-----------------
        // create the context menus
        this.createContextMenu();

        //------------------------ LISTENER
		// allow other views to listen for selections in this view (site)
		this.getSite().setSelectionProvider(treeViewer);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event)
		      {
		        IStructuredSelection selection =
		          (IStructuredSelection) event.getSelection();

		        if(selection.getFirstElement() instanceof Scope.Node) {
			        Scope.Node nodeSelected = (Scope.Node) selection.getFirstElement();
			        if(nodeSelected != null) {
			        	// update the state of the toolbar items
			        	objViewActions.checkButtons(nodeSelected);
						if(nodeSelected.hasSourceCodeFile)
							displayFileEditor(nodeSelected);
			        }
		        } else {
		        	// selection on wrong node
		        }
		      }
		});
		
	}
	
    //======================================================
    // ................ UPDATE ............................
    //======================================================
    // laks: we need experiment and rootscope
    /**
     * Update the data input for Scope View, depending also on the scope
     */
    public void setInput(Experiment ex, RootScope scope) {
    	myExperiment = ex;
    	myRootScope = scope;// try to get the aggregate value
    	updateDisplay();
    }
    
    /**
     * Update the data view based on the XML experiment data
     * @param ex
     */
    public void setInput(Experiment ex) {
    	myExperiment = ex;
    	if (ex != null) {
    		myRootScope = ex.getRootScope();
    	}
    	updateDisplay();
    }

	/**
	 * Update the content of the tree view
	 */
	private void updateDisplay() {
        if (myExperiment == null)
        	return;
        int iColCount = this.treeViewer.getTree().getColumnCount();
        if(iColCount>1) {
        	// remove the columns blindly
        	// TODO we need to have a more elegant solution here
        	for(int i=1;i<iColCount;i++) {
        		this.treeViewer.getTree().getColumn(1).dispose();
        	}
        }
        // prepare the data for the sorter class for tree
        this.sorterTreeColummn.setMetric(myExperiment.getMetric(0));
        // prepare the experiment for the content provider of the tree column
        this.treeContentProvider.setExperiment(myExperiment);
        // dirty solution to update titles
        this.colMetrics = new TreeViewerColumn[myExperiment.getMetricCount()];
        {
            // Update metric title labels
            String[] titles = new String[myExperiment.getMetricCount()+1];
            titles[0] = "Scope";	// unused element. Already defined
            // add table column for each metric
        	for (int i=0; i<myExperiment.getMetricCount(); i++)
        	{
        		titles[i+1] = myExperiment.getMetric(i).getDisplayName();	// get the title
        		colMetrics[i] = new TreeViewerColumn(treeViewer,SWT.LEFT);	// add column
        		colMetrics[i].getColumn().setText(titles[i+1]);	// set the title
        		colMetrics[i].getColumn().setWidth(120); //TODO dynamic size
        		colMetrics[i].getColumn().setAlignment(SWT.RIGHT);
        		
        		// laks: addendum for column        		
        		this.colMetrics[i].setLabelProvider(new MetricLabelProvider( 
        				myExperiment.getMetric(i), Utilities.fontMetric));
        		this.colMetrics[i].getColumn().setMoveable(true);
        		//tmp.pack();			// resize as much as possible
        		ColumnViewerSorter colSorter = new ColumnViewerSorter(this.treeViewer, 
        				colMetrics[i].getColumn(), myExperiment.getMetric(i),i+1); // sorting mechanism
        		if(i==0)
        			colSorter.setSorter(colSorter, ColumnViewerSorter.ASC); // laks: by default, the first
        						// column will be sorted here, instead of initializing inside the sort class.
        		
        	}
            treeViewer.setColumnProperties(titles);
        }
        
        // Update root scope
        treeViewer.setInput(myRootScope.getTreeNode());
        //treeViewer.setInput(myRootScope.getTreeNode().getChildAt(0));

        // update the window title
        this.getSite().getShell().setText("hpcviewer: "+myExperiment.getName());
        
        // generate flattening structure 
        if(((RootScope)this.myRootScope).getType() == RootScopeType.Flat) {
            ((RootScope)this.myRootScope).createFlattenNode();
        }
        // update the root scope of the actions !
        this.objViewActions.updateContent(this.myExperiment, this.myRootScope, this.colMetrics);
   	}

    //======================================================
    // ................ MISC ............................
    //======================================================
	/**
	 * Modify the title of the view
	 * @param sName
	 */
	public void setViewTitle(String sName) {
		super.setPartName(sName);
	}
    public void setFocus() {
            treeViewer.getTree().setFocus();
    }
    

}
