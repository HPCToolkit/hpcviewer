package edu.rice.cs.hpc.traceviewer.depth;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.common.ui.Util;
import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.PositionOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;
import edu.rice.cs.hpc.traceviewer.operation.ZoomOperation;
import edu.rice.cs.hpc.traceviewer.painter.AbstractTimeCanvas;
import edu.rice.cs.hpc.traceviewer.painter.BaseViewPaint;
import edu.rice.cs.hpc.traceviewer.painter.ISpaceTimeCanvas;

import edu.rice.cs.hpc.traceviewer.util.Utility;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.Frame;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.Position;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.util.Constants;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;

/**A view for displaying the depthview.*/
public class DepthTimeCanvas extends AbstractTimeCanvas 
	implements IOperationHistoryListener, ISpaceTimeCanvas
{	
	final private ExecutorService threadExecutor;

	private SpaceTimeDataController stData;
	private int currentProcess = Integer.MIN_VALUE;
	private boolean needToRedraw = false;
	private Rectangle bound;

	/********************
	 * constructor to create this canvas
	 * 
	 * @param composite : the parent composite
	 */
	public DepthTimeCanvas(Composite composite)
    {
		super(composite, SWT.NONE);
		
		threadExecutor = Executors.newFixedThreadPool( Utility.getNumThreads(0) ); 
		addDisposeListener( new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				dispose();				
			}
		});
	}
	
	/****
	 * new data update
	 * @param _stData
	 */
	public void updateView(SpaceTimeDataController stData)
	{
		super.init();
		setVisible(true);
		
		if (this.stData == null) {
			// just initialize once
			TraceOperation.getOperationHistory().addOperationHistoryListener(this);
		}
		this.stData = stData; 		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	 */
	public void paintControl(PaintEvent event)
	{
		bound = getClientArea();

		if (stData == null || !stData.isTimelineFilled())
			return;
		
		if (needToRedraw) {
			refreshWithCondition();
			
			// set the flag that we don't need to redraw again
			needToRedraw = false;
		}
		super.paintControl(event);
		
		final long topLeftPixelX = Math.round(stData.getAttributes().getTimeBegin()*getScalePixelsPerTime());
		final int viewHeight 	 = bound.height;

		//--------------------
		//draws cross hairs
		//--------------------
		
		event.gc.setBackground(Constants.COLOR_WHITE);
		event.gc.setAlpha(240);
		
		long selectedTime = stData.getAttributes().getFrame().position.time;
		
		int topPixelCrossHairX = (int)(Math.round(selectedTime*getScalePixelsPerTime())-2-topLeftPixelX);
		event.gc.fillRectangle(topPixelCrossHairX,0,4,viewHeight);
		
		final int maxDepth = stData.getMaxDepth();
		final int depth    = stData.getAttributes().getDepth();
		
		final int width    = depth*viewHeight/maxDepth+viewHeight/(2*maxDepth);
		event.gc.fillRectangle(topPixelCrossHairX-8,width-1,20,4);
	}
	
	
    /***
     * force to refresh the content of the canvas. 
     */
    public void refresh() 
    {
		rebuffer();
    }
    
    public void activate(boolean isActivated)
    {
    	this.needToRedraw = isActivated;
    }
    
    /****
     *  refresh only if the size of the buffer doesn't match with the size of the canvas
     */
    private void refreshWithCondition() 
    {
		if (getBuffer() == null) {
			rebuffer();
			return;
		}
		
		// ------------------------------------------------------------------------
		// we need to avoid repainting if the size of the image buffer is not the same
		// as the image of the canvas. This case happens when the view is resize while
		// it's in hidden state, and then it turns visible. 
		// this will cause misalignment in the view
		// ------------------------------------------------------------------------
		
		final Rectangle r1 = getBuffer().getBounds();
		final Rectangle r2 = bound;
		
		if (!(r1.height == r2.height && r1.width == r2.width))
		{
			// the size if not the same, we need to recompute and repaint again
			rebuffer();
		}
    }
    
    
	public double getScalePixelsPerTime()
	{
		final int viewWidth = bound.width;

		return (double)viewWidth / (double)getNumTimeDisplayed();
	}

	public double getScalePixelsPerRank() {
		return Math.max(bound.height/(double)stData.getMaxDepth(), 1);
	}

	
	//---------------------------------------------------------------------------------------
	// PRIVATE METHODS
	//---------------------------------------------------------------------------------------

	private long getNumTimeDisplayed()
	{
		return (stData.getAttributes().getTimeInterval());
	}
	
	
    /************
     * method to repaint the canvas
     * this method can be costly, please do not call this unless the data has changed
     * 
     */
	private void rebuffer()
	{
		if (stData == null )
			return;

		final ImageTraceAttributes attributes = stData.getAttributes();
		final Frame frame = attributes.getFrame();

		// store the current process so that we don't need to rebuffer every time
		// we change the position within the same process
		currentProcess = frame.position.process;

		final Display display = Display.getDefault();
		display.syncExec( new Runnable() {
			
			@Override
			public void run() {
				final Rectangle rb = getBounds();
				
				final int viewWidth  = rb.width;
				final int viewHeight = rb.height;

				if (viewWidth <= 0 && viewHeight<= 0) {
					return;
				}
				//paints the current screen

				final Image imageBuffer = new Image(getDisplay(), viewWidth, viewHeight);
				setBuffer(imageBuffer);

				final GC bufferGC = new GC(getBuffer());
				bufferGC.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
				bufferGC.fillRectangle(0,0,viewWidth,viewHeight);
				
				attributes.numPixelsDepthV = viewHeight;
				
				Debugger.printDebug(1, "DTC rebuffering " + attributes);
				
				BaseViewPaint depthPaint = new DepthViewPaint(Util.getActiveWindow(), 
						bufferGC, 
						stData, attributes, true, DepthTimeCanvas.this, threadExecutor);
				
				depthPaint.addJobChangeListener(new DepthJobListener());
				depthPaint.schedule();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose()
	{
		threadExecutor.shutdown();
		super.dispose();
	}


	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IOperationHistoryListener#historyNotification(org.eclipse.core.commands.operations.OperationHistoryEvent)
	 */
	public void historyNotification(final OperationHistoryEvent event) {
		
		if (event.getEventType() == OperationHistoryEvent.DONE) 
		{
			final IUndoableOperation operation = event.getOperation();

			if (operation.hasContext(BufferRefreshOperation.context)) {
				// this event includes if there's a change of colors definition, so everyone needs
				// to refresh the content
				try {
					super.init();
					rebuffer();
				} catch (java.lang.NullPointerException e) {
					// ignore exception when there's (possibly) multiple thread access  
				}
				
			} else if (operation.hasContext(PositionOperation.context)) {
				PositionOperation opPos = (PositionOperation) operation;
				Position position = opPos.getPosition();
				if (position.process == currentProcess)
				{
					// changing cursor position within the same process
					redraw();
				} else {
					// different process, we need to repaint every thing
					rebuffer();
				}
			}
		}
	}

	@Override
	protected void changePosition(Point point) {
    	long closeTime = stData.getAttributes().getTimeBegin() + (long)(point.x / getScalePixelsPerTime());
    	
    	Position currentPosition = stData.getAttributes().getPosition();
    	Position newPosition = new Position(closeTime, currentPosition.process);
    		
    	try {
			TraceOperation.getOperationHistory().execute(
					new PositionOperation(newPosition), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	protected void changeRegion(Rectangle region) 
	{
		final ImageTraceAttributes attributes = stData.getAttributes();

		long topLeftTime 	 = attributes.getTimeBegin() + (long)(region.x / getScalePixelsPerTime());
		long bottomRightTime = attributes.getTimeBegin() + (long)((region.width+region.x) / getScalePixelsPerTime());
		
		final Frame oldFrame 	= attributes.getFrame();
		final Position position = oldFrame.position;
		
		Frame frame = new Frame(topLeftTime, bottomRightTime,
				attributes.getProcessBegin(), attributes.getProcessEnd(),
				attributes.getDepth(), position.time, position.process);
		try {
			TraceOperation.getOperationHistory().execute(
					new ZoomOperation("Time zoom out", frame), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private class DepthJobListener implements IJobChangeListener
	{
		
		@Override
		public void sleeping(IJobChangeEvent event) {}
		
		@Override
		public void scheduled(IJobChangeEvent event) {}
		
		@Override
		public void running(IJobChangeEvent event) {}
		
		@Override
		public void done(IJobChangeEvent event) {

			Display display = Display.getDefault();
			display.asyncExec(new Runnable() {
				
				@Override
				public void run() {
					redraw();
/*					final Image image = getBuffer();
					System.out.println("dispose: " + image.isDisposed() + ", type: " + image.type 
							+ ", bounds: " + image.getBounds() );
					if (image.isDisposed()) {
						System.err.println("image is disposed");
					}*/
				}
			} );
		}
		
		@Override
		public void awake(IJobChangeEvent event) {}
		
		@Override
		public void aboutToRun(IJobChangeEvent event) {}

	}

	@Override
	public void setMessage(String message) {
		
	}

	@Override
	protected String tooltipText(int pixel, RGB rgb) {
		return null;
	}

	@Override
	protected ColorTable getColorTable() {
		return stData.getColorTable();
	}
}
