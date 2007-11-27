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
import org.eclipse.jface.resource.ImageDescriptor;
//import org.eclipse.swt.widgets.CoolBar;
//import org.eclipse.swt.widgets.CoolItem;

import edu.rice.cs.hpc.data.experiment.*;
import edu.rice.cs.hpc.data.experiment.source.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.data.experiment.pnode.*;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeFilter;

public class ScopeView extends ViewPart {
    public static final String ID = "edu.rice.cs.hpc.scope.ScopeView";

    private final String ICONPATH="../../../../../../../icons/";
    private TreeViewer 	treeViewer;
    private Experiment 	myExperiment;
    private PNode[] 	myPNodes;
    private Scope 		myRootScope;
    
    private ScopeTreeFilter treeFilter;

	/**
	 * Action for double click in the view: show the file source code if possible
	 */
	IDoubleClickListener dblListener = new IDoubleClickListener() {
		public void doubleClick(DoubleClickEvent event) {
			if (!(event.getSelection() instanceof StructuredSelection))
				return;
			StructuredSelection sel = (StructuredSelection) event.getSelection();
			Scope.Node node = (Scope.Node) sel.getFirstElement();
			// check if the source code is available
			if (node.getScope().getSourceFile() == SourceFile.NONE
				|| !node.getScope().getSourceFile().isAvailable())
				return;
			// get the complete file name
			FileSystemSourceFile newFile = ((FileSystemSourceFile)node.getScope().getSourceFile());
			String sLongName= newFile.getCompleteFilename();
			System.out.println("Loading source file "+ ":"+ sLongName + "("+newFile.getName()+")");
			// laks: try to show the editor
			int iLine = node.getScope().getFirstLineNumber();
			openFileEditor( sLongName, newFile.getName(), iLine );
		}
	};
	
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
	    		System.out.println("openEditorOnFileStore:"+objFile.toString());
	    		org.eclipse.ui.ide.IDE.openEditorOnFileStore(wbPage, objFile);
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
	 * Modify the title of the view
	 * @param sName
	 */
	public void setViewTitle(String sName) {
		super.setPartName(sName);
	}
    public void setFocus() {
            treeViewer.getTree().setFocus();
    }
    
