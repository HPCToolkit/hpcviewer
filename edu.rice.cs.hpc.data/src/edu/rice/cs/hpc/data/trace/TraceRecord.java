package edu.rice.cs.hpc.data.trace;

public class TraceRecord 
{
	public long timestamp;
	public int cpId;

	public TraceRecord(long timestamp, int cpid) {
		this.timestamp = timestamp;
		this.cpId	   = cpid;
	}
 }
