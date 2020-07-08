package edu.rice.cs.hpc.traceviewer.main;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
	static private final int TICK_X_PIXEL = 110;
	static private final int MINIMUM_PIXEL_BETWEEN_TICKS = 10;
	static private final int TICK_BIG   = 4;
	static private final int TICK_SMALL = 2;
	
	final private DecimalFormat formatTime;

	final private Color bgColor;
	
	private Font fontX;
	
	/***
	 * Constructor of time axis canvas.
	 * 
	 * @param parent
	 * @param style
	 */
	public TimeAxisCanvas(Composite parent, int style) {
		super(parent, SWT.NO_BACKGROUND | style);
		
		bgColor = parent.getBackground();
		
		formatTime = new DecimalFormat("###,###,###");
	}

	
	@Override
	public void dispose() {
		if (fontX != null && !fontX.isDisposed()) {
			fontX.dispose();
		}
		super.dispose();
	}
	
	
	@Override
	public void paintControl(PaintEvent e) {

		if (getData() == null)
			return;
		
		final SpaceTimeDataController data = (SpaceTimeDataController) getData();

        if (data == null)
        	return;
        
        
        // try to adapt automatically the font height if the height of the canvas isn't sufficient
        // Theoretically, the height is always constant in the beginning of the first appearance. 
        // However, during the view creation, SWT returns the size of height and width are both zero.
        // So Apparently the best way is to compute the height during the paint control :-(
        
		FontData []fd = e.gc.getFont().getFontData();
		int height = fd[0].getHeight();
		
		if (e.height > 0 && height > e.height-7) {
			int heightAdapt = Math.min(e.height-7, height);
			if (heightAdapt > 1) {
				fd[0].setHeight(heightAdapt);
				if (fontX != null && !fontX.isDisposed()) {
					fontX.dispose();
				}
				fontX = new Font(e.display, fd);
				e.gc.setFont(fontX);
			}
		}

		final ImageTraceAttributes attribute = data.getAttributes();

		final Rectangle area = getClientArea();
		
		// --------------------------------------------------------------------------
		// finding some HINTs of number of ticks, and distance between ticks 	
		// --------------------------------------------------------------------------
		
		int numAxisLabel = area.width / TICK_X_PIXEL;
		double numTicks  = (double)area.width / TICK_X_PIXEL;
		double fraction  = (double)attribute.getTimeInterval() / numTicks;
		
		TimeUnit dbTimeUnit = data.getTimeUnit();
		
		// --------------------------------------------------------------------------
		// find the right unit time (s, ms, us, ns) 
		// we want to display ticks to something like:
		//  10s .... 20s ... 30s ... 40s
		// 
		// --------------------------------------------------------------------------
		
		TimeUnit displayTimeUnit = attribute.getDisplayTimeUnit(data);

		// --------------------------------------------------------------------------
		// find the nice rounded number
		// if dt < 10:  1, 2, 3, 4...
		// if dt < 100: 10, 20, 30, 40, ..
		// ...
		// --------------------------------------------------------------------------
		
		long t1 = attribute.getTimeBegin();
		long t2 = (long) (t1 + fraction);
		long dt = displayTimeUnit.convert(t2 - t1, dbTimeUnit);
		
		// there is nothing we can do if the difference between the ticks is less than 1 ns.
		if (dt<1) {
			int ordinal = attribute.getTimeUnitOrdinal(displayTimeUnit);
			if (ordinal>0 && (t1 != t2)) {
				ordinal--;
				displayTimeUnit = attribute.getTimeUnit(ordinal);
				
				// recompute dt with the new time unit
				dt = displayTimeUnit.convert(t2-t1, dbTimeUnit);
			} else {
				dt = 1;
			}
		}
		
		// find rounded delta_time
		// if delta_time is 12 --> rounded to 10
		// 					3  --> rounded to 1
		// 					32 --> rounded to 10
		// 					312 --> rounded to 100
		
		int logdt 	 = (int) Math.log10(dt);
		long dtRound = (int) Math.pow(10, logdt);
		int maxTicks = (area.width/MINIMUM_PIXEL_BETWEEN_TICKS); 
		do {
			numAxisLabel = (int) (displayTimeUnit.convert(attribute.getTimeInterval(), dbTimeUnit) / dtRound);
			if (numAxisLabel > maxTicks) {
				dtRound *= 10;
			}
		} while (numAxisLabel > maxTicks);
		
		double timeBegin    = displayTimeUnit.convert(attribute.getTimeBegin(), dbTimeUnit);
		
		// round the time to the upper bound
		// if the time is 22, we round it to 30
		
		long remainder      = (long) timeBegin % dtRound;
		if (remainder > 0)
			timeBegin       = timeBegin + (dtRound - remainder);
		
		// --------------------------------------------------------------------------
        // Manually fill the client area with the default background color
        // Some platforms don't paint the background properly 
		// --------------------------------------------------------------------------
        
		e.gc.setBackground(bgColor);
		e.gc.fillRectangle(getClientArea());
		
		// --------------------------------------------------------------------------
		// draw the x-axis
		// --------------------------------------------------------------------------
		
		// it is possible in some cases that the height is so small that we cannot
		// display axis
		
		final int position_y = 0;
		e.gc.drawLine(area.x, position_y, area.width, position_y);
		
		Point prevTextArea  = new Point(0, 0);
		int   prevPositionX = 0;

		// --------------------------------------------------------------------------
		// draw the ticks and the labels if there's enough space
		// --------------------------------------------------------------------------

		for(int i=0; i <= numAxisLabel; i++) {
			
			double time      = timeBegin + dtRound * i;
			
			String strTime   = formatTime.format((long)time) + attribute.getTimeUnitName(displayTimeUnit);
			
			Point textArea   = e.gc.stringExtent(strTime);
			
			int axis_x_pos	 = (int) convertTimeToPixel(attribute, dbTimeUnit, (long)time, displayTimeUnit);

			// by default x position is in the middle of the tick
			int position_x   = (int) axis_x_pos - (textArea.x/2);
			
			// make sure we don't trim the text in the beginning of the axis
			if (position_x<0) {
				position_x = 0;
			}
			// make sure x position is not beyond the view's width
			else if (position_x + textArea.x > area.width) {
				position_x = axis_x_pos - textArea.x;
			}
			int axis_tick_mark_height = position_y+TICK_SMALL;
			
			// draw the label only if we have space
			if (i==0 || prevPositionX+prevTextArea.x + 10 < position_x) {
				e.gc.drawText(strTime, position_x, position_y + TICK_BIG+1);

				prevTextArea.x = textArea.x;
				prevPositionX  = position_x;
				
				axis_tick_mark_height+=TICK_BIG;
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
	private int convertTimeToPixel(ImageTraceAttributes attribute, 
			TimeUnit dbUnit, long time, TimeUnit displayUnit)
	{
		// define pixel : (time - TimeBegin) x number_of_pixel_per_time 
		//				  (time - TimeBegin) x (numPixelsH/timeInterval)
		double dT = (double)attribute.numPixelsH / displayUnit.convert(attribute.getTimeInterval(), dbUnit);
		long dTime = time-displayUnit.convert(attribute.getTimeBegin(),dbUnit);
		int pixel = (int) (dT * dTime);
		
		return pixel;
	}

}
