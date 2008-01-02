package edu.rice.cs.hpc.viewer.scope;

// User interface
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;

// SWT
import org.eclipse.swt.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

// Jface
import org.eclipse.jface.viewers.*;
//import org.eclipse.core.runtime.Platform;
//import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.Action;

// HPC
import edu.rice.cs.hpc.data.experiment.*;
import edu.rice.cs.hpc.data.experiment.source.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.pnode.*;
import edu.rice.cs.hpc.viewer.resources.*;
import edu.rice.cs.hpc.viewer.util.EditorManager;

public class ScopeView extends ViewPart {
    public static final String ID = "edu.rice.cs.hpc.scope.ScopeView";

    private TreeViewer 	treeViewer;		  	// tree for the caller and callees
    private TreeColumn []tcMetricColumns; 	// metric columns
    private Experiment 	myExperiment;		// experiment data	
    private PNode[] 	myPNodes;
    private Scope 		myRootScope;		// the root scope of this view
    private ColumnViewerSorter sorterTreeColummn;
    private EditorManager editorSourceCode;
	
    //======================================================
    // ................ HELPER ............................
    //======================================================
    /**
     * Retrieve the short file name of the node (based on the information from the scope)
     */
    private String getFilename(Scope.Node node) {
    	return node.getScope().getSourceFile().getName();
    }
    
    /**
     * See whether the node has the information of the file name of the code
     *
     * @param node
     * @return true if the information exist, false otherwise
     */
    private boolean isSourceCodeAvailable(Scope.Node node) {
		return (node.getScope().getSourceFile() != SourceFile.NONE
				|| node.getScope().getSourceFile().isAvailable());
    }
    
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
	 * Action for double click in the view: show the file source code if possible
	 */
	private IDoubleClickListener dblListener = new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent event) {
			if (!(event.getSelection() instanceof StructuredSelection))
				return;
			StructuredSelection sel = (StructuredSelection) event.getSelection();
			Scope.Node node = (Scope.Node) sel.getFirstElement();
			// check if the source code is available
			if (node.getScope().getSourceFile() == SourceFile.NONE
				|| !node.getScope().getSourceFile().isAvailable())
				return;
			displayFileEditor(node);
		}
	};

	
	/**
	 * Go deeper one level
	 */
	private void flattenNode() {
		Object objRoot = this.treeViewer.getTree().getItem(0).getData();
		Scope.Node objNode = (Scope.Node) objRoot;
		if (objRoot instanceof Scope.Node) {
			// original scope node
			objNode = (Scope.Node) objRoot;
		} else if(objRoot instanceof ArrayOfNodes) {
			// this item has been flatten. we will flatten again based on the first item
			ArrayOfNodes objArrays = (ArrayOfNodes) objRoot;
			objNode = objArrays.get(0);
		} else {
			// unknown class. this is almost impossible unless the code is really messy or we have bugs
			System.err.println("ScopeView-Flatten err unknown object:"+objRoot.getClass());
			return;
		}
		// get the reference node
		Scope.Node node = (Scope.Node) objNode;
		Integer objLevel = Integer.valueOf(node.iLevel+1);
		// get the next level
		ArrayOfNodes nodeArray = ((RootScope)this.myRootScope).getTableOfNodes().get(objLevel);
		if(nodeArray != null) {
			this.treeViewer.setInput(nodeArray);
			treeViewer.refresh();		
		} else {
			// there is something wrong. we return to the original node
			//treeViewer.setInput(node);
			System.err.println("ScopeView-flatten: error cannot flatten further");
		}
	}
	
	/**
	 * go back one level
	 */
	private void unflattenNode() {
		Object objRoot = this.treeViewer.getTree().getItem(0).getData();
		Scope.Node objNode = (Scope.Node) objRoot;
		if (objRoot instanceof Scope.Node) {
			// original scope node
			objNode = (Scope.Node) objRoot;
		} else if(objRoot instanceof ArrayOfNodes) {
			// this item has been flatten. we will flatten again based on the first item
			ArrayOfNodes objArrays = (ArrayOfNodes) objRoot;
			objNode = objArrays.get(0);
		} else {
			// unknown class. this is almost impossible unless the code is really messy or we have bugs
			System.err.println("ScopeView-Flatten err unknown object:"+objRoot.getClass());
			return;
		}
		// get the reference node
		Scope.Node node = (Scope.Node) objNode;
		Integer objLevel = Integer.valueOf(node.iLevel-1);
		ArrayOfNodes nodeArray = ((RootScope)this.myRootScope).getTableOfNodes().get(objLevel);
		if(nodeArray != null) {
			this.treeViewer.setInput(nodeArray);
		} else {
			//Scope.Node nodeFlatten = node.tryFlatten();
			treeViewer.setInput(node);
		}
		//treeViewer.setInput((Scope.Node)node.getParent());
		treeViewer.refresh();
	}
	
	/**
	 * Zoom-in the children
	 */
	private void zoomIn() {
		ISelection sel = treeViewer.getSelection();
		if (!(sel instanceof StructuredSelection))
			return;
		Object o = ((StructuredSelection)sel).getFirstElement();
		if (!(o instanceof Scope.Node)) {
			System.err.println("ScopeView - zoomin:"+o.getClass());
			return;
		}
		treeViewer.setInput(o);
		treeViewer.refresh();
	}
	
	/**
	 * Zoom-out the node
	 */
	private void zoomOut() {
		Object o = treeViewer.getInput();
		Scope.Node child;
		if (!(o instanceof Scope.Node)) {
			if(o instanceof ArrayOfNodes) {
				TreeItem []tiObjects = this.treeViewer.getTree().getItems();
				child = (Scope.Node)tiObjects[0].getData();
				// tricky solution when zoom-out the flattened node
				child = (Scope.Node)child.getParent();
			} else {
				System.err.println("ScopeView - zoomout:"+o.getClass());
				return;
			}
		} else
			child = (Scope.Node) o;
		Scope.Node parent = (Scope.Node)child.getParent();
		if (parent == null)
			return;
		treeViewer.setInput( parent );
		treeViewer.refresh();
	}
	
	/**
	 * Resize the columns automatically
	 * ATT: Please call this method once the data has been populated
	 */
	private void resizeTableColumns() {
        // resize the column according to the data size
        for (int i=0; i<myExperiment.getMetricCount(); i++) {
        	tcMetricColumns[i].pack();
        }
	}

	//======================================================
    // ................ GUI and LAYOUT ....................
    //======================================================
	
	//------------------------------------DATA

	private ToolItem tiFlatten;
	private ToolItem tiUnFlatten ;
	private ToolItem tiZoomin;
	private ToolItem tiZoomout ;
	private ToolItem tiResize ;
	
