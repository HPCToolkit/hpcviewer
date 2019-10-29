package edu.rice.cs.hpc.traceviewer.timeline;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.DataLinePainting;
import edu.rice.cs.hpc.traceviewer.data.db.DataPreparation;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.TimelineDataSet;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;


/*****************************************************************************
 * 
 * Abstract class for handling data collection
 * The class is based on Java Callable, and returns the number of
 * traces the thread has been processed
 *
 * @author Michael Franco 
 * 	modified by Laksono and Philip
 *****************************************************************************/
public abstract class BaseTimelineThread implements Callable<Integer> {

	/**The minimum height the samples need to be in order to paint the white separator lines.*/
	final static byte MIN_HEIGHT_FOR_SEPARATOR_LINES = 15;

	/**The SpaceTimeData that this thread gets its files from and adds it data and images to.*/
	final protected SpaceTimeDataController stData;
	
	/**The scale in the y-direction of pixels to processors (for the drawing of the images).*/
	final private double scaleY;	
	final protected boolean usingMidpoint;
	final private Queue<TimelineDataSet> queue;
	final private AtomicInteger currentLine;
	final protected IProgressMonitor monitor;
	protected final ImageTraceAttributes attributes;
	

	public BaseTimelineThread(SpaceTimeDataController stData,
			ImageTraceAttributes attributes,
			double scaleY, Queue<TimelineDataSet> queue, 
			AtomicInteger currentLine, 
			boolean usingMidpoint, IProgressMonitor monitor)
	{
		this.stData 	   = stData;
		this.scaleY 	   = scaleY;
		this.usingMidpoint = usingMidpoint;
		this.queue 		   = queue;
		this.currentLine   = currentLine;
		this.monitor 	   = monitor;
		this.attributes	   = attributes;
	}
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	public Integer call() throws Exception {

		ProcessTimeline trace = getNextTrace(currentLine);
		final double pixelLength = (attributes.getTimeInterval())/(double)attributes.numPixelsH;
		final long timeBegin = attributes.getTimeBegin();
		int num_invalid_samples = 0;
		
		DataLinePainting data = new DataLinePainting();
		
		data.colorTable    = stData.getColorTable();
		data.usingMidpoint = usingMidpoint;
		data.pixelLength   = pixelLength;
		data.begTime       = timeBegin;
		data.depth		   = stData.getAttributes().getDepth();
		
		while (trace != null)
		{
			// ---------------------------------
			// begin collecting the data if needed
			// ---------------------------------
			if (init(trace))
			{	
				// this height computation causes inconsistency between different lines
				// we need to find a better way, more deterministic and consistent
				int h1 = (int) Math.round(scaleY*trace.line());
				int h2 = (int) Math.round(scaleY*(trace.line()+1)) ;			
				int imageHeight = h2 - h1;
				
				if (scaleY > MIN_HEIGHT_FOR_SEPARATOR_LINES)
					imageHeight--;
				else
					imageHeight++;

    			// ---------------------------------
    			// do the data preparation
    			// ---------------------------------
				data.ptl    = trace;
				data.height = imageHeight;

				final DataPreparation dataTo = getData(data);
				
				num_invalid_samples += dataTo.collect();
				
				final TimelineDataSet dataSet = dataTo.getList();
				queue.add(dataSet);				
			} else {
				// empty trace, we need to notify the BasePaintThread class
				// of this anomaly by adding NullTimeline
				queue.add(TimelineDataSet.NULLTimeline);
				//System.out.println("init incorrect at " + trace.line());
			}
			if (monitor.isCanceled())
				return null;
			
			monitor.worked(1);
			
			trace = getNextTrace(currentLine);
			
			// ---------------------------------
			// finalize
			// ---------------------------------
			finalize();
		}
		//System.out.println("BTT q: " + queue.size() + " line:" + currentLine.get() +" tot: " + numTraces);
		// terminate the monitor progress bar (if any) when there's no more work to do 
		//monitor.done();
		return Integer.valueOf(num_invalid_samples);
	}

	/****
	 * Retrieve the next available trace, null if no more trace available 
	 * 
	 * @return
	 ****/
	abstract protected ProcessTimeline getNextTrace(AtomicInteger currentLine);
	
	abstract protected boolean init(ProcessTimeline trace) throws IOException;
	
	abstract protected void finalize();
	
	abstract protected DataPreparation getData( DataLinePainting data);
}
