package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.Action;
//import org.eclipse.ui.views.contentoutline.*;

//import org.eclipse.swt.widgets.CoolBar;
//import org.eclipse.swt.widgets.CoolItem;

import edu.rice.cs.hpc.data.experiment.*;
import edu.rice.cs.hpc.data.experiment.source.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.data.experiment.pnode.*;

public class ScopeView extends ViewPart {
    public static final String ID = "edu.rice.cs.hpc.scope.ScopeView";

    private TreeViewer 	treeViewer;		  	// tree for the caller and callees
    private TreeColumn []tcMetricColumns; 	// metric columns
    private Experiment 	myExperiment;		// experiment data	
    private PNode[] 	myPNodes;
    private Scope 		myRootScope;		// the root scope of this view
    private ColumnViewerSorter sorterTreeColummn;
	
    //======================================================
    // ................ ACTIONS ............................
    //======================================================
    private String getFilename(Scope.Node node) {
    	return node.getScope().getSourceFile().getName();
    }
    
    private boolean isSourceCodeAvailable(Scope.Node node) {
		return (node.getScope().getSourceFile() != SourceFile.NONE
				|| node.getScope().getSourceFile().isAvailable());
    }
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
	 * Open and Display editor
	 * @param node
	 */
	private void displayFileEditor(Scope.Node node) {
		// get the complete file name
		FileSystemSourceFile newFile = ((FileSystemSourceFile)node.getScope().getSourceFile());
		if(newFile!=null) {
			if(newFile.isAvailable()) {
				String sLongName;
				sLongName = newFile.getCompleteFilename();
				int iLine = node.getScope().getFirstLineNumber();
				openFileEditor( sLongName, newFile.getName(), iLine );
			} else
				System.out.println("Source file not available"+ ":"+ "("+newFile.getName()+")");
			// laks: try to show the editor
		} else
			System.err.println("ScopeView-displayFileEditor:"+node.getScope().getShortName());
	}
	/**
	 * Open Eclipse IDE editor for a given filename. 
	 * Beware: for Eclipse 3.2, we need to create a "hidden" project of the file
	 * 			this project should be cleaned in the future !
	 * @param sFilename the complete path of the file to display in IDE
	 */
	private void openFileEditor(String sLongFilename, String sFilename, int iLineNumber) {
		// get the complete path of the file
		org.eclipse.core.filesystem.IFileStore objFile = 
			org.eclipse.core.filesystem.EFS.getLocalFileSystem().getStore(new 
					org.eclipse.core.runtime.Path(sLongFilename).removeLastSegments(1));
		// get the active page for the editor
		org.eclipse.ui.IWorkbenchPage wbPage = this.getSite().getWorkbenchWindow().getActivePage();
		if(wbPage != null ){
			objFile=objFile.getChild(sFilename);
	    	if(!objFile.fetchInfo().exists()) {
	    		 MessageDialog.openInformation(this.getSite().getShell(), "File not found", 
	    		 	sFilename+": File cannot be opened in " + objFile.getName());
	    		 return; // do we need this ?
	    	}
	    	try {
	    		IEditorPart objEditor = org.eclipse.ui.ide.IDE.openEditorOnFileStore(wbPage, objFile);
	    		/*IContentOutlinePage outlinePage = (IContentOutlinePage) objEditor.getAdapter(IContentOutlinePage.class);
	    		 if (outlinePage != null) {
	    		    // editor wishes to contribute outlinePage to content outline view
	    			 
	 	    		IViewPart objOutlineView = wbPage.showView("org.eclipse.ui.views.ContentOutline");
	 	    		wbPage.showView(this.ID);
	 	    		this.setFocus();	 	    		
	 	    		this.treeViewer.getTree().setFocus();
	    		 }
		    	System.out.println(" ScopeView: " + objEditor.getClass() + " outline: "+ outlinePage.getClass());
		    	*/
	    		this.setEditorMarker(wbPage, iLineNumber);
	    	} catch (PartInitException e) {
	    		e.printStackTrace();
	    		MessageDialog.openError(this.getSite().getShell(), "Error opening the file", e.getMessage());
	       /* some code */
	     }
		}
	}

