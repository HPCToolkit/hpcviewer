/**
 * 
 */
package edu.rice.cs.hpc.viewer.actions;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;


/**
 * @author laksonoadhianto
 *
 */
public class CloseWindow extends AbstractHandler {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.commands.IHandler#execute(java.util.Map)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		
		// get an array of open databases for this window
		// do not remove the window yet right now
		// ViewerWindowManager.removeWindow(window);
		
		// close editor windows
		window.getActivePage().closeAllEditors(false);
		
		// remove window registered in the list
		ViewerWindowManager.removeWindow(window);

		// close the workbench (which will close the application as well)
		window.close();
		
		return null;
	}

}