    // laks: we need experiment and rootscope
    /**
     * Update the data input for Scope View, depending also on the scope
     */
    public void setInput(Experiment ex, RootScope scope) {
    	myExperiment = ex;
    	myRootScope = scope; //.getParentScope(); // try to get the aggregate value
    	updateDisplay();
    	this.treeFilter = new ScopeTreeFilter(ex);
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

    public TreeViewer getTreeViewer()
    {
            return treeViewer;
    }


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
    	
    	// ------------- prepare the items
    	// flatten
    	org.eclipse.swt.widgets.ToolItem tiFlatten = new ToolItem(toolbar, SWT.PUSH);
    	tiFlatten.setToolTipText("Flatten the node");
    	ImageDescriptor imgDesc = ImageDescriptor.createFromFile(this.getClass(), this.ICONPATH+"Flatten.gif");
    	tiFlatten.setImage(imgDesc.createImage());
    	tiFlatten.addSelectionListener(new SelectionAdapter() {
      	  	public void widgetSelected(SelectionEvent e) {
				ISelection sel = treeViewer.getSelection();
				if (!(sel instanceof StructuredSelection))
					return;
				Object o = ((StructuredSelection)sel).getFirstElement();
				if (!(o instanceof Scope.Node))
					return;
				Scope.Node node = (Scope.Node) o;
				Scope.Node nodeFlatten = node.tryFlatten();
				treeViewer.setInput(nodeFlatten);
				//Scope scope = (Scope)node.getUserObject();
				/*treeFilter.flattenView(node, 1);
				//node.setFlattenCount(1);
				treeViewer.setInput(node);
				treeViewer.refresh();
				*/
      	  	}
      	});
    	
    	// unflatten
    	org.eclipse.swt.widgets.ToolItem tiUnFlatten = new ToolItem(toolbar, SWT.PUSH);
    	tiUnFlatten.setToolTipText("Unflatten the node");
    	imgDesc = ImageDescriptor.createFromFile(this.getClass(), this.ICONPATH+"Unflatten.gif");
    	tiUnFlatten.setImage(imgDesc.createImage());
    	tiUnFlatten.addSelectionListener(new SelectionAdapter(){
      	  	public void widgetSelected(SelectionEvent e) {
				ISelection sel = treeViewer.getSelection();
				if (!(sel instanceof StructuredSelection))
					return;
				Object o = ((StructuredSelection)sel).getFirstElement();
				if (!(o instanceof Scope.Node))
					return;
				Scope.Node node = (Scope.Node) o;
				Scope.Node nodeUnFlatten = node.tryUnFlatten();
				treeViewer.setInput(nodeUnFlatten);
				/*ISelection sel = treeViewer.getSelection();
				if (!(sel instanceof StructuredSelection))
					return;
				Object o = ((StructuredSelection)sel).getFirstElement();
				if (!(o instanceof Scope.Node))
					return;
				Scope.Node node = (Scope.Node) o;
				treeFilter.unflattenView(node, 1);
				treeViewer.setInput(node);
				treeViewer.refresh();
				*/
      	  	}    		
    	});
    	
    	// zoom in
    	org.eclipse.swt.widgets.ToolItem tiZoomin = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomin.setToolTipText("Zoom-in");
    	imgDesc = ImageDescriptor.createFromFile(this.getClass(), this.ICONPATH+"Zoom in large.gif");
    	tiZoomin.setImage(imgDesc.createImage());
    	tiZoomin.addSelectionListener(new SelectionAdapter() {
      	  	public void widgetSelected(SelectionEvent e) {
				ISelection sel = treeViewer.getSelection();
				if (!(sel instanceof StructuredSelection))
					return;
				Object o = ((StructuredSelection)sel).getFirstElement();
				if (!(o instanceof Scope.Node))
					return;
				treeViewer.setInput(o);
				treeViewer.refresh();
      	  	}
      	});
    	
    	// zoom out
    	org.eclipse.swt.widgets.ToolItem tiZoomout = new ToolItem(toolbar, SWT.PUSH);
    	tiZoomout.setToolTipText("Zoom-out");
    	imgDesc = ImageDescriptor.createFromFile(this.getClass(), this.ICONPATH+"Zoom out large.gif");
    	// debugging purpose for RCP: check if this is done correctly
    	System.err.println(this.getClass()+" image:"+imgDesc);
    	tiZoomout.setImage(imgDesc.createImage());
    	tiZoomout.addSelectionListener(new SelectionAdapter() {
    	  public void widgetSelected(SelectionEvent e) {
    		Object o = treeViewer.getInput();
			if (!(o instanceof Scope.Node))
				return;
			Scope.Node child = (Scope.Node) o;
			Scope.Node parent = (Scope.Node)child.getParent();
			if (parent == null)
				return;
			treeViewer.setInput( parent );
			treeViewer.refresh();
    	  }
    	});
    	
    	// set the coolitem
    	this.createCoolItem(coolBar, toolbar);
    	
	    return aParent;
    }
    
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
     * Create the content of the view
     */
    public void createPartControl(Composite aParent) {
		// ----- coolbar
    	Composite parent = this.createCoolBar(aParent);

		// -----
        treeViewer = new TreeViewer(parent, SWT.SINGLE|SWT.FULL_SELECTION | SWT.BORDER);
        treeViewer.setContentProvider(new ScopeTreeContentProvider());
        treeViewer.setLabelProvider(new ScopeTreeLabelProvider());
        
        treeViewer.getTree().setHeaderVisible(true);
        treeViewer.getTree().setLinesVisible(true);

        TreeColumn tmp = new TreeColumn(treeViewer.getTree(),SWT.LEFT, 0);
        tmp.setText("Scope");
        tmp.setWidth(200); //TODO dynamic size

        //-----------------
        // Laks 11.11.07: need this to expand the tree for all view
        org.eclipse.swt.layout.GridData data = new org.eclipse.swt.layout.GridData(GridData.FILL_BOTH);
        treeViewer.getTree().setLayoutData(data);
        //-----------------

        treeViewer.setInput(null);
        
		// allow other views to listen for selections in this view (site)
		this.getSite().setSelectionProvider(treeViewer);
				
		treeViewer.addDoubleClickListener(dblListener);
		//makeActions();
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
        // dirty solution to update titles
        new ColumnViewerSorter(this.treeViewer, this.treeViewer.getTree().getColumn(0), myExperiment.getMetric(0),0);

        {
            // Update metric title labels
            String[] titles = new String[myExperiment.getMetricCount()+1];
            titles[0] = "Scope";
            TreeColumn tmp;
            edu.rice.cs.hpc.viewer.Icons icon = new edu.rice.cs.hpc.viewer.Icons();
        	for (int i=0; i<myExperiment.getMetricCount(); i++)
        	{
        		titles[i+1] = myExperiment.getMetric(i).getDisplayName();
        		tmp = new TreeColumn(treeViewer.getTree(),SWT.LEFT, i+1);
        		tmp.setText(titles[i+1]);
        		tmp.setWidth(120); //TODO dynamic size
        		//tmp.setImage(icon.imgCallTo);
        		//tmp.pack();
        		new ColumnViewerSorter(this.treeViewer, tmp, myExperiment.getMetric(i),i+1);
        		
        	}
            treeViewer.setColumnProperties(titles);
        }
        
        // Update metric value table
        ((ScopeTreeLabelProvider)treeViewer.getLabelProvider()).
        		setMetrics(myExperiment.getMetrics());
        
        // Update active pnodes
        ((ScopeTreeLabelProvider)treeViewer.getLabelProvider()).
        		setPNodes(myPNodes);

        // TODO Update scope focus
        //treeViewer.getTree().showItem(myFocusScope.getTreeNode());
        
        // Update root scope
        treeViewer.setInput(myRootScope.getTreeNode());
        this.getSite().getShell().setText("HPCViewer:"+myExperiment.getName());
        treeViewer.refresh();
	}

    //------------------ sorting stuff
    
	private static class ColumnViewerSorter extends ViewerComparator {
		public static final int ASC = 1;
		public static final int NONE = 0;
		public static final int DESC = -1;
		private int direction = 0;
		private TreeColumn column;
		private ColumnViewer viewer;
		private int iColNumber;
		private Metric metric;
		
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

		public int compare(Viewer viewer, Object e1, Object e2) {
			return direction * doCompare(viewer, e1, e2);
		}
		
		// laks: lazy comparison
		protected int doCompare(Viewer viewer, Object e1, Object e2) {
			if(e1 instanceof Scope.Node && e2 instanceof Scope.Node) {
				Scope.Node node1 = (Scope.Node) e1;
				Scope.Node node2 = (Scope.Node) e2;
				
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


}
