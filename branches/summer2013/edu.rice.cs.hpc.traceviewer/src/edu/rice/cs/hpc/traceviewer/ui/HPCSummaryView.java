package edu.rice.cs.hpc.traceviewer.ui;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.painter.SummaryTimeCanvas;
import edu.rice.cs.hpc.traceviewer.services.DataService;
import edu.rice.cs.hpc.traceviewer.spaceTimeData.SpaceTimeData;

public class HPCSummaryView extends ViewPart
{

	public static final String ID = "hpcsummaryview.view";
	
	/**The canvas that actually displays this view*/
	SummaryTimeCanvas summaryCanvas;
	
	
	public void createPartControl(Composite _master)
	{
		setupEverything(_master);
		setListener();
	}
	
	private void setupEverything(Composite master)
	{
		/*************************************************************************
		 * Master Composite
		 *************************************************************************/
		
		master.setLayout(new GridLayout());
		master.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/*************************************************************************
		 * Summary View Canvas
		 *************************************************************************/
		
		summaryCanvas = new SummaryTimeCanvas(master);
		summaryCanvas.setLayout(new GridLayout());
		summaryCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summaryCanvas.setVisible(false);		
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
	}

	public void updateView(SpaceTimeData stData)
	{
		summaryCanvas.setVisible(true);
	}
	
	public void setFocus() 
	{
		this.summaryCanvas.setFocus();
	}
}