	/**
	 * Set the marker into the active editor
	 * @param wbPage
	 * @param iLineNumber
	 */
	private void setEditorMarker(org.eclipse.ui.IWorkbenchPage wbPage, int iLineNumber) {
	       //IFile file;
	       try{
	    	   IResource resource = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot();
	    	   IMarker marker=resource.createMarker("HPCViewer"); 
			   marker.setAttribute(IMarker.LINE_NUMBER, iLineNumber+1);
			   marker.setAttribute(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_INFO));
			   org.eclipse.ui.ide.IDE.gotoMarker(wbPage.getActiveEditor(), marker);
	    	   
	       } catch (org.eclipse.core.runtime.CoreException e) {
	    	   e.printStackTrace();
	       }

	}
	
	/**
	 * Go deeper one level
	 */
	private void flattenNode() {
		ISelection sel = treeViewer.getSelection();
		if (!(sel instanceof StructuredSelection))
			return;
		Object o = ((StructuredSelection)sel).getFirstElement();
		if (!(o instanceof Scope.Node))
			return;
		Scope.Node node = (Scope.Node) o;
		Integer objLevel = Integer.valueOf(node.iLevel+1);
		ArrayOfNodes nodeArray = ((RootScope)this.myRootScope).getTableOfNodes().get(objLevel);
		if(nodeArray != null) {
			this.treeViewer.setInput(nodeArray);
		} else {
			//Scope.Node nodeFlatten = node.tryFlatten();
			treeViewer.setInput(node);
		}
		treeViewer.refresh();		
	}
	
	/**
	 * go back one level
	 */
	private void unflattenNode() {
		ISelection sel = treeViewer.getSelection();
		if (!(sel instanceof StructuredSelection))
			return;
		Object o = ((StructuredSelection)sel).getFirstElement();
		if (!(o instanceof Scope.Node))
			return;
		Scope.Node node = (Scope.Node) o;
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
    // ................ GUI ............................
    //======================================================
	private ToolItem tiFlatten;
	private ToolItem tiUnFlatten ;
	private ToolItem tiZoomin;
	private ToolItem tiZoomout ;
	private ToolItem tiResize ;
    /**
     * Create a toolbar region on the top of the view. This toolbar will be used to host some buttons
     * to make actions on the treeview.
     * @param aParent
     * @return Composite of the view. The tree should be based on this composite.
     */
    private Composite createCoolBar(Composite aParent) {
    	// make the parent with grid layout
    	org.eclipse.swt.layout.GridLayout grid = new org.eclipse.swt.layout.GridLayout(1,false);
    	aParent.setLayout(grid);
    	CoolBar coolBar = new CoolBar(aParent, SWT.FLAT);
        org.eclipse.swt.layout.GridData data = new org.eclipse.swt.layout.GridData(GridData.FILL_HORIZONTAL);
    	coolBar.setLayoutData(data);
    	// prepare the toolbar
    	org.eclipse.swt.widgets.ToolBar toolbar = new ToolBar(coolBar, SWT.FLAT);
    	edu.rice.cs.hpc.viewer.resources.Icons iconsCollection = edu.rice.cs.hpc.viewer.resources.Icons.getInstance();
    	
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
        mgr.add(new Action("Flatten"){
        	public void run() {
        		//zoomIn();
        		flattenNode();
        	}
        });
        mgr.add(new Action("Unflatten"){
        	public void run() {
        		zoomOut();
        		//unflattenNode();
        	}
        });
        mgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        mgr.add(new Action("Zoom-in"){
        	public void run() {
        		zoomIn();
        	}
        });
        mgr.add(new Action("Zoom-out"){
        	public void run() {
        		zoomOut();
        	}
        });

        mgr.add(new Separator());
        Scope.Node node = this.getSelectedItem();
        if (this.isSourceCodeAvailable(node)) {
        	System.out.println("Selected node:"+node.getScope().getShortName());
            mgr.add(new ScopeViewTreeAction(this.getFilename(node), node){
            	public void run() {
            		displayFileEditor(this.nodeSelected);
            	}
            });
        }
    }
    
    private class ScopeViewTreeAction extends Action {
    	protected Scope.Node nodeSelected;
    	public ScopeViewTreeAction(String sTitle, Scope.Node nodeCurrent) {
    		super(sTitle);
    		this.nodeSelected = nodeCurrent;
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
        org.eclipse.swt.layout.GridData data = new org.eclipse.swt.layout.GridData(GridData.FILL_BOTH);
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
			        //int nbChildren = nodeSelected.getChildCount();
			        //System.err.println(this.getClass()+"->"+nodeSelected.getLevel()+" has "+nbChildren);
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
        //((RootScope)this.myRootScope).generateFlatteningStructure();
        ((RootScope)this.myRootScope).createFlattenNode();
        //((RootScope)this.myRootScope).printFlattenNodes();
	}

    //======================================================
    // ................ SORTING ............................
    //======================================================

	private static class ColumnViewerSorter extends ViewerComparator {
		public static final int ASC = 1;
		public static final int NONE = 0;
		public static final int DESC = -1;
		private int direction = 0;
		private TreeColumn column;
		private ColumnViewer viewer;
		private int iColNumber;
		private Metric metric;
		
		/**
		 * Update the metric for this column
		 * @param newMetric
		 */
		public void setMetric(Metric newMetric) {
			this.metric = newMetric;
		}
		/**
		 * Class to sort a column
		 * @param viewer: the table tree
		 * @param column: the column
		 * @param newMetric: the metric
		 * @param colNum: the position
		 */
		public ColumnViewerSorter(ColumnViewer viewer, TreeColumn column, Metric newMetric, int colNum) {
			this.column = column;
			this.iColNumber = colNum;
			this.viewer = viewer;
			this.metric = newMetric;
			this.column.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					if( ColumnViewerSorter.this.viewer.getComparator() != null ) {
						if( ColumnViewerSorter.this.viewer.getComparator() == ColumnViewerSorter.this ) {
							int tdirection = ColumnViewerSorter.this.direction;
							
							if( tdirection == ASC ) {
								setSorter(ColumnViewerSorter.this, DESC);
							} else if( tdirection == DESC ) {
								setSorter(ColumnViewerSorter.this, NONE);
							}
						} else {
							setSorter(ColumnViewerSorter.this, ASC);
						}
					} else {
						setSorter(ColumnViewerSorter.this, ASC);
					}
				}
			});
		}
		
		/**
		 * Sort the column according to the direction
		 * @param sorter
		 * @param direction
		 */
		public void setSorter(ColumnViewerSorter sorter, int direction) {
			if( direction == NONE ) {
				column.getParent().setSortColumn(null);
				column.getParent().setSortDirection(SWT.NONE);
				viewer.setComparator(null);
			} else {
				column.getParent().setSortColumn(column);
				sorter.direction = direction;
				
				if( direction == ASC ) {
					column.getParent().setSortDirection(SWT.DOWN);
				} else {
					column.getParent().setSortDirection(SWT.UP);
				}
				
				if( viewer.getComparator() == sorter ) {
					viewer.refresh();
				} else {
					viewer.setComparator(sorter);
				}
				
			}
		}

		/**
		 * general comparison for sorting
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			return direction * doCompare(viewer, e1, e2);
		}
		
		// laks: lazy comparison
		/**
		 * This method is to compare one object to another
		 * Please implement this method in the child class if necessary
		 */
		protected int doCompare(Viewer viewer, Object e1, Object e2) {
			if(e1 instanceof Scope.Node && e2 instanceof Scope.Node) {
				Scope.Node node1 = (Scope.Node) e1;
				Scope.Node node2 = (Scope.Node) e2;

				// dirty solution: if the column position is 0 then we sort
				// according to its element name
				// otherwise, sort according to the metric
				if(this.iColNumber==0) {
					String text1 = node1.getScope().getShortName();
					String text2 = node2.getScope().getShortName();
					return text1.compareTo(text2);
				} else {
					// get the metric
					MetricValue mv1 = node1.getScope().getMetricValue(metric);
					MetricValue mv2 = node2.getScope().getMetricValue(metric);
					
					if (mv1.getValue()>mv2.getValue()) return -1;
					if (mv1.getValue()<mv2.getValue()) return 1;
				}
			}
			return 0;
		}
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
