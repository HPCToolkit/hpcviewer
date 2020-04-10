package edu.rice.cs.hpc.traceviewer.main;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;


/*********************
 * 
 * Canvas to draw vertical axis of the main view
 *
 *********************/
public class ThreadAxisCanvas extends AbstractAxisCanvas 
{
	static final private int COLUMN_WIDTH = 15;
	
	private final Color COLOR_PROC[];
	private final Color COLOR_THREAD[];
	
	private final ProcessTimelineService timeLine;

	public ThreadAxisCanvas(ProcessTimelineService timeLine, Composite parent, int style) {
		super(parent, style);
		
		COLOR_PROC = new Color[2];
		COLOR_PROC[0] = getDisplay().getSystemColor(SWT.COLOR_BLUE);
		COLOR_PROC[1] = getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE);
		
		COLOR_THREAD = new Color[2];
		COLOR_THREAD[0] = getDisplay().getSystemColor(SWT.COLOR_MAGENTA);
		COLOR_THREAD[1] = getDisplay().getSystemColor(SWT.COLOR_DARK_MAGENTA);
		
		this.timeLine = timeLine;
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
			
			e.gc.fillRectangle(COLUMN_WIDTH, threadPosition, xEnd, nextPosition);
			
			currentColor   = 1 - currentColor;
		}
	}
}
