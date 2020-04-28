package edu.rice.cs.hpc.viewer.framework;

import javax.swing.JOptionPane;

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

	private String[] checkArguments(IApplicationContext context) {
		String[] args = (String[])context.getArguments().get("application.args");
		return args;
	}

	private boolean checkVersion(String version) {
		
		if (version == null)
			return false;
		
		String verNumber[]  = version.split("\\.", 3);
		String majorVersion = verNumber[0];
		
		try {
			Integer major = Integer.valueOf(majorVersion);
			if (major == 1) {
				Integer minor = Integer.valueOf(verNumber[1]);
				return minor == 8;
			}
			return major == 8;
		} catch (Exception e) {
			System.err.println("Unknown java version: " + version);
		}
		return false;
	}
	
	private boolean checkJava() {
		String version = System.getProperty("java.version");

		System.out.println("java version: " + version);

		boolean isCorrect = checkVersion(version);
		if (!isCorrect) {
			String message = "Error: Java " + 
					  System.getProperty("java.version") +
					  " is not supported.\nOnly Java 8 is supported.";
			
			System.out.println(message);
			
			JOptionPane.showMessageDialog(null, message);
		}
		return isCorrect;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	@Override
	public Object start(IApplicationContext context) {
		
		if (!checkJava()) {
			
			return IApplication.EXIT_OK;
		}
		
		String []args = this.checkArguments(context);
		Display display = PlatformUI.createDisplay();

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
