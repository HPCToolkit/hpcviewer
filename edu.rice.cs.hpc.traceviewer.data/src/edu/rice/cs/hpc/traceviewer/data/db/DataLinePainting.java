package edu.rice.cs.hpc.traceviewer.data.db;

import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;

/***
 * 
 * struct class for painting a line
 *
 */
public class DataLinePainting 
{
	public IWorkbenchWindow window;
	
	public ProcessTimeline ptl;
	public int depth;
	public int height;
	public double pixelLength;
	public ColorTable colorTable;
	public long begTime;
	public boolean usingMidpoint;
}
