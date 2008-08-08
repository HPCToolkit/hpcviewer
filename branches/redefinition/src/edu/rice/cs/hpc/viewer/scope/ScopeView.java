package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;

/**
 * Basic class for scope views: calling context and caller view
 * @author laksonoadhianto
 *
 */
public class ScopeView extends BaseScopeView {
    public static final String ID = "edu.rice.cs.hpc.scope.ScopeView";

    protected ScopeViewActions createActions(Composite parent, CoolBar coolbar) {
        return new ScopeViewActions(this.getViewSite().getShell(), parent, coolbar); 
    }

}
