package edu.rice.cs.hpc.traceviewer.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import edu.rice.cs.hpc.common.util.ProcedureClassData;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.util.ProcedureClassMap;

/*********************************
 * 
 * Dialog window to show the class and the procedure associated
 *
 */
public class ProcedureClassDialog extends TitleAreaDialog {

	final private String UnknownData = "unknown";
	
	private TableViewer tableViewer ;
	final private ProcedureClassMap data;
	
	private Button btnRemove;
	private Button btnEdit;
	
	private boolean isModified;
	
	/***
	 * constructor 
	 * @param parentShell
	 */
	public ProcedureClassDialog(Shell parentShell, ProcedureClassMap data ) {
		super(parentShell);
		this.data = data;
	}

	/***
	 * return true if the data has been modified
	 * 
	 * @return
	 */
	public boolean isModified() {
		return isModified;
	}
	
	/****
	 * update data in the map and in the table 
	 * it is unfortunate we have two storage of the same data.
	 * 
	 * @param proc
	 * @param procClass
	 * @param rgb
	 */
	private void updateData(String proc, String procClass, RGB rgb) {
		// update the map
		data.put(proc, procClass, rgb);
		
		// update the table of colors
		cacheImages.put(proc, rgb);
		
		isModified = true;
		tableViewer.setInput(data.getEntrySet());
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
		btnAdd.setToolTipText("Add a procedure-color pair");
		btnAdd.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				ProcedureMapDetailDialog dlg = new ProcedureMapDetailDialog(getShell(), 
						"Add a new procedure-color map", "", "", null);
				if (dlg.open() == Dialog.OK) {
					// update the map and the table
					ProcedureClassDialog.this.updateData(dlg.getProcedure(), dlg.getDescription(), dlg.getRGB());
				}
			}
		});
		
		btnRemove   = new Button(areaAction, SWT.PUSH| SWT.FLAT);
		btnRemove.setText("Delete");
		btnRemove.setToolTipText("Remove a selected procedure-color pair");
		btnRemove.setEnabled(false);
		btnRemove.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				remove(e);
			}
		});
		
		btnEdit   = new Button(areaAction, SWT.PUSH| SWT.FLAT);
		btnEdit.setText("Edit");
		btnEdit.setToolTipText("Edit a selected procedure-color pair");
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) 
			{
				IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
				Object item = selection.getFirstElement();
				if (item instanceof Entry<?,?>) {
					final String proc = (String)((Entry<?, ?>) item).getKey();
					final ProcedureClassData pclass = (ProcedureClassData) 
							((Entry<?, ?>)item).getValue();

					ProcedureMapDetailDialog dlg = new ProcedureMapDetailDialog(getShell(), 
							"Edit procedure-color map", proc, pclass.getProcedureClass(), pclass.getRGB());
					if (dlg.open() == Dialog.OK) {
						// update: remove the old data, and then insert a new one
						// Attention: these two actions have to be atomic !
						ProcedureClassDialog.this.data.remove(proc);
						// update the map and the table
						ProcedureClassDialog.this.updateData(dlg.getProcedure(), dlg.getDescription(), dlg.getRGB());
					}
				}
			}
		});
		
		final Button btnReset = new Button(areaAction, SWT.PUSH | SWT.FLAT);
		btnReset.setText("Reset");
		btnReset.setToolTipText("Reset to the default configuration");
		btnReset.addSelectionListener( new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {

				ProcedureClassDialog.this.data.initDefault();
				ProcedureClassDialog.this.tableViewer.setInput(data.getEntrySet());
			}
		});
		
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).grab(true, false).applyTo(areaAction);
		GridLayoutFactory.fillDefaults().numColumns(4).applyTo(areaAction);
		
		//-----------------------------------------------------------------
		// table area
		//-----------------------------------------------------------------
		
		tableViewer = new TableViewer(composite, SWT.MULTI | SWT.VIRTUAL);

		// set procedure column
		final TableViewerColumn colProcedure = new TableViewerColumn(tableViewer, SWT.LEFT);
		TableColumn col = colProcedure.getColumn();
		col.setText("Procedure");
		col.setResizable(true);
		col.setMoveable(true);
		col.setWidth(220);
		
		colProcedure.setLabelProvider( new ClassColumnLabelProvider() );
		new ColumnViewerSorter(tableViewer, colProcedure, COLUMN_ID.PROCEDURE);
		
		// set description column
		final TableViewerColumn colClass = new TableViewerColumn(tableViewer, SWT.LEFT);
		col = colClass.getColumn();
		col.setText("Description");
		col.setResizable(true);
		col.setMoveable(true);
		col.setWidth(150);
		
		colClass.setLabelProvider( new ColumnLabelProvider(){
			public String getText(Object element) {
				return ProcedureClassDialog.this.getClassName(element);
			}
		});
		ColumnViewerSorter sortColClass = new ColumnViewerSorter(tableViewer, colClass, COLUMN_ID.CLASS);
		
		tableViewer.setUseHashlookup(true);
		
		tableViewer.setContentProvider(new ArrayContentProvider());

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo( table );
		
		tableViewer.setInput(data.getEntrySet());
		tableViewer.getTable().addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e) {
				checkButton();
			}
		});
		tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				btnEdit.notifyListeners(SWT.Selection, new Event());
			}
		});
		
		sortColClass.setSorter(sortColClass, Direction.ASC);

		table.pack();

		setTitle("Procedure and color mapping");
		setMessage("Add, remove or edit a procedure-color mapping");
		getShell().setText("Procedure-color mapping");
		
		return composite;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.window.Window#setShellStyle(int)
	 */
	protected void setShellStyle(int newShellStyle) {

		super.setShellStyle(newShellStyle | SWT.RESIZE );
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
		
		// remove the data
		for (Object o: sels) {
			if (o instanceof Entry<?,?>) {
				Entry<?,?> elem = (Entry<?,?>) o;
				data.remove((String) elem.getKey());

				isModified = true;
			}
		}
		// remove the color table
		
		// refresh the table
		refresh();
	}
	
	
	/**
	 * retrieve the class name
	 * @param element
	 * @return
	 */
	private String getClassName(Object element) {
		if (element instanceof Entry<?,?>) {
			final Entry<?,?> oLine = (Entry<?, ?>) element;
			final Object o = oLine.getValue();
			if (o instanceof ProcedureClassData) {
				final ProcedureClassData objValue = (ProcedureClassData) oLine.getValue();
				return objValue.getProcedureClass();
			}
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
			final Entry<?,?> oLine = (Entry<?, ?>) element;
			return (String) oLine.getKey();
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
	
	private ImageCache cacheImages;
	
	/**
	 * 
	 * Label provider for column class
	 *
	 */
	private class ClassColumnLabelProvider extends ColumnLabelProvider 
	{
		
		public ClassColumnLabelProvider() {
			cacheImages = new ImageCache();
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			return ProcedureClassDialog.this.getProcedureName(element);
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ColumnLabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			String key = ProcedureClassDialog.this.getProcedureName(element);
			if (key != UnknownData) {
				// procedure exist
				final ProcedureClassData val = ProcedureClassDialog.this.data.get(key);
				if (val != null) {
					Image image = cacheImages.get(key, val.getRGB());
					return image;
				}
			}
			return null;
		}
		
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
		 */
		public void dispose() {
			// dispose images before disposing the parent
			if (cacheImages != null) {
				cacheImages.dispose();
			}
			// now, dispose the parent
			super.dispose();
		} 
	}
	
	private class ImageCache {
		private HashMap<String,Image> images;

		public ImageCache() {
			images = new HashMap<String, Image>(10);
		}
		
		public Image get(String name, RGB rgb) {
			Image image = images.get(name);
			if (image == null) {
				image = put(name, rgb);
			}
			return image;
		}
		
		public Image put(String name, RGB rgb) {
			Image image = ColorTable.createImage(ProcedureClassDialog.this.getShell().getDisplay(), rgb);
			images.put(name, image);
			return image;
		}
		
		public void dispose() {
			Collection<Image> col = images.values();
			Iterator<Image> iterator = col.iterator();
			while(iterator.hasNext()) {
				Image img = iterator.next();
				img.dispose();
			}
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
		
		ProcedureClassMap pcMap = new ProcedureClassMap(display);
		ProcedureClassDialog dlg = new ProcedureClassDialog(shell, pcMap );

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
