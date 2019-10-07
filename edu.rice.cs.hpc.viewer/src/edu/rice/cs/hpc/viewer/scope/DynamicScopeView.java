package edu.rice.cs.hpc.viewer.scope;

import java.io.File;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.viewer.window.Database;
import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

/****************************************************************************
 * 
 * Abstract class to manage a dynamic view
 * This view only creates the tree if and only if it is activated or visible.
 * <br/>
 * Otherwise, it's just an empty tree.
 * 
 * <p>The children needs to implements {@link IDynamicRootTree.createTree}
 * method to create the tree when needed.
 *
 ****************************************************************************/
abstract public class DynamicScopeView extends BaseScopeView 
implements IDynamicRootTree
{
    public void createPartControl(Composite aParent) {
    	super.createPartControl(aParent);
    	
    	// ----------------------------------------------------------
    	// add a listener to check if this view is visible or not
    	// if it is visible, we need to init the tree of the view
    	// ----------------------------------------------------------
    	
    	final IWorkbenchPage page = getSite().getPage();
    	if (page != null) {
    		final String firstID	    = getViewSite().getId();
    		final String secondID		= getViewSite().getSecondaryId();
    		final PartListener listener = new PartListener(firstID, secondID);
        	page.addPartListener(listener);
    	}
    }

    /*
     * (non-Javadoc)
     * @see edu.rice.cs.hpc.viewer.scope.BaseScopeView#refreshTree(edu.rice.cs.hpc.data.experiment.scope.RootScope)
     */
    @Override
    protected void refreshTree(RootScope root)
    {
		if (!root.hasChildren()) {
			// do not recreate the children if it's already created
			// unless if we are in filtering mode
			Experiment experiment = database.getExperiment();
			if (experiment.getRootScope() != null) {
				root = createTree(experiment);
				
				// fix issue #51
				// https://github.com/HPCToolkit/hpcviewer/issues/51
				// set the 3rd argument to true to keep the existing column width
				
				setInput(database, root, true);
			}
		} else {
			// check whether the view has the new created tree.
			// this special case happens when we "merge" two uncreated flat trees.
			// the merge method will force to create a flat tree WITHIN the experiment,
			//  but the view doesn't detect it.
			ScopeTreeViewer viewer = getTreeViewer();
			final Tree tree		   = viewer.getTree();
			if (tree.getItemCount() < 2) {
				// the tree is created, but the view doesn't know it.
				// let's force to reset the input
				setInput(database, root, true);
			}
		}
    }
	
	//////////////////////////////////////////////////////////////////////////////
	// Private classes
	//////////////////////////////////////////////////////////////////////////////
	static private final class PartListener implements IPartListener2
	{
		final private String firstID, secondaryID;
		
		public PartListener(String firstID, String secondaryID) {
			this.secondaryID = secondaryID;
			this.firstID	 = firstID;
		}

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			partVisible(partRef);
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			if (isMyView(partRef)) {
				partRef.getPage().removePartListener(this);
			}
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
			if (shouldBeRendered(partRef)) {
				// i am visible now
				DynamicScopeView view = (DynamicScopeView) partRef.getPart(false);
				Database database  = view.getDatabase();
				RootScope root = view.getRootScope();
				
				if (database != null) {
					view.refreshTree(root);
				}
			}
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {}

		/*****
		 * check if the view part is identical to this view or not
		 * @param partRef
		 * @return
		 */
		private boolean isMyView(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			
			if (part instanceof DynamicScopeView) {
				DynamicScopeView view = (DynamicScopeView) part;
				
				final String ID	   = view.getViewSite().getId();
				final String secID = view.getViewSite().getSecondaryId();
				
				boolean me = (secondaryID.equals(secID) && firstID.equals(ID));

				return me;
			}
			return false;
		}
		
		/***
		 * check if the view should be rendered or not.
		 * This method will check if the reference is the same as the ID of the view or not.
		 * IF this is the case, then it checks if we are closing or not. Sometimes eclipse
		 * send "visible" notification before it closes (I know it's annoying), so we have 
		 * to be careful when we should render the view.
		 * 
		 * @param partRef : the reference to the view part
		 * 
		 * @return boolean true if the view has to be rendered. false otherwise.
		 */
		private boolean shouldBeRendered(IWorkbenchPartReference partRef) {
			if ( (ViewerWindowManager.size()>0) && isMyView(partRef) ) {

				IWorkbenchPart part = partRef.getPart(false);
				DynamicScopeView view = (DynamicScopeView) part;

				IWorkbenchWindow window = part.getSite().getWorkbenchWindow();
				final ViewerWindow vw 	= ViewerWindowManager.getViewerWindow(window);
				if (vw != null && view.database != null) {
					File file = view.database.getExperiment().getXMLExperimentFile();
					String path = vw.getDatabasePath(file);
					boolean exist = (vw.getDb(path) != null); 
					return exist ;
				}
			}
			return false;
		}
	}
}
