package edu.rice.cs.hpc.traceviewer.spaceTimeData;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.lang.Math;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.traceviewer.events.TraceEvents;
import edu.rice.cs.hpc.traceviewer.painter.DepthTimeCanvas;
import edu.rice.cs.hpc.traceviewer.painter.Position;
import edu.rice.cs.hpc.traceviewer.painter.SpaceTimeDetailCanvas;
import edu.rice.cs.hpc.traceviewer.painter.SpaceTimeSamplePainter;

/*************************************************************************
 * 
 *	SpaceTimeData stores and creates all of the data involved in creating
 *	the view including all of the ProcessTimelines.
 *
 ************************************************************************/
public class SpaceTimeData extends TraceEvents
{
	/** Contains all of the ProcessTimelines. It's a HashMap because,
	 * due to the multithreading, the traces may not get added in order.
	 * So, each ProcessTimeline now knows which line it is, and the
	 * HashMap is a map between that line and the ProcessTimeline.*/
	private ProcessTimeline traces[];
	
	private ProcessTimeline depthTrace;
	
	/**The composite images created by painting all of the samples in a given line to it.*/
	private Image[] compositeLines;
	
	/** Contains the Call Path Trace files that are parsed by CallStackTrace to construct the ProcessTimelines.*/
	private ArrayList<File> traceFiles;
	
	/** Stores the color to function name assignments for all of the functions in all of the processes.*/
	private ColorTable colorTable;
	
	/** The composite that holds the Context View canvas and the Detail View canvas.*/
	private Composite canvasHolder;
	
	/**The map between the nodes and the cpid's.*/
	private HashMap<Integer, Scope> scopeMap;
	
	/**The total number of traces.*/
	private int height;
	
	/**The maximum depth of any single CallStackSample in any trace.*/
	private int maxDepth;
	
	/**The minimum beginning and maximum ending time stamp across all traces (in microseconds)).*/
	private long minBegTime;
	private long maxEndTime;
	
	/**The beginning/end of the process range on the viewer.*/
	private int begProcess;
	private int endProcess;
	
	/**The process to be painted in the depth time viewer.*/
	private int dtProcess;
	
	/**The beginning/end of the time range on the viewer.*/
	private long begTime;
	private long endTime;
	
	/** The width of the detail canvas in pixels.*/
	private int numPixelsH;
	
	/** The height of the detail canvas in pixels.*/
	private int numPixelsV;
	
	/**The number of the line that's being processed (for threads).*/
	private int lineNum;
	
	/**The file that's the SpaceTimeData is initializing (getting first and last timestamps) - 
	used in initialization for threads.*/
	private int fileNum;
	
	/** Stores the current depth that is being displayed.*/
	private int currentDepth;
	
	/** Stores the current position of cursor */
	private Position currentPosition;
	
	private String dbName;
	
	final private boolean debug =  true;
	
	/*************************************************************************
	 *	Creates, stores, and adjusts the ProcessTimelines and the ColorTable.
	 ************************************************************************/
	public SpaceTimeData(Composite _canvasHolder, File expFile, ArrayList<File> _traceFiles)
	{
		canvasHolder = _canvasHolder;
		colorTable = new ColorTable(canvasHolder.getDisplay());
		
		//Initializes the CSS that represents time values outside of the time-line.
		colorTable.addProcedure(CallStackSample.NULL_FUNCTION); 
		traceFiles = _traceFiles;
		
		System.out.println("Reading experiment database file '" + expFile.getPath() + "'");

		Experiment exp = new Experiment(expFile);
		try {
			exp.open(false);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InvalExperimentException e) {
			System.out.println("Parse error in Experiment XML at line " + e.getLineNumber());
			e.printStackTrace();
			return;
		}
		
		scopeMap = new HashMap<Integer, Scope>();
		TraceDataVisitor visitor = new TraceDataVisitor(scopeMap);	
		maxDepth = exp.getRootScope().dfsSetup(visitor, colorTable, 1);
		
		colorTable.setColorTable();
		
		minBegTime = exp.trace_minBegTime;
		maxEndTime = exp.trace_maxEndTime;
		
		height = traceFiles.size();
		
		// default position
		this.currentPosition = new Position(0,0);
		this.dbName = exp.getName();
		//System.gc();
	}

