package edu.rice.cs.hpc.traceviewer.filter;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.common.util.UserInputHistory;
import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.data.trace.Filter;
import edu.rice.cs.hpc.data.trace.FilterSet;


/*****
 * 
 * Filter dialog to create/edit filter glob pattern of processes
 *
 */
public class FilterDialog extends TitleAreaDialog {

	private List list;
	private IFilteredData filterData;
	private Button btnRemove;
	private Button btnShow;
	
	private static final String THREAD_FILTER_KEY = "thread_filter";
	private static final String PROCESS_FILTER_KEY = "process_filter";
	/****
	 * constructor for displaying filter glob pattern
	 * @param parentShell
	 */
	public FilterDialog(Shell parentShell, IFilteredData filteredBaseData) {
		super(parentShell);
		filterData = filteredBaseData;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Group grpMode = new Group(composite, SWT.NONE);
		grpMode.setText("Mode of filter");
		
		btnShow = new Button(grpMode, SWT.RADIO);
		btnShow.setText("To show");
		btnShow.setToolTipText("An option to show matching patterns");

		Button btnHide = new Button(grpMode, SWT.RADIO);
		btnHide.setText("To hide");
		btnHide.setToolTipText("An option to hide matching patterns");
		
		
		FilterSet filter = null;
		if (filterData != null) {
			filter = filterData.getFilter();
			
			if (filter != null && filter.isShownMode())
				btnShow.setSelection(true);
			else
				btnHide.setSelection(true);
		}
		
		Label lblMode = new Label(grpMode, SWT.LEFT | SWT.WRAP);
		lblMode.setText("Selecting the 'To show' radio button will show matching processes, " +
						 "while selecting the 'To hide' button \nwill hide them.");
		
		GridDataFactory.swtDefaults().span(2, 1).grab(true, false).applyTo(lblMode);
		
		GridDataFactory.swtDefaults().grab(true, false).applyTo(grpMode);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(grpMode);

		Group grpFilter = new Group(composite, SWT.NONE);
		grpFilter.setText("Filter");

		GridDataFactory.fillDefaults().grab(true, true).applyTo(grpFilter);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(grpFilter);

		Composite coButtons = new Composite(grpFilter, SWT.NONE);

		GridDataFactory.fillDefaults().grab(false, true).applyTo(coButtons);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(coButtons);
		
		Button btnAdd = new Button(coButtons, SWT.PUSH | SWT.FLAT);
		btnAdd.setText("Add");
		btnAdd.setToolTipText("Add a new filtering pattern");
		btnAdd.addSelectionListener( new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				String thread_title = null, process_title = "Rank";
				String message;
				
				if (filterData.isHybridRank()) {
					thread_title  = "Thread";
					process_title = "Process";
					message  	  = "Please type a pattern in the format minimum:maximum:stride.\n" + 
							"Any omitted or invalid sections will match as many processes \nor threads as possible.\n\n" +
							"For instance, 3:7:2 in the process box with the thread box empty \nwill match all threads of processes 3, 5, and 7.\n"+
							"1 in the thread box with the process box empty will match \nthread 1 of all processes.\n"+
							"1::2 in the process box and 2:4:2 in the thread box will match \n1.2, 1.4, 3.2, 3.4, 5.2 ...";
 				} else {
 					message = "Please type a pattern in the format minimum:maximum:stride.\n" + 
 							"Any omitted or invalid sections will match as many ranks as possible.\n\n" +
 							"For instance, 3:7:2  will match all ranks 3, 5, and 7.\n";
 				}
				DualInputDialog dlg = new DualInputDialog(getShell(), "Add a pattern", message, 
						process_title, thread_title, PROCESS_FILTER_KEY, THREAD_FILTER_KEY);
				if (dlg.open() == Dialog.OK) {
					list.add(dlg.getValue());
					checkButtons();
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnAdd);
		//btnAdd.setLayoutData(new RowData(80,20));
		
		btnRemove = new Button(coButtons, SWT.PUSH | SWT.FLAT);
		btnRemove.setText("Remove");
		btnRemove.setToolTipText("Remove a selected filtering pattern");
		btnRemove.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int i = list.getSelectionCount();
				if (i > 0) {
					final String item = list.getSelection()[0];
					list.remove(item);
					checkButtons();
				}
			}
		});
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnRemove);
		//btnRemove.setLayoutData(new RowData(80,20));
		
		final Button btnRemoveAll = new Button(coButtons, SWT.PUSH | SWT.FLAT);
		btnRemoveAll.setText("Remove all");
		btnRemoveAll.setToolTipText("Remove all filtering patterns");
		btnRemoveAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int count = list.getItemCount();
				if (count>0) {
					if (MessageDialog.openQuestion(getShell(), "Remove all patterns",
							"Are you sure to remove all " + count + " patterns ?")) {
						list.removeAll();
						checkButtons();
					}
				}
			}
		}) ;
		GridDataFactory.fillDefaults().grab(true, false).applyTo(btnRemoveAll);
		//btnRemoveAll.setLayoutData(new RowData(80,20));
		
		list = new List(grpFilter, SWT.SINGLE | SWT.V_SCROLL);
		list.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				checkButtons();
			}			
		});
		GridDataFactory.fillDefaults().grab(true, true).hint(40, 80).applyTo(list);
		
		this.setMessage("Add/remove glob patterns to filter displayed processes");
		this.setTitle("Filter patterns");
		
		// add pattern into the list
		if (filterData != null && filter != null && filter.getPatterns() != null) {
			for (Filter flt : filter.getPatterns()) {
				list.add(flt.toString());
			}
		}

		checkButtons();
		
		return parent;
	}
	
	
	private void checkButtons() {
		boolean selected = (list.getSelectionCount()>0);
		btnRemove.setEnabled(selected);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#setShellStyle(int)
	 */
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(newShellStyle | SWT.RESIZE);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		ArrayList<Filter> filterList = new ArrayList<Filter>();
		for(int i=0; i<list.getItemCount(); i++) {
			String item = list.getItem(i);
			filterList.add(new Filter(item));
		}
		FilterSet filterSet = filterData.getFilter();
		
		// put the glob pattern back
		filterSet.setPatterns(filterList);
		// set the show mode (to show or to hide)
		filterSet.setShowMode( btnShow.getSelection() );
		
		// check if the filter is correct
		filterData.setFilter(filterSet);
		if (filterData.isGoodFilter())
			super.okPressed();
		else {
			// it is not allowed to filter everything
			MessageDialog.openError(getShell(), "Error", 
					"The result of filter is empty ranks.\nIt isn't allowed to filter all the ranks.");
		}
	}
	
	static public void main(String []args) {
		Shell shell = new Shell();
		FilterDialog dlgMain = new FilterDialog(shell, null);
		dlgMain.open();
	}
}

