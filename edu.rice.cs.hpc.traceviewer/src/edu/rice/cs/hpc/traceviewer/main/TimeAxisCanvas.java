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
	
	static final private int PADDING_Y   = 2;
	static final private int TICK_MARK_Y = 3;

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
		
		int numAxisLabel = area.width / TICK_X_PIXEL;
		double numTicks  = (double)area.width / TICK_X_PIXEL;
		double fraction  = (double)attribute.getTimeInterval() / numTicks  * data.getUnitTimePerNanosecond();
		
		int unit = 0;
		
		// find the right unit time that the different between ticks is equal or bigger than 10
		// we want to display ticks to something like:
		//  10 .... 20 ... 30 ... 40
		// 
		
		do {			
			double t1 = attribute.getTimeBegin() * data.getUnitTimePerNanosecond() / unitConversion[unit];
			double t2 = t1 + fraction / unitConversion[unit];
			double dt = t2 - t1;

			if (t2-t1 < 100.0) {
				if (dt < 2)
					unit--;
				
				break;
			}
			unit++;

		} while(unit < unitConversion.length);
		
		if (unit >= unitConversion.length)
			unit = unitConversion.length - 1;
		
		// find the nice rounded number
		// for second: 10, 20, 30, ...
		// for ms:     100, 200, 300, ...
		// for us:     1000, 2000, 3000, ...
		// etc.

		double timeBegin = attribute.getTimeBegin() * data.getUnitTimePerNanosecond() / unitConversion[unit];
		long unit_time   = unitConversion[unit];
		
		for(int i=0; i <= numAxisLabel; i++) {
			
			double time      = timeBegin + fraction * i / unit_time;
			
/*			long remainder   = (long) (time % 10);
			
			if (remainder > 0)
				time = time + (10-remainder);*/

			String strTime   = formatTime.format((long)time) + listStringUnit[unit];
			
			Point textArea   = e.gc.stringExtent(strTime);
			
			int axis_x_pos	 = (int) (TICK_X_PIXEL * i);
			int position_x   = (int) (axis_x_pos);
			
/*			if (remainder > 0)
				position_x   = (int) (axis_x_pos  +( TICK_X_PIXEL / (10-remainder)));*/
			
			if (i>0) {
				// by default x position is in the middle of the tick
				position_x   = axis_x_pos - (textArea.x/2);
				
				// make sure x position is not beyond the view's width
				if (position_x + textArea.x > area.width) {
					position_x = axis_x_pos - textArea.x;
				}
			} 
			e.gc.drawLine(axis_x_pos, position_y, axis_x_pos, position_y+2);

			// give more space for nano and micro-seconds
			if (unit >= UNIT_MILLISEC || (i%2==0))
				e.gc.drawText(strTime, position_x, position_y + 4);
		}
	}
}
