package edu.rice.cs.hpc.traceviewer.data.db;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;

/***********
 * Struct class to store attributes of a trace view like:
 * <ul>
 * <li> {@link Frame} : ROI and current position
 * <li> Number of horizontal pixels
 * <li> Number of vertical pixels for the main view
 * <li> Number of vertical pixels for the depth view
 * </ul>
 * <br/>
 * It contains methods to check the bounds and to covert from pixel to position 
 */
public class ImageTraceAttributes 
{
	final private Map<Integer, TimeUnit> mapIntegerToUnit;
	final private Map<TimeUnit, String>  mapUnitToString;
	
	public int numPixelsH, numPixelsV;
	public int numPixelsDepthV;

	private Frame frame;

	public ImageTraceAttributes()
	{
		frame = new Frame();

		mapIntegerToUnit = new HashMap<Integer, TimeUnit>(6);
		
		mapIntegerToUnit.put(0, TimeUnit.NANOSECONDS);
		mapIntegerToUnit.put(1, TimeUnit.MICROSECONDS);
		mapIntegerToUnit.put(2, TimeUnit.MILLISECONDS);
		mapIntegerToUnit.put(3, TimeUnit.SECONDS);
		mapIntegerToUnit.put(4, TimeUnit.MINUTES);
		mapIntegerToUnit.put(5, TimeUnit.HOURS);
		
		mapUnitToString = new HashMap<TimeUnit, String>(6);
		
		mapUnitToString.put(TimeUnit.NANOSECONDS, "ns");
		mapUnitToString.put(TimeUnit.MICROSECONDS, "us");
		mapUnitToString.put(TimeUnit.MILLISECONDS, "ms");
		mapUnitToString.put(TimeUnit.SECONDS, "s");
		mapUnitToString.put(TimeUnit.MINUTES, "min");
		mapUnitToString.put(TimeUnit.HOURS, "hr");
	}

	/****
	 * return the position in the list of a given time unit.
	 * This is an inverse version of {@code getTimeUnit}
	 * @param timeUnit
	 * @return
	 */
	public int getTimeUnitOrdinal(TimeUnit timeUnit) {

		for (Map.Entry<Integer, TimeUnit> entry : mapIntegerToUnit.entrySet()) {
			if (entry.getValue() == timeUnit) {
				return entry.getKey();
			}
		}
		return -1;
	}
	
	/****
	 * return the time unit for a given ordinal. 
	 * This is an inverse version of {@code getTimeUnitOrdinal}
	 * @param ordinal
	 * @return
	 */
	public TimeUnit getTimeUnit(int ordinal) {
		return mapIntegerToUnit.get(ordinal);
	}
	
	/***
	 * Returns the name of the give time unit
	 * 
	 * @param unit
	 * @return
	 */
	public String getTimeUnitName(TimeUnit unit) {
		return mapUnitToString.get(unit);
	}
	
	
	/****
	 * Compute the suggested time unit for a give space time data
	 * 
	 * @param data
	 * @return
	 */
	public TimeUnit getDisplayTimeUnit(SpaceTimeDataController data) {
				
		TimeUnit unitInDatabase = data.getTimeUnit();
		
		int unit = 0;
		
		// --------------------------------------------------------------------------
		// find the right unit time (s, ms, us, ns) 
		// To pick the time unit, the time interval should be comfortably within the 
		//  range of the unit. 
		// If the database time unit is ns, and the interval is:
		//   10,000,000,000 --> 10 sec
		//      100,000,000 --> 100ms
		//           20,000 --> 20 us
		//              100 --> 100ns
		// --------------------------------------------------------------------------
		
		do {
			TimeUnit timeUnit = mapIntegerToUnit.get(unit);
			
			long t1 = getTimeBegin();
			long t2 = getTimeEnd();
			long dt = timeUnit.convert(t2 - t1, unitInDatabase);

			if (dt < 5000) {
				// distance between ticks is at least 2 if possible
				// if it's 1 or 0.8, then we should degrade it to higher precision 
				// (higher unit time)
				if (dt <= 2 && unit > 0)
					unit--;
				
				break;
			}
			unit++;

		} while(unit < mapIntegerToUnit.size());
		
		if (unit >= mapIntegerToUnit.size())
			unit = mapIntegerToUnit.size() - 1;
		
		return mapIntegerToUnit.get(unit);
	}
	
	/*************************************************************************
	 * Asserts the process bounds to make sure they're within the actual
	 * bounds of the database, are integers, and adjusts the process zoom 
	 * button accordingly.
	 *************************************************************************/
	public void assertProcessBounds(int maxProcesses)
	{
		if (frame.begProcess < 0)
			frame.begProcess = 0;
		if (frame.endProcess > maxProcesses)
			frame.endProcess = maxProcesses;
	}
	
