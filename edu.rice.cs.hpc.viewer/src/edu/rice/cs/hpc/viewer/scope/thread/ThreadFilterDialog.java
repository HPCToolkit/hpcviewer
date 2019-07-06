package edu.rice.cs.hpc.viewer.scope.thread;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.viewer.util.AbstractFilterDialog;

public class ThreadFilterDialog extends AbstractFilterDialog {

	public ThreadFilterDialog(Shell parentShell, String []labels) {
		super(parentShell, "Select threads to view", "Please check any threads to be viewed.\nYou can narrow the list by specifying partial name of the threads on the filter.", labels, null);
	}

	@Override
	protected void createAdditionalButton(Composite parent) {}

	
	// unit test
	
	static public void main(String argv[]) {
		Shell shell = new Shell();
		String []labels = new String[10];
		
		for(int i=0; i<10; i++) {
			labels[i] = String.valueOf("i="+i);
		}
		ThreadFilterDialog dialog = new ThreadFilterDialog(shell, labels);
		if (dialog.open() == Dialog.OK) {
			System.out.println("result-ok: " + dialog.getReturnCode());
			boolean results[] = dialog.getResult();
			
			int i=0;
			for(boolean res : results) {
				System.out.println("\t" + i + ": " + res);
				i++;
			}
		}
	}
}
