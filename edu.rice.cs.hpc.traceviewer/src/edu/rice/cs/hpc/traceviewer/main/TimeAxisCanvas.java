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
	
	static private final int TICK_X_PIXEL = 120;
	
	static final private int PADDING_Y   = 2;
	static final private int TICK_MARK_Y = 3;

	final private String[]	 listStringUnit;
	
	final private DecimalFormat formatTime;


	
	/***
	 * Constructor of time axis canvas.
	 * 
	 * @param parent
	 * @param style
	 */
	public TimeAxisCanvas(Composite parent, int style) {
		super(parent, SWT.NO_BACKGROUND | style);
		
		formatTime = new DecimalFormat("###,###");
		
		listStringUnit = new String[4];
		listStringUnit[0] = "s";
		listStringUnit[1] = "ms";
		listStringUnit[2] = "us";
		listStringUnit[3] = "ns";
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
        
		final double unit_time = data.getUnitTimePerSecond();

		final ImageTraceAttributes attribute = data.getAttributes();
		final long timeLength = attribute.getTimeInterval();
		
		int numAxisLabel = area.width / TICK_X_PIXEL;
		double numTicks  = (double)area.width / TICK_X_PIXEL;
		double fraction  = (double)timeLength / numTicks;
		
		int unit = 0;
		
		// find the right unit time that the different between ticks is equal or bigger than 10
		// we want to display ticks to something like:
		//  10 .... 20 ... 30 ... 40
		// 
		// if the t0 is 12345670, the t1 should be at least 12345680:
		//  12345670 ... 12345680 ... 12345690 ... 12345700 ...
		
		do {			
			double t1 =  (attribute.getTimeBegin() * Math.pow(10, unit)+ fraction) / unit_time ;
			double t2 =  t1 + (fraction * 2 / unit_time);

			if (t2-t1 >= 10.0 || unit >= listStringUnit.length-1)
				break;
			
			unit++;
			fraction = fraction * Math.pow(10, 3);

		} while(unit < listStringUnit.length);
		
		double multiplier = Math.pow(10, 3*unit);
		
		for(int i=0; i <= numAxisLabel; i++) {
			double time 	 = attribute.getTimeBegin() * multiplier + fraction * i;
			double timeInSec = time/unit_time;
			String strTime   = formatTime.format((long)timeInSec) + listStringUnit[unit];
			
			Point textArea   = e.gc.stringExtent(strTime);
			int axis_x_pos	 = TICK_X_PIXEL * i;

			int position_x   = axis_x_pos;
			if (i>0) {
				// by default x position is in the middle of the tick
				position_x   = axis_x_pos - (textArea.x/2);
				
				// make sure x position is not beyond the view's width
				if (position_x + textArea.x > area.width) {
					position_x = axis_x_pos - textArea.x;
				}
			} 
			e.gc.drawLine(axis_x_pos, position_y, axis_x_pos, position_y+2);
			e.gc.drawText(strTime, position_x, position_y + 4);
		}
	}
}
