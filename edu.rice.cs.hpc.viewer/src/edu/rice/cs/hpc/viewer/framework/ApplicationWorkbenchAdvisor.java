package edu.rice.cs.hpc.viewer.framework;

import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.application.IWorkbenchConfigurer;

import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	public static final String PERSPECTIVE_ID = "edu.rice.cs.hpc.perspective";
	private String[] args;

	// laks: we need to save and restore the configuration
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		// enable the workbench state save mechanism
		configurer.setSaveAndRestore(true);
	}
	
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer, this.args);
	}

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	public ApplicationWorkbenchAdvisor() {
		super();
	}
	
	public ApplicationWorkbenchAdvisor(String []arguments) {
		super();
		this.args = arguments;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.application.WorkbenchAdvisor#postStartup()
	 */
	public void postStartup() {
		super.postStartup();
		
		final IWorkbench workbench = this.getWorkbenchConfigurer().getWorkbench();
		
		// -----------------------------------------------------------------------------
		// add listener when a workbench window is activated. 
		//  this listener is useful to update the status of "merge" menu when we 
		//	have  multiple instances of windows. If one window has only one database
		//	and the other has two databases, then the "merge" menu only visible for
		//	the latter window, and not the former
		// -----------------------------------------------------------------------------
		workbench.addWindowListener( new IWindowListener() {

			public void windowActivated(IWorkbenchWindow window){}
			public void windowDeactivated(IWorkbenchWindow window) {}
			public void windowClosed(IWorkbenchWindow window) {}
			
			public void windowOpened(IWorkbenchWindow window) {
				ViewerWindow vWin = ViewerWindowManager.getViewerWindow(window);
				if (vWin != null)
				{
					vWin.checkService();
				}
			}
			
		});
	}
}
