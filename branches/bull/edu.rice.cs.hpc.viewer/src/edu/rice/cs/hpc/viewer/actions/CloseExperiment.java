package edu.rice.cs.hpc.viewer.actions;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.dialogs.ListSelectionDialog;

import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

public class CloseExperiment implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;
	private String[] dbArray;

	public void run(IAction action) {
		// get an array of open databases for this window
		ViewerWindow vWin = ViewerWindowManager.getViewerWindow(window);
		if ( vWin == null) {
			return;		// get method already issued error dialog
		}
		dbArray = vWin.getDatabasePaths();
		if (dbArray.length == 0) {
			MessageDialog.openError(window.getShell(), 
					"Error: No Open Database's Found.", 
					"There are no databases in this window which can be closed.");
			return;		// set method already issued error dialog
		}

		List<String> dbList = Arrays.asList(dbArray);

		// put up a dialog with the open databases in the current window in a drop down selection box
		ListSelectionDialog dlg = new ListSelectionDialog(window.getShell(), dbList, 
			new ArrayContentProvider(), new LabelProvider(), "Select the databases to close:");
		dlg.setTitle("Select Databases");
		dlg.open();
		Object[] selectedDatabases = dlg.getResult();

		if ((selectedDatabases == null) || (selectedDatabases.length <= 0)) {
			return;
		}

		String[] selectedStrings = new String[selectedDatabases.length];
		for (int i=0 ; i<selectedDatabases.length ; i++) {
			selectedStrings[i] = selectedDatabases[i].toString();
			// remove the database from our database manager information
			int dbNum = vWin.removeDatabase(selectedStrings[i]);
			if (dbNum < 0) {
				// can close views for an entry we could not find
				continue;
			}
		
			// close this databases metrics views
			IWorkbenchPage curPage = window.getActivePage();
			org.eclipse.ui.IViewReference views[] = curPage.getViewReferences();
			int nbViews = views.length;
			for(int j=0;j<nbViews;j++) {
				String title = views[j].getTitle();
				// if this is for the database being closed, remove it (hiding it actually deletes it)
				if (title.startsWith((dbNum+1) + "-")) {
					curPage.hideView(views[j]);
				}
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
	}

	public void dispose() {
		// TODO Auto-generated method stub
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}
