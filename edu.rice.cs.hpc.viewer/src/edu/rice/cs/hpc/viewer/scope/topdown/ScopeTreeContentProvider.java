package edu.rice.cs.hpc.viewer.scope.topdown;

import org.eclipse.jface.viewers.Viewer;

import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;

/************************************************************************
 * 
 * content provider for CCT view
 *
 ************************************************************************/
public class ScopeTreeContentProvider extends AbstractContentProvider 
{

	public ScopeTreeContentProvider(ScopeTreeViewer viewer) {
		super(viewer);
	}
    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
    	if(element instanceof Scope)
            return ((Scope) element).hasChildren(); // !((Scope.Node) element).isLeaf();
    	else
    		return false;
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
}