/**
 * 
 */
package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * View class for caller view. At the moment, the caller view is the same as calling context view
 * @author laksonoadhianto
 *
 */
public class CallerScopeView extends BaseScopeView {
    public static final String ID = "edu.rice.cs.hpc.viewer.scope.CallerScopeView";

	/* (non-Javadoc)
	 * @see edu.rice.cs.hpc.viewer.scope.BaseScopeView#createActions(org.eclipse.swt.widgets.Composite, org.eclipse.swt.widgets.CoolBar)
	 */
	@Override
	protected ScopeViewActions createActions(Composite parent, CoolBar coolbar) {
    	IWorkbenchWindow window = this.getSite().getWorkbenchWindow();
        return new BaseScopeViewActions(this.getViewSite().getShell(), window, parent, coolbar); 
	}

	@Override
	protected CellLabelProvider getLabelProvider() {
		CallerScopeLabelProvider objScopeLabel = new CallerScopeLabelProvider(this.getSite().getWorkbenchWindow());
		final DelegatingStyledCellLabelProvider objLabelProvider = new DelegatingStyledCellLabelProvider( objScopeLabel );
		return objLabelProvider;
	}

}
