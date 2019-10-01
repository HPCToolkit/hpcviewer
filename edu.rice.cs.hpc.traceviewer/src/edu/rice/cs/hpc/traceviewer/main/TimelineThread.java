package edu.rice.cs.hpc.traceviewer.main;


import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.DataLinePainting;
import edu.rice.cs.hpc.traceviewer.data.db.DataPreparation;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.TimelineDataSet;
import edu.rice.cs.hpc.traceviewer.timeline.BaseTimelineThread;

import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;

public class TimelineThread 
	extends BaseTimelineThread
{
	final private int totalLines;
	final private ProcessTimelineService traceService;

	/**Stores whether or not the bounds have been changed*/
	private boolean changedBounds;
	
	/***********************************************************************************************************
	 * Creates a TimelineThread with SpaceTimeData _stData; the rest of the parameters are things for drawing
	 * @param changedBounds - whether or not the thread needs to go get the data for its ProcessTimelines.
	 ***********************************************************************************************************/
	public TimelineThread(SpaceTimeDataController stData, ImageTraceAttributes attributes,
			ProcessTimelineService traceService,
			boolean _changedBounds, double _scaleY, Queue<TimelineDataSet> queue, 
			AtomicInteger currentLine, int totalLines, IProgressMonitor monitor)
	{
		super(stData, attributes, _scaleY, queue, currentLine, stData.isEnableMidpoint(), monitor);
		changedBounds = _changedBounds;		
		this.traceService = traceService;
		this.totalLines	  = totalLines;
	}
	
	
	@Override
	protected ProcessTimeline getNextTrace(AtomicInteger currentLine) {
		return stData.getNextTrace(currentLine, totalLines, attributes, changedBounds, monitor);
	}

	
	@Override
	protected boolean init(ProcessTimeline trace) throws IOException {
		//nextTrace.data is not empty if the data is from the server
		if(changedBounds)
		{
			if (trace.isEmpty()) {
				
				trace.readInData();
				if (!traceService.setProcessTimeline(trace.line(), trace)) {
					// something wrong happens, perhaps data races ?
					monitor.setCanceled(true);
					monitor.done();
					return false;
				}
			}
			trace.shiftTimeBy(stData.getMinBegTime());
		}
		boolean res = (trace.size()>=2);
		return res;
	}

	@Override
	protected void finalize() {
	}

	@Override
	protected DataPreparation getData(DataLinePainting data) {

		return new DetailDataPreparation(data);
	}

}