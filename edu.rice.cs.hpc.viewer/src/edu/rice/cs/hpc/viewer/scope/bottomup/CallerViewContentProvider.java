package edu.rice.cs.hpc.viewer.scope.bottomup;

import org.eclipse.jface.viewers.Viewer;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeCallerView;
import edu.rice.cs.hpc.data.experiment.scope.IMergedScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;

import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.filters.ExclusiveOnlyMetricPropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.filters.InclusiveOnlyMetricPropagationFilter;

import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;

/************************************************************************
 * 
 * Content provider class specifically for caller view
 * This class will update the children of a scope dynamically, unlike
 * other views
 *
 ************************************************************************/
public class CallerViewContentProvider extends AbstractContentProvider 
{
	private ExclusiveOnlyMetricPropagationFilter exclusiveOnly;
	private InclusiveOnlyMetricPropagationFilter inclusiveOnly;
	
	public CallerViewContentProvider(ScopeTreeViewer viewer) {
		super(viewer);
	}
	

    /**
     * find the list of children
     */
    public Object[] getChildren(Object parentElement) {
    	Object []results = null;
    	
    	if(parentElement instanceof IMergedScope) {
    		// normal mode
    		IMergedScope parent = ((IMergedScope) parentElement);
    		results = parent.getAllChildren(inclusiveOnly, exclusiveOnly);
        	
    	} else if (parentElement instanceof Scope) {
    		Scope scope = (Scope) parentElement;
    		results = scope.getChildren();
    	}
    	return results;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
    	if(element instanceof Scope) {
    		Scope node = (Scope) element;
    		boolean has_children = node.hasChildren();
    		if (!has_children) {
    			if (node instanceof CallSiteScopeCallerView) {
        			CallSiteScopeCallerView cc = (CallSiteScopeCallerView) node;
        			has_children = cc.hasScopeChildren(); //cc.numChildren>0;
    			} else if ( !(node instanceof ProcedureScope)){
    				throw new RuntimeException("Unexpected scope node: " + node);
    			}
    		}
            return has_children;   		
    	}
    	else
    		return false;
    }

    
    /***
     * Update the database
     * @param experiment
     */
    public void setDatabase(Experiment experiment) {
    	exclusiveOnly = new ExclusiveOnlyMetricPropagationFilter(experiment);
    	inclusiveOnly = new InclusiveOnlyMetricPropagationFilter(experiment);
    }


	@Override
	public void dispose() {
		inclusiveOnly = null;
		exclusiveOnly = null;
	}


	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

	}    
}
