package edu.rice.cs.hpc.traceviewer.main;

import java.text.DecimalFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.data.trace.TraceAttribute;
import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.DepthOperation;
import edu.rice.cs.hpc.traceviewer.operation.PositionOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;
import edu.rice.cs.hpc.traceviewer.operation.WindowResizeOperation;
import edu.rice.cs.hpc.traceviewer.operation.ZoomOperation;
import edu.rice.cs.hpc.traceviewer.painter.AbstractTimeCanvas;
import edu.rice.cs.hpc.traceviewer.painter.BaseViewPaint;
import edu.rice.cs.hpc.traceviewer.painter.BufferPaint;
import edu.rice.cs.hpc.traceviewer.painter.ISpaceTimeCanvas;
import edu.rice.cs.hpc.traceviewer.painter.ResizeListener;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.Frame;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.Position;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpc.traceviewer.util.MessageLabelManager;
import edu.rice.cs.hpc.traceviewer.util.Utility;
import edu.rice.cs.hpc.traceviewer.data.util.Constants;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;


/*************************************************************************
 * 
 *	Canvas onto which the detail view is painted. Also takes care of
 *	zooming responsibilities of the detail view.
 *
 ************************************************************************/
public class SpaceTimeDetailCanvas extends AbstractTimeCanvas 
	implements IOperationHistoryListener, ISpaceTimeCanvas
{	
	final long PER_MICRO_SECOND = 1000000;
	
	/**The SpaceTimeData corresponding to this canvas.*/
	protected SpaceTimeDataController stData;

	/**Triggers zoom back to beginning view screen.*/
	private Action homeButton;
	
	/**Triggers open function to open previously saved frame.*/
	private Action openButton;
	
	/**Triggers save function to save current frame to file.*/
	private Action saveButton;
	
	/** Triggers zoom-in on the time axis.*/
	private Action tZoomInButton;
	
	/** Triggers zoom-out on the time axis.*/
	private Action tZoomOutButton;
	
	/** Triggers zoom-in on the process axis.*/
	private Action pZoomInButton;
	
	/** Triggers zoom-out on the process axis.*/
	private Action pZoomOutButton;

	private Action goEastButton, goNorthButton, goWestButton, goSouthButton;
		
	/** The top-left and bottom-right point that you selected.*/
	final private Point selectionTopLeft, selectionBottomRight;
		
	/**The Group containing the labels. labelGroup.redraw() is called from the Detail Canvas.*/
	private Composite labelGroup;
   
    /**The Label with the time boundaries.*/
	private Label timeLabel;
   
    /**The Label with the process boundaries.*/
	private Label processLabel;
    
    /**The Label with the current cross hair information.*/
	private Label crossHairLabel;
        
    /**The Label for displaying a message info/warning to users.*/
	private Label messageLabel;

	/** thread to remove message */
	MessageLabelManager restoreMessage;
	
	/**The min number of process units you can zoom in.*/
    private final static int MIN_PROC_DISP = 1;
	
	final private ImageTraceAttributes oldAttributes;
	
	final private ProcessTimelineService ptlService;
	
	final IWorkbenchWindow window;

	final private ExecutorService threadExecutor;
	
	final private DecimalFormat formatTime;

	
    /**Creates a SpaceTimeDetailCanvas with the given parameters*/
	public SpaceTimeDetailCanvas(IWorkbenchWindow window, Composite _composite)
	{
		super(_composite, SWT.NO_BACKGROUND, RegionType.Rectangle );
		oldAttributes = new ImageTraceAttributes();

		selectionTopLeft     = new Point(0,0);
		selectionBottomRight = new Point(0,0);
		stData  = null;
		
		initMouseSelection();
		
		ISourceProviderService service = (ISourceProviderService)window.
				getService(ISourceProviderService.class);
		ptlService = (ProcessTimelineService) service.
				getSourceProvider(ProcessTimelineService.PROCESS_TIMELINE_PROVIDER);
		
		this.window = window;
		
		// set the number of maximum threads in the pool to the number of hardware threads
		threadExecutor = Executors.newFixedThreadPool( Utility.getNumThreads(0) ); 
		formatTime = new DecimalFormat("#.###");
		
		addDisposeListener( new DisposeListener() {
			
			@Override
			public void widgetDisposed(DisposeEvent e) {
				// TODO Auto-generated method stub
				dispose();
			}
		});
	}


	private void initMouseSelection()
	{
		initSelectionRectangle();
	}
	
	/*****
	 * set new database and refresh the screen
	 * @param dataTraces
	 *****/
	public void updateView(SpaceTimeDataController _stData) {

		super.init();

		if (this.stData == null) 
		{
			addCanvasListener();
			OperationHistoryFactory.getOperationHistory().addOperationHistoryListener(this);
		}

		// reinitialize the selection rectangle
		initSelectionRectangle();

		this.stData = _stData;

		// init configuration
		Position p = new Position(-1, -1);
		stData.getAttributes().setPosition(p);
		stData.getAttributes().setDepth(0);
		
		this.home();

		this.saveButton.setEnabled(true);
		this.openButton.setEnabled(true);
	}
	
	/***
	 * add listeners to the canvas 
	 * caution: this method can only be called at most once ! 
	 */
	private void addCanvasListener() {

		addPaintListener(this);
		
		addKeyListener( new KeyListener(){
			public void keyPressed(KeyEvent e) {}

			public void keyReleased(KeyEvent e) {
				switch (e.keyCode) {
				
				case SWT.ARROW_DOWN:
					goSouth();
					break;
				case SWT.ARROW_UP:
					goNorth();
					break;
				case SWT.ARROW_LEFT:
					goEast();
					break;
				case SWT.ARROW_RIGHT:
					goWest();
					break;				
				}
			}			
		});
				
		// ------------------------------------------------------------------------------------
		// A listener for resizing the the window.
		// In order to get the last resize position, we will use timer to check if the current
		//  resize event is invoked "long" enough to the first resize event.
		// If this is the case, then run rebuffering, otherwise just no-op.
		// ------------------------------------------------------------------------------------
		final ResizeListener listener = new ResizeListener( new DetailBufferPaint() ); 
		addControlListener(listener);
		getDisplay().addFilter(SWT.MouseDown, listener);
		getDisplay().addFilter(SWT.MouseUp, listener);
	}
	
	
	/*************************************************************************
	 * Sets the bounds of the data displayed on the detail canvas to be those 
	 * specified by the zoom operation and adjusts everything accordingly.
	 *************************************************************************/
	public void zoom(long _topLeftTime, int _topLeftProcess, long _bottomRightTime, int _bottomRightProcess)
	{
		final ImageTraceAttributes attributes = stData.getAttributes();
		attributes.setTime(_topLeftTime, _bottomRightTime);
		attributes.assertTimeBounds(stData.getTimeWidth());
		
		attributes.setProcess(_topLeftProcess, _bottomRightProcess);
		attributes.assertProcessBounds(stData.getTotalTraceCount());
		
		final long numTimeDisplayed = this.getNumTimeUnitDisplayed();
		if (numTimeDisplayed < Constants.MIN_TIME_UNITS_DISP)
		{
			long begTime = _topLeftTime + (numTimeDisplayed - Constants.MIN_TIME_UNITS_DISP) / 2;
			long endTime = _topLeftTime + Constants.MIN_TIME_UNITS_DISP;
			attributes.setTime(begTime, endTime);
		}
		
		final double numProcessDisp = this.getNumProcessesDisplayed();
		if (numProcessDisp < MIN_PROC_DISP)
		{
			int endProcess = _topLeftProcess + MIN_PROC_DISP;
			attributes.setProcess(_topLeftProcess, endProcess );
		}

		updateButtonStates();
    	
		// ----------------------------------------------------------------------------
		// hack solution: we need to gather the data first, then we ask other views 
		//	to update their contents
		// ----------------------------------------------------------------------------
		refresh(true);
	}
	
	/*******************************************************************************
	 * Initialize attributes of selection rectangle
	 *******************************************************************************/
	private void initSelectionRectangle() 
	{
		selectionTopLeft.x = 0;
		selectionTopLeft.y = 0;
		selectionBottomRight.x = 0;
		selectionBottomRight.y = 0;
	}
	
	/*******************************************************************************
	 * Actually does the repainting of the canvas when a PaintEvent is sent to it
	 * (basically when anything at all is changed anywhere on the application 
	 * OR when redraw() is called).
	 ******************************************************************************/
	public void paintControl(PaintEvent event)
	{		
		if (this.stData == null)
			return;

		super.paintControl(event);

		final ImageTraceAttributes attributes = stData.getAttributes();
		
		//draws cross hairs
		long selectedTime = attributes.getPosition().time - attributes.getTimeBegin();
		int selectedProcess = attributes.getPosition().process - attributes.getProcessBegin();
		
		int topPixelCrossHairX = (int)(Math.round(selectedTime*getScalePixelsPerTime())-10);
		int topPixelCrossHairY = (int)(Math.round((selectedProcess+.5)*getScalePixelsPerRank())-10);
		
		event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
		event.gc.fillRectangle(topPixelCrossHairX,topPixelCrossHairY+8,20,4);
		event.gc.fillRectangle(topPixelCrossHairX+8,topPixelCrossHairY,4,20);
	}

	/**************************************************************************
	 * Initializes the buttons above the detail canvas.
	 **************************************************************************/
	public void setButtons(Action[] toolItems)
	{
		homeButton = toolItems[0];
		openButton = toolItems[1];
		saveButton = toolItems[2];
		tZoomInButton = toolItems[5];
		tZoomOutButton = toolItems[6];
		pZoomInButton = toolItems[7];
		pZoomOutButton = toolItems[8];
		
		goEastButton = toolItems[9];
		goNorthButton = toolItems[10];
		goSouthButton = toolItems[11];
		goWestButton = toolItems[12];
	}
	
	/**************************************************************************
	 * The action that gets performed when the 'home' button is pressed - 
	 * the bounds are reset so that the viewer is zoomed all the way out on the
	 * image.
	 **************************************************************************/
	public void home()
	{
		//if this is the first time painting,
		//some stuff needs to get initialized
		Frame frame = new Frame(stData.getAttributes().getFrame());
		frame.begProcess = 0;
		frame.endProcess = stData.getTotalTraceCount();
		frame.begTime = 0;
		frame.endTime = stData.getTimeWidth();
		
		notifyChanges(ZoomOperation.ActionHome, frame);
	}
	
	/**************************************************************************
	 * The action that gets performed when the 'open' button is pressed - 
	 * sets everything to the data stored in the Frame toBeOpened.
	 **************************************************************************/
	public void open(Frame toBeOpened)
	{
		notifyChanges("Frame", toBeOpened);	
	}
	
	/**************************************************************************
	 * The action that gets performed when the 'save' button is pressed - 
	 * it stores all the relevant data to this current configuration to a new 
	 * Frame.
	 **************************************************************************/
	public Frame save()
	{
		return stData.getAttributes().getFrame();
	}
	
	
	
	/**************************************************************************
	 * The action that gets performed when the 'process zoom in' button is pressed - 
	 * zooms in processwise with a scale of .4.
	 **************************************************************************/
	public void processZoomIn()
	{
		final double SCALE = .4;
		final ImageTraceAttributes attributes = stData.getAttributes();
		
		double yMid = (attributes.getProcessEnd()+attributes.getProcessBegin())/2.0;
		
		final double numProcessDisp = attributes.getProcessInterval();
		
		int p2 = (int) Math.ceil( yMid+numProcessDisp*SCALE );
		int p1 = (int) Math.floor( yMid-numProcessDisp*SCALE );
		
		attributes.assertProcessBounds(stData.getTotalTraceCount());
		
		if(p2 == attributes.getProcessEnd() && p1 == attributes.getProcessBegin())
		{
			if(numProcessDisp == 2)
				p2--;
			else if(numProcessDisp > 2)
			{
				p2--;
				p1++;
			}
		}
		final Frame frame = new Frame(stData.getAttributes().getFrame());
		frame.begProcess = p1;
		frame.endProcess = p2;

		notifyChanges("Zoom-in ranks", frame);
	}

	/**************************************************************************
	 * The action that gets performed when the 'process zoom out' button is pressed - 
	 * zooms out processwise with a scale of .625.
	 **************************************************************************/
	public void processZoomOut()
	{
		final double SCALE = .625;
		final ImageTraceAttributes attributes = stData.getAttributes();
		
		//zoom out works as follows: find mid point of times (yMid).
		//Add/Subtract 1/2 of the scaled numProcessDisp to yMid to get new endProcess and begProcess
		double yMid = ((double)attributes.getProcessEnd() + (double)attributes.getProcessBegin())/2.0;
		
		final double numProcessDisp = attributes.getProcessInterval();
		

		int p2 = (int) Math.min( stData.getTotalTraceCount(), Math.ceil( yMid+numProcessDisp*SCALE ) );
		int p1 = (int) Math.max( 0, Math.floor( yMid-numProcessDisp*SCALE ) );
		
		if(p2 == attributes.getProcessEnd() && p1 == attributes.getProcessBegin())
		{
			if(numProcessDisp == 2)
				p2++;
			else if(numProcessDisp > 2)
			{
				p2++;
				p1--;
			}
		}
		final Frame frame = new Frame(stData.getAttributes().getFrame());
		frame.begProcess = p1;
		frame.endProcess = p2;

		notifyChanges("Zoom-out ranks", frame);
	}

	
	/**************************************************************************
	 * The action that gets performed when the 'time zoom in' button is pressed - 
	 * zooms in timewise with a scale of .4.
	 **************************************************************************/
	public void timeZoomIn()
	{
		final double SCALE = .4;
		final ImageTraceAttributes attributes = stData.getAttributes();
		
		long xMid = (attributes.getTimeEnd() + attributes.getTimeBegin()) / 2;
		
		final long numTimeUnitsDisp = attributes.getTimeInterval();
		
		long t2 = xMid + (long)(numTimeUnitsDisp * SCALE);
		long t1 = xMid - (long)(numTimeUnitsDisp * SCALE);

		final Frame frame = new Frame(stData.getAttributes().getFrame());
		frame.begTime = t1;
		frame.endTime = t2;
		
		notifyChanges("Zoom-in time", frame);
	}

	/**************************************************************************
	 * The action that gets performed when the 'time zoom out' button is pressed - 
	 * zooms out timewise with a scale of .625.
	 **************************************************************************/
	public void timeZoomOut()
	{
		final double SCALE = 0.625;
		final ImageTraceAttributes attributes = stData.getAttributes();
		
		//zoom out works as follows: find mid point of times (xMid).
		//Add/Subtract 1/2 of the scaled numTimeUnitsDisp to xMid to get new endTime and begTime
		long xMid = (attributes.getTimeEnd() + attributes.getTimeBegin()) / 2;
		
		final long td2 = (long)(this.getNumTimeUnitDisplayed() * SCALE); 
		long t2 = Math.min( stData.getTimeWidth(), xMid + td2);
		final long td1 = (long)(this.getNumTimeUnitDisplayed() * SCALE);
		long t1 = Math.max(0, xMid - td1);

		final Frame frame = new Frame(stData.getAttributes().getFrame());
		frame.begTime = t1;
		frame.endTime = t2;
		
		notifyChanges("Zoom-out time", frame);
	}
	
	/**************************************************************************
	 * Gets the scale along the x-axis (pixels per time unit).
	 **************************************************************************/
	@Override
	public double getScalePixelsPerTime()
	{
		return (double)stData.getAttributes().numPixelsH / (double)this.getNumTimeUnitDisplayed();
	}
	
	/**************************************************************************
	 * Gets the scale along the y-axis (pixels per process).
	 **************************************************************************/
	@Override
	public double getScalePixelsPerRank()
	{
		return (double)stData.getAttributes().numPixelsV / this.getNumProcessesDisplayed();
	}
	
	/**************************************************************************
	 * Sets the depth to newDepth.
	 **************************************************************************/
	public void setDepth(int newDepth)
	{
		if (isDisposed()) return;
		
		stData.getAttributes().setDepth(newDepth);
		refresh(false);
    }
	
	
	/**************************************************************************
	 * Sets up the labels (the ones below the detail canvas).
	 **************************************************************************/
	public void setLabels(Composite _labelGroup)
    {
		labelGroup = _labelGroup;
        
        timeLabel  	   = new Label(labelGroup, SWT.LEFT);
        processLabel   = new Label(labelGroup, SWT.CENTER);
        crossHairLabel = new Label(labelGroup, SWT.RIGHT);
        messageLabel   = new Label(labelGroup, SWT.LEFT);
    }
   
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.traceviewer.painter.ISpaceTimeCanvas#setMessage(java.lang.String)
	 */
	public void setMessage(String message)
	{
		if (restoreMessage == null)
			restoreMessage = new MessageLabelManager(getDisplay(), messageLabel);
		
		restoreMessage.showWarning(message);
	}
	
	
	/**************************************************************************
	 * Updates what the labels display to the viewer's current state.
	 **************************************************************************/
	private void adjustLabels()
    {
		final ImageTraceAttributes attributes = stData.getAttributes();
		double unit_time = stData.getUnitTimePerSecond();
		
		double timeInSec = attributes.getTimeBegin()/unit_time;
		final String timeStart = formatTime.format(timeInSec);
		
		timeInSec = attributes.getTimeEnd()/unit_time;
		final String timeEnd   = formatTime.format(timeInSec);
        
		timeLabel.setText("Time Range: [" + timeStart + "s, " + timeEnd +  "s]");
        timeLabel.setSize(timeLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        

        final IBaseData traceData = stData.getBaseData();
        if (traceData == null) {
        	// we don't want to throw an exception here, so just do nothing
        	System.out.println("Data null, skipping the rest.");
        	return;
        }
        stData.getAttributes().assertProcessBounds(traceData.getNumberOfRanks());

        final String processes[] = traceData.getListOfRanks();

        int proc_start = attributes.getProcessBegin();
        if (proc_start < 0 || proc_start >= processes.length)
        	proc_start = 0;
        
        // -------------------------------------------------------------------------------------------------
        // bug fix: since the end of the process is the ceiling of the selected region,
        //			and the range of process rendering is based on inclusive min and exclusive max, then
        //			we need to decrement the value of max process (in the range).
        // WARN: the display of process range should be then between inclusive min and inclusive max
        //
        // TODO: we should fix the rendering to inclusive min and inclusive max, otherwise it is too much
        //		 headache to maintain
        // -------------------------------------------------------------------------------------------------
        int proc_end   = attributes.getProcessEnd() - 1;
        if (proc_end>=processes.length)
        	proc_end = processes.length-1;
        
        processLabel.setText("Rank Range: [" + processes[proc_start] + ", " + processes[proc_end]+"]");
        processLabel.setSize(processLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
        if(stData == null)
            crossHairLabel.setText("Select Sample For Cross Hair");
        else
        {
        	final Position position = stData.getAttributes().getPosition();
    		final long selectedTime = position.time;
    		
    		ProcessTimeline ptl     = stData.getCurrentDepthTrace();
    		if (ptl == null) return;
    		
    		final int selectedProc  = ptl.getProcessNum();
    		
    		if ( selectedProc >= 0 && selectedProc < processes.length ) {  
    			crossHairLabel.setText("Cross Hair: " + getCrossHairText(selectedTime, selectedProc));
            	//crossHairLabel.setText("Cross Hair: (" + (selectedTime/1000)/1000.0 + "s, " + processes[rank] + ")");
    		} else {
    			// in case of incorrect filtering where user may have empty ranks or incorrect filters, we don't display the rank
    			crossHairLabel.setText("Cross Hair: (" + (selectedTime/stData.getUnitTimePerSecond())/.0 + "s, ?)");
    		}
        }
        
        labelGroup.setSize(labelGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
	
	/**************************************************************************
	 * get formatted text to indicate the current position of the cursor
	 * this function is a bit buggy if the number of process to display is higher
	 * than the height resolution. somehow, the selected process is over 1 position 
	 * 
	 * @param closeTime
	 * @param selectedProcess
	 * @return
	 **************************************************************************/
	private String getCrossHairText(long closeTime, int selectedProcess) 
	{
		final float selectedTime = (float) ((float)closeTime / stData.getUnitTimePerSecond());
        final IBaseData traceData = stData.getBaseData();
        final String processes[] = traceData.getListOfRanks();

        final String buffer = "(" + formatTime.format(selectedTime) + "s, " + 
        						processes[selectedProcess] + ")";
        return buffer;
	}
	
    /**************************************************************************
	 * Updates what the position of the selected box is.
	 **************************************************************************/
    private void adjustSelection(Rectangle selection)
	{
    	selectionTopLeft.x = Math.max(selection.x, 0);
        selectionTopLeft.y = Math.max(selection.y, 0);
        
        final Rectangle view = getClientArea();
        
        selectionBottomRight.x = Math.min(selection.width+selection.x, view.width-1);
        selectionBottomRight.y = Math.min(selection.height+selection.y, view.height-1);
        
        if (selectionTopLeft.x < 0 || selectionBottomRight.x < 0 || view.x < 0) {
        	Debugger.printDebug(1, "STDC Error: negative time " + view + 
        			" [" + selectionTopLeft.x + ", " + selectionBottomRight.x + "]");
        }
    }
    
	/**************************************************************************
	 * create a new region of trace view, and check if the cross hair is inside
	 * 	the new region or not. If this is not the case, we force the position
	 * 	of crosshair to be inside the region 
	 **************************************************************************/
	private void setDetail()
    {
		ImageTraceAttributes attributes = stData.getAttributes();
		int topLeftProcess = attributes.getProcessBegin() + (int) (selectionTopLeft.y / getScalePixelsPerRank());
		long topLeftTime   = attributes.getTimeBegin() + (long)(selectionTopLeft.x / getScalePixelsPerTime());
		
		// ---------------------------------------------------------------------------------------
		// we should include the partial selection of a time or a process
		// for instance if the user selects processes where the max process is between
		// 	10 and 11, we should include process 11 (just like keynote selection)
		// ---------------------------------------------------------------------------------------
		int bottomRightProcess = attributes.getProcessBegin() + (int) Math.ceil( (selectionBottomRight.y / getScalePixelsPerRank()) );
		long bottomRightTime   = attributes.getTimeBegin() + (long)Math.ceil( (selectionBottomRight.x / getScalePixelsPerTime()) );


		final Frame frame = new Frame(stData.getAttributes().getFrame());
		frame.begTime = topLeftTime;
		frame.endTime = bottomRightTime;
		frame.begProcess = topLeftProcess;
		frame.endProcess = bottomRightProcess;
		
		notifyChanges("Zoom", frame);
    }
    
  
    private boolean canGoEast() {
    	return (stData.getAttributes().getTimeBegin() > 0);
    }
    
    private boolean canGoWest() {
    	return (stData.getAttributes().getTimeEnd()< this.stData.getTimeWidth());
    }
    
    private boolean canGoNorth() {
    	return (stData.getAttributes().getProcessBegin()>0);
    }
    
    private boolean canGoSouth() {
    	return (stData.getAttributes().getProcessEnd()<this.stData.getTotalTraceCount());
    }
    /**********
     * check the status of all buttons
     */
    private void updateButtonStates() 
    {
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	
		this.tZoomInButton.setEnabled( this.getNumTimeUnitDisplayed() > Constants.MIN_TIME_UNITS_DISP );
		this.tZoomOutButton.setEnabled(attributes.getTimeBegin()>0 || attributes.getTimeEnd()<stData.getTimeWidth() );
		
		this.pZoomInButton.setEnabled( getNumProcessesDisplayed() > MIN_PROC_DISP );
		this.pZoomOutButton.setEnabled( attributes.getProcessBegin()>0 || attributes.getProcessEnd()<stData.getTotalTraceCount());
		
		this.goEastButton.setEnabled( canGoEast() );
		this.goWestButton.setEnabled( canGoWest() );
		this.goNorthButton.setEnabled( canGoNorth() );
		this.goSouthButton.setEnabled( canGoSouth() );
		
		homeButton.setEnabled( attributes.getTimeBegin()>0 || attributes.getTimeEnd()<stData.getTimeWidth()
				|| attributes.getProcessBegin()>0 || attributes.getProcessEnd()<stData.getTotalTraceCount() );
    }
    
	final static private double SCALE_MOVE = 0.20;
    
	/***
	 * go to the left one step
	 */
    public void goEast()
    {
    	if (!canGoEast())
    		return;
    	
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	
    	long topLeftTime = attributes.getTimeBegin();
		long bottomRightTime = attributes.getTimeEnd();
		
		long deltaTime = bottomRightTime - topLeftTime;
		final long moveTime = (long)java.lang.Math.ceil(deltaTime * SCALE_MOVE);
		topLeftTime = topLeftTime - moveTime;
		
		if (topLeftTime < 0) {
			topLeftTime = 0;
		}
		bottomRightTime = topLeftTime + deltaTime;
		
		setTimeRange(topLeftTime, bottomRightTime);
		
		updateButtonStates();
    }
    
    /***
     * go to the right one step
     */
    public void goWest()
    {
    	if (!canGoWest())
    		return;
    	
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	
    	long topLeftTime = attributes.getTimeBegin();
		long bottomRightTime = attributes.getTimeEnd();
		
		long deltaTime = bottomRightTime - topLeftTime;
		final long moveTime = (long)java.lang.Math.ceil(deltaTime * SCALE_MOVE);
		bottomRightTime = bottomRightTime + moveTime;
		
		if (bottomRightTime > stData.getTimeWidth()) {
			bottomRightTime = stData.getTimeWidth();
		}
		topLeftTime = bottomRightTime - deltaTime;
		
		setTimeRange(topLeftTime, bottomRightTime);
		
		this.updateButtonStates();
    }
    
    /***
     * set a new range of X-axis
     * @param topLeftTime
     * @param bottomRightTime
     */
    public void setTimeRange(long topLeftTime, long bottomRightTime)
    {
    	final Frame frame = new Frame(stData.getAttributes().getFrame());
    	frame.begTime = topLeftTime;
    	frame.endTime = bottomRightTime;
    	
    	notifyChanges("Zoom H", frame);
    }

    /*******
     * go north one step
     */
    public void goNorth() {
    	if (!canGoNorth())
    		return;
    	
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	
    	int proc_begin = attributes.getProcessBegin();
    	int proc_end = attributes.getProcessEnd();
    	final int delta = proc_end - proc_begin;
    	final int move = (int) java.lang.Math.ceil(delta * SCALE_MOVE);

    	proc_begin = proc_begin - move;
    	
    	if (proc_begin < 0) {
    		proc_begin = 0;
    	}
    	proc_end = proc_begin + delta;
    	this.setProcessRange(proc_begin, proc_end);
		this.updateButtonStates();
    }

    /*******
     * go south one step
     */
    public void goSouth() {
    	if (!canGoSouth())
    		return;
    	
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	
    	int proc_begin = attributes.getProcessBegin();
    	int proc_end = attributes.getProcessEnd();
    	final int delta = proc_end - proc_begin;
    	final int move = (int) java.lang.Math.ceil(delta * SCALE_MOVE);

    	proc_end = proc_end + move;
    	
    	if (proc_end > stData.getTotalTraceCount()) {
    		proc_end = stData.getTotalTraceCount();
    	}
    	proc_begin = proc_end - delta;
    	this.setProcessRange(proc_begin, proc_end);
		this.updateButtonStates();
    }
    
    /***
     * set a new range for Y-axis
     * @param pBegin: the top position
     * @param pEnd: the bottom position
     */
	private void setProcessRange(int pBegin, int pEnd) 
	{
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	final Frame frame = new Frame(attributes.getFrame());
    	frame.begProcess = pBegin;
    	frame.endProcess = pEnd;
    	
		notifyChanges("Zoom V", frame);
	}

	private Position updatePosition(Point mouseDown)
	{
    	final ImageTraceAttributes attributes = stData.getAttributes();
    	final Rectangle view = getClientArea();
    	int selectedProcess;

    	//need to do different things if there are more traces to paint than pixels
    	if(view.height > getNumProcessesDisplayed())
    	{
    		selectedProcess = (int)(attributes.getProcessBegin()+mouseDown.y/getScalePixelsPerRank());
    	}
    	else
    	{
    		selectedProcess = (int)(attributes.getProcessBegin()+(mouseDown.y*(getNumProcessesDisplayed()))/view.height);
    	}
    	long closeTime = attributes.getTimeBegin() + (long)(mouseDown.x / getScalePixelsPerTime());
    	
    	if (closeTime > attributes.getTimeEnd()) {
    		System.err.println("ERR STDC SCSSample time: " + closeTime +" max time: " + 
    				attributes.getTimeEnd() + "\t new: " + ((attributes.getTimeBegin() + attributes.getTimeEnd()) >> 1));
    		closeTime = (attributes.getTimeBegin() + attributes.getTimeEnd()) >> 1;
    	}

    	return new Position(closeTime, selectedProcess);
	}
	
	
	private long getNumTimeUnitDisplayed()
	{
		return (stData.getAttributes().getTimeInterval());
	}
	
	private double getNumProcessesDisplayed()
	{
		return (stData.getAttributes().getProcessInterval());
	}
	
	// remove queue of jobs because it causes deadlock 
	// 
	final private ConcurrentLinkedQueue<BaseViewPaint> queue = new ConcurrentLinkedQueue<>();
	
	/*********************************************************************************
	 * Refresh the content of the canvas with new input data or boundary or parameters
	 *  
	 *  @param refreshData boolean whether we need to refresh and read again the data or not
	 *********************************************************************************/
	public void refresh(boolean refreshData) {
		//Debugger.printTrace("STDC rebuffer");
		//Okay, so here's how this works. In order to draw to an Image (the Eclipse kind)
		//you need to draw to its GC. So, we have this bufferImage that we draw to, so
		//we get its GC (bufferGC), and then pass that GC to paintViewport, which draws
		//everything to it. Then the image is copied to the canvas on the screen with that
		//event.gc.drawImage call down there below the 'if' block - this is called "double buffering," 
		//and it's useful because it prevent the screen from flickering (if you draw directly then
		//you would see each sample as it was getting drawn very quickly, which you would
		//interpret as flickering. This way, you finish the puzzle before you put it on the
		//table).

		// -----------------------------------------------------------------------
		// imageFinal is the final image with info of the depth and number of samples
		// the size of the final image is the same of the size of the canvas
		// -----------------------------------------------------------------------

		
		final Rectangle view = getClientArea();
		final ImageTraceAttributes attributes = stData.getAttributes();
		
		attributes.numPixelsH = view.width;
		attributes.numPixelsV = view.height;
				
		final Image imageFinal = new Image(getDisplay(), view.width, view.height);
		final GC bufferGC = new GC(imageFinal);
		bufferGC.setBackground(Constants.COLOR_WHITE);
		bufferGC.fillRectangle(0,0,view.width,view.height);
		
		// -----------------------------------------------------------------------
		// imageOrig is the original image without "attributes" such as depth
		// this imageOrig will be used by SummaryView to count the number of colors
		// the size of the "original" image should be equivalent to the minimum of 
		//	the number of ranks or the number of pixels
		// -----------------------------------------------------------------------
		
		final int numLines = Math.min(view.height, attributes.getProcessInterval() );
		final Image imageOrig = new Image(getDisplay(), view.width, numLines);
		
		final GC origGC = new GC(imageOrig);
		origGC.setBackground(Constants.COLOR_WHITE);
		origGC.fillRectangle(0,0,view.width, numLines);

		// -----------------------------------------------------------------------
		// main method to paint to the canvas
		// if there's no exception or interruption, we redraw the canvas
		// -----------------------------------------------------------------------

		final boolean changedBounds = (refreshData? refreshData : !attributes.sameTrace(oldAttributes) );
		
		oldAttributes.copy(attributes);
		if (changedBounds) {
			ProcessTimeline []traces = new ProcessTimeline[ numLines ];
			ptlService.setProcessTimeline(traces);
		}

		/*************************************************************************
		 *	Paints the specified time units and processes at the specified depth
		 *	on the SpaceTimeCanvas using the SpaceTimeSamplePainter given. Also paints
		 *	the sample's max depth before becoming overDepth on samples that have gone over depth.
		 *************************************************************************/
		final DetailViewPaint detailPaint = new DetailViewPaint(bufferGC, origGC, stData, 
					attributes, numLines, changedBounds, window, this, threadExecutor); 

		//detailPaint.setUser(true);
		detailPaint.addJobChangeListener(new DetailPaintJobListener(
											imageOrig, imageFinal, 
											bufferGC, origGC, 
											detailPaint, queue, 
											changedBounds));
		
/*		this part of the code causes deadlock on VirtualBox Ubuntu
 *      since we don't clear the queue
 */
  		if (!queue.isEmpty()) {
			for (BaseViewPaint job : queue) {
				if (!job.cancel()) {
					// a job cannot be cancel
					// this is fine, we should wait until it terminates or
					// response that it will cancel in the future
				}
			}
		}
		
		detailPaint.schedule();
		queue.add(detailPaint);
	}

	
	private void asyncRedraw() 
	{
		Display display = Display.getDefault();
		display.asyncExec(new Runnable() {
			
			@Override
			public void run() {
				redraw();
			}
		});
	}

	private void donePainting(Image imageOrig, Image imageFinal, boolean refreshData)
	{		
		initBuffer();
		setBuffer( imageFinal );
		
		// in case of filter, we may need to change the cursor position
		if (refreshData) {
			final String []ranks = stData.getBaseData().getListOfRanks();
			final Position p = stData.getAttributes().getPosition();
			
			if (p.process > ranks.length-1) {
				// out of range: need to change the cursor position
				Position new_p = new Position( p.time, ranks.length >> 1 );
				notifyChangePosition(new_p);
			}
		}
		asyncRedraw();

		// -----------------------------------------------------------------------
		// notify to all other views that a new image has been created,
		//	and it needs to refresh the view
		// -----------------------------------------------------------------------
		notifyChangeBuffer(imageOrig.getImageData());
		
		updateButtonStates();
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose () { 
		threadExecutor.shutdown();
		super.dispose();
	}

	//-----------------------------------------------------------------------------------------
	// Part for notifying changes to other views
	//-----------------------------------------------------------------------------------------
	
	
	/***********************************************************************************
	 * notify changes to other views
	 * 
	 * @param _topLeftTime
	 * @param _topLeftProcess
	 * @param _bottomRightTime
	 * @param _bottomRightProcess
	 ***********************************************************************************/
	private void notifyChanges(String label, Frame frame) 
	{
		String sLabel = (label == null ? "Set region" : label);
		Debugger.printDebug(1, "STDC " + sLabel + " : " + frame);
		// forces all other views to refresh with the new region
		try {
			// notify change of ROI
			TraceOperation.getOperationHistory().execute(
					new ZoomOperation(sLabel, frame), 
					null, null);
			
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/***********************************************************************************
	 * notify cursor position change to other views
	 * 
	 * @param position
	 ***********************************************************************************/
	private void notifyChangePosition(Position position) 
	{
		try {
			TraceOperation.getOperationHistory().execute(
					new PositionOperation(position), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	/***********************************************************************************
	 * Notify other views (especially summary view) that we have changed the buffer.
	 * The other views need to refresh the display if needed.
	 * 
	 * @param imageData
	 ***********************************************************************************/
	private void notifyChangeBuffer(ImageData imageData)
	{
		// -----------------------------------------------------------------------
		// notify to SummaryView that a new image has been created,
		//	and it needs to refresh the view
		// -----------------------------------------------------------------------

		BufferRefreshOperation brOp = new BufferRefreshOperation("refresh", imageData);
		try {
			TraceOperation.getOperationHistory().execute(brOp, null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	//-----------------------------------------------------------------------------------------
	// Part for handling operation triggered from other views
	//-----------------------------------------------------------------------------------------
	private HistoryOperation historyOperation = new HistoryOperation();
	
	@Override
	public void historyNotification(final OperationHistoryEvent event) {
		final IUndoableOperation operation = event.getOperation();

		// handling the operations
		if (operation.hasContext(TraceOperation.traceContext) ||
				operation.hasContext(PositionOperation.context)) 
		{
			int type = event.getEventType();
			// warning: hack solution
			// this space time detail canvas has priority to execute first before the others
			// the reason is most objects requires a new value of process time lines
			//	however this objects are set by this class
			switch (type)
			{
			case OperationHistoryEvent.ABOUT_TO_EXECUTE:
			case OperationHistoryEvent.ABOUT_TO_REDO:
			case OperationHistoryEvent.ABOUT_TO_UNDO:
				historyOperation.setOperation(operation);
				
				// we don't want to run the operation in a separate thread or in the UI thread
				// since this operation can incur an exception such as time out or connection error.
				// instead, we should run within the current process (it isn't ideal, but it works
				//	just fine at the moment)
				
				historyOperation.run();
				break;
			case OperationHistoryEvent.DONE:
				if (operation instanceof PositionOperation) {
					adjustLabels();
				}
				break;
			}
		} else if (operation.hasContext(BufferRefreshOperation.context)) {
			if (event.getEventType() == OperationHistoryEvent.DONE) {
				// this event is triggered by non ui-thread
				// we need to ask ui thread to execute it
				Display.getDefault().asyncExec(new Runnable() {					
					@Override
					public void run() {
						adjustLabels();
					}
				});
			}
		}
	}
	
	
	//-----------------------------------------------------------------------------------------
	// PRIVATE CLASSES
	//-----------------------------------------------------------------------------------------
	

	/******
	 * 
	 * private class to listen to the job status.
	 * once a job is done, we need to free resources and remove the job
	 * from the queue.
	 *
	 ******/
	private class DetailPaintJobListener implements IJobChangeListener
	{
		final Image imageOrig, imageFinal;
		final GC	bufferGC, origGC;
		final BaseViewPaint detailPaint;
		
		final ConcurrentLinkedQueue<BaseViewPaint> queue;

		final boolean changedBounds;
		
		public DetailPaintJobListener(Image imageOrig, Image imageFinal,
							  GC	bufferGC,  GC    origGC,
							  BaseViewPaint detailPaint,
							  ConcurrentLinkedQueue<BaseViewPaint> queue,
							  boolean 		changedBounds)	 {

			this.imageFinal = imageFinal;
			this.imageOrig  = imageOrig;
			this.bufferGC   = bufferGC;
			this.origGC     = origGC;
			this.detailPaint = detailPaint;
			
			this.queue		= queue;
			
			this.changedBounds = changedBounds;
		}
		
		@Override
		public void sleeping(IJobChangeEvent event) {}
		
		@Override
		public void scheduled(IJobChangeEvent event) {}
		
		@Override
		public void running(IJobChangeEvent event) {}
		
		@Override
		public void done(IJobChangeEvent event) {
			if (event.getResult() == Status.OK_STATUS)
			{
				SpaceTimeDetailCanvas.this.donePainting(imageOrig, imageFinal, changedBounds);
			} else
			{
				// we don't need this "new image" since the paint fails
				imageFinal.dispose();	
				asyncRedraw();
			}
			// free resources 
			bufferGC.dispose();
			origGC.dispose();
			imageOrig.dispose();
			
			queue.remove(detailPaint);
		}
		
		@Override
		public void awake(IJobChangeEvent event) {}
		
		@Override
		public void aboutToRun(IJobChangeEvent event) {}
	}

	
	/*************************************************************************
	 * 
	 * Resizing thread by listening to the event if a user has finished
	 * 	the resizing or not
	 *
	 *************************************************************************/
	private class DetailBufferPaint implements BufferPaint
	{

		@Override
		public void rebuffering() {
			// force the paint to refresh the data			
			final ImageTraceAttributes attr = stData.getAttributes();
			try {
				TraceOperation.getOperationHistory().execute(
						new WindowResizeOperation(attr.getFrame()), null, null);
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			//notifyChanges("Resize", attr.getFrame() );
		}
	}

	/*****
	 * 
	 * Thread-centric operation to perform undoable operations asynchronously
	 *
	 *****/
	private class HistoryOperation implements Runnable
	{
		private IUndoableOperation operation;
		
		public void setOperation(IUndoableOperation operation) {
			this.operation = operation;
		}
		
		@Override
		public void run() {
			// zoom in/out or change of ROI ?
			if (operation instanceof ZoomOperation) {
				Frame frame = ((ZoomOperation)operation).getFrame();
				final ImageTraceAttributes attributes = stData.getAttributes();
				
				Debugger.printDebug(1, "STDC: " + attributes + "\t New: " + frame);
				//stData.getAttributes().setPosition(frame.position);
				stData.getAttributes().setFrame(frame);
				zoom(frame.begTime, frame.begProcess, frame.endTime, frame.endProcess);
			}
			// change of cursor position ?
			else if (operation instanceof PositionOperation) {
				Position p = ((PositionOperation)operation).getPosition();
				stData.getAttributes().setPosition(p);

				// just change the position, doesn't need to fully refresh
				redraw();
			} 
			else if (operation instanceof DepthOperation) {
				int depth = ((DepthOperation)operation).getDepth();
				setDepth(depth);
			}
			//adjustLabels();
		}
	}

	@Override
	protected void changePosition(Point point) 
	{
    	Position position = updatePosition(point);
    	notifyChangePosition(position);
	}


	@Override
	protected void changeRegion(Rectangle region) 
	{
		//If we're zoomed in all the way don't do anything
		if(getNumTimeUnitDisplayed() == Constants.MIN_TIME_UNITS_DISP)
		{
			if(getNumTimeUnitDisplayed() > MIN_PROC_DISP)
			{
				adjustSelection(region);
				setDetail();
			}
		}
		else
		{
			adjustSelection(region);
			setDetail();
		}
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