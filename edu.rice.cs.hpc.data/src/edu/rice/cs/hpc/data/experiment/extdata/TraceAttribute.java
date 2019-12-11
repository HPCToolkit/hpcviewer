package edu.rice.cs.hpc.data.experiment.extdata;

public class TraceAttribute 
{
	public static enum UnitTime {Undefined, MicroSecond, NanoSecond};
	public static final int DEFAULT_RECORD_SIZE = 24;
	
	public String dbGlob;
	public long dbTimeMin;
	public long dbTimeMax;
	public int dbHeaderSize;	
	public UnitTime dbUnitTime;
	
	public TraceAttribute() {
		dbGlob = null;
		dbTimeMax = Integer.MIN_VALUE;
		dbTimeMin = Integer.MAX_VALUE;
		dbHeaderSize = 0;
		dbUnitTime   = UnitTime.Undefined;
	}
}
