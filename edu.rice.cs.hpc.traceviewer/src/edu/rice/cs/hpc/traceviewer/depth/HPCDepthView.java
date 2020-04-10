package edu.rice.cs.hpc.traceviewer.depth;

import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
		
		/*************************************************************************
		 * Padding Canvas
		 *************************************************************************/
		
		final Canvas axisCanvas = new Canvas(master, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).hint(HPCTraceView.AXIS_WIDTH, 40).applyTo(axisCanvas);
		
		/*************************************************************************
		 * Depth View Canvas
		 *************************************************************************/
		
		depthCanvas = new DepthTimeCanvas(master);
		depthCanvas.setLayout(new GridLayout());
		depthCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		depthCanvas.setVisible(false);		

		/*************************************************************************
		 * Master Composite
		 *************************************************************************/
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(master);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(master);
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