/*******
 * 
 * Input dialog with two input fields using user history
 *
 */
class DualInputDialog extends Dialog{
	private Combo firstEntry;
	private Combo secondEntry;
	final String message;
	final String prompt1;
	final String prompt2;
	final String title;
	final UserInputHistory firstHistory;
	final UserInputHistory secondHistory;
	String value;

	
	public DualInputDialog(Shell parentShell, String dialogTitle,
			String dialogMessage, String firstPrompt, String secondPrompt, String firstHistoryKey, String secondHistoryKey) {
		super(parentShell);
		title = dialogTitle;
		message = dialogMessage;
		prompt1 = firstPrompt;
		prompt2 = secondPrompt;
		
		firstHistory = new UserInputHistory(firstHistoryKey);
		secondHistory = new UserInputHistory(secondHistoryKey);
		
	}
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);
		GridDataFactory.swtDefaults().grab(true, true).align(SWT.CENTER, SWT.CENTER).applyTo(composite);

		Label prompt = new Label(composite, SWT.NONE);
		prompt.setText(message);

		
		Group fieldArea = new Group(composite, SWT.SHADOW_IN);
		
		Label firstLabel = new Label(fieldArea, SWT.NONE);
		firstLabel.setText(prompt1);
		firstEntry = new Combo(fieldArea, SWT.SINGLE | SWT.BORDER);
		firstEntry.setItems(firstHistory.getHistory());
		
		if (prompt2 != null)
		{
			Label secondLabel = new Label(fieldArea, SWT.NONE);
			secondLabel.setText(prompt2);
			secondEntry = new Combo(fieldArea, SWT.SINGLE | SWT.BORDER);
			secondEntry.setItems(secondHistory.getHistory());			
		}

		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(2, 4, 3, 5).generateLayout(fieldArea);
		GridLayoutFactory.fillDefaults().numColumns(1).extendedMargins(10, 10, 10, 10).generateLayout(composite);
		return composite;
	}
	
	@Override
	protected void okPressed() {
		value =  firstEntry.getText() + Filter.PROCESS_THREAD_SEPARATOR;
		if (secondEntry != null) {
			value += secondEntry.getText();
			secondHistory.addLine(secondEntry.getText());
		}
		firstHistory.addLine(firstEntry.getText());
		super.okPressed();
	}
	public String getValue() {
		return value;
	}
	
	static public void main(String []argv) {
		Shell shell = new Shell();
		DualInputDialog dlg = new DualInputDialog(shell, "test input", 
				"just a message asd srjt jlkbdfkrejldf ajlwi more asdkhuiq eger \n\n more text alks jlrk adsgf\nja reiotp", "Process", "Threads", "hist_1", "hist_2");
		dlg.open();
	}
}

