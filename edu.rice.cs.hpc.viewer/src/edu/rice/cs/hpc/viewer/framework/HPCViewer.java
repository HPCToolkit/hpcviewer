package edu.rice.cs.hpc.viewer.framework;

/*import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;*/
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/**
 * This class controls all aspects of the application's execution
 */
public class HPCViewer implements IApplication 
{
	//static private final String FILE_NAME = "hpcviewer.log";

	private String[] checkArguments(IApplicationContext context) {
		String[] args = (String[])context.getArguments().get("application.args");
		return args;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	@Override
	public Object start(IApplicationContext context) {
		
		String []args = this.checkArguments(context);
		Display display = PlatformUI.createDisplay();
		
		// Issue #47 : redirect standard error to hpcviewer.log
		// https://github.com/HPCToolkit/hpcviewer/issues/47
		// 
		// We don't want users to see tons of SWT error message during the launch
		// but we still keep the log in user workspace
		
/*		IPath path 		= Platform.getLocation().makeAbsolute();
		String filename = path.append(FILE_NAME).makeAbsolute().toString();
		
		try {
			File file = new File(filename);
			if (!file.exists()) {
				file.createNewFile();
			}
			System.setErr(new PrintStream(filename));
			
			System.out.println("Standard error is redirected to " + filename);
			
		} catch (FileNotFoundException e1) {
			System.err.println( filename + ": File not found. " + e1.getMessage());
		} catch (IOException e2) {
			System.err.println( filename + ": I/O error. " + e2.getMessage());
		}
*/		
		// create the application
		
		try {		
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor(args));
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
		} catch (Exception e) {
			
		} finally {
			display.dispose();
		}
		return IApplication.EXIT_OK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null)
			return;
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}
}
