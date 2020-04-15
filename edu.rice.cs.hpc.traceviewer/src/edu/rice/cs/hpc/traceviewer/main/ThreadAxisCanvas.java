package edu.rice.cs.hpc.traceviewer.main;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpc.traceviewer.painter.ITraceCanvas;
import edu.rice.cs.hpc.traceviewer.painter.ITraceCanvas.MouseState;


/*********************
 * 
 * Canvas to draw vertical axis of the main view
 *
 *********************/
public class ThreadAxisCanvas extends AbstractAxisCanvas 
{
	static final private int COLUMN_WIDTH = 5;
	
	private final Color COLOR_PROC[];
	private final Color COLOR_THREAD[];

	private final Color ColorLightBlue;
	
	private final ProcessTimelineService timeLine;
	
	/** Relates to the condition that the mouse is in.*/
	private ITraceCanvas.MouseState mouseState;
	
	private AxisToolTip tooltip = null;

	public ThreadAxisCanvas(ProcessTimelineService timeLine, Composite parent, int style) {
		super(parent, style);
		
		ColorLightBlue = new Color(getDisplay(), 173, 216, 250);
		
		COLOR_PROC = new Color[2];
		COLOR_PROC[0] = ColorLightBlue;
		COLOR_PROC[1] = getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
		
		COLOR_THREAD = new Color[2];
		COLOR_THREAD[0] = getDisplay().getSystemColor(SWT.COLOR_MAGENTA);
		COLOR_THREAD[1] = getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA);
		
		this.timeLine = timeLine;
		
		mouseState = MouseState.ST_MOUSE_INIT;
	}

	public void init(SpaceTimeDataController data) {
		if (mouseState == MouseState.ST_MOUSE_INIT) {
			
			tooltip = new AxisToolTip(this);
			
			mouseState = MouseState.ST_MOUSE_NONE;
		}
		tooltip.setData(data);
	}
	
	public void setData(Object data) {
		super.setData(data);
		
		init( (SpaceTimeDataController)data);
	}
	
	@Override
	public void paintControl(PaintEvent e) {

		if (getData() == null)
			return;
				
		final SpaceTimeDataController data   = (SpaceTimeDataController) getData();
        final IBaseData traceData 		     = data.getBaseData();

        if (traceData == null)
        	return;

		final ImageTraceAttributes attribute = data.getAttributes();
		
        final String processes[] = traceData.getListOfRanks();

		// -----------------------------------------------------
		// collect the position and the length of each process
		// -----------------------------------------------------
		List<Integer> listProcPosition   = new ArrayList<Integer>();
		List<Integer> listThreadPosition = new ArrayList<Integer>();
		
		int oldRank   = 0;
		int oldThread = 0;
		
		for (int i=0; i<timeLine.getNumProcessTimeline(); i++) {
			ProcessTimeline procTimeline = timeLine.getProcessTimeline(i);
			if (procTimeline == null)
				continue;
			
			final int procNumber  = procTimeline.getProcessNum();
			final String procName = processes[procNumber]; 
			final int position    = attribute.convertToPixel(procNumber);
	
			int rank = 0;
			int thread = 0;
			
			int dotIndex = procName.indexOf('.');
			if (dotIndex >= 0) {
				// hybrid application
				String strRank = procName.substring(0, dotIndex);
				String strThread = procName.substring(dotIndex+1);
				
				rank   = Integer.valueOf(strRank);
				thread = Integer.valueOf(strThread);
			} else {
				// either pure MPI or pure OpenMP threads
				rank = Integer.valueOf(procName);
			}

			if (i == 0) {
				oldRank   = rank;
				oldThread = thread;
				
				listProcPosition.add(position);
				listThreadPosition.add(position);
			}
			
			if (oldRank != rank) {
				listProcPosition.add(position);
				oldRank = rank;
			}
			if (oldThread != thread) {
				listThreadPosition.add(position);
				oldThread = thread;
			}
		}
		listProcPosition.  add(getClientArea().height);
		listThreadPosition.add(getClientArea().height);
		
		// -----------------------------------------------------
		// draw MPI column
		// -----------------------------------------------------
		int currentColor = 0;
		
		for (int i=0; i<listProcPosition.size()-1; i++) {
			e.gc.setBackground(COLOR_PROC[currentColor]);
			
			Integer procPosition = listProcPosition.get(i);
			Integer nextPosition = listProcPosition.get(i+1);
			
			e.gc.fillRectangle(0, procPosition, COLUMN_WIDTH, nextPosition);
			
			currentColor   = 1 - currentColor;
		}
		
		// -----------------------------------------------------
		// draw thread column
		// -----------------------------------------------------
		int xEnd = 2 * COLUMN_WIDTH;
		
		for (int i=0; i<listThreadPosition.size()-1; i++) {
			e.gc.setBackground(COLOR_THREAD[currentColor]);
			
			Integer threadPosition = listThreadPosition.get(i);
			Integer nextPosition   = listThreadPosition.get(i+1);
			
			e.gc.fillRectangle(COLUMN_WIDTH+1, threadPosition, xEnd, nextPosition);
			
			currentColor   = 1 - currentColor;
		}
	}
	
	
	@Override
	public void dispose() {
		if (tooltip != null) {
			tooltip.deactivate();
			tooltip = null;
		}
		if (ColorLightBlue != null && !ColorLightBlue.isDisposed()) {
			ColorLightBlue.dispose();
		}
		super.dispose();
	}
	
	
	/********************************************************
	 * 
	 * Tooltip class to show the rank and/or the thread of the 
	 *  current position.
	 *  
	 *  Caller needs to set data every time there is a new data
	 *
	 ********************************************************/
	static private class AxisToolTip extends DefaultToolTip
	{
		private SpaceTimeDataController data;

		public AxisToolTip(Control control) {
			super(control);
		}
		
		void setData(SpaceTimeDataController data) {
			this.data = data;
		}
	
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.DefaultToolTip#getText(org.eclipse.swt.widgets.Event)
		 */
		protected String getText(Event event) {
			
	        final IBaseData traceData = data.getBaseData();

	        if (traceData == null)
	        	return null;

			final ImageTraceAttributes attribute = data.getAttributes();
			int process = attribute.convertToPosition(event.y);
			
			String procNames[] = traceData.getListOfRanks();
			
			if (process >= 0 && process < procNames.length)
				return procNames[process];
			
			return null;
		}
		
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.ToolTip#getLocation(org.eclipse.swt.graphics.Point, org.eclipse.swt.widgets.Event)
		 */
		public Point getLocation(Point tipSize, Event event) {
			Object obj = getToolTipArea(event);
			Control control = (Control) obj;
			
			return control.toDisplay(event.x+5, event.y-15);
		}
	}
}