	public String getName() {
		return this.dbName;
	}
	
	public void setDepth(int _depth) {
		this.currentDepth = _depth;
	}
	
	public int getDepth() {
		return this.currentDepth;
	}
	/*************************************************************************
	 *	Returns width of the spaceTimeData:
	 *	The width (the last time in the ProcessTimeline) of the longest 
	 *	ProcessTimeline. 
	 ************************************************************************/
	public long getWidth()
	{
		return maxEndTime - minBegTime;
	}
	
	/******************************************************************************
	 *	Returns number of processes (ProcessTimelines) held in this SpaceTimeData.
	 ******************************************************************************/
	public int getHeight()
	{
		return height;
	}
	
	/*************************************************************************
	 *	Returns the ColorTable holding all of the color to function name 
	 *	associations for this SpaceTimeData.
	 ************************************************************************/
	public ColorTable getColorTable()
	{
		return colorTable;
	}
	
	/*************************************************************************
	 *	Returns the lowest starting time of all of the ProcessTimelines.
	 ************************************************************************/
	public long getMinBegTime()
	{
		return minBegTime;
	}

	/*************************************************************************
	 * 
	 * @return the highest end time of all of the process time lines
	 *************************************************************************/
	public long getMaxBegTime()
	{
		return maxEndTime;
	}
	
	public long getViewTimeBegin() {
		return this.begTime;
	}
	
	public long getViewTimeEnd() {
		return this.endTime;
	}

	/*************************************************************************
	 *	Returns the largest depth of all of the CallStackSamples of all of the
	 *	ProcessTimelines.
	 ************************************************************************/
	public int getMaxDepth()
	{
		return maxDepth;
	}
	
