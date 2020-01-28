package edu.rice.cs.hpc.traceviewer.misc;

import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.string.StringUtil;
import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.DepthOperation;
import edu.rice.cs.hpc.traceviewer.operation.PositionOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;
import edu.rice.cs.hpc.traceviewer.services.DataService;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.Position;
import edu.rice.cs.hpc.traceviewer.data.graph.CallPath;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;


/**************************************************
 * A viewer for CallStackSamples.
 *************************************************/
public class CallStackViewer extends TableViewer
	implements IOperationHistoryListener
{
	private final TableViewerColumn viewerColumn;
	
	private final static String EMPTY_FUNCTION = "--------------";
	
	private final ProcessTimelineService ptlService;
	
	private final DataService dataService;
	
    /**Creates a CallStackViewer with Composite parent, SpaceTimeDataController _stData, and HPCTraceView _view.*/
	public CallStackViewer(Composite parent, final HPCCallStackView csview)
	{
		super(parent, SWT.SINGLE | SWT.READ_ONLY );
		
		final IWorkbenchWindow window = (IWorkbenchWindow)csview.getSite().
				getWorkbenchWindow();
		final ISourceProviderService service = (ISourceProviderService) window.getService(ISourceProviderService.class);

		dataService = (DataService) service.getSourceProvider(DataService.DATA_PROVIDER);
				
		ptlService = (ProcessTimelineService) service.getSourceProvider(ProcessTimelineService.PROCESS_TIMELINE_PROVIDER);
		
        final Table stack = this.getTable();
        
        GridData data = new GridData(GridData.FILL_BOTH);
        stack.setLayoutData(data);
        
        //------------------------------------------------
        // add content provider
        //------------------------------------------------
        this.setContentProvider( new IStructuredContentProvider(){

			public void dispose() {}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) { }

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof ArrayList<?>) {
					Object o[] = ((ArrayList<?>) inputElement).toArray();
					return o;
				}
				return null;
			}
        	
        });
        
        stack.setVisible(false);
        final CallStackViewer csviewer = this;
		stack.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event)
			{
				int depth = stack.getSelectionIndex(); 

				// ask the depth editor to update the depth and launch the updateDepth event
				csview.depthEditor.setSelection(depth);
				notifyChange(depth);
			}
		});
		
        //------------------------------------------------
        // add label provider
        //------------------------------------------------

		final ColumnLabelProvider myLableProvider = new ColumnLabelProvider() {
        	public Image getImage(Object element) {
        		if (element instanceof String) {
        			if (element == EMPTY_FUNCTION)
        				return null;
        			
        			Image img = null;
        			
    				SpaceTimeDataController stData = dataService.getData();
        			if (stData != null) {
        				img = stData.getColorTable().getImage((String)element);
        			}
        			return img;
        		}
        		
				return null;        		
        	}
        	
        	public String getText(Object element)
        	{
        		if (element instanceof String)
        			return (String) element;
        		return null;
        	}
        	
        	public String getToolTipText(Object element)
        	{
        		final String originalText = getText(element);
        		return StringUtil.wrapScopeName(originalText, 100);
        	}
        	
        	public int getToolTipDisplayDelayTime(Object object)
        	{
        		return Constants.TOOLTIP_DELAY_MS;
        	}
		};
		viewerColumn = new TableViewerColumn(this, SWT.NONE);
		viewerColumn.setLabelProvider(myLableProvider);
		viewerColumn.getColumn().setWidth(100);
		getTable().setVisible(true);

		ColumnViewerToolTipSupport.enableFor(csviewer, ToolTip.NO_RECREATE);
		
		TraceOperation.getOperationHistory().addOperationHistoryListener(this);
	}
	
	/***
	 * refresh the call stack in case there's a new data
	 */
	public void updateView()
	{
		final SpaceTimeDataController data = dataService.getData();
		this.setSample(data.getAttributes().getPosition(), data.getAttributes().getDepth());
	}
	
	/**********************************************************************
	 * Sets the sample displayed on the callstack viewer to be the one
	 * that most closely corresponds to (closeTime, process). Additionally,
	 * sets the depth to _depth.
	 *********************************************************************/
	public void setSample(Position position, final int depth)
	{
		//-------------------------------------------------------------------------------------------
		// dirty hack: the call stack viewer requires relative index of process, not the absolute !
		// so if the region is zoomed, then the relative index is based on the displayed processes
		//
		// however, if the selected process is less than the start of displayed process, 
		// 	then we keep the selected process
		//-------------------------------------------------------------------------------------------

		SpaceTimeDataController stData = dataService.getData();
		
		if (stData == null) {
			return;
		}
		// general case
		final ImageTraceAttributes attributes = stData.getAttributes();
    	int estimatedProcess = (attributes.getPosition().process - attributes.getProcessBegin());
    	int numDisplayedProcess = ptlService.getNumProcessTimeline();
    	
    	// case for num displayed processes is less than the number of processes
    	estimatedProcess = (int) ((float)estimatedProcess* 
    			((float)numDisplayedProcess/(attributes.getProcessInterval())));
    	
    	// case for single process
    	estimatedProcess = Math.min(estimatedProcess, numDisplayedProcess-1);

		ProcessTimeline ptl = ptlService.getProcessTimeline(estimatedProcess);
		if (ptl != null) {
			int sample = 0;
			
			try {
				sample = ptl.findMidpointBefore(position.time, stData.isEnableMidpoint());				
			} catch (Exception e) {
				// Error: data has changed (resize, zoom-in/out, ...) but we are not notified yet.
				// let the new thread finish the job
				Debugger.printTimestampDebug("CSV: Fail to get sample for time " + position.time);
				return;
			}
			final Vector<String> sampleVector;
			if (sample>=0) {
				final CallPath cp = ptl.getCallPath(sample, depth);
				if (cp != null)
					sampleVector = ptl.getCallPath(sample, depth).getFunctionNames();
				else
					// empty array of string
					sampleVector = new Vector<String>();

				if (sampleVector != null && sampleVector.size()<=depth)
				{
					//-----------------------------------
					// case of over depth
					//-----------------------------------
					final int numOverDepth = depth-sampleVector.size()+1;
					for(int l = 0; l<numOverDepth; l++)
						sampleVector.add(EMPTY_FUNCTION);
				}
			} else {
				// empty array of string
				sampleVector = new Vector<String>();
				
				for(int l = 0; l<=depth; l++)
					sampleVector.add(EMPTY_FUNCTION);
			}
			// fill the call stack and select the current depth
			final Display display = Display.getDefault();
			display.asyncExec( new Runnable() {
				
				@Override
				public void run() {
					setInput(new ArrayList<String>(sampleVector));
					selectDepth(depth);
					viewerColumn.getColumn().pack();
				}
			} );
		}
		else
		{
			Debugger.printTrace("CSV traces: ");
		}
	}
	
	/**Sets the viewer's depth to _depth.*/
	public void setDepth(int _depth)
	{
		final int itemCount = this.getTable().getItemCount();
		if (itemCount<=_depth)
		{
			//-----------------------------------
			// case of over depth
			//-----------------------------------
			final int overDepth = _depth - itemCount + 1;
			for (int i=0; i<overDepth; i++) 
			{
				this.add(EMPTY_FUNCTION);
			}
		}
		selectDepth(_depth);
		
		notifyChange(_depth);
	}
	
	
	/*****
	 * Select a specified depth in the call path
	 * @param _depth
	 */
	private void selectDepth(final int _depth)
	{
		this.getTable().select(_depth);
		this.getTable().redraw();
	}
	
	
	private void notifyChange(int depth)
	{
		try {
			DepthOperation op = new DepthOperation("Set depth to "+depth, depth);
			IStatus status = TraceOperation.getOperationHistory().execute(
					op, null, null);
			if (status.isOK()) {
				op.dispose();
			}
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void historyNotification(final OperationHistoryEvent event) {
		final IUndoableOperation operation = event.getOperation();

		if (operation.hasContext(BufferRefreshOperation.context) ||
				operation.hasContext(PositionOperation.context)) {
			if (event.getEventType() == OperationHistoryEvent.DONE) {
				updateView();
			}
		}
	}
}
