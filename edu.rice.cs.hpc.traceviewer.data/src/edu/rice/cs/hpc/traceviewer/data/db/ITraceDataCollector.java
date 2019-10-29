package edu.rice.cs.hpc.traceviewer.data.db;

import java.io.IOException;

public interface ITraceDataCollector 
{
	public boolean isEmpty();
	public int findClosestSample(long time, boolean usingMidpoint)  throws Exception;
	public void readInData(int rank, long timeStart, long timeRange, double pixelLength) throws IOException;
	public long getTime(int sample);
	public int getCpid(int sample);
	public int size();
	public void shiftTimeBy(long lowestStartingTime);
	public void duplicate(ITraceDataCollector traceData);
}
