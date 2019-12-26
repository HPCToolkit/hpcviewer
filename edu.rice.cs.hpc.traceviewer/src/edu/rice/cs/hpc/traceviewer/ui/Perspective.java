package edu.rice.cs.hpc.traceviewer.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import edu.rice.cs.hpc.traceviewer.depth.HPCDepthView;
import edu.rice.cs.hpc.traceviewer.main.HPCTraceView;
import edu.rice.cs.hpc.traceviewer.misc.HPCCallStackView;
import edu.rice.cs.hpc.traceviewer.statistic.HPCStatView;
import edu.rice.cs.hpc.traceviewer.summary.HPCSummaryView;

public class Perspective implements IPerspectiveFactory
{
	private final static float SPACE_TIME_VIEW_WIDTH_FRACTION = 0.85f;
	private final static float SPACE_TIME_VIEW_HEIGHT_FRACTION = 0.80f;
	private final static float CALLSTACK_VIEW_WIDTH_FRACTION = 0.15f;

	public void createInitialLayout(IPageLayout layout)
	{
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(false);
		IFolderLayout topLeft = 
			layout.createFolder("topLeft", IPageLayout.LEFT, SPACE_TIME_VIEW_WIDTH_FRACTION, editorArea);
		topLeft.addView(HPCTraceView.ID);
		IFolderLayout bottomLeft = 
			layout.createFolder("bottomLeft", IPageLayout.BOTTOM, SPACE_TIME_VIEW_HEIGHT_FRACTION, "topLeft");
		bottomLeft.addView(HPCDepthView.ID);
		bottomLeft.addView(HPCSummaryView.ID);
		
		IFolderLayout right = 
			layout.createFolder("right", IPageLayout.BOTTOM,  CALLSTACK_VIEW_WIDTH_FRACTION, editorArea);
		right.addView(HPCCallStackView.ID);
		right.addView(HPCStatView.ID);
		
		layout.getViewLayout(HPCTraceView.ID).setCloseable(false);
		layout.getViewLayout(HPCDepthView.ID).setCloseable(false);
		layout.getViewLayout(HPCCallStackView.ID).setCloseable(false);
		layout.getViewLayout(HPCSummaryView.ID).setCloseable(false);
		layout.getViewLayout(HPCStatView.ID).setCloseable(false);
	}
}