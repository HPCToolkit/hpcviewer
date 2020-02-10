package edu.rice.cs.hpc.viewer.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.rice.cs.hpc.viewer.metric.MetricPropertyDialog;
import edu.rice.cs.hpc.viewer.window.Database;
import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;


/*******************************************************************
 * 
 * Action class to show metric properties 
 *
 */
public class ShowMetricProperties extends AbstractHandler {

	final public static String COMMAND_REFRESH_METRICS = "edu.rice.cs.hpc.viewer.actions.ShowMetricProperties";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		
		final ViewerWindow vw = ViewerWindowManager.getViewerWindow(window);
		final Database   []db = vw.getDatabases();
		final Shell shell = window.getShell();
		
		if (db == null || db.length == 0) {
			MessageDialog.openInformation(shell, "No database", "There is no database opened");
			return null;
		}
		
		// show the metric properties window
		MetricPropertyDialog dialog = new MetricPropertyDialog(shell, 
				HandlerUtil.getActiveWorkbenchWindow(event));
			
		if ( dialog.open() == Dialog.OK ) {
			
			// broadcast message to all views that a metric may have been changed
			final ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
			commandService.refreshElements(COMMAND_REFRESH_METRICS, null);
		}

		return null;
	}

}