	/**************************************************************************
	 * Asserts the time bounds to make sure they're within the actual
	 * bounds of the database and adjusts the time zoom button accordingly.
	 *************************************************************************/
	public void assertTimeBounds(long maxTime)
	{
		if (frame.begTime < 0)
			frame.begTime = 0;
		if (frame.endTime > maxTime)
			frame.endTime = maxTime;
	}
	
	
	public void setFrame(Frame frame)
	{
		this.frame = frame;
	}
	
	public Frame getFrame()
	{
		return frame;
	}
	
	public void setProcess(int p1, int p2)
	{
		frame.begProcess = p1;
		frame.endProcess = p2;
		
		frame.fixPosition();
	}
	
	public int getProcessBegin()
	{
		return frame.begProcess;
	}
	
	public int getProcessEnd()
	{
		return frame.endProcess;
	}

	public int getProcessInterval()
	{
		return (frame.endProcess - frame.begProcess);
	}
	
	public void setTime(long t1, long t2)
	{
		frame.begTime = t1;
		frame.endTime = t2;
	}
	
	public long getTimeBegin()
	{
		return frame.begTime;
	}
	
	public long getTimeEnd()
	{
		return frame.endTime;
	}
	
	public long getTimeInterval()
	{
		long dt = frame.endTime - frame.begTime;
		// make sure we have positive time interval, even if users selects 0 time
		if (dt>0)
			return (frame.endTime - frame.begTime);
		else
			return 1;
	}
	
	public boolean sameTrace(ImageTraceAttributes other)
	{
		return ( frame.begTime==other.frame.begTime && frame.endTime==other.frame.endTime &&
				frame.begProcess==other.frame.begProcess && frame.endProcess==other.frame.endProcess &&
				 numPixelsH==other.numPixelsH && numPixelsV==other.numPixelsV);
	}
	
	public void setDepth(int depth)
	{
		frame.depth = depth;
	}
	
	public int getDepth()
	{
		return frame.depth;
	}
	
	public void setPosition(Position p)
	{
		frame.position = p;
	}
	
	public Position getPosition()
	{
		return frame.position;
	}
	
	public double getScalePixelsPerRank()
	{
		return (double)numPixelsV / getProcessInterval();
	}

	public int convertPixelToRank(int pixelY) 
	{
		int process = 0;
		
    	//need to do different things if there are more traces to paint than pixels
    	if(numPixelsV > getProcessInterval())
    	{
    		process = (int)(getProcessBegin()+pixelY/getScalePixelsPerRank());
    	}
    	else
    	{
    		process = (int)(getProcessBegin()+(pixelY*(getProcessInterval()))/numPixelsV);
    	}
    	return process;
	}
	
	public int convertRankToPixel(int process)
	{
		int pixel = 0;
		if (numPixelsV > getProcessInterval()) 
		{
			pixel = (int) ((process - getProcessBegin()) * getScalePixelsPerRank());
		}
		else {
			pixel = (process - getProcessBegin()) * numPixelsV / getProcessInterval();
		}
		return pixel;
	}
	
	public long convertPixelToTime(int pixelX)
	{
		double pixelsPerTime = numPixelsH / (double) getTimeInterval();
		long   time = (long) (getTimeBegin() + (long) pixelX / pixelsPerTime);
		
		return time;
	}
	
	/***
	 * Check if two attribute instances have the same depth attribute
	 * 
	 * @param other
	 * @return
	 */
	public boolean sameDepth(ImageTraceAttributes other)
	{
		return ( frame.begTime==other.frame.begTime && frame.endTime==other.frame.endTime &&
				 numPixelsH==other.numPixelsH && numPixelsDepthV==other.numPixelsDepthV);
	}
	
	/***
	 * Copy from another attribute
	 * @param other
	 */
	public void copy(ImageTraceAttributes other)
	{
		frame.begTime = other.frame.begTime;
		frame.endTime = other.frame.endTime;
		frame.begProcess = other.frame.begProcess;
		frame.endProcess = other.frame.endProcess;
		numPixelsH = other.numPixelsH;
		numPixelsV = other.numPixelsV;
		numPixelsDepthV = other.numPixelsDepthV;
	}
	
	/****
	 * return a new duplicate of this attributes
	 * 
	 * @return a new image attribute
	 */
	public ImageTraceAttributes duplicate() 
	{
		ImageTraceAttributes att = new ImageTraceAttributes();
		att.copy(this);
		return att;
	}
	
	public String toString()
	{
		return ("T [ " + frame.begTime + ","  + frame.endTime+ " ]" +
				"P [ " + frame.begProcess + "," + frame.endProcess + " ]" + 
				" PH: " + numPixelsH + " , PV: " + numPixelsV );
	}
}