/*	private Action acFlatten = new Action("Flatten"){
    	public void run() {
    		//zoomIn();
    		flattenNode();
    	}
    };
    
    private Action acUnflatten = new Action("Unflatten"){
    	public void run() {
    		//zoomOut();
    		unflattenNode();
    	}
    };*/
    
    private Action acZoomin = new Action("Zoom-in"){
    	public void run() {
    		zoomIn();
    	}
    };
    
    private Action acZoomout = new Action("Zoom-out"){
    	public void run() {
    		zoomOut();
    	}
    };
	/**
	 * Reset the button and actions into disabled state
	 */
	public void resetActions() {
		this.tiFlatten.setEnabled(false);
		this.tiUnFlatten.setEnabled(false);
		this.tiZoomin.setEnabled(false);
		this.tiZoomout.setEnabled(false);
	}
	
	/**
	 * Enable the actions for this view
	 */
	public void enableActions() {
		this.tiFlatten.setEnabled(true);
		this.tiUnFlatten.setEnabled(true);
		this.tiZoomin.setEnabled(true);
		this.tiZoomout.setEnabled(true);
	}
    /**
     * Create a toolbar region on the top of the view. This toolbar will be used to host some buttons
     * to make actions on the treeview.
     * @param aParent
     * @return Composite of the view. The tree should be based on this composite.
     */
    private Composite createCoolBar(Composite aParent) {
    	// make the parent with grid layout
    	GridLayout grid = new GridLayout(1,false);
    	aParent.setLayout(grid);
    	CoolBar coolBar = new CoolBar(aParent, SWT.FLAT);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
    	coolBar.setLayoutData(data);
    	// prepare the toolbar
    	ToolBar toolbar = new ToolBar(coolBar, SWT.FLAT);
    	Icons iconsCollection = Icons.getInstance();
    	
    	// ------------- prepare the items
    	// flatten
    	tiFlatten = new ToolItem(toolbar, SWT.PUSH);
    	tiFlatten.setToolTipText("Flatten the node");
    	tiFlatten.setImage(iconsCollection.imgFlatten);
    	tiFlatten.addSelectionListener(new SelectionAdapter() {
      	  	public void widgetSelected(SelectionEvent e) {
      	  		flattenNode();
      	  	}
      	});
    	
    	// unflatten
    	tiUnFlatten = new ToolItem(toolbar, SWT.PUSH);
    	tiUnFlatten.setToolTipText("Unflatten the node");
    	tiUnFlatten.setImage(iconsCollection.imgUnFlatten);
    	tiUnFlatten.addSelectionListener(new SelectionAdapter(){
      	  	public void widgetSelected(SelectionEvent e) {
      	  	unflattenNode();
      	  	}    		
    	});
    	
    	// zoom in
    	tiZoomin = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomin.setToolTipText("Zoom-in");
    	tiZoomin.setImage(iconsCollection.imgZoomIn);
    	tiZoomin.addSelectionListener(new SelectionAdapter() {
      	  	public void widgetSelected(SelectionEvent e) {
      	  	zoomIn();
      	  	}
      	});
    	
    	// zoom out
    	tiZoomout = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomout.setToolTipText("Zoom-out");
    	tiZoomout.setImage(iconsCollection.imgZoomOut);
    	tiZoomout.addSelectionListener(new SelectionAdapter() {
    	  public void widgetSelected(SelectionEvent e) {
    		  zoomOut();
    	  }
    	});
    	
    	tiResize = new ToolItem(toolbar, SWT.PUSH);
    	tiResize.setToolTipText("Resize columns width");
    	tiResize.setImage(iconsCollection.imgResize);
    	tiResize.addSelectionListener(new SelectionAdapter() {
      	  public void widgetSelected(SelectionEvent e) {
          	resizeTableColumns();
      	  }
      	});
    	// set the coolitem
    	this.createCoolItem(coolBar, toolbar);
    	this.resetActions();
	    return aParent;
    }
    
    //======================================================
    // ................ CREATION ............................
    //======================================================
    /**
     * Creating an item for the existing coolbar
     * @param coolBar
     * @param toolBar
     */
    private void createCoolItem(CoolBar coolBar, ToolBar toolBar) {
    	CoolItem coolItem = new CoolItem(coolBar, SWT.NULL);
    	coolItem.setControl(toolBar);
    	org.eclipse.swt.graphics.Point size =
    		toolBar.computeSize( SWT.DEFAULT,
    	                           SWT.DEFAULT);
    	org.eclipse.swt.graphics.Point coolSize = coolItem.computeSize (size.x, size.y);
    	coolItem.setSize(coolSize);    	
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

    private boolean shouldZoomInBeEnabled(Scope.Node node) {
    	return (node.getChildCount()>0);
    }
    
    private boolean shouldZoomOutBeEnabled(Scope.Node node) {
    	return (node.iLevel>1);
    }
    
    private boolean shouldFlattenBeEnabled(Scope.Node node) {
    	return (!node.isLeaf());
    }
    
    private boolean shouldUnflattenBeEnabled(Scope.Node node) {
    	return (node.iLevel>1);
    }
    /**
     * Helper method to know if an item has been selected
     * @return true if an item is selected, false otherwise
     */
    public boolean isItemSelected() {
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
    	//acFlatten.setEnabled(this.isFlattenShouldbeEnabled());
    	Scope.Node node = this.getSelectedItem();
//        mgr.add(acFlatten);
//        acFlatten.setEnabled(this.isFlattenShouldbeEnabled(node));
//        mgr.add(acUnflatten);
//        acUnflatten.setEnabled(this.isUnflattenShouldbeEnabled(node));
        mgr.add(acZoomin);
        acZoomin.setEnabled(this.shouldZoomInBeEnabled(node));
        mgr.add(acZoomout);
        acZoomout.setEnabled(this.shouldZoomOutBeEnabled(node));
        mgr.add(new Separator());
        mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        if (this.isSourceCodeAvailable(node)) {
            mgr.add(new ScopeViewTreeAction("Show "+this.getFilename(node), node){
            	public void run() {
            		displayFileEditor(this.nodeSelected);
            	}
            });
        }
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
    /**
     * Create the content of the view
     */
    public void createPartControl(Composite aParent) {
		// ----- coolbar
    	Composite parent = this.createCoolBar(aParent);

		// -----
    	treeViewer = new TreeViewer(parent);
        //treeViewer = new CommonViewer(parent, SWT.SINGLE|SWT.FULL_SELECTION | SWT.BORDER);
        treeViewer.setContentProvider(new ScopeTreeContentProvider());
        treeViewer.setLabelProvider(new ScopeTreeLabelProvider());
        
        treeViewer.getTree().setHeaderVisible(true);
        treeViewer.getTree().setLinesVisible(true);

        TreeColumn tmp = new TreeColumn(treeViewer.getTree(),SWT.LEFT, 0);
        tmp.setText("Scope");
        tmp.setWidth(200); //TODO dynamic size
        sorterTreeColummn = new ColumnViewerSorter(this.treeViewer, tmp, null,0); 
        
        //-----------------
        // Laks 11.11.07: need this to expand the tree for all view
        GridData data = new GridData(GridData.FILL_BOTH);
        treeViewer.getTree().setLayoutData(data);
        //-----------------
        this.createContextMenu();

        treeViewer.setInput(null);
        
		// allow other views to listen for selections in this view (site)
		this.getSite().setSelectionProvider(treeViewer);
				
		treeViewer.addDoubleClickListener(dblListener);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener(){
			public void selectionChanged(SelectionChangedEvent event)
		      {
		        IStructuredSelection selection =
		          (IStructuredSelection) event.getSelection();

		        Scope.Node nodeSelected = (Scope.Node) selection.getFirstElement();
		        if(nodeSelected != null) {
		        	/*System.out.println("ScopeView: select "+nodeSelected.getScope().getName()+
		        			" level:"+nodeSelected.iLevel + " children:"+nodeSelected.getChildCount());
		        	*/
		        	tiZoomout.setEnabled(shouldZoomOutBeEnabled(nodeSelected));
		        	tiZoomin.setEnabled(shouldZoomInBeEnabled(nodeSelected));
		        	tiFlatten.setEnabled(shouldFlattenBeEnabled(nodeSelected));
		        	tiUnFlatten.setEnabled(shouldUnflattenBeEnabled(nodeSelected));
		        }
		      }
		});
		//makeActions();
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
    	myRootScope = scope; //.getParentScope(); // try to get the aggregate value
    	updateDisplay();
    }
    
    /**
     * Update the data view based on the XML experiment data
     * @param ex
     */
    public void setInput(Experiment ex) {
    	myExperiment = ex;
    	if (ex != null) {
    		//myPNodes = ex.getPNodes();
    		myRootScope = ex.getRootScope();
    		System.err.println("Experiment is not null");
    		//TODO myFocusScope = ex.getRootScope();
    	}
    	updateDisplay();
    	//this.myGlobalData = ExperimentData.getInstance();
    	//this.myGlobalData.setExperiment(this.myExperiment);
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
        this.sorterTreeColummn.setMetric(myExperiment.getMetric(0));
        // dirty solution to update titles
        this.tcMetricColumns = new TreeColumn[myExperiment.getMetricCount()];
        {
            // Update metric title labels
            String[] titles = new String[myExperiment.getMetricCount()+1];
            titles[0] = "Scope";	// unused element. Already defined
            // add table column for each metric
        	for (int i=0; i<myExperiment.getMetricCount(); i++)
        	{
        		titles[i+1] = myExperiment.getMetric(i).getDisplayName();	// get the title
        		tcMetricColumns[i] = new TreeColumn(treeViewer.getTree(),SWT.LEFT, i+1);	// add column
        		tcMetricColumns[i].setText(titles[i+1]);	// set the title
        		tcMetricColumns[i].setWidth(120); //TODO dynamic size
        		//tmp.pack();			// resize as much as possible
        		new ColumnViewerSorter(this.treeViewer, tcMetricColumns[i], myExperiment.getMetric(i),i+1); // sorting mechanism
        		
        	}
            treeViewer.setColumnProperties(titles);
        }
        
        // Update metric value table
        ((ScopeTreeLabelProvider)treeViewer.getLabelProvider()).
        		setMetrics(myExperiment.getMetrics());
        
        // Update active pnodes
        ((ScopeTreeLabelProvider)treeViewer.getLabelProvider()).
        		setPNodes(myPNodes);

        // Update root scope
        treeViewer.setInput(myRootScope.getTreeNode());

        // update the window title
        this.getSite().getShell().setText("HPCViewer: "+myExperiment.getName());
    	resizeTableColumns();
        // refresh the content
        treeViewer.refresh();
        
        // generate flattening structure 
        ((RootScope)this.myRootScope).createFlattenNode();
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
