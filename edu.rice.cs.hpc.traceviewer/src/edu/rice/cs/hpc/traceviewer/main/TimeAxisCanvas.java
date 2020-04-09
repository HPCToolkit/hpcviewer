package edu.rice.cs.hpc.traceviewer.main;

import java.text.DecimalFormat;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;


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
public class TimeAxisCanvas extends Canvas 
	implements PaintListener, IOperationHistoryListener
{	
	static private final int TICK_X_PIXEL = 100;

	static final private int MAX_HEIGHT  = 20;
	static final private int PADDING_Y   = 2;
	static final private int TICK_MARK_Y = 3;
	
	final private DecimalFormat formatTime;

	/***
	 * Constructor of time axis canvas.
	 * 
	 * @param parent
	 * @param style
	 */
	public TimeAxisCanvas(Composite parent, int style) {
		super(parent, SWT.NO_BACKGROUND | style);
		
		formatTime = new DecimalFormat("#.###");

		addPaintListener(this);
	}

	@Override
	public void setData(Object data) {
		
		if (getData() == null) {
			// just initialize once
			TraceOperation.getOperationHistory().addOperationHistoryListener(this);
		}

		super.setData(data);
		
		syncRedraw();
	}
	
	/***
	 * Just like normal redraw, but this method uses UI thread
	 * and wait until the thread is available
	 */
	public void syncRedraw() {
		
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				if (getData() == null)
					return;
				
				redraw();
			}
		});
	}
	
	@Override
	public void paintControl(PaintEvent e) {

		if (getData() == null)
			return;
		
		final int position_y = Integer.min(0, e.height - PADDING_Y - TICK_MARK_Y);
		final Rectangle area = getClientArea();
		
		e.gc.drawLine(area.x, position_y, area.width, position_y);
		for(int i=0; i<area.width; i+=100) {
			
		}
		
		final SpaceTimeDataController data   = (SpaceTimeDataController) getData();
        final IBaseData traceData 		     = data.getBaseData();

        if (traceData == null)
        	return;
        
		final double unit_time   = data.getUnitTimePerSecond();

		final ImageTraceAttributes attribute = data.getAttributes();
		final double timeLength = attribute.getTimeEnd() - attribute.getTimeBegin();
		
		int numAxisLabel = area.width / TICK_X_PIXEL;
		double numTicks  = (double)area.width / TICK_X_PIXEL;
		double fraction  = timeLength / numTicks;
		
		for(int i=0; i <= numAxisLabel; i++) {
			double time 	 = attribute.getTimeBegin() + fraction * i;
			double timeInSec = time/unit_time;
			String strTime   = formatTime.format(timeInSec) + "s";
			
			Point textArea   = e.gc.stringExtent(strTime);
			int axis_x_pos	 = TICK_X_PIXEL * i;

			int position_x   = axis_x_pos;
			if (i>0) {
				position_x   = axis_x_pos - (textArea.x/2);
				
				if (position_x + textArea.x > area.width) {
					position_x = axis_x_pos - textArea.x;
				}
			} 
			e.gc.drawLine(axis_x_pos, position_y, axis_x_pos, position_y+2);
			e.gc.drawText(strTime, position_x, position_y + 4);
		}
	}


	@Override
	public void historyNotification(OperationHistoryEvent event) {

		if (event.getEventType() == OperationHistoryEvent.DONE) {
			final IUndoableOperation operation = event.getOperation();

			if (operation.hasContext(BufferRefreshOperation.context)) {
				syncRedraw();
			}
		}
	}
}
