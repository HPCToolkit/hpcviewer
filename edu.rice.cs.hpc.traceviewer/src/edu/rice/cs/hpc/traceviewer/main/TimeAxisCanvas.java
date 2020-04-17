package edu.rice.cs.hpc.traceviewer.main;

import java.text.DecimalFormat;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;


/*******************************************************
 * 
 * Time axis canvas
 * 
 * The canvas draws adaptively a range of axis from time T0 to Tx
 *   according to the current display range.
 *   
 *   If the range changes, it will change the axis automatically.
 *
 *******************************************************/
public class TimeAxisCanvas extends AbstractAxisCanvas 
	implements PaintListener, IOperationHistoryListener
{	
	static final private int UNIT_HOUR     = 5;
	static final private int UNIT_MINUTE   = 4;
	static final private int UNIT_SECOND   = 3;
	static final private int UNIT_MILLISEC = 2;
	static final private int UNIT_MICROSEC = 1;
	static final private int UNIT_NANOSEC  = 0;
	
	static private final int TICK_X_PIXEL = 110;
	
	static final private int PADDING_Y   = 1;
	static final private int TICK_MARK_Y = 4;

	final private String[] listStringUnit;
	final private long[]   unitConversion; 
	
	final private DecimalFormat formatTime;

	
	/***
	 * Constructor of time axis canvas.
	 * 
	 * @param parent
	 * @param style
	 */
	public TimeAxisCanvas(Composite parent, int style) {
		super(parent, SWT.NO_BACKGROUND | style);
		
		formatTime = new DecimalFormat("###,###,###");
		
		listStringUnit = new String[6];
		
		listStringUnit[UNIT_HOUR]     = "hr";	// hour
		listStringUnit[UNIT_MINUTE]   = "mnt";	// minute
		listStringUnit[UNIT_SECOND]   = "s";	// second
		listStringUnit[UNIT_MILLISEC] = "ms";	// Millisecond
		listStringUnit[UNIT_MICROSEC] = "us";	// microsecond
		listStringUnit[UNIT_NANOSEC]  = "ns";	// nanosecond
		
		unitConversion = new long[6];
		
		unitConversion[UNIT_NANOSEC]  = 1; 				// from nanosecond  to nanosecond
		unitConversion[UNIT_MICROSEC] = 1000; 		 	// from us to nanosecond
		unitConversion[UNIT_MILLISEC] = 1000000; 		// from ms to nanosecond
		unitConversion[UNIT_SECOND]   = 1000000000; 	// from s  to nanosecond
		unitConversion[UNIT_MINUTE]   = 1000000000*60; 	// from minute to nanosecond
		unitConversion[UNIT_HOUR] 	  = 1000000000*60*60; 	// from hour to nanosecond
	}

	

	
	@Override
	public void paintControl(PaintEvent e) {

		if (getData() == null)
			return;
		
		final int position_y = Integer.min(0, e.height - PADDING_Y - TICK_MARK_Y);
		final Rectangle area = getClientArea();
		
		e.gc.drawLine(area.x, position_y, area.width, position_y);
		
		final SpaceTimeDataController data = (SpaceTimeDataController) getData();

        if (data == null)
        	return;
        
		final ImageTraceAttributes attribute = data.getAttributes();
		
		// --------------------------------------------------------------------------
		// finding some HINTs of number of ticks, and distance between ticks 
		// --------------------------------------------------------------------------
		
		int numAxisLabel = area.width / TICK_X_PIXEL;
		double numTicks  = (double)area.width / TICK_X_PIXEL;
		double fraction  = (double)attribute.getTimeInterval() / numTicks  * data.getUnitTimePerNanosecond();
		
		int unit = 0;
		
		// --------------------------------------------------------------------------
		// find the right unit time (s, ms, us, ns) 
		// we want to display ticks to something like:
		//  10s .... 20s ... 30s ... 40s
		// 
		// --------------------------------------------------------------------------
		
		do {			
			double t1 = attribute.getTimeBegin() * data.getUnitTimePerNanosecond() / unitConversion[unit];
			double t2 = t1 + fraction / unitConversion[unit];
			double dt = t2 - t1;

			if (t2-t1 < 1000.0) {
				// distance between ticks is at least 2 if possible
				// if it's 1 or 0.8, then we should degrade it to higher precision 
				// (higher unit time)
				if (dt < 2 && unit > 0)
					unit--;
				
				break;
			}
			unit++;

		} while(unit < unitConversion.length);
		
		if (unit >= unitConversion.length)
			unit = unitConversion.length - 1;

		long unit_time = unitConversion[unit];

		// --------------------------------------------------------------------------
		// find the nice rounded number
		// if dt < 10:  1, 2, 3, 4...
		// if dt < 100: 10, 20, 30, 40, ..
		// ...
		// --------------------------------------------------------------------------
		
		double t1 = attribute.getTimeBegin() * data.getUnitTimePerNanosecond() / unit_time;
		double t2 = t1 + fraction / unit_time;
		double dt = t2 - t1;
		
		// find rounded delta_time
		// if delta_time is 12 --> rounded to 10
		// 					3  --> rounded to 1
		// 					32 --> rounded to 10
		// 					312 --> rounded to 100
		
		int logdt 	 = (int) Math.log10(dt);
		long dtRound = (int) Math.pow(10, logdt) ;
		dtRound     *= unit_time;
		numAxisLabel = (int) (attribute.getTimeInterval() * data.getUnitTimePerNanosecond() / dtRound);
		
		double timeBegin    = attribute.getTimeBegin() * data.getUnitTimePerNanosecond();
		
		// round the time to the upper bound
		// if the time is 22, we round it to 30
		
		long remainder      = (long) timeBegin % dtRound;
		if (remainder > 0)
			timeBegin       = timeBegin + (dtRound - remainder);
		
		Point prevTextArea  = new Point(0, 0);
		int   prevPositionX = 0;

		// --------------------------------------------------------------------------
		// draw the ticks and the labels if there's enough space
		// --------------------------------------------------------------------------

		for(int i=0; i <= numAxisLabel; i++) {
			
			double time      = timeBegin + dtRound * i;
			
			String strTime   = formatTime.format((long)time / unit_time) + listStringUnit[unit];
			
			Point textArea   = e.gc.stringExtent(strTime);
			
			int axis_x_pos	 = (int) convertTimeToPixel(attribute, data.getUnitTimePerNanosecond(), (long)time);
			int position_x   = (int) (axis_x_pos);
			
			if (i>0) {
				// by default x position is in the middle of the tick
				position_x   = axis_x_pos - (textArea.x/2);
				
				// make sure x position is not beyond the view's width
				if (position_x + textArea.x > area.width) {
					position_x = axis_x_pos - textArea.x;
				}
			} 
			int axis_tick_mark_height = position_y+2;
			
			// draw the label only if we have space
			if (i==0 || prevPositionX+prevTextArea.x + 10 < position_x) {
				e.gc.drawText(strTime, position_x, position_y + 4);

				prevTextArea.x = textArea.x;
				prevPositionX  = position_x;
				
				axis_tick_mark_height+=2;
			}
			// always draw the ticks
			e.gc.drawLine(axis_x_pos, position_y, axis_x_pos, axis_tick_mark_height);
		}
	}
	
	/*****
	 * convert from time to pixel
	 * 
	 * @param attribute current attribute time configuration
	 * @param unitTimeNs conversion multipler from time to nanosecond 
	 * @param time the time to convert
	 * 
	 * @return pixel (x-axis)
	 */
	private int convertTimeToPixel(ImageTraceAttributes attribute, double unitTimeNs, long time)
	{
		// define pixel : (time - TimeBegin) x number_of_pixel_per_time 
		//				  (time - TimeBegin) x (numPixelsH/timeInterval)
		double dT = attribute.numPixelsH / (unitTimeNs*attribute.getTimeInterval());
		long dTime = (long) (time-(attribute.getTimeBegin()*unitTimeNs));
		int pixel = (int) (dTime * dT);
		
		return pixel;
	}

}
