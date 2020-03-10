package edu.rice.cs.hpc.data.trace;

public class TraceAttribute 
{
	public static final int DEFAULT_RECORD_SIZE = 24;
	public static final long PER_NANO_SECOND  = 1000000000;
	
	public String dbGlob;
	public long dbTimeMin;
	public long dbTimeMax;
	public int dbHeaderSize;	
	public long dbUnitTime;
	
	public TraceAttribute() {
		dbGlob = null;
		dbTimeMax = Integer.MIN_VALUE;
		dbTimeMin = Integer.MAX_VALUE;
		dbHeaderSize = 0;
		dbUnitTime   = PER_NANO_SECOND;
	}
}