	/**********************************************************************************
	 *	Paints the specified time units and processes at the specified depth
	 *	on the SpaceTimeCanvas using the SpaceTimeSamplePainter given. Also paints
	 *	the sample's max depth before becoming overDepth on samples that have gone over depth.
	 *
	 *	@param masterGC   		 The GC that will contain the combination of all the 1-line GCs.
	 *	@param canvas   		 The SpaceTimeDetailCanvas that will be painted on.
	 *	@param depth 			 The depth-slice in the Call Paths that will be painted.
	 *	@param begProcess        The first process that will be painted.
	 *	@param endProcess 		 The last process that will be painted.
	 *	@param begTime           The first time unit that will be displayed.
	 *	@param endTime 			 The last time unit that will be displayed.
	 *  @param numPixelsH		 The number of horizontal pixels to be painted.
	 *  @param numPixelsV		 The number of vertical pixels to be painted.
	 ***********************************************************************************/
	public void paintDetailViewport(GC masterGC, SpaceTimeDetailCanvas canvas, int depth, int _begProcess, int _endProcess, long _begTime, long _endTime, int _numPixelsH, int _numPixelsV)
	{
		//long programTime = System.currentTimeMillis();
		boolean changedBounds = true;
		if (begTime == _begTime && endTime == _endTime && begProcess == _begProcess && endProcess == _endProcess && numPixelsH == _numPixelsH && numPixelsV == _numPixelsV)
			changedBounds = false;
		else
			traces = new ProcessTimeline[Math.min(_numPixelsV, _endProcess - _begProcess)];

		begTime = _begTime;
		endTime = _endTime;
		begProcess = _begProcess;
		endProcess = _endProcess;
		numPixelsH = _numPixelsH;
		numPixelsV = _numPixelsV;
		
		//depending upon how zoomed out you are, the iteration you will be making will be either the number of pixels or the processor
		int linesToPaint = Math.min(numPixelsV, endProcess - begProcess);
		
		compositeLines = new Image[linesToPaint];
		lineNum = 0;
		TimelineThread[] threads;
		threads = new TimelineThread[Math.min(linesToPaint, Runtime.getRuntime().availableProcessors())];
		
		for (int threadNum = 0; threadNum < threads.length; threadNum++)
		{
			threads[threadNum] = new TimelineThread(this, changedBounds, canvas, numPixelsH, canvas.getScaleX(), Math.max(canvas.getScaleY(), 1));
			threads[threadNum].start();
		}
			try
			{
				for (int threadNum = 0; threadNum < threads.length; threadNum++)
				{
					if (threads[threadNum].isAlive())
						threads[threadNum].join();
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		for (int i = 0; i < linesToPaint; i++)
		{
			masterGC.drawImage(compositeLines[i], 0, 0, compositeLines[i].getBounds().width, compositeLines[i].getBounds().height, 0, (int)Math.round(i*Math.max(canvas.getScaleY(),1)), 
					compositeLines[i].getBounds().width, compositeLines[i].getBounds().height);
		}
	}
	
	public void paintDepthViewport(GC masterGC, DepthTimeCanvas canvas, long _begTime, long _endTime, int _numPixelsH, int _numPixelsV)
	{
		boolean changedBounds = true;
		int process = this.currentPosition.process;
		
		if (begTime == _begTime && endTime == _endTime && dtProcess == process && numPixelsH == _numPixelsH && numPixelsV == _numPixelsV)
		{
			changedBounds = false;
		}
		else
		{
			depthTrace = null;
		}
		
		//depending upon how zoomed out you are, the iteration you will be making will be either the number of pixels or the processor
		//long programTime = System.currentTimeMillis();
		int linesToPaint = Math.min(_numPixelsV, maxDepth);
		if (changedBounds)
		{
			begTime = _begTime;
			endTime = _endTime;
			dtProcess = process;
			numPixelsH = _numPixelsH;
			numPixelsV = _numPixelsV;
			
			compositeLines = new Image[linesToPaint];
			lineNum = 0;
			depthTrace = new ProcessTimeline(0, scopeMap, traceFiles.get(dtProcess), numPixelsH, endTime-begTime, minBegTime+begTime);
			depthTrace.readInData();
			depthTrace.shiftTimeBy(minBegTime);
			
			TimelineThread[] threads;
			threads = new TimelineThread[Math.min(linesToPaint, Runtime.getRuntime().availableProcessors())];
			
			for (int threadNum = 0; threadNum < threads.length; threadNum++)
			{
				threads[threadNum] = new TimelineThread(this, false, canvas, numPixelsH, canvas.getScaleX(), Math.max(numPixelsV/(double)maxDepth, 1));
				threads[threadNum].start();
			}
			
				try
				{
					for (int threadNum = 0; threadNum < threads.length; threadNum++)
					{
						if (threads[threadNum].isAlive())
							threads[threadNum].join();
					}
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
		
		}
		for (int i = 0; i < linesToPaint; i++)
		{
			masterGC.drawImage(compositeLines[i], 0, 0, compositeLines[i].getBounds().width, 
					compositeLines[i].getBounds().height, 0,(int)Math.round(i*numPixelsV/(float)maxDepth), 
					compositeLines[i].getBounds().width, compositeLines[i].getBounds().height);
		}
		//System.out.println("Took "+(System.currentTimeMillis()-programTime)+" milliseconds to paint depth time canvas.");
		
	}
	

	
	/**********************************************************************
	 * Paints one "line" (the timeline for one processor) to its own image,
	 * which is later copied to a master image with the rest of the lines.
	 ********************************************************************/
	/**////////////////////////////////////////////////////////////////////////////////////
	//Because you will be painting between midpoints of samples,
	//you need to paint from the midpoint of the two nearest samples off screen
	//--which, when Math.max()'d with 0 will always return 0--
	//to the midpoint of the samples straddling the edge of the screen
	//--which, when Math.max()'d with 0 sometimes will return 0--
	//as well as from the midpoint of the samples straddling the edge of the screen
	//to the midpoint of the first two samples officially in the view
	//before even entering the loop that paints samples that exist fully in view
	////////////////////////////////////////////////////////////////////////////////////*/
	public void paintDepthLine(SpaceTimeSamplePainter spp, int depth, int height)
	{
		//System.out.println("I'm painting process "+process+" at depth "+depth);
		ProcessTimeline ptl = depthTrace;

		double pixelLength = (endTime - begTime)/(double)numPixelsH;
		//Reed - special cases were giving me a headache, so I threw them in a switch
		switch(ptl.size())
		{
			case 0:
			case 1:
				this.printDebug("Warning! incorrect timestamp size: " + ptl.size() );
				break;
			case 2:
			{
				//System.out.println("two");
				//only one or two samples to be painted; special case
				
				//the call path sample based on the sample index
				CallStackSample css = ptl.getSample(0);
				//the updated depth
				int sampleDepth = css.getSize()-1;
				//the function name of the sample at the correct depth
				String functionName = css.getFunctionName(Math.min(depth, sampleDepth));
				//the "midpoints" of the sample to be painted
				int currSampleMidpoint = (int) Math.max(0, (ptl.getTime(0)-begTime)/pixelLength);
				int succSampleMidpoint = (int) Math.min(numPixelsH, (midpoint(ptl.getTime(0), ptl.getTime(1))-begTime)/pixelLength);
				succSampleMidpoint = Math.max(0, succSampleMidpoint);
				if(ptl.getCpid(0) == ptl.getCpid(1))
				{
					succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(1)-begTime)/pixelLength);
					succSampleMidpoint = Math.max(0, succSampleMidpoint);
					if (sampleDepth >= depth)
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);				
					}
				}
				else
				{
					if (sampleDepth >= depth)
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					}
					
					css = ptl.getSample(1);
					sampleDepth = css.getSize()-1;
					functionName = css.getFunctionName(Math.min(depth, sampleDepth));
					
					currSampleMidpoint = succSampleMidpoint;
					succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(1)-begTime)/pixelLength);
					succSampleMidpoint = Math.max(0, succSampleMidpoint);
					
