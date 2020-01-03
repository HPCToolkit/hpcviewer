package edu.rice.cs.hpc.traceviewer.summary;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.services.DataService;
import edu.rice.cs.hpc.traceviewer.services.SummaryDataService;
import edu.rice.cs.hpc.traceviewer.ui.AbstractDynamicView;

/*************************************************************************
 * 
 * View part of the summary window 
 *
 *************************************************************************/
public class HPCSummaryView extends AbstractDynamicView
{

	public static final String ID = "hpcsummaryview.view";
	
	/**The canvas that actually displays this view*/
	SummaryTimeCanvas summaryCanvas;
	
	public void createPartControl(Composite master)
	{
		/*************************************************************************
		 * Master Composite
		 */
		
		master.setLayout(new GridLayout());
		master.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*************************************************************************
		 * Summary View Canvas
		 */
		
		summaryCanvas = new SummaryTimeCanvas(master);
		summaryCanvas.setLayout(new GridLayout());
		summaryCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summaryCanvas.setVisible(false);
		
		setListener();
		super.addListener();
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
					summaryCanvas.redraw();
				}
			}
		});

		ISourceProvider requestProvider = service.getSourceProvider(SummaryDataService.DATA_REQUEST);
		requestProvider.addSourceProviderListener( new ISourceProviderListener() {
			
			@Override
			public void sourceChanged(int sourcePriority, String sourceName, Object sourceValue) {
				if (sourceName.equals(SummaryDataService.DATA_REQUEST)) {

					// force summary view to be "active" so that it can repaint the canvas
					// and send the new summary data to other views
					active(true);
					summaryCanvas.broadcast();
				}				
			}
			
			@Override
			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {}
		});
	}

	public void updateView(SpaceTimeDataController dataTraces)
	{
		summaryCanvas.updateData(dataTraces);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() 
	{
		summaryCanvas.setFocus();
	}

	@Override
	public void active(boolean isActive) 
	{
	}

}
