package edu.rice.cs.hpc.traceviewer.painter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Device;
import org.eclipse.ui.IWorkbenchWindow;
import edu.rice.cs.hpc.common.ui.Util;
import edu.rice.cs.hpc.data.util.OSValidator;

import edu.rice.cs.hpc.traceviewer.timeline.BaseTimelineThread;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.TimelineDataSet;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;
import edu.rice.cs.hpc.traceviewer.util.Utility;


/******************************************************
 * 
 * Abstract class to paint depth view and detail view
 * The instance of the children of the class needs to 
 * implement the start and the end method of the painting
 * 
 *
 *******************************************************/
public abstract class BaseViewPaint extends Job
{
	protected boolean changedBounds;
	
	private final IWorkbenchWindow window;
	
	protected SpaceTimeDataController controller;

	final private ExecutorService threadExecutor;
	final private ISpaceTimeCanvas canvas;
	
	final protected ImageTraceAttributes attributes;

	/**
	 * Constructor to paint a view (trace and depth view)
	 * 
	 * @param title name of this view (job title purpose)
	 * @param _data global data of the traces
	 * @param _attributes the attribute of the trace view
	 * @param _changeBound true or false if it requires changes of bound
	 * @param window
	 * @param canvas
	 * @param threadExecutor executor
	 */

