package edu.rice.cs.hpc.traceviewer.painter;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import edu.rice.cs.hpc.traceviewer.operation.DepthOperation;
import edu.rice.cs.hpc.traceviewer.operation.ITraceAction;
import edu.rice.cs.hpc.traceviewer.operation.PositionOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;
import edu.rice.cs.hpc.traceviewer.operation.ZoomOperation;
import edu.rice.cs.hpc.traceviewer.spaceTimeData.SpaceTimeData;
import edu.rice.cs.hpc.traceviewer.ui.Frame;
import edu.rice.cs.hpc.traceviewer.util.Constants;

/**A view for displaying the depthview.*/
//all the GUI setup for the depth view is here
public class DepthTimeCanvas extends SpaceTimeCanvas 
implements MouseListener, MouseMoveListener, PaintListener, IOperationHistoryListener
{
	
	int maxDepth;
	
	Image imageBuffer;
	
	/**The left pixel's x location*/
	long topLeftPixelX;
	
	/**The first/last time being viewed now*/
    long oldBegTime;
    long oldEndTime;
	
	/**The selected time that is open in the csViewer.*/
	double selectedTime;
	
	/**The selected depth that is open in the csViewer.*/
	int selectedDepth;
	
	/** Relates to the condition that the mouse is in.*/
	SpaceTimeCanvas.MouseState mouseState;
	
	/** The point at which the mouse was clicked.*/
	Point mouseDown;
	
	/** The point at which the mouse was released.*/
	Point mouseUp;
	
	/** The left/right point that you selected.*/
	long leftSelection;
	long rightSelection;
	
	private int currentProcess = -1;
    
	
	public DepthTimeCanvas(Composite composite)
    {
		super(composite);

		mouseState = SpaceTimeCanvas.MouseState.ST_MOUSE_INIT;

		selectedTime = -20;
		selectedDepth = -1;
		leftSelection = 0;
		rightSelection = 0;
	}
	
	/****
	 * new data update
	 * @param _stData
	 */
	public void updateView(SpaceTimeData _stData)
	{
		this.stData = _stData;
		this.maxDepth = _stData.getMaxDepth();
		
		if (this.mouseState == SpaceTimeCanvas.MouseState.ST_MOUSE_INIT)
		{
			this.mouseState = SpaceTimeCanvas.MouseState.ST_MOUSE_NONE;
			this.addCanvasListener();
		}
		this.home();
	}
	
	/***
	 * add listeners (need to called only once)
	 */
	private void addCanvasListener() {
		addMouseListener(this);
		addMouseMoveListener(this);
		addPaintListener(this);
		
		addListener(SWT.Resize, new Listener(){
			public void handleEvent(Event event)
			{
				//init();
				final int viewWidth = getClientArea().width;
				final int viewHeight = getClientArea().height;

				//assertTimeBounds();
				
				if (viewWidth > 0 && viewHeight > 0) {
					getDisplay().asyncExec(new ResizeThread( new DepthBufferPaint()));
				}
			}
		});
		TraceOperation.getOperationHistory().addOperationHistoryListener(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	 */
	public void paintControl(PaintEvent event)
	{
		if (this.stData == null || imageBuffer == null)
			return;
		
		topLeftPixelX = Math.round(stData.attributes.begTime*getScaleX());
		
		final int viewWidth = getClientArea().width;
		final int viewHeight = getClientArea().height;

		try
		{
			event.gc.drawImage(imageBuffer, 0, 0, viewWidth, viewHeight, 0, 0, viewWidth, viewHeight);
		}
		catch (Exception e)
		{
			// An exception "Illegal argument" will be raised if the resize method is not "fast" enough to create the image
			//		buffer before the painting is called. Thus, it causes inconsistency between the size of the image buffer
			//		and the size of client area. 
			//		If this happens, either we wait for the creation of image buffer, or do nothing. 
			//		I prefer to do nothing because of scalability concerns.
			return;
		}
 		//paints the selection currently being made
		if (mouseState==SpaceTimeCanvas.MouseState.ST_MOUSE_DOWN)
		{
        	event.gc.setForeground(Constants.COLOR_WHITE);
    		event.gc.setLineWidth(2);
    		event.gc.drawRectangle((int)(leftSelection-topLeftPixelX), 0, (int)(rightSelection-leftSelection), viewHeight);
        }
		
		//draws cross hairs
		int topPixelCrossHairX = (int)(Math.round(selectedTime*getScaleX())-2-topLeftPixelX);
		event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		event.gc.fillRectangle(topPixelCrossHairX,0,4,viewHeight);
		event.gc.fillRectangle(topPixelCrossHairX-8,selectedDepth*viewHeight/maxDepth+viewHeight/(2*maxDepth)-1,20,4);
	}
	
	public void home()
	{
		setTimeZoom(0, (long)stData.getWidth());
	}
	
	/**************************************************************************
	 * Sets the location of the crosshair to (_selectedTime, _selectedProcess).
	 * Also updates the rest of the program to know that this is the selected
	 * point (so that the CallStackViewer can update, etc.).
	 **************************************************************************/
	private void setPosition(Position position)
	{
		selectedTime = (double)position.time;
		if (currentProcess != position.process) {
			rebuffer();
			currentProcess = position.process;
		} else
			// just display a new cross
			redraw();
	}
	
	/***
	 * set new depth
	 * @param _selectedDepth
	 */
	private void setDepth(int _selectedDepth) {
		selectedDepth = _selectedDepth;
		redraw();
	}
	
	/****
	 * update the new range position
	 * 
	 * @param p1
	 * @param p2
	 */
	private void adjustSelection(Point p1, Point p2)
	{
		final int viewWidth = getClientArea().width;

    	leftSelection = topLeftPixelX + Math.max(Math.min(p1.x, p2.x), 0);
        rightSelection = topLeftPixelX + Math.min(Math.max(p1.x, p2.x), viewWidth-1);
    }
    
    /***
     * force to refresh the content of the canvas. 
     */
    public void refresh() {
		rebuffer();
    }
    
    
    private void setCSSample()
    {
    	if(mouseDown == null)
    		return;

    	long closeTime = stData.attributes.begTime + (long)((double)mouseDown.x / getScaleX());
    	
    	Position currentPosition = stData.getPosition();
    	Position position = new Position(closeTime, currentPosition.process);
    	
    	notifyPositionChange(position);
    }

    @Override
	public double getScaleX()
	{
		final int viewWidth = getClientArea().width;

		return (double)viewWidth / (double)getNumTimeDisplayed();
	}

	@Override
	public double getScaleY() {
		final Rectangle r = this.getClientArea();
		return Math.max(r.height/(double)maxDepth, 1);
	}

	//---------------------------------------------------------------------------------------
	// PRIVATE CLASS
	//---------------------------------------------------------------------------------------

	private class DepthBufferPaint implements BufferPaint
	{
		public void rebuffering()
		{
			rebuffer();
		}
	}

	//---------------------------------------------------------------------------------------
	// PRIVATE METHODS
	//---------------------------------------------------------------------------------------

	private long getNumTimeDisplayed()
	{
		return (stData.attributes.endTime - stData.attributes.begTime);
	}
	
	private void setTimeZoom(long leftTime, long rightTime)
	{
		stData.attributes.begTime= leftTime;
		stData.attributes.endTime = rightTime;
		
		stData.attributes.assertTimeBounds(stData.getWidth());
		
		if (getNumTimeDisplayed() < Constants.MIN_TIME_UNITS_DISP)
		{
			stData.attributes.begTime += (getNumTimeDisplayed() - Constants.MIN_TIME_UNITS_DISP)/2;
			stData.attributes.endTime = stData.attributes.begTime + getNumTimeDisplayed();
			
			stData.attributes.assertTimeBounds(stData.getWidth());
		}
		
		rebuffer();
		
		oldBegTime = stData.attributes.begTime;
		oldEndTime = stData.attributes.endTime;
	}

	/**** time zoom action **/
	final private ITraceAction zoomAction = new ITraceAction() {
		@Override
		public void doAction(Frame frame) 
		{
			zoom(frame.begTime, frame.endTime);
		}
	};
	
	final private ITraceAction positionAction = new ITraceAction() {
		@Override
		public void doAction(Frame frame) 
		{
			setPosition(frame.position);
		}		
	};
	
    private void setDetail()
    {
		long topLeftTime = (long)((double)leftSelection / getScaleX());
		long bottomRightTime = (long)((double)rightSelection / getScaleX());
		
		stData.attributes.begTime = topLeftTime;
		stData.attributes.endTime = bottomRightTime;
		
		Frame frame = new Frame(stData.attributes, selectedDepth, (long)selectedTime, currentProcess);
		try {
			TraceOperation.getOperationHistory().execute(
					new ZoomOperation("Time zoom out", frame, zoomAction), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
    }
	
    private void notifyPositionChange(Position newPosition)
    {    	
    	try {
			TraceOperation.getOperationHistory().execute(
					new PositionOperation("change time position", newPosition, positionAction), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
    }
    
    private void zoom(long time1, long time2)
    {
    	setTimeZoom(time1, time2);
    	adjustCrossHair(time1, time2);
    }
    
    /******************
     * Forcing the crosshair to be always inside the region
     * 
     * @param t1: the leftmost time
     * @param t2: the rightmost time
     */
    private void adjustCrossHair(long t1, long t2) {
    	Position currentPosition = stData.getPosition();
    	long time = currentPosition.time;
    	
    	if (time<t1 || time>t2)
    		time = (t1+t2)>>1;
		
    	Position position = new Position(time, currentPosition.process);
    	setPosition(position);
    }
    
	private void rebuffer()
	{
		if (stData == null)
			return;

		final int viewWidth = getClientArea().width;
		final int viewHeight = getClientArea().height;

		if (viewWidth>0 && viewHeight>0) {
			if (imageBuffer != null) {
				imageBuffer.dispose();
			}
			//paints the current screen
			imageBuffer = new Image(getDisplay(), viewWidth, viewHeight);
		}
		GC bufferGC = new GC(imageBuffer);
		bufferGC.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		bufferGC.fillRectangle(0,0,viewWidth,viewHeight);
		
		stData.attributes.numPixelsDepthV = viewHeight;
		
		try
		{
			paintDepthViewport(bufferGC,  
					stData.attributes.begTime, stData.attributes.endTime, viewWidth, viewHeight);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		bufferGC.dispose();
		
		redraw();
	}

	
	/*************************************************************************
	 * Paint the depth view
	 * 
	 * @param masterGC
	 * @param canvas
	 * @param _begTime
	 * @param _endTime
	 * @param _numPixelsH
	 * @param _numPixelsV
	 *************************************************************************/
	private void paintDepthViewport(final GC masterGC, 
			long _begTime, long _endTime, int _numPixelsH, int _numPixelsV)
	{
		boolean changedBounds = true ; //!( dtProcess == currentPosition.process && attributes.sameDepth(oldAttributes));
		
		ImageTraceAttributes attributes = stData.attributes;
		attributes.numPixelsDepthV = _numPixelsV;
		attributes.setTime(_begTime, _endTime);
		
		//oldAttributes.copy(attributes);
		
		BaseViewPaint depthPaint = new DepthViewPaint(null, masterGC, stData, attributes, changedBounds);		
		depthPaint.paint(this);
	}
	

	/******************************************************************
	 *		
	 *	MouseListener and MouseMoveListener interface Implementation
	 *      
	 ******************************************************************/

	public void mouseDoubleClick(MouseEvent e) { }

	public void mouseDown(MouseEvent e)
	{
		if (mouseState == SpaceTimeCanvas.MouseState.ST_MOUSE_NONE)
		{
			mouseState = SpaceTimeCanvas.MouseState.ST_MOUSE_DOWN;
			mouseDown = new Point(e.x,e.y);
		}
	}

	public void mouseUp(MouseEvent e)
	{
		if (mouseState == SpaceTimeCanvas.MouseState.ST_MOUSE_DOWN)
		{
			mouseUp = new Point(e.x,e.y);
			mouseState = SpaceTimeCanvas.MouseState.ST_MOUSE_NONE;
			
			//difference in mouse movement < 3 constitutes a "single click"
			if(Math.abs(mouseUp.x-mouseDown.x)<3 && Math.abs(mouseUp.y-mouseDown.y)<3)
			{
				setCSSample();
			}
			else
			{
				//If we're zoomed in all the way don't do anything
				if(getNumTimeDisplayed() > Constants.MIN_TIME_UNITS_DISP)
				{
					//pushUndo();
					adjustSelection(mouseDown,mouseUp);
					setDetail();
				}
			}
		}
	}
	
	public void mouseMove(MouseEvent e)
	{
		if(mouseState == SpaceTimeCanvas.MouseState.ST_MOUSE_DOWN)
		{
			Point mouseTemp = new Point(e.x,e.y);
			adjustSelection(mouseDown,mouseTemp);
			redraw();
		}
	}


	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.commands.operations.IOperationHistoryListener#historyNotification(org.eclipse.core.commands.operations.OperationHistoryEvent)
	 */
	public void historyNotification(final OperationHistoryEvent event) {
		final IUndoableOperation operation = event.getOperation();
		
		if (operation.hasContext(TraceOperation.context)) {
			final TraceOperation traceOperation =  (TraceOperation) operation;
			
			if (event.getEventType() == OperationHistoryEvent.DONE) {
				if (traceOperation instanceof ZoomOperation) {
					getDisplay().syncExec(new Runnable() {
						@Override
						public void run() {
							Frame frame = traceOperation.getFrame();
							zoom(frame.begTime, frame.endTime);
							setPosition(frame.position);
						}
					});
				} else if (traceOperation instanceof PositionOperation) {
					Position p = ((PositionOperation)traceOperation).getPosition();
					setPosition(p);
				} else if (traceOperation instanceof DepthOperation) {
					int depth = ((DepthOperation)traceOperation).getDepth();
					setDepth(depth);
				}
			}
		}
	}

}
