package edu.rice.cs.hpc.traceviewer.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/***
 * 
 * class to manage a message label to show a message
 * for a couple of seconds, and then remove it back,
 * returns to the original empty label.
 *
 */
public class MessageLabelManager 
{
	final private Label   messageLabel;
	final private Display display;

	private Color colorBackground;

	public MessageLabelManager(final Display display, Label messageLabel) {

		this.messageLabel   = messageLabel;
		this.display 		= display;
		
		// not to use asynchronous Eclipse job
		// we need to make sure the background color is initialized
		// before we use it.
		
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				// sometimes the application exits without cleaning up
				// the message
				// we need to be careful not to use disposed display
				
				if (display != null && !display.isDisposed()) {
					Shell shell = display.getActiveShell();
					if (shell != null && !shell.isDisposed())
						colorBackground = display.getActiveShell().getBackground();	
				}
			}
			
		});
	}
	
	public void showWarning(String message) {
		showMessage(SWT.COLOR_BLACK, SWT.COLOR_YELLOW, message);
	}
	
	public void showInfo(String message) {
		showMessage(SWT.COLOR_BLACK, SWT.COLOR_WHITE, message);
	}
	
	public void showError(String message) {
		showMessage(SWT.COLOR_WHITE, SWT.COLOR_RED, message);
	}

	
	private void showMessage(final int foreground, final int background, String message) {
		
		initLabel(foreground, background, message);

		Alarm threadAlarm = new Alarm(display, messageLabel, colorBackground);
		threadAlarm.start();
	}
	
	private void initLabel(final int foreground, final int background, final String message) {
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				Color colorFont = display.getSystemColor(foreground);
				Color colorBack = display.getSystemColor(background);

				messageLabel.setForeground(colorFont);
				messageLabel.setBackground(colorBack);
				messageLabel.setText(message);
			}
		});		
	}
	
	
	static class Alarm extends Thread
	{
		final int WAIT_TIME = 5000;
		
		final Display display;

		final Label   messageLabel;
		final Color colorBackground;

		Alarm(Display display, Label messageLabel, Color colorBackground) {
			this.display = display;
			this.colorBackground = colorBackground;
			
			this.messageLabel = messageLabel;
		}
			
		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			try {
				// wait for a couple of seconds
				sleep(WAIT_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			restoreLabel();
		}
		
		private void restoreLabel() {
			display.asyncExec(new Runnable() {

				@Override
				public void run() {
					if (messageLabel == null || messageLabel.isDisposed()) {
						return;
					}
					// possible data race:
					//
					// When there is a message and suddenly the application exits,
					// this thread may still exist and try to access disposed messageLabel
					// 
					try {
						messageLabel.setText("");
						
						if (colorBackground != null)
							// if color background is null we are doom
							messageLabel.setBackground(colorBackground);
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}
				}
				
			});
		}
	};
}