					if (sampleDepth >= depth)
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);			
					}
				}
				break;
			}
			case 3:
			{
				//System.out.println("three");
				//there are three items in the ptl; either one, two, or three samples to be painted,
				//depending on the locations of the midpoints
				
				//the call path sample based on the sample index
				CallStackSample css = ptl.getSample(0);
				//the updated depth
				int sampleDepth = css.getSize()-1;
				//the function name of the sample at the correct depth
				String functionName = css.getFunctionName(Math.min(depth, sampleDepth));
				//the "midpoints" of the sample to be painted
				int currSampleMidpoint = (int) Math.max(0, (ptl.getTime(0)-begTime)/pixelLength);
				int succSampleMidpoint = (int) Math.max(0, (midpoint(ptl.getTime(0),ptl.getTime(1))-begTime)/pixelLength);
				succSampleMidpoint = Math.min(numPixelsH, succSampleMidpoint);
				if(ptl.getCpid(0) == ptl.getCpid(1))
				{
					succSampleMidpoint = (int) Math.min(numPixelsH, (midpoint(ptl.getTime(1),ptl.getTime(2))-begTime)/pixelLength);
					if (sampleDepth >= depth)
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					}
					
					currSampleMidpoint = succSampleMidpoint;
					succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(2)-begTime)/pixelLength);
					if (sampleDepth >= depth)
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					}
				}
				else
				{
					if (sampleDepth >= depth)
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					}
					
					css = ptl.getSample(1);
					sampleDepth = css.getSize()-1;
					functionName = css.getFunctionName(Math.min(depth, sampleDepth));
					
					currSampleMidpoint = succSampleMidpoint;
					succSampleMidpoint = (int) Math.min(numPixelsH, (midpoint(ptl.getTime(1),ptl.getTime(2))-begTime)/pixelLength);
					if(ptl.getCpid(1) == ptl.getCpid(2))
					{
						succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(2)-begTime)/pixelLength);
						if (sampleDepth >= depth)
						{
							spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						}
					}
					else
					{
						if (sampleDepth >= depth)
						{
							spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						}
						
						css = ptl.getSample(2);
						sampleDepth = css.getSize()-1;
						functionName = css.getFunctionName(Math.min(depth, sampleDepth));
						
						currSampleMidpoint = succSampleMidpoint;
						succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(2)-begTime)/pixelLength);
						if (sampleDepth >= depth)
						{
							spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						}
					}
				}
				break;
			}
			default:
			{

				BasePaintLine depthPaint = new BasePaintLine(colorTable, ptl, spp, depth, height, pixelLength) {
					@Override
					public void finishPaint(int currSampleMidpoint, int succSampleMidpoint, int currDepth, String functionName) {
						if (currDepth >= depth)
						{
							spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						}
					}
				};
				
				// do the paint
				depthPaint.paint();

			}
			break;
		}
	}
	
	/***********************************************************************
	 * 
	 * Basic abstract class to paint for trace view and depth view
	 * 
	 * we will use an abstract method to finalize the painting since 
	 * 	depth view has slightly different way to paint compared to
	 * 	trace view
	 * 
	 ***********************************************************************/
	private abstract class BasePaintLine {
		protected ProcessTimeline ptl;
		protected SpaceTimeSamplePainter spp;
		protected int depth;
		protected int height;
		protected double pixelLength;
		protected ColorTable colorTable;
		
		public BasePaintLine(ColorTable _colorTable, ProcessTimeline _ptl, SpaceTimeSamplePainter _spp, 
				int _depth, int _height, double _pixelLength) {
			this.ptl = _ptl;
			this.spp = _spp;
			this.depth = _depth;
			this.height = _height;
			this.pixelLength = _pixelLength;
			this.colorTable = _colorTable;
		}
		
		/***
		 * Painting action
		 */
		public void paint() {
			int succSampleMidpoint = (int) Math.max(0, (ptl.getTime(0)-begTime)/pixelLength);
			CallStackSample succSample = ptl.getSample(0);
			int succDepth = Math.min(depth, succSample.getSize()-1);
			String succFunction = succSample.getFunctionName(succDepth);
			Color succColor = colorTable.getColor(succFunction);

			for (int index = 0; index < ptl.size(); index++)
			{
				int currDepth = succDepth;
				int currSampleMidpoint = succSampleMidpoint;
				
				//-----------------------------------------------------------------------
				// skipping if the successor has the same color and depth
				//-----------------------------------------------------------------------
				boolean still_the_same = true;
				int indexSucc = index;
				final String functionName = succFunction;
				final Color currColor = succColor;
				
				while(still_the_same && (indexSucc < ptl.size()-1)) {
					indexSucc++;
					succSample = ptl.getSample(indexSucc);
					succDepth = Math.min(depth, succSample.getSize()-1);
					succFunction = succSample.getFunctionName(succDepth);
					succColor = this.colorTable.getColor(succFunction);
					
					still_the_same = (succDepth == currDepth) && (succColor==currColor);
					if (still_the_same)
						index = indexSucc;
				};
				
				if (index<ptl.size()-1) {
					// --------------------------------------------------------------------
					// start and middle samples: the rightmost point is the midpoint between
					// 	the two samples
					// --------------------------------------------------------------------
					succSampleMidpoint = (int) Math.max(0, ((midpoint(ptl.getTime(index),ptl.getTime(index+1))-begTime)/pixelLength));

				} else {
					// --------------------------------------------------------------------
					// for the last iteration (or last sample), we don't have midpoint
					// 	so the rightmost point will be the time of the last sample
					// --------------------------------------------------------------------
					succSampleMidpoint = (int) Math.max(0, ((ptl.getTime(index+1)-begTime)/pixelLength));
				}
				this.finishPaint(currSampleMidpoint, succSampleMidpoint, currDepth, functionName);
			}			
		}
		
		/***
		 * Abstract method to finalize the painting given its range, depth and the function name
		 * 
		 * @param currSampleMidpoint
		 * @param succSampleMidpoint
		 * @param currDepth
		 * @param functionName
		 */
		public abstract void finishPaint(int currSampleMidpoint, int succSampleMidpoint, int currDepth, String functionName);
	}


	
	public void paintDetailLine(SpaceTimeSamplePainter spp, int depth, int process, int height, boolean changedBounds)
	{
		//System.out.println("I'm painting process "+process+" at depth "+depth);
		ProcessTimeline ptl = traces[process];
		if (ptl == null)
			return;
		
		if (changedBounds)
			ptl.shiftTimeBy(minBegTime);
		double pixelLength = (endTime - begTime)/(double)numPixelsH;
		//Reed - special cases were giving me a headache, so I threw them in a switch
		switch(ptl.size())
		{
			case 0:
			case 1:
				this.printDebug("Warning! incorrect timestamp size: " + ptl.size() );
				break;
			case 2:
			{
				//System.out.println("two");
				//only one or two samples to be painted; special case
				
				//the call path sample based on the sample index
				CallStackSample css = ptl.getSample(0);
				//the updated depth
				int sampleDepth = css.getSize()-1;
				//the function name of the sample at the correct depth
				String functionName = css.getFunctionName(Math.min(depth, sampleDepth));
				//the "midpoints" of the sample to be painted
				int currSampleMidpoint = (int) Math.max(0, (ptl.getTime(0)-begTime)/pixelLength);
				int succSampleMidpoint = (int) Math.min(numPixelsH, (midpoint(ptl.getTime(0), ptl.getTime(1))-begTime)/pixelLength);
				succSampleMidpoint = Math.max(0, succSampleMidpoint);
				if(ptl.getCpid(0) == ptl.getCpid(1))
				{
					succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(1)-begTime)/pixelLength);
					succSampleMidpoint = Math.max(0, succSampleMidpoint);
					spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);				
					if (sampleDepth < depth)
					{
						spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
					}
				}
				else
				{
					spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);				
					if (sampleDepth < depth)
					{
						spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
					}
					
					css = ptl.getSample(1);
					sampleDepth = css.getSize()-1;
					functionName = css.getFunctionName(Math.min(depth, sampleDepth));
					
					currSampleMidpoint = succSampleMidpoint;
					succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(1)-begTime)/pixelLength);
					succSampleMidpoint = Math.max(0, succSampleMidpoint);
					
					spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);				
					if (sampleDepth < depth)
					{
						spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
					}
				}
				break;
			}
			case 3:
			{
				//System.out.println("three");
				//there are three items in the ptl; either one, two, or three samples to be painted,
				//depending on the locations of the midpoints
				
				//the call path sample based on the sample index
				CallStackSample css = ptl.getSample(0);
				//the updated depth
				int sampleDepth = css.getSize()-1;
				//the function name of the sample at the correct depth
				String functionName = css.getFunctionName(Math.min(depth, sampleDepth));
				//the "midpoints" of the sample to be painted
				int currSampleMidpoint = (int) Math.max(0, (ptl.getTime(0)-begTime)/pixelLength);
				int succSampleMidpoint = (int) Math.max(0, (midpoint(ptl.getTime(0),ptl.getTime(1))-begTime)/pixelLength);
				succSampleMidpoint = Math.min(numPixelsH, succSampleMidpoint);
				if(ptl.getCpid(0) == ptl.getCpid(1))
				{
					succSampleMidpoint = (int) Math.min(numPixelsH, (midpoint(ptl.getTime(1),ptl.getTime(2))-begTime)/pixelLength);
					spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					if (sampleDepth < depth)
					{
						spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
					}
					
					currSampleMidpoint = succSampleMidpoint;
					succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(2)-begTime)/pixelLength);
					spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					if (sampleDepth < depth)
					{
						spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
					}
				}
				else
				{
					spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
					if (sampleDepth < depth)
					{
						spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
					}
					
					css = ptl.getSample(1);
					sampleDepth = css.getSize()-1;
					functionName = css.getFunctionName(Math.min(depth, sampleDepth));
					
					currSampleMidpoint = succSampleMidpoint;
					succSampleMidpoint = (int) Math.min(numPixelsH, (midpoint(ptl.getTime(1),ptl.getTime(2))-begTime)/pixelLength);
					if(ptl.getCpid(1) == ptl.getCpid(2))
					{
						succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(2)-begTime)/pixelLength);
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						if (sampleDepth < depth)
						{
							spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
						}
					}
					else
					{
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						if (sampleDepth < depth)
						{
							spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
						}
						
						css = ptl.getSample(2);
						sampleDepth = css.getSize()-1;
						functionName = css.getFunctionName(Math.min(depth, sampleDepth));
						
						currSampleMidpoint = succSampleMidpoint;
						succSampleMidpoint = (int) Math.min(numPixelsH, (ptl.getTime(2)-begTime)/pixelLength);
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);
						if (sampleDepth < depth)
						{
							spp.paintOverDepthText(currSampleMidpoint, succSampleMidpoint, sampleDepth, functionName);
						}
					}
				}
				break;
			}
			default:
			{
				// do the paint
				BasePaintLine detailPaint = new BasePaintLine(colorTable, ptl, spp, depth, height, pixelLength) {

					@Override
					public void finishPaint(int currSampleMidpoint,
							int succSampleMidpoint, int currDepth,
							String functionName) {
						
						spp.paintSample(currSampleMidpoint, succSampleMidpoint, height, functionName);			
						if (currDepth < depth)
						{
							spp.paintOverDepthText(currSampleMidpoint, Math.min(succSampleMidpoint, numPixelsH), currDepth, functionName);
						}
					}
				};
				detailPaint.paint();
			}
			break;
		}
	}

	/*************************************************************************
	 *	Returns the process that has been specified.
	 ************************************************************************/
	public ProcessTimeline getProcess(int process)
	{
		return traces[process];
	}

	public int getNumberOfDisplayedProcesses() {
		return this.traces.length;
	}
	 
	/**Returns the midpoint between x1 and x2*/
	public static double midpoint(double x1, double x2)
	{
		return (x1 + x2)/2.0;
	}
	
	/**Returns the index of the file to which the line-th line corresponds.*/
	public int lineToPaint(int line)
	{
		if(endProcess-begProcess > numPixelsV)
			return begProcess + (line*(endProcess-begProcess))/(numPixelsV);
		else
			return begProcess + line;
	}
	
	/*******************************************************************************
	 * Used by InitializeThreads to check in their startingTimes and
	 * endingTimes and compare them to the lowestStartingTime and
	 * highestEndingTime.
	 ******************************************************************************/
	public synchronized void checkIn(long firstTime, long lastTime)
	{
		if (firstTime < minBegTime)
			minBegTime = firstTime;
		if (lastTime > maxEndTime)
			maxEndTime = lastTime;
	}
	
	/***********************************************************************
	 * Gets the next available trace to be filled/painted
	 * @param changedBounds Whether or not the thread should get the data.
	 * @return The next trace.
	 **********************************************************************/
	public synchronized ProcessTimeline getNextTrace(boolean changedBounds)
	{
		if(lineNum < Math.min(numPixelsV, endProcess-begProcess))
		{
			lineNum++;
			if(changedBounds)
				return new ProcessTimeline(lineNum-1, scopeMap, traceFiles.get(lineToPaint(lineNum-1)), numPixelsH, endTime-begTime, minBegTime + begTime);
			else
				return traces[lineNum-1];
		}
		else
			return null;
	}
	
	/***********************************************************************
	 * Gets the next available trace to be filled/painted from the DepthTimeView
	 * @return The next trace.
	 **********************************************************************/
	public synchronized ProcessTimeline getNextDepthTrace()
	{
		if (lineNum < Math.min(numPixelsV, maxDepth))
		{
			if (lineNum==0)
			{
				lineNum++;
				return depthTrace;
			}
			ProcessTimeline toDonate = new ProcessTimeline(lineNum, scopeMap, traceFiles.get(dtProcess), numPixelsH, endTime-begTime, minBegTime+begTime);
			toDonate.times = depthTrace.times;
			toDonate.timeLine = depthTrace.timeLine;
			lineNum++;
			return toDonate;
		}
		else
			return null;
	}
	
	/**Returns the next available File during initialization - used by InitializeThreads.*/
	public synchronized File getNextFile()
	{
		if(fileNum < traceFiles.size())
		{
			fileNum++;
			return traceFiles.get(fileNum - 1);
		}
		else
			return null;
	}
	
	/**Adds a filled ProcessTimeline to traces - used by TimelineThreads.*/
	public synchronized void addNextTrace(ProcessTimeline nextPtl)
	{
		traces[nextPtl.line()] = nextPtl;
	}
	
	public synchronized void setDepthTrace(ProcessTimeline ptl)
	{
		depthTrace = ptl;
	}
	
	/**Adds a painted Image to compositeLines - used by TimelineThreads.*/
	public synchronized void addNextImage(Image line, int index)
	{
		compositeLines[index] = line;
	}


	public int getBegProcess() {
		return this.begProcess;
	}
	
	
	public int getEndProcess() {
		return this.endProcess;
	}
	
	@Override
	public void setPosition(Position position) {
		this.currentPosition = position;
	}
	
	public Position getPosition() {
		return this.currentPosition;
	}
	
	private void printDebug(String str) {
		
		if (this.debug)
			System.err.println(str);
	}
}