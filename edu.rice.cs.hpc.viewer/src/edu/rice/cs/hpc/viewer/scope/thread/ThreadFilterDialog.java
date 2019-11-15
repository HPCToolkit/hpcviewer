package edu.rice.cs.hpc.viewer.scope.thread;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.viewer.util.AbstractFilterDialog;
import edu.rice.cs.hpc.viewer.util.FilterDataItem;

public class ThreadFilterDialog extends AbstractFilterDialog {

	public ThreadFilterDialog(Shell parentShell, FilterDataItem items[]) {
		super(parentShell, "Select threads to view", 
				"Please check any threads to be viewed.\nYou can narrow the list by specifying partial name of the threads on the filter.", 
				items);
	}

	@Override
	protected void createAdditionalButton(Composite parent) {}

	
	// unit test
	
	static public void main(String argv[]) {
		Shell shell = new Shell();
		FilterDataItem items[] = new FilterDataItem[10];
		
		for(int i=0; i<10; i++) {
			items[i] = new FilterDataItem("i="+i, i<6, i>3);
		}
		ThreadFilterDialog dialog = new ThreadFilterDialog(shell, items);
		if (dialog.open() == Dialog.OK) {
			System.out.println("result-ok: " + dialog.getReturnCode());
			items = dialog.getResult();
			
			int i=0;
			for(FilterDataItem res : items) {
				System.out.println("\t" + i + ": " + res.label + " -> " + res.checked);
				i++;
			}
		}
	}
}
