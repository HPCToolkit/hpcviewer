package edu.rice.cs.hpc.test.gui;

import static org.junit.Assert.*;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.rice.cs.hpc.viewer.metric.MetricColumnDialog;
import edu.rice.cs.hpc.viewer.util.FilterDataItem;

public class MetricColumnDialogTest 
{
	final static private int MAX_ITEMS = 10;
	private MetricColumnDialog dialog;
	
	@Before
	public void setUp() throws Exception {
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);
		FilterDataItem []items = new FilterDataItem[MAX_ITEMS];
		
		for(int i=0; i<MAX_ITEMS; i++) {
			items[i].label   = "Column "+String.valueOf(i);
			items[i].checked = (i < 5);
			items[i].enabled = i > 3;
		}
		dialog = new MetricColumnDialog(shell, items);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testOkPressed() {
		if (dialog.open() == Window.OK) {
			FilterDataItem[]result = dialog.getResult();
			assertNotNull(result);
		}
	}

}
