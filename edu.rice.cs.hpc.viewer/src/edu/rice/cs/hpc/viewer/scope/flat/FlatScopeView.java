/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope.flat;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.DynamicScopeView;
import edu.rice.cs.hpc.viewer.scope.ScopeViewActions;
import edu.rice.cs.hpc.viewer.scope.StyledScopeLabelProvider;

/**
 * Class for flat view scope. 
 * This class has special actions differed from calling context and caller view
 *
 */
public class FlatScopeView extends DynamicScopeView {
    public static final String ID = "edu.rice.cs.hpc.viewer.scope.FlatScopeView";
    
        
    protected ScopeViewActions createActions(Composite parent, CoolBar coolbar) {
    	
    	setTitleToolTip("The view to show the performance data according to the code structure of an application");
    	
    	IWorkbenchWindow window = this.getSite().getWorkbenchWindow();
        return new FlatScopeViewActions(this.getViewSite().getShell(), window, parent, coolbar); 
    }

	@Override
	protected CellLabelProvider getLabelProvider() {
		return new StyledScopeLabelProvider(this.getSite().getWorkbenchWindow());
	}

	@Override
	protected void createAdditionalContextMenu(IMenuManager mgr, Scope scope) {}

	@Override
	protected void mouseDownEvent(Event event) {}

	@Override
	protected AbstractContentProvider getScopeContentProvider() {
		return new FlatViewContentProvider(getTreeViewer());
	}

	@Override
	protected void updateDatabase(Experiment newDatabase) {}

	@Override
	public RootScope createTree(Experiment experiment) {
		RootScope rootCCT 	  = experiment.getRootScope(RootScopeType.CallingContextTree);
		RootScope rootFTree	  = getRootScope(); 
		return experiment.createFlatView(rootCCT, rootFTree);
	}

}
