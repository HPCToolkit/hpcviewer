package edu.rice.cs.hpc.viewer.framework;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchListener;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;

import edu.rice.cs.hpc.common.ui.Util;
import edu.rice.cs.hpc.common.util.SleakManager;
import edu.rice.cs.hpc.viewer.experiment.ExperimentManager;
import edu.rice.cs.hpc.viewer.experiment.ExperimentView;
import edu.rice.cs.hpc.viewer.util.Utilities;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor 
{
	private String[] sArgs = null; // command line arguments
	final private IWorkbench workbench;
	final private IWorkbenchWindow window;
	
	/**
	 * Creates a new workbench window advisor for configuring a workbench window via the given workbench window configurer
	 * Retrieve the RCP's arguments and verify if it contains database to open
	 * 
	 * @param configurer
	 * @param args
	 */
	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer, String []args) {
		super(configurer);
		window = configurer.getWindow();
		workbench = window.getWorkbench();
		this.sArgs = args;
	}

	/**
	 * Creates a new action bar advisor to configure the action bars of the window via 
	 * the given action bar configurer. The default implementation returns a new instance of ActionBarAdvisor
	 */
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	/**
	 * Performs arbitrary actions before the window is opened.
	 */
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		
		final IWorkbenchWindow window = configurer.getWindow();
		if (!Util.checkJavaVendor(window.getShell()))
			window.close();

		configurer.setShowCoolBar(false);	// remove toolbar/coolbar
		configurer.setShowStatusLine(true);	// show status bar
        configurer.setShowProgressIndicator(true);

		// tell the viewer window manager we are creating a new window
		ViewerWindowManager vwm = new ViewerWindowManager();
		vwm.addNewWindow(window);
	}

	/**
	 * Action when the window is already opened
	 */
	public void postWindowOpen() {
    	boolean withCallerView = true;

		// -------------------
    	// set the status bar
		// -------------------
		org.eclipse.jface.action.IStatusLineManager statusline = getWindowConfigurer()
		.getActionBarConfigurer().getStatusLineManager();

		// -------------------
		// close existing views and editors in this window
		// -------------------
		removeViews();
		closeAllEditors();

		// -------------------
		// set the default metric
		// -------------------
		Utilities.initFontMetric(window.getShell().getDisplay());

		this.shutdownEvent(this.workbench, window.getActivePage());

		// -------------------
		// memleak is on if needed
		// -------------------
		final Display display = window.getShell().getDisplay();
		SleakManager.init(display);

		// -------------------
		// see if the argument provides the database to load
		// -------------------
		if(sArgs != null && (sArgs.length > 0) &&
				// only the first window open the database specified in the command line 
				(ViewerWindowManager.getWindowNumber(window)==0) ) {
			
			// possibly we have express the experiment file in the command line
			IWorkbenchPage pageCurrent = window.getActivePage();
			assert (pageCurrent != null);
			
			ExperimentView expViewer = new ExperimentView(pageCurrent);
		    assert (expViewer != null); 
		    
		    String sPath = null;
		    // find the file in the list of arguments
		    for(int i=0;i<sArgs.length;i++) {

		    	if(sArgs[i].charAt(0) == '-') {
		    		String sOption = sArgs[i].substring(1);
		    		if (sArgs[i].charAt(1) == 'n' || 
		    				(sArgs.length == 14 && sArgs[i].charAt(2)=='-' && sOption.equalsIgnoreCase("nocallerview")) ){
		    			// user has set the option not to display caller view
		    			withCallerView = false;
		    		}
		    	} else {
		    		sPath = sArgs[i];
		    	}
		    }
		    
			// -------------------
		    // if a filename exist, try to open it
			// -------------------
		    if(sPath != null) {
		    	IFileStore fileStore;
		    	boolean doPrint = false;
				try {
					fileStore = EFS.getLocalFileSystem().getStore(new URI(sPath));
				} catch (URISyntaxException e) {
					// somehow, URI may throw an exception for certain schemes. 
					// in this case, let's do it traditional way
					fileStore = EFS.getLocalFileSystem().getStore(new Path(sPath));
					e.printStackTrace();
				}
		    	IFileInfo objFileInfo = fileStore.fetchInfo();
		    	
		    	if( objFileInfo.exists() ) {
		    		if ( objFileInfo.isDirectory() ) {
		    			if (doPrint) System.out.println("Opening database: " + fileStore.fetchInfo().getName() + " " + withCallerView);

		    			final ExperimentManager objManager = new ExperimentManager(window); 
		    			objManager.openDatabaseFromDirectory(sPath, this.getFlag(withCallerView));
		    		} else {
		    			File objFile = new File(sPath);
		    			EFS.getLocalFileSystem().fromLocalFile(new File(sPath));
		    			if (doPrint) System.out.println("Opening file: " + objFile.getAbsolutePath() + " " +  fileStore.fetchInfo().getName());
		    			expViewer.loadExperimentAndProcess( objFile.getAbsolutePath(), withCallerView);
		    		}
		    	} else {
		    		final String sMsg = "File doesn't exist: " + fileStore.getName();
		    		//System.err.println(sMsg );
		    		MessageDialog.openError(window.getShell(), "Fail to open a database", sMsg);
					//this.removeViews();
		    	}

				return;
		    } 
		}
		
		// there is no information about the database
		statusline.setMessage(null, "Load a database to start.");
		// we need load the file ASAP
		this.openDatabase(withCallerView);
	}
	
	/**
	 * Open an experiment database. A database is a folder that contains XML experiment files 
	 * (only the first one will be taken into account)
	 */
	private void openDatabase( boolean withCallerView ) {

		final ExperimentManager expFile = new ExperimentManager(window); 
		boolean has_database = expFile.openFileExperiment( this.getFlag(withCallerView));

		if (!has_database) {
			this.removeViews();
		}
	}


	/****
	 * when there is no database opened, we have to hide views to avoid users to click buttons
	 * since most action buttons assume the database is already opened, then an action of this
	 * button while there is no database will cause chaos
	 */
	private void removeViews() {
		IWorkbenchPage page = this.window.getActivePage();
		removeViews( page );
	}
	
	private void removeViews( IWorkbenchPage page )
	{
		org.eclipse.ui.IViewReference views[] = page.getViewReferences();
		int nbViews = views.length;
		
		// needs to hide the flat view and the callers view first. 
		// these views are created dynamically, and when a user exits the application
		//  the view may not be created.
		//
		// If we close the top-down view first, Eclipse will activate the callers view,
		//  and then close it. When it's activated, it creates the tree and that takes time
		
		for(int i=nbViews-1;i>=0; i--)
			page.hideView(views[i]);
	}
	
	/**
	 * Close all editors in the current active page
	 */
	private void closeAllEditors() {
		IWorkbenchPage page = this.window.getActivePage();
		page.closeAllEditors(false);
	}
	
	/**
	 * return the flag to indicate if a caller view needs to be displayed or not
	 * @param withCallerView
	 * @return
	 */
	private int getFlag (boolean withCallerView) {
		if (withCallerView) return ExperimentManager.FLAG_DEFAULT;
		else return ExperimentManager.FLAG_WITHOUT_CALLER_VIEW;
	}
	/**
	 * Performs arbitrary actions as the window's shell is being closed directly, and possibly veto the close.
	 */
	public boolean preWindowShellClose() {
		
		// ----------------------------------------------------------------------------------------------
		// get "my" window from the configurer instead of from the active window
		// in some platforms such as Mac OS, in order to close window, we don't need to activate it first.
		// ----------------------------------------------------------------------------------------------
		closeAllEditors();
		
		// in some cases, "removing" views cause Eclipse to make other views visible, and then
		// activate it. This causes the creation of bottom-up tree or flat tree.
		//removeViews();
		
		ViewerWindowManager.removeWindow(window);

		return true; // we allow app to shut down
	}
	
	/**
	 * add a shutdown event to the workbench
	 */
	private void shutdownEvent(IWorkbench workbench, final IWorkbenchPage pageCurrent) {
		// attach a new listener to the workbench
		workbench.addWorkbenchListener(new IWorkbenchListener(){
			public boolean preShutdown(IWorkbench workbench,
                    boolean forced) {
				// bug on Mac OS: Mac will allow user to close via menu system while hpcviewer is still displaying 
				// 	a modal dialog box (such as  open file dialog). This will create infinite loop in the SWT events
				//  and has to be killed 
				if (pageCurrent != null){
					// somehow, closeEditors method works better than closeAllEditors.
					pageCurrent.closeEditors(pageCurrent.getEditorReferences(), false);

					// in some cases, "removing" views cause Eclipse to make other views visible, and then
					// activate it. This causes the creation of bottom-up tree or flat tree.
					//removeViews( pageCurrent );
				}
				
				return true;
			}
			public void postShutdown(IWorkbench workbench) {
				
				//---------------------------------------------------------------------------
				// we need to explicitly remove all allocated native resources since Eclipse
				// 	will not do this for us (due to resources that are platform dependent)
				//---------------------------------------------------------------------------
				Utilities.dispose();
			}
		});
	}
	
	/**
	 * Laksono 2009.02.11: removing unwanted menus
	 */
	public void postWindowCreate()
	{
	  IContributionItem[] mItems, mSubItems;
	  IMenuManager mm = getWindowConfigurer ().getActionBarConfigurer ().getMenuManager ();
	  mItems = mm.getItems ();
	  for (int i = 0; i < mItems.length; i++)
	  {
	    if (mItems[i] instanceof MenuManager)
	    {
	      mSubItems = ((MenuManager) mItems[i]).getItems ();
	      for (int j = 0; j < mSubItems.length; j++)
	      {
	    	  if (mItems[i].getId ().equals ("help"))
	        {
	    		  // John doesn't like some of the key assist tooltips. Let's remove all for once
	          ((MenuManager) mItems[i]).remove ("org.eclipse.ui.actions.showKeyAssistHandler");
	        }
	      }
	    }
	  }
	}
}
