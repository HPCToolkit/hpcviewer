package edu.rice.cs.hpc.traceviewer.ui;

import java.util.Map.Entry;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import edu.rice.cs.hpc.traceviewer.spaceTimeData.ColorTable;
import edu.rice.cs.hpc.traceviewer.util.ProcedureClassMap;

/*********************************
 * 
 * Dialog window to show the class and the procedure associated
 *
 */
public class ProcedureClassDialog extends TitleAreaDialog {

	final private String UnknownData = "unknown";
	
	private TableViewer tableViewer ;
	final private ProcedureClassMap data;
	final private ColorTable colorTable;
	
	private Button btnRemove;
	private Button btnEdit;
	
	private boolean isModified;
	
	/***
	 * constructor 
	 * @param parentShell
	 */
	public ProcedureClassDialog(Shell parentShell, ProcedureClassMap data, ColorTable colorTable ) {
		super(parentShell);
		this.data = data;
		this.colorTable = colorTable;
	}

	/***
	 * return true if the data has been modified
	 * 
	 * @return
	 */
	public boolean isModified() {
		return isModified;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		
		final Composite composite = new Composite(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(composite);
		
		//-----------------------------------------------------------------
		// Toolbar area
		//-----------------------------------------------------------------
		
		final Composite areaAction = new Composite( composite, SWT.NULL );
		
		final Button btnAdd   = new Button(areaAction, SWT.PUSH | SWT.FLAT);
		btnAdd.setText("Add");
		btnAdd.setToolTipText("Add a class-procedure pair");
		btnAdd.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				ProcedureMapDetailDialog dlg = new ProcedureMapDetailDialog(getShell(), 
						"Add a new procedure-class map", "", "");
				if (dlg.open() == Dialog.OK) {
					ProcedureClassDialog.this.data.put(dlg.getProcedure(), dlg.getProcedureClass());
					isModified = true;
					ProcedureClassDialog.this.refresh();
				}
			}
		});
		
		btnRemove   = new Button(areaAction, SWT.PUSH| SWT.FLAT);
		btnRemove.setText("Delete");
		btnRemove.setToolTipText("Remove a selected class-procedure pair");
		btnRemove.setEnabled(false);
		btnRemove.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				remove(e);
			}
		});
		
		btnEdit   = new Button(areaAction, SWT.PUSH| SWT.FLAT);
		btnEdit.setText("Edit");
		btnEdit.setToolTipText("Edit a selected class-procedure pair");
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) 
			{
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				Object item = selection.getFirstElement();
				if (item instanceof Entry<?,?>) {
					final String proc = (String)((Entry<?, ?>) item).getKey();
					final String pclass = (String) ((Entry<?, ?>)item).getValue();
					
					ProcedureMapDetailDialog dlg = new ProcedureMapDetailDialog(getShell(), 
							"Edit procedure-class map", proc, pclass);
					if (dlg.open() == Dialog.OK) {
						ProcedureClassDialog.this.data.remove(proc);
						ProcedureClassDialog.this.data.put(dlg.getProcedure(), dlg.getProcedureClass());
						isModified = true;
						ProcedureClassDialog.this.refresh();
					}
				}
			}
		});
				
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(areaAction);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(areaAction);
		
		//-----------------------------------------------------------------
		// table area
		//-----------------------------------------------------------------
		
		tableViewer = new TableViewer(composite, SWT.MULTI | SWT.VIRTUAL);
		
		final TableViewerColumn colClass = new TableViewerColumn(tableViewer, SWT.LEFT);
		TableColumn col = colClass.getColumn();
		col.setText("Class");
		col.setResizable(true);
		col.setMoveable(true);
		col.setWidth(120);
		
		colClass.setLabelProvider( new ClassColumnLabelProvider() );
		ColumnViewerSorter sortColClass = new ColumnViewerSorter(tableViewer, colClass, COLUMN_ID.CLASS);

		final TableViewerColumn colProcedure = new TableViewerColumn(tableViewer, SWT.LEFT);
		col = colProcedure.getColumn();
		col.setText("Procedure");
		col.setResizable(true);
		col.setMoveable(true);
		col.setWidth(220);
		
		colProcedure.setLabelProvider( new ColumnLabelProvider(){
			public String getText(Object element) {
				return ProcedureClassDialog.this.getProcedureName(element);
			}
		});
		new ColumnViewerSorter(tableViewer, colProcedure, COLUMN_ID.PROCEDURE);
		
		tableViewer.setUseHashlookup(true);
		
		tableViewer.setContentProvider(new ArrayContentProvider());

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.pack();
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo( table );
		
		tableViewer.setInput(data.getEntrySet());
		tableViewer.getTable().addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				checkButton();
			}
		});
		
		sortColClass.setSorter(sortColClass, Direction.ASC);
		setTitle("Procedure and class map");
		setMessage("Add, remove or edit a procedure-class mapping");
		getShell().setText("Procedure-class map");
		
		return composite;
	}
	
	/***
	 * check and set button status
	 */
	private void checkButton() {
		
		int numSelection = tableViewer.getTable().getSelectionCount();

		btnEdit.setEnabled(numSelection == 1);		
		btnRemove.setEnabled(numSelection>0);
	}
	
	/***
	 * refresh the data and reset the table
	 */
	private void refresh() {
		tableViewer.setInput(data.getEntrySet());
		tableViewer.refresh();
		checkButton();
	}
	/***
	 * removing selected element in the table
	 * @param event
	 */
	private void remove(SelectionEvent event) {
		IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
		Object sels[] = selection.toArray();
		
		boolean cont = false;
		if (sels != null) {
			if (sels.length>1) {
				cont = MessageDialog.openQuestion(getShell(), "Removing " + sels.length+ " mappings", 
						"Are you sure to remove " + sels.length + " mapping elements ?");
			} else {
				cont = MessageDialog.openQuestion(getShell(), "Removing an element", 
						"Are you sure to remove this mapping element ?" );
			}
		}
		if (!cont)
			return;
		
		for (Object o: sels) {
			if (o instanceof Entry<?,?>) {
				Entry<?,?> elem = (Entry<?,?>) o;
				data.remove((String) elem.getKey());

				isModified = true;
			}
		}
		refresh();
	}
	
	
	/**
	 * retrieve the class name
	 * @param element
	 * @return
	 */
	private String getClassName(Object element) {
		if (element instanceof Entry<?,?>) {
			Entry<?,?> oLine = (Entry<?,?>) element;
			String val = (String) oLine.getValue();
			return val;
		}
		return UnknownData;	
	}
	
	/***
	 * retrieve the procedure name
	 * @param element
	 * @return
	 */
	private String getProcedureName(Object element) {
		if (element instanceof Entry<?,?>) {
			Entry<?,?> oLine = (Entry<?,?>) element;
			String key = (String) oLine.getKey();
			return key;
		}
		return UnknownData;
	}
	
	
	/***
	 * enumeration type to determine the sorting: ascending or descending 
	 *
	 */
	static private enum Direction {ASC, DESC};
	static private enum COLUMN_ID {CLASS, PROCEDURE};
	
	/***
	 * 
	 * Sorting a column
	 *
	 */
	private class ColumnViewerSorter extends ViewerComparator
	{
		private final TableViewerColumn column;
		private final TableViewer viewer;
		private Direction direction = Direction.ASC;
		private final COLUMN_ID colid;
		
		/**
		 * Initialization to sort a column of a table viewer
		 * 
		 * @param viewer
		 * @param column : column to sort
		 */
		public ColumnViewerSorter(TableViewer viewer, TableViewerColumn column, COLUMN_ID columnID) {
			this.viewer = viewer;
			this.column = column;
			this.colid = columnID;
			
			column.getColumn().addSelectionListener( new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					ViewerComparator comparator = ColumnViewerSorter.this.viewer.getComparator();
					if ( comparator != null ) {
						if (comparator == ColumnViewerSorter.this) {
							Direction dir = ColumnViewerSorter.this.direction;
							dir = (dir==Direction.ASC? Direction.DESC : Direction.ASC);
							setSorter (ColumnViewerSorter.this, dir);
							return;
						}
					}
					setSorter (ColumnViewerSorter.this, Direction.ASC);
				}
			});
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public int compare(Viewer viewer, Object e1, Object e2) {
			final String elem1 = (colid == COLUMN_ID.CLASS ? 
					ProcedureClassDialog.this.getClassName(e1) : 
					ProcedureClassDialog.this.getProcedureName(e1));
			final String elem2 = (colid == COLUMN_ID.CLASS ? 
					ProcedureClassDialog.this.getClassName(e2) : 
					ProcedureClassDialog.this.getProcedureName(e2));
			
			int k = (direction == Direction.ASC ? 1 : -1 );
			int res = k * super.compare(viewer, elem1, elem2);
			return res;
		}

		/****
		 * 
		 * @param sorter
		 * @param dir
		 */
		public void setSorter(ColumnViewerSorter sorter, Direction dir) {
			column.getColumn().getParent().setSortColumn(column.getColumn());
			sorter.direction = dir;
			if( direction == Direction.ASC ) {
				column.getColumn().getParent().setSortDirection(SWT.DOWN);
			} else {
				column.getColumn().getParent().setSortDirection(SWT.UP);
			}
			
			if( viewer.getComparator() == sorter ) {
				viewer.refresh();
			} else {
				viewer.setComparator(sorter);
			}
			ProcedureClassDialog.this.checkButton();
		}
	}
	
	
	/**
	 * 
	 * Label provider for column class
	 *
	 */
	private class ClassColumnLabelProvider extends ColumnLabelProvider 
	{
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return ProcedureClassDialog.this.getClassName(element);
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			String key = ProcedureClassDialog.this.getProcedureName(element);
			if (key != UnknownData) {
				return colorTable.getImage(key);
			}
			return null;
		}
	}
	
	/***
	 * unit test
	 * 
	 * @param argv
	 */
	static public void main(String argv[]) {
		Display display = new Display ();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		
		shell.open();
		
		ProcedureClassMap pcMap = new ProcedureClassMap();
		ColorTable ct = new ColorTable(display);
		Object list[] = pcMap.getEntrySet();
		for (Object o: list) {
			Entry<?,?> entry = (Entry<?,?>) o;
			ct.addProcedure((String) entry.getKey());
		}
		ct.setColorTable();
		ProcedureClassDialog dlg = new ProcedureClassDialog(shell, pcMap, ct );

		if ( dlg.open() == Dialog.OK ) {
			if (dlg.isModified()) {
				pcMap.save();
			}
		}
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
		display.dispose();
	}
}
