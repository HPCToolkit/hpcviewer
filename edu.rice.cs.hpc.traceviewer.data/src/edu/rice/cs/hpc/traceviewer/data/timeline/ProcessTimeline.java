package edu.rice.cs.hpc.traceviewer.data.timeline;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.Assert;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.traceviewer.data.db.ITraceDataCollector;
import edu.rice.cs.hpc.traceviewer.data.db.TraceDataByRank;
import edu.rice.cs.hpc.traceviewer.data.db.DataRecord;
import edu.rice.cs.hpc.traceviewer.data.graph.CallPath;
import edu.rice.cs.hpc.traceviewer.data.version2.AbstractBaseData;

/** A data structure that stores one line of timestamp-cpid data. */
public class ProcessTimeline {

	/** The mapping between the cpid's and the actual scopes. */
	private HashMap<Integer, CallPath> scopeMap;

	/** This ProcessTimeline's line number. */
	private int lineNum, processNumber;

	/** The initial time in view. */
	private long startingTime;

	/** The range of time in view. */
	private long timeRange;

	/** The amount of time that each pixel on the screen correlates to. */
	private double pixelLength;

	final ITraceDataCollector data;

	/*************************************************************************
	 * Reads in the call-stack trace data from the binary traceFile in the form:
	 * double time-stamp int Call-Path ID double time-stamp int Call-Path ID ...
	 ************************************************************************/

	/** Creates a new ProcessTimeline with the given parameters. 
	 * @param _numPixelH The number of Horizontal pixels
	 * @param _timeRange The difference between the start time and the end time
	 */
	public ProcessTimeline(int _lineNum, HashMap<Integer, CallPath> _scopeMap, IBaseData dataTrace, 
			int processNumber, int _numPixelH, long _timeRange, long _startingTime)
	{

		lineNum 			= _lineNum;
		scopeMap 			= _scopeMap;

		timeRange			= _timeRange;
		startingTime 		= _startingTime;
		this.processNumber  = processNumber;

		pixelLength = timeRange / (double) _numPixelH;
		
		//TODO: Beautify
		if (dataTrace instanceof AbstractBaseData)
			data = new TraceDataByRank((AbstractBaseData) dataTrace, processNumber, _numPixelH);
		else
			data = new TraceDataByRank(new DataRecord[0]);
	}

	//Remote version
	public ProcessTimeline(TraceDataByRank _data,
			HashMap<Integer, CallPath> _scopeMap, int _processNumber,
			int _numPixelH, long _timeRange, long _startingTime) {
		lineNum = _processNumber;
		scopeMap = _scopeMap;
		timeRange = _timeRange;
		startingTime = _startingTime;

		pixelLength = timeRange / (double) _numPixelH;
		if (_data == null)
			data = new TraceDataByRank(new DataRecord[0]);
		else
			data = _data;
		
		// laks 2016.06.23: hack for remote data: we don't have process number 
		// information from the server, so we just assign the same as the line number
		// for local data, the line number is different than the process number
		//  if the screen resolution is smaller than the number of ranks
		// at the moment, this fix works. not sure why.
		this.processNumber = _processNumber;
	}

	/**
	 * Fills the ProcessTimeline with data from the file. If this is being
	 * called, it must be on local, so the cast is fine
	 * @throws IOException 
	 */
	public void readInData() throws IOException {

		data.readInData(processNumber, startingTime, timeRange,
				pixelLength);
	}

	/** Gets the time that corresponds to the index sample in times. */
	public long getTime(int sample) {
		return data.getTime(sample);
	}

	/** Gets the cpid that corresponds to the index sample in timeLine. */
	private int getCpid(int sample) {
		return data.getCpid(sample);
	}

	public void shiftTimeBy(long lowestStartingTime) {
		data.shiftTimeBy(lowestStartingTime);
	}

	/** returns the call path corresponding to the sample and depth given */
	public CallPath getCallPath(int sample, int depth) {
		Assert.isTrue(sample>=0, "sample number is negative");
		int cpid = getCpid(sample);

		CallPath cp = scopeMap.get(cpid);
 		return cp;
	}
/**
 * Fills this one with the data from another
 * @param another
 */
	public void copyDataFrom(ProcessTimeline another) {
		data.duplicate(another.data);
	}

	/** Returns the number of elements in this ProcessTimeline. */
	public int size() {
		return data.size();
	}

	/** Returns this ProcessTimeline's line number. */
	public int line() {
		return lineNum;
	}
	
	/***
	 * return the current depth process number, which is the current
	 * selected process
	 * 
	 * @return current selected process
	 */
	public int getProcessNum() {
		return processNumber;
	}

	/**
	 * Finds the sample to which 'time' most closely corresponds in the
	 * ProcessTimeline.
	 * 
	 * @param time : the requested time
	 * @return the index of the sample if the time is within the range, -1  otherwise
	 * */
	public int findMidpointBefore(long time, boolean usingMidpoint)  throws Exception
	{
		return data.findClosestSample(time, usingMidpoint);
	}

	
	public boolean isEmpty()
	{
		return data.isEmpty();
	}
	// These are potentially useful for debugging, but otherwise serve no use.
//	@Override
//	public String toString() {
//		return hashCode() + "#" + data.getRank();
//	}
//
//	@Override
//	public int hashCode() {
//		double hash = 0.0;
//		for (Record r : data.getListOfData()) {
//			hash += r.cpId + Math.log(r.timestamp);
//		}
//		return (int) Math.round(hash);
//	}

}