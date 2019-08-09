package edu.rice.cs.hpc.viewer.scope.flat;

import org.eclipse.jface.viewers.Viewer;

import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.viewer.scope.AbstractContentProvider;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;

public class FlatViewContentProvider extends AbstractContentProvider {

	public FlatViewContentProvider(ScopeTreeViewer viewer) {
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

}
