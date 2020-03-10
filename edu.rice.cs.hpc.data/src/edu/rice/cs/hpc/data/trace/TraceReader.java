package edu.rice.cs.hpc.data.trace;

import java.io.IOException;

import edu.rice.cs.hpc.data.experiment.extdata.IFileDB;
import edu.rice.cs.hpc.data.util.Constants;

public class TraceReader 
{
	final static public int RECORD_SIZE = Constants.SIZEOF_LONG + Constants.SIZEOF_INT;
	
	final private IFileDB data;
	
	public TraceReader(IFileDB data) {
		this.data = data;
	}
	
	public TraceRecord getData(long location) throws IOException
	{
		final long time = data.getLong(location);
		final int cpId = data.getInt(location + Constants.SIZEOF_LONG);
		
		return new TraceRecord(time, cpId);
	}
	
	public TraceRecord getData(int rank, long relativeIndex) throws IOException 
	{
		long location = getAbsoluteLocation(rank, relativeIndex);
		return getData(location);
	}
	
	public long getNumberOfRecords(int rank)
	{
		long start = data.getMinLoc(rank);
		long end   = data.getMaxLoc(rank);
		
		return getNumberOfRecords(start, end);
	}
	
	public long getNumberOfRecords(long start, long end)
	{
		return (end-start) / (getRecordSize());
	}

	public int getRecordSize() {
		return Constants.SIZEOF_INT + Constants.SIZEOF_LONG;
	}
	
	public long getAbsoluteLocation(int rank, long relativePosition)
	{
		return data.getMinLoc(rank) + (relativePosition * getRecordSize());
	}
	
	public long getRelativeLocation(int rank, long absolutePosition)
	{
		return (absolutePosition-data.getMinLoc(rank)) / getRecordSize();
	}
	

}
