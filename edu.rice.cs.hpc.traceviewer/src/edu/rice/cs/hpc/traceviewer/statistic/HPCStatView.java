package edu.rice.cs.hpc.traceviewer.statistic;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.services.SummaryDataService;
import edu.rice.cs.hpc.traceviewer.ui.AbstractDynamicView;


/*************************************************************************
 * 
 * A view to show the statistics of the current region selection
 *
 *************************************************************************/
public class HPCStatView extends AbstractDynamicView 
{
	public static final String ID = "hpcstat.view";
	
	private static final String UNIDENTIFIED_PROC_OBJECT = "<unknown>";
	
	private TableViewer tableViewer;
	private TableStatComparator comparator;
	
	private ArrayList<StatisticItem> listItems;	
	private ColorTable colorTable = null;

	public HPCStatView() {
		super();
		listItems   = null;
		tableViewer = null;
	}

	@Override
	public void createPartControl(Composite parent) {
		
		final Composite tableComposite = new Composite(parent, SWT.NONE);
		
		tableViewer = new TableViewer(tableComposite, SWT.BORDER|SWT.VIRTUAL | SWT.SINGLE | SWT.READ_ONLY);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableComposite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tableViewer.getTable());
		
		GridLayoutFactory.fillDefaults().applyTo(tableComposite);
		GridLayoutFactory.fillDefaults().applyTo(tableViewer.getTable());
		
		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		// column for procedure name
		final TableViewerColumn colProc  = new TableViewerColumn(tableViewer, SWT.LEFT, 0);
		TableColumn column = colProc.getColumn();
		
		column.setText("Procedure");
		column.setWidth(120);
		column.addSelectionListener(getSelectionAdapter(column, 0));
		
		// column for the percentage
		final TableViewerColumn colCount = new TableViewerColumn(tableViewer, SWT.LEFT, 1);
		column = colCount.getColumn();
		
		column.setText("Percent");
		column.setWidth(80);
		column.setAlignment(SWT.RIGHT);
		column.addSelectionListener(getSelectionAdapter(column, 1));

		// setup the table viewer
		tableViewer.setContentProvider(new StatisticContentProvider());		
		tableViewer.setLabelProvider(new TableStatLabelProvider());
		
		comparator = new TableStatComparator();
		tableViewer.setComparator(comparator);
		
		getSite().setSelectionProvider(tableViewer);

		// listen to any changes about the database from summary view 
		ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		ISourceProvider serviceProvider = service.getSourceProvider(SummaryDataService.DATA_PROVIDER);
		serviceProvider.addSourceProviderListener(new StatSourceProvider());
	}
	
	
	/*****
	 * Create and return a new selection adapter for a given column
	 * 
	 * @param column
	 * @param index
	 * @return
	 */
	private SelectionAdapter getSelectionAdapter(final TableColumn column,
										 		 final int index) {
		SelectionAdapter adapter = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparator.setColumn(index);
				int dir = comparator.getDirection();
				tableViewer.getTable().setSortDirection(dir);
				tableViewer.getTable().setSortColumn(column);
				tableViewer.refresh();
			}
		};
		return adapter;
	}
	
	/**
	 * Refresh the content of the viewer and reinitialize the content
	 *  with the new data
	 * 
	 * @param palette
	 * @param mapPixelToCount
	 * @param colorTable
	 * @param totalPixels
	 */
	private void refresh(
			PaletteData palette,
			AbstractMap<Integer, Integer> mapPixelToCount, 
			ColorTable colorTable, 
			int totalPixels) {
		
		this.colorTable = colorTable;
		this.listItems  = new ArrayList<>();
		
		Set<Integer> set = mapPixelToCount.keySet();
		
		for(Iterator<Integer> it = set.iterator(); it.hasNext(); ) {
			final Integer pixel = it.next();
			final Integer count = mapPixelToCount.get(pixel);
			final RGB rgb	 	= palette.getRGB(pixel);
			
			String proc = colorTable.getProcedureNameByColorHash(rgb.hashCode());
			if (proc == null) {
				proc = UNIDENTIFIED_PROC_OBJECT;
			}
			listItems.add(new StatisticItem(proc, (float)100.0 * count/totalPixels));
		}
		tableViewer.setInput(listItems);
		tableViewer.refresh();
	}

	/*************************************************************
	 * 
	 * Content provider for the table in statistic view
	 *
	 *************************************************************/
	static private class StatisticContentProvider 
	implements IStructuredContentProvider
	{
		StatisticContentProvider() {
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement == null)
				return null;
			
			ArrayList<StatisticItem> list = (ArrayList<HPCStatView.StatisticItem>) inputElement;
			int size = list.size();
			StatisticItem []items = new StatisticItem[size];
			
			list.toArray(items);
			
			return items;
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			
		}
	}
	
	
	/*************************************************************
	 * 
	 * Label provider for the stat label
	 *
	 *************************************************************/
	private class TableStatLabelProvider implements ITableLabelProvider
	{		
		public TableStatLabelProvider() {
		}
		
		@Override
		public void addListener(ILabelProviderListener listener) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				if (element != null && element instanceof StatisticItem) {
					final StatisticItem item = (StatisticItem) element;
					if (item.procedureName == UNIDENTIFIED_PROC_OBJECT)
						return null;
					
					return HPCStatView.this.colorTable.getImage(((StatisticItem)element).procedureName);
				}
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element == null || !(element instanceof StatisticItem))
				return null;
			
			StatisticItem item = (StatisticItem) element;

			switch(columnIndex) {
			case 0:					
				return item.procedureName;
				
			case 1:
				return String.format("%.2f %%", item.percent);
			}
			return null;
		}
	}
	
	
	/*************************************************************
	 * 
	 * class to manage changes in the source provider
	 *
	 *************************************************************/
	private class StatSourceProvider implements ISourceProviderListener 
	{	
		@Override
		public void sourceChanged(int sourcePriority, String sourceName, Object sourceValue) {
			if (sourceName.equals(SummaryDataService.DATA_PROVIDER)) {
				if (sourceValue == null) return;
				
				SummaryDataService.SummaryData data = (SummaryDataService.SummaryData) sourceValue;
				HPCStatView.this.refresh(data.palette, data.mapPixelToCount, data.colorTable, data.totalPixels);
			}
		}
		
		@Override
		public void sourceChanged(int sourcePriority, Map sourceValuesByName) {
		}
	}
	
	/*************************************************************
	 * 
	 * Pair procedure and its percentage count
	 *
	 *************************************************************/
	static public class StatisticItem
	{
		public String procedureName;
		public float  percent;
		
		StatisticItem(String procName, float percent) {
			this.procedureName = procName;
			this.percent 	   = percent;
		}
	}


	@Override
	public void setFocus() {
		tableViewer.getTable().setFocus();
	}

	
	@Override
	public void active(boolean isActive) {
		if (isActive) {
			final IWorkbenchPage activePage = getViewSite().getPage();
			ISourceProviderService provider = activePage.getWorkbenchWindow().getService(ISourceProviderService.class);
			SummaryDataService service = (SummaryDataService) provider.getSourceProvider(SummaryDataService.DATA_REQUEST);
			service.requestData();
		}		
	}	
}
