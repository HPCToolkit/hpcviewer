package edu.rice.cs.hpc.traceviewer.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.services.DataService;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.ui.ProcedureClassDialog;
import edu.rice.cs.hpc.traceviewer.data.util.ProcedureClassMap;


/****
 * 
 * action handler to show procedure-class map dialog
 *
 */
public class ProcedureClassMapAction extends AbstractHandler {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final Shell shell = HandlerUtil.getActiveShell(event);
		final IWorkbenchWindow win = HandlerUtil.getActiveWorkbenchWindow(event);
		
		ProcedureClassMap classMap = new ProcedureClassMap(shell.getDisplay());
		ProcedureClassDialog dlg = new ProcedureClassDialog(shell, classMap);
		if ( dlg.open() == Dialog.OK ) {
			classMap.save();
			broadcastChanges(win);
		}
		
		return null;
	}

	private void broadcastChanges(IWorkbenchWindow winObj) {
		
		if (winObj == null) {
			// impossible to get a window handle
			return;
		}
		ISourceProviderService sourceProviderService = (ISourceProviderService) winObj.getService(
				ISourceProviderService.class);
		
		DataService dataService = (DataService) sourceProviderService.getSourceProvider(DataService.DATA_PROVIDER);
		// reset the color table
		SpaceTimeDataController stdc = dataService.getData();
		if (stdc == null) {
			// no database is opened
			return;
		}
		
		stdc.resetPredefinedColor();;
		
		// broadcast to all views
		dataService.broadcastUpdate(null);
	}
}
