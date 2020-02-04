package edu.rice.cs.hpc.viewer.metric;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.Preferences;

import edu.rice.cs.hpc.common.util.UserInputHistory;
import edu.rice.cs.hpc.viewer.util.AbstractFilterDialog;
import edu.rice.cs.hpc.viewer.util.FilterDataItem;

/*************************************************************
 * 
 * Dialog window to hide/show metric columns
 *
 *************************************************************/
public class MetricColumnDialog extends AbstractFilterDialog 
{
	static final private String HISTORY_COLUMN_PROPERTY = "column_property";
	static final private String HISTORY_APPLY_ALL = "apply-all";

	protected Button btnApplyToAllViews;
	protected boolean isAppliedToAllViews = false;
	private boolean applyToAllViewOption = true;

	/****
	 * Constructor of the class.
	 * 
	 * @param parentShell : the current shell
	 * @param label : set of labels of the metrics
	 * @param checked : set of boolean whether the metric is hidden/showed
	 */
	public MetricColumnDialog(Shell parentShell, List<FilterDataItem> items) {
		super(parentShell, "Column Selection", 
				"Check columns to be shown and uncheck columns to be hidden", 
				items);
	}
	
	
	public void enableAllViewOption(boolean applyToAllViewOption)
	{
		this.applyToAllViewOption = applyToAllViewOption;
	}
	

	/**
	 * Return the status if the modification is to apply to all views or not
	 * @return
	 */
	public boolean isAppliedToAllViews() {
		return this.isAppliedToAllViews;
	}


	@Override
	protected void createAdditionalButton(Composite parent) {

		btnApplyToAllViews = new Button(parent, SWT.CHECK);
		btnApplyToAllViews.setText("Apply to all views");
		btnApplyToAllViews.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		btnApplyToAllViews.setEnabled(applyToAllViewOption);
		if (applyToAllViewOption) {
			// Laks 2009.01.26: by default, we apply for all views
			btnApplyToAllViews.setSelection( getHistory() );
		}
	}
	
	@Override
	protected void okPressed() {
		isAppliedToAllViews = btnApplyToAllViews.getSelection();
		setHistory(isAppliedToAllViews);
		super.okPressed();
	}
	
	/***
	 * get the user preference of "apply-all"
	 * @return
	 */
	private boolean getHistory() {
		return UserInputHistory.getPreference(HISTORY_COLUMN_PROPERTY).getBoolean(HISTORY_APPLY_ALL, true);
	}
	
	/***
	 * set the user preference of "apply-all"
	 * @param value
	 */
	private void setHistory( boolean value ) {
		Preferences pref = UserInputHistory.getPreference(HISTORY_COLUMN_PROPERTY);
		pref.putBoolean(HISTORY_APPLY_ALL, value);
		UserInputHistory.setPreference(pref);
	}
}
