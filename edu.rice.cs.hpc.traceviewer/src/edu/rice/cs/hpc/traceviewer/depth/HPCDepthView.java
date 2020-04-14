package edu.rice.cs.hpc.traceviewer.depth;

import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.main.HPCTraceView;
import edu.rice.cs.hpc.traceviewer.services.DataService;
import edu.rice.cs.hpc.traceviewer.ui.AbstractTimeView;

/*****************************************************
 * 
 * Depth view
 *
 *****************************************************/
public class HPCDepthView extends AbstractTimeView
{
	public static final String ID = "hpcdepthview.view";

	private static final int VIEW_HEIGHT_HINT = 40;
	
	/** Paints and displays the detail view. */
	DepthTimeCanvas depthCanvas;
		
	public void createPartControl(Composite master)
	{		
		setupEverything(master);
		setListener();
		super.addListener();
	}
	
	private void setupEverything(Composite master)
	{
		final Composite plotArea = new Composite(master, SWT.NONE);
		
		/*************************************************************************
		 * Padding Canvas
		 *************************************************************************/
		
		final Canvas axisCanvas = new Canvas(plotArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).
						hint(HPCTraceView.Y_AXIS_WIDTH, VIEW_HEIGHT_HINT).applyTo(axisCanvas);
		
		/*************************************************************************
		 * Depth View Canvas
		 *************************************************************************/
		
		depthCanvas = new DepthTimeCanvas(plotArea);
		GridDataFactory.fillDefaults().grab(true, true).
						hint(500, VIEW_HEIGHT_HINT).applyTo(depthCanvas);

		depthCanvas.setVisible(false);		

		/*************************************************************************
		 * Master Composite
		 *************************************************************************/
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(plotArea);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(plotArea);
	}
	
	private void setListener() {
		ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		ISourceProvider serviceProvider = service.getSourceProvider(DataService.DATA_UPDATE);
		serviceProvider.addSourceProviderListener( new ISourceProviderListener(){

			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {	}
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				// eclipse bug: even if we set a very specific source provider, eclipse still
				//	gather event from other source. we then require to put a guard to avoid this.
				if (sourceName.equals(DataService.DATA_UPDATE)) {
					depthCanvas.refresh();
				}
			}
		});		
	}

	public void updateView(SpaceTimeDataController _stData)
	{
		this.depthCanvas.updateView(_stData);
		depthCanvas.setVisible(true);
	}

	public void setFocus()
	{
		this.depthCanvas.setFocus();
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.traceviewer.ui.IActiveNotification#active(boolean)
	 */
	public void active(boolean isActive) 
	{
		depthCanvas.activate(isActive);
	}
}