	public BaseViewPaint(String title, SpaceTimeDataController _data, ImageTraceAttributes attributes, boolean _changeBound, 
			IWorkbenchWindow window, ISpaceTimeCanvas canvas, ExecutorService threadExecutor) 
	{
		super(title);
		
		changedBounds = _changeBound;
		controller = _data;

		this.window 		= (window == null ? Util.getActiveWindow() : window);
		this.canvas 		= canvas;
		this.threadExecutor = threadExecutor;
		this.attributes		= attributes;
		
		setRule(new MutexRule(this));
	}
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) {
		IStatus status = Status.OK_STATUS;
		
		//BusyIndicator.showWhile(getDisplay(), getThread());
		if (!paint( monitor))
		{
			status = Status.CANCEL_STATUS;
		}
		
		return status;
	}	

	/**********************************************************************************
	 *	Paints the specified time units and processes at the specified depth
	 *	on the SpaceTimeCanvas using the SpaceTimeSamplePainter given. Also paints
	 *	the sample's max depth before becoming overDepth on samples that have gone over depth.
	 *
	 *	@param canvas   		 The SpaceTimeDetailCanvas that will be painted on.
	 *  @return boolean true of the pain is successful, false otherwise
	 ***********************************************************************************/
	public boolean paint(IProgressMonitor monitor)
	{	
		// depending upon how zoomed out you are, the iteration you will be
		// making will be either the number of pixels or the processors
		int linesToPaint = getNumberOfLines();
		Debugger.printDebug(2, "BVP-begin " + linesToPaint + " lines");

		// -------------------------------------------------------------------
		// hack fix: if the number of horizontal pixels is less than 1 we
		// return immediately, otherwise it throws an exception
		// -------------------------------------------------------------------
		if (attributes.numPixelsH <= 0)
			return false;
		
		// -------------------------------------------------------------------
		// initialize the painting (to be implemented by the instance)
		// -------------------------------------------------------------------
		int launch_threads = Utility.getNumThreads(Math.min(linesToPaint, 4));
		if (!startPainting(linesToPaint, launch_threads, changedBounds))
			return false;

		monitor.beginTask(getName(), linesToPaint);

		// -------------------------------------------------------------------
		// Create multiple threads to collect data
		// -------------------------------------------------------------------
		
		// decompression can be done with multiple threads without accessing gtk (on linux)
		// It looks like there's no major performance effect though
		Debugger.printDebug(2, "BVP launch threads " + launch_threads);
		try {
			launchDataGettingThreads(changedBounds, launch_threads);
			
		} catch (Exception e) {
			/*MessageDialog.openError(window.getShell(), "Error while reading data", 
					e.getMessage());*/
			e.printStackTrace();
			
			// shutdown the monitor to end the progress bar
			monitor.done();
			return false;
		}
		
		// -------------------------------------------------------------------
		// instantiate queue based on whether we need multi-threading or not
		// in case of multithreading, we want a thread-safe queue
		// -------------------------------------------------------------------
		final Queue<TimelineDataSet> queue;
		if (launch_threads > 1) {
			queue = new ConcurrentLinkedQueue<TimelineDataSet>();
		} else {
			queue = new LinkedList<TimelineDataSet>();
		}

		// -------------------------------------------------------------------
		// case where everything works fine, and all the data has been read,
		//	we paint the canvas using multiple threads
		// -------------------------------------------------------------------
		
		Debugger.printTimestampDebug("Rendering beginning (" + canvas.toString()+")");
		
		final double xscale = canvas.getScalePixelsPerTime();
		final double yscale = Math.max(canvas.getScalePixelsPerRank(), 1);
		
		ExecutorCompletionService<Integer> ecs = new ExecutorCompletionService<Integer>(threadExecutor);
		
		for (int threadNum = 0; threadNum < launch_threads; threadNum++) {
			final BaseTimelineThread thread = getTimelineThread(canvas, xscale, yscale, queue, 
					monitor);
			
			try {
				// when hpctraceviewer exits while a job is still running,
				// there will be a sporadic of error messages.
				// we should just warning the user instead of scaring them with
				// error message and backtrace
				
				ecs.submit(thread);
			} catch (Exception e) {
				System.err.println("Warning. Fail to launch job: " + e.getMessage());
			}
		}
		
		// -------------------------------------------------------------------
		// draw to the canvas
		// -------------------------------------------------------------------

		// -------------------------------------------------------------------
		// hack: On Linux, gtk is not threads-safe, and SWT-gtk implementation
		//		 uses lock everytime it calls gtk functions. This greatly impact
		//		 performance degradation, and we don't have the solution until now.
		//	At the moment we don't see any reason to use multi-threading to render
		//	 	 the canvas
		// -------------------------------------------------------------------

		Debugger.printDebug(1, canvas.toString() + " BVP --- lp: " + linesToPaint + ", qs: " + queue.size());
		Debugger.printTimestampDebug("Rendering mostly finished. (" + canvas.toString()+")");
		
		if (OSValidator.isUnix()) 
		{
			// -------------------------------------------------------------------
			// sequential painting for Unix/Linux platform
			// -------------------------------------------------------------------
			executePaint(ecs, launch_threads, 1, queue, linesToPaint, monitor);
		} else
		{
			// -------------------------------------------------------------------
			// painting to the buffer "concurrently" if numPaintThreads > 1
			// -------------------------------------------------------------------
			executePaint(ecs, launch_threads, launch_threads, 
					queue, linesToPaint, monitor);
		}		
		Debugger.printTimestampDebug("Rendering finished. (" + canvas.toString()+")");
		monitor.done();
		changedBounds = false;

		return !monitor.isCanceled();
	}
	
	/****
	 * run jobs for collecting data and painting the image
	 * 
	 * @param ecs : completion service from data collection job
	 * @param num_threads : number of threads for collecting data (see ecs)
	 * @param num_paint_threads : number of threads for paiting
	 * @param queue : the data to be collected
	 * @param linesToPaint : number of lines to paint
	 * @param timelineDone : atomic integer for number of lines collected
	 * @param monitor : UI progress monitor 
	 */
	private void executePaint(ExecutorCompletionService<Integer> ecs,
			int num_threads, int num_paint_threads, Queue<TimelineDataSet> queue, 
			int linesToPaint, IProgressMonitor monitor) 
	{
		final List<Future<List<ImagePosition>>> threadsPaint = new ArrayList<Future<List<ImagePosition>>>();
		Device device = window.getShell().getDisplay();
		
		// for threads as many as the number of paint threads (specified by the caller)
		for (int threadNum=0; threadNum < num_paint_threads; threadNum++) 
		{
			final BasePaintThread thread = getPaintThread(queue, linesToPaint,
					device, attributes.numPixelsH, monitor);
			if (thread != null) {
				final Future<List<ImagePosition>> submit = threadExecutor.submit( thread );
				threadsPaint.add(submit);
			}
		}
		// -------------------------------------------------------------------
		// Finalize the painting (to be implemented by the instance)
		// -------------------------------------------------------------------
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (waitDataPreparationThreads(ecs, result, num_threads, monitor))
		{
			endPainting(threadsPaint, monitor);
		} else {
			monitor.setCanceled(true);
			// whatever the result, we need to clear the 
			monitor.setTaskName("");
		}
	}
	
	/******
	 * Wait for any thread who finishes first, and then add the result to the list
	 * 
	 * @param ecs : executor service
	 * @param result : the list of the result
	 * @param launch_threads : number of launched threads
	 */
	private boolean waitDataPreparationThreads(ExecutorCompletionService<Integer> ecs, 
			ArrayList<Integer> result, int launch_threads, IProgressMonitor monitor)
	{
		int num_invalid_samples = 0;
		for (int i=0; i<launch_threads; i++)
		{
			if (monitor.isCanceled())
				return false;
			
			try {
				Integer linenum = ecs.take().get();
				if (linenum == null)
					return false;
				num_invalid_samples += linenum.intValue();
				
				result.add(linenum);
				Debugger.printDebug(1, getClass() + " thread " + i + "/" + launch_threads + " finish " + linenum);
			} catch (Exception e) {
				// we don't need to show exception message everywhere unless if we are in develop mode
				if (Debugger.getDebugLevel()>0)
					e.printStackTrace();
				return false;
			}
		}
		if (num_invalid_samples > 0) {
			final String message = "Warning: " + num_invalid_samples + 
					" sample(s) have invalid call-path ID.";
			canvas.setMessage(message);
		}
		return true;
	}

	/******
	 * finalize the data collection, and put all images into a canvas
	 * 
	 * @param canvas
	 * @param listOfImageThreads
	 */
	private boolean endPainting(List<Future<List<ImagePosition>>> listOfImageThreads,
			IProgressMonitor monitor)
	{
		for( Future<List<ImagePosition>> listFutures : listOfImageThreads ) 
		{
			if (monitor.isCanceled())
				return false;
			
			try {
				List<ImagePosition> listImages = listFutures.get();
				if (listImages == null)
					return false;
				for (ImagePosition image : listImages) 
				{
					if (!monitor.isCanceled())
						drawPainting(canvas, image);
					else
						return false;
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// notify user that we have finished painting
		endPainting(monitor.isCanceled());
		
		return true;
	}
		
	/*********************************
	 * 
	 * Rule for avoiding two jobs execute simultaneously
	 *
	 *********************************/
	static private class MutexRule implements ISchedulingRule
	{
		final private BaseViewPaint paint;
		
		public MutexRule(BaseViewPaint paint) {
			this.paint = paint;
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return theSame(rule);
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return theSame(rule);
		}
		
		private boolean theSame(ISchedulingRule rule) {
			if (rule instanceof MutexRule) {
				// we try to avoid conflict between painting with the same class:
				// main trace job shouldn't be run simultaneously with other main trace job
				boolean ret = paint.getName().equals( ((MutexRule)rule).paint.getName() );
				return ret;
			}
			return false;
		}
	}
	
	//------------------------------------------------------------------------------------------------
	// abstract methods 
	//------------------------------------------------------------------------------------------------
	
	/**
	 * Initialize the paint, before creating the threads to paint.
	 * The method return false to exit the paint, true to paint.
	 * 
	 * The implementer is also responsible to reset its counter (yikes!)
	 * 
	 * @param linesToPaint
	 * @param changedBounds
	 * @return false will exit the painting
	 */
	abstract protected boolean startPainting(int linesToPaint, int numThreads, boolean changedBounds);
	
	/*****
	 * notification for the termination of painting
	 * 
	 * @param isCanceled : flag if the process has been canceled or not
	 */
	abstract protected void endPainting(boolean isCanceled);
	
	/***
	 * start painting an image to the canvas
	 * 
	 * @param canvas: canvas to be painted
	 * @param imagePosition : a pair of image and position
	 */
	abstract protected void drawPainting(ISpaceTimeCanvas canvas, ImagePosition imagePosition);
	
	/**
	 * Retrieve the number of lines to paint 
	 * @return
	 */
	abstract protected int getNumberOfLines();
	
	/****
	 * launching threads for remote communication
	 * 
	 * @param changedBounds
	 * @param numThreads
	 * @throws IOException
	 */
	abstract protected void launchDataGettingThreads(boolean changedBounds, int numThreads) 
			throws IOException;

	/****
	 * get a thread for collecting timeline data
	 * @param canvas
	 * @param xscale
	 * @param yscale
	 * @param queue
	 * @param timelineDone
	 * @return
	 */
	abstract protected BaseTimelineThread  getTimelineThread(ISpaceTimeCanvas canvas, double xscale, double yscale,
			Queue<TimelineDataSet> queue, IProgressMonitor monitor);
	
	/***
	 * get a thread for painting a number of lines
	 * @param queue
	 * @param numLines
	 * @param timelineDone
	 * @param device
	 * @param width
	 * @return
	 */
	abstract protected BasePaintThread getPaintThread( Queue<TimelineDataSet> queue, int numLines, 
			Device device, int width, IProgressMonitor monitor);
}
