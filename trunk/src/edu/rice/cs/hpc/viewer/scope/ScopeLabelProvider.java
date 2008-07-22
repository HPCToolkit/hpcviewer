package edu.rice.cs.hpc.viewer.scope;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.swt.SWT;

import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.viewer.resources.Icons;
import edu.rice.cs.hpc.viewer.util.Utilities;

public class ScopeLabelProvider extends ColumnLabelProvider {
	static protected Icons iconCollection = Icons.getInstance();
	//static boolean debug = true;
	private IWorkbenchWindow windowCurrent;
	/**
	 * Default constructor
	 */
	public ScopeLabelProvider(IWorkbenchWindow window) {
		// TODO Auto-generated constructor stub
		super();
		this.windowCurrent = window;
	}

	/**
	 * Return the image of the column. By default no image
	 */
	public Image getImage(Object element) {
		if(element instanceof Scope.Node) {
			Scope.Node node;
			node = (Scope.Node) element;
			Scope scope = node.getScope();
			return Utilities.getScopeNavButton(scope);
		}
		return null;
	}
	
	/**
	 * Return the text of the scope tree. By default is the scope name.
	 */
	public String getText(Object element) {
		String text = "-";
		if (element instanceof Scope.Node){
			Scope.Node node = (Scope.Node) element;
			Scope scope = node.getScope();
			/*if (debug)
				text = scope.getScopeType().substring(0, 1) + ":" + scope.getName();
			else */
				text = scope.getName();
		} else
			text = element.getClass().toString();
		return text;
	}
	
	/**
	 * Mark blue for node that has source code file name information.
	 * Attention: we do not verify if the source code exist or not !!
	 */
	public Color getForeground(Object element) {
		if(element instanceof Scope.Node) {
			Scope.Node node = (Scope.Node) element;
			Scope scope = node.getScope();
			if(Utilities.isFileReadable(scope)) {
				node.hasSourceCodeFile = true; //update the indicator flag in the node
				// put the color blue
				return this.windowCurrent.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
			}
		}
		return null;
	}
}
