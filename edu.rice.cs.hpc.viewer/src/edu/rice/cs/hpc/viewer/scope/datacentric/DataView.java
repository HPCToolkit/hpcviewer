package edu.rice.cs.hpc.viewer.scope.datacentric;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.graph.GraphMenu;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.BaseScopeView;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;
import edu.rice.cs.hpc.viewer.scope.ScopeViewActions;
import edu.rice.cs.hpc.viewer.scope.StyledScopeLabelProvider;
import edu.rice.cs.hpc.viewer.scope.topdown.CallingContextViewActions;

public class DataView extends BaseScopeView 
{
	final static public String ID = "edu.rice.cs.hpc.viewer.scope.datacentric.DataView";

	@Override
	protected void updateDatabase(Experiment new_database) {
	}

	@Override
	protected void refreshTree(RootScope root) {
		updateDisplay();
	}

	@Override
	protected ScopeViewActions createActions(Composite parent, CoolBar coolbar) {
		IWorkbenchWindow window = this.getSite().getWorkbenchWindow();		
		
    	ScopeViewActions action = new CallingContextViewActions(
    			getViewSite().getShell(), window, parent, coolbar);
    	
    	return action;
	}

	@Override
	protected void mouseDownEvent(Event event) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void createAdditionalContextMenu(IMenuManager mgr, Scope scope) {
		IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
		GraphMenu.createAdditionalContextMenu(window, mgr, database, scope);
	}

	@Override
	protected AbstractContentProvider getScopeContentProvider() {

		return new DataViewContentProvider(getTreeViewer());
	}

	@Override
	protected CellLabelProvider getLabelProvider() {
		return new StyledScopeLabelProvider(this.getSite().getWorkbenchWindow());
	}

	static class DataViewContentProvider extends AbstractContentProvider
	{

		public DataViewContentProvider(ScopeTreeViewer viewer) {
			super(viewer);
		}

		@Override
		public Object[] getChildren(Object node) {
			if (node instanceof Scope) {
				return ((Scope)node).getChildren();
			}
			return null;
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
			
		}

	};

}

