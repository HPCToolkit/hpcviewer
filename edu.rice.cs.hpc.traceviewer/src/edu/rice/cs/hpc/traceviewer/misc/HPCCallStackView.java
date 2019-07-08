package edu.rice.cs.hpc.traceviewer.misc;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.ISizeProvider;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.services.DataService;

/**A view for displaying the call path viewer and minimap.*/
//all the GUI setup for the call path and minimap are here//
public class HPCCallStackView extends ViewPart implements ISizeProvider
{
	
	public static final String ID = "hpccallstackview.view";
	
	CallStackViewer csViewer;
	
	/** Paints and displays the miniMap.*/
	SpaceTimeMiniCanvas miniCanvas;
	
	Spinner depthEditor;
	

	public void createPartControl(Composite master) 
	{
		setupEverything(master);
		setListener();
	}
	
	private void setupEverything(Composite master)
	{
		/*************************************************************************
		 * Master Composite
		 ************************************************************************/
		
		master.setLayout(new GridLayout());
		master.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true));
		
		/*************************************************************************
		 * Depth View Spinner (the thing with the text box and little arrow buttons)
		 ************************************************************************/
		depthEditor = new Spinner(master, SWT.EMBEDDED);
		depthEditor.setMinimum(0);
		depthEditor.setPageIncrement(1);
		
		depthEditor.setLayout(new GridLayout());
		GridData depthData = new GridData(SWT.CENTER, SWT.TOP, true, false);
		depthData.widthHint = 100;
		depthEditor.setLayoutData(depthData);
		depthEditor.setVisible(false);
		
		depthEditor.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				String string = depthEditor.getText();
				int value;
				if (string.length()<1)
					// be careful: on linux/GTK, any change in the spinner will consists of two steps:
					//  1) empty the string
					//  2) set with the specified value
					// therefore, we consider any empty string to be illegal
					return;
				else
					value = Integer.valueOf(string);
				
				int maximum = depthEditor.getMaximum();
				int minimum = 0;

				if (value > maximum) {
					value = maximum;
				}
				if (value < minimum) {
					value = minimum;
				}
				csViewer.setDepth(value);
			}
		});
		
		/*************************************************************************
		 * CallStackViewer
		 ************************************************************************/
		csViewer = new CallStackViewer(master, this);
		
		/*************************************************************************
		 * MiniMap
		 ************************************************************************/
		
		Label l = new Label(master, SWT.SINGLE);
		l.setText("Mini Map");
		miniCanvas = new SpaceTimeMiniCanvas(master);
		miniCanvas.setLayout(new GridLayout());
		GridData miniCanvasData = new GridData(SWT.CENTER, SWT.BOTTOM, true, false);
		miniCanvasData.heightHint = 100;
		miniCanvasData.widthHint = 140;
		miniCanvas.setLayoutData(miniCanvasData);
		
		miniCanvas.setVisible(false);
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
					if (sourceValue instanceof SpaceTimeDataController) {
						// new color mapping
						csViewer.updateView();
					} else if (sourceValue instanceof Boolean) {
						// operations when every ones need to refresh their data
						//	this event can happen when a filter event occurs
						miniCanvas.updateView();
						
						// for the callstack viewer, we'll rely on BufferRefreshOperation to refresh
						// the content to ensure that the data from the main view is ready to be fetched
						// csViewer.updateView(); // not needed
					}
				}
			}
		});
	}
	
	
	public void updateView(SpaceTimeDataController _stData) 
	{
		depthEditor.setMaximum(_stData.getMaxDepth());
		depthEditor.setSelection(0);
		depthEditor.setVisible(true);
		depthEditor.setToolTipText("Change the current depth.\nMax depth is " + _stData.getMaxDepth());
		//this.csViewer.updateView();
		
		// instead of updating the content of the view, we just make the table
		// visible, and let other event to trigger the update content.
		// at this point, a data may not be ready to be processed
		csViewer.getTable().setVisible(true);
		
		this.miniCanvas.updateView(_stData);
		
		miniCanvas.setVisible(true);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() 
	{
		// by default, make the table to be the center of the focus
		this.csViewer.getTable().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ISizeProvider#computePreferredSize(boolean, int, int, int)
	 */
	public int computePreferredSize(boolean width, int availableParallel, int availablePerpendicular, int preferredSize) 
	{
		return preferredSize;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ISizeProvider#getSizeFlags(boolean)
	 */
	public int getSizeFlags(boolean width) 
	{
		return width ? SWT.MAX : 0;
	}
}
