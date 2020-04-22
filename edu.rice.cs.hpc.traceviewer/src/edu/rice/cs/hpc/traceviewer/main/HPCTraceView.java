package edu.rice.cs.hpc.traceviewer.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.ISourceProviderListener;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.ISourceProviderService;

import edu.rice.cs.hpc.common.util.SleakManager;
import edu.rice.cs.hpc.traceviewer.actions.OptionMidpoint;
import edu.rice.cs.hpc.traceviewer.actions.OptionRecordsDisplay;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.Frame;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;
import edu.rice.cs.hpc.traceviewer.services.DataService;


/*************************************************************************
 * A view for displaying the main view of traceviewer.
 * 
 *************************************************************************/
public class HPCTraceView extends ViewPart 
implements ITraceViewAction
{
	
	/**The ID needed to create this view (used in plugin.xml).*/
	public static final String ID = "hpctraceview.view";
	
	public static final int Y_AXIS_WIDTH  = 13;
	public static final int X_AXIS_HEIGHT = 20;
	
	/** Stores/Creates all of the data that is used in the view.*/
	private SpaceTimeDataController stData = null;
	
	private TimeAxisCanvas axisArea = null;
	private ThreadAxisCanvas processCanvas = null;
	
	/** Paints and displays the detail view.*/
	SpaceTimeDetailCanvas detailCanvas;
	
	/*************************************************************************
	 *	Creates the view.
	 ************************************************************************/
	public void createPartControl(Composite master)
	{		
		/**************************************************************************
         * Process and Time dimension labels
         *************************************************************************/
		final Composite headerArea = new Composite(master, SWT.NONE);
		
		Canvas headerCanvas = new Canvas(headerArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).
						hint(Y_AXIS_WIDTH, 20).applyTo(headerCanvas);
		
		final Composite labelGroup = new Composite(headerArea, SWT.NONE);

		GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(headerArea);
		GridDataFactory.fillDefaults().grab(true, false).
						applyTo(headerArea);

		
		/*************************************************************************
		 * Detail View Canvas
		 ************************************************************************/
		
		ISourceProviderService service = (ISourceProviderService)getSite().getWorkbenchWindow().
				getService(ISourceProviderService.class);
		ProcessTimelineService ptlService = (ProcessTimelineService) service.
				getSourceProvider(ProcessTimelineService.PROCESS_TIMELINE_PROVIDER);

		Composite plotArea = new Composite(master, SWT.NONE);
		
		processCanvas = new ThreadAxisCanvas(ptlService, plotArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).
						hint(Y_AXIS_WIDTH, 500).applyTo(processCanvas);

		
		final IToolBarManager tbMgr     = getViewSite().getActionBars().getToolBarManager();
		final TraceCoolBar traceCoolBar = new TraceCoolBar(tbMgr, this, SWT.NONE);

		detailCanvas = new SpaceTimeDetailCanvas(getSite().getWorkbenchWindow(), plotArea); 

		detailCanvas.setLabels(labelGroup);
		detailCanvas.setButtons(new Action[]{traceCoolBar.home, traceCoolBar.open, traceCoolBar.save, null,
				null, traceCoolBar.tZoomIn, traceCoolBar.tZoomOut, traceCoolBar.pZoomIn, traceCoolBar.pZoomOut,
				traceCoolBar.goEast, traceCoolBar.goNorth, traceCoolBar.goSouth, traceCoolBar.goWest});
		
		GridDataFactory.fillDefaults().grab(true, true).
						hint(500, 500).applyTo(detailCanvas);
		
		detailCanvas.setVisible(false);
		
		
		/*************************************************************************
		 * Horizontal axis label 
		 *************************************************************************/
		
		Canvas footerCanvas = new Canvas(plotArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).
						hint(Y_AXIS_WIDTH, X_AXIS_HEIGHT).applyTo(footerCanvas);

		axisArea = new TimeAxisCanvas(plotArea, SWT.NO_BACKGROUND);
		GridDataFactory.fillDefaults().grab(true, false).
						hint(500, X_AXIS_HEIGHT).applyTo(axisArea);
				
		GridLayoutFactory.fillDefaults().numColumns(2).generateLayout(plotArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(plotArea);
		
		
		/*************************************************************************
		 * Master layout 
		 *************************************************************************/
		
		GridLayoutFactory.fillDefaults().numColumns(1).generateLayout(master);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(master);

		//--------------------------------------
		// memory checking
		//--------------------------------------
		final Display display = getSite().getShell().getDisplay();
		SleakManager.init(display);		
		addTraceViewListener();
	}

	
	/*************************************************************************
	 * update new data
	 *************************************************************************/
	public void updateView(SpaceTimeDataController _stData)
	{
		this.stData = _stData;
		this.detailCanvas.updateView(_stData);
		
		detailCanvas.setVisible(true);
		axisArea.setData(stData);
		processCanvas.setData(stData);
	}
	
	
	/**Required in order to extend ViewPart.*/
	public void setFocus()
	{
		this.detailCanvas.setFocus();
	}
	


	/*************************************************************************
	 * method to add listener
	 *************************************************************************/
	private void addTraceViewListener() 
	{
		final ISourceProviderService service = (ISourceProviderService)getSite().getService(ISourceProviderService.class);
		final ISourceProvider yourProvider = service.getSourceProvider(DataService.DATA_UPDATE);
		yourProvider.addSourceProviderListener( new ISourceProviderListener(){

			public void sourceChanged(int sourcePriority, Map sourceValuesByName) {	}
			public void sourceChanged(int sourcePriority, String sourceName,
					Object sourceValue) {
				// eclipse bug: even if we set a very specific source provider, eclipse still
				//	gather event from other source. we then require to put a guard to avoid this.
				if (sourceName.equals(DataService.DATA_UPDATE)) {
					boolean needRefresh = false;
					if (sourceValue instanceof Boolean) {
						Boolean refresh = (Boolean) sourceValue;
						needRefresh = refresh.booleanValue();
					}
					detailCanvas.refresh(needRefresh);
				}
			}
		});
		
		// ---------------------------------------------------------------
		// register listener to capture event in menus or commands
		// ---------------------------------------------------------------
		final ICommandService commandService = (ICommandService) this.getSite().getService(ICommandService.class);
		commandService.addExecutionListener( new IExecutionListener(){

			public void notHandled(String commandId, NotHandledException exception) {}
			public void postExecuteFailure(String commandId, ExecutionException exception) {}
			public void preExecute(String commandId, ExecutionEvent event) {}

			/*
			 * (non-Javadoc)
			 * @see org.eclipse.core.commands.IExecutionListener#postExecuteSuccess(java.lang.String, java.lang.Object)
			 */
			public void postExecuteSuccess(String commandId, Object returnValue) 
			{
				if (stData == null)
					return;
				
				// add listener when user change the state of "Show trace record" menu
				if (commandId.equals(OptionRecordsDisplay.commandId))
				{
					// force the canvas to redraw the content
					detailCanvas.refresh(false);
				} else if (commandId.equals(OptionMidpoint.commandId)) 
				{
					// changing painting policy means changing the content
					// we should force all views to refresh
					detailCanvas.refresh(true);
				}
			}
		});
	}
	


	//----------------------------------------------------------------------------------------------------
	// Implementation of ITraceAction
	//----------------------------------------------------------------------------------------------------
	
	public void home() {
		detailCanvas.home();
	}

	public void timeZoomIn() {
		detailCanvas.timeZoomIn();
	}

	public void timeZoomOut() {
		detailCanvas.timeZoomOut();
	}

	public void processZoomIn() {
		detailCanvas.processZoomIn();
	}

	public void processZoomOut() {
		detailCanvas.processZoomOut();
	}
	
	public void save() {
		FileDialog saveDialog;
		saveDialog = new FileDialog(this.getViewSite().getShell(), SWT.SAVE);
		saveDialog.setText("Save View Configuration");
		String fileName = "";
		boolean validSaveFileFound = false;
		while(!validSaveFileFound)
		{
			Frame toSave = detailCanvas.save();
			saveDialog.setFileName((int)toSave.begTime+"-"+(int)toSave.endTime+", "
				+(int)toSave.begProcess+"-"+(int)toSave.endProcess+".bin");
			fileName = saveDialog.open();
			
			if (fileName == null)
				return;
			else
			{
				if (!new File(fileName).exists())
					validSaveFileFound = true;
				else
				{
					//open message box confirming whether or not they want to overwrite saved file
					//if they select yes, validSaveFileFound = true;
					//if they selct no, validSaveFileFound = false;

					MessageBox msg = new MessageBox(this.getViewSite().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					msg.setText("File Exists");
					msg.setMessage("This file path already exists.\nDo you want to overwrite this save file?");
					int selectionChoice = msg.open();

					validSaveFileFound = (selectionChoice==SWT.YES);
				}
			}
		}
		
		try
		{
			ObjectOutputStream out = null;
			try
			{
				out = new ObjectOutputStream(new FileOutputStream(fileName));
				out.writeObject(detailCanvas.save());
			}
			finally
			{
				out.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	public void open() {
		FileDialog openDialog;
		openDialog = new FileDialog(this.getViewSite().getShell(), SWT.OPEN);
		openDialog.setText("Open View Configuration");
		String fileName = "";
		boolean validFrameFound = false;
		while(!validFrameFound)
		{
			fileName = openDialog.open();
			
			if (fileName == null) return;
			File binFile = new File(fileName);
			
			if (binFile.exists())
			{
				ObjectInputStream in = null;
				try
				{
					in = new ObjectInputStream(new FileInputStream(fileName));
					Frame current = (Frame)in.readObject();
					detailCanvas.open(current);
					validFrameFound = true;
				}
				catch (IOException e)
				{
					validFrameFound = false;
					MessageDialog.openError(getViewSite().getShell(), "Error reading the file",
							"Fail to read the file: " + fileName );
				}
				catch (ClassNotFoundException e)
				{
					validFrameFound = false;
					MessageDialog.openError(getViewSite().getShell(), "Error reading the file", 
							"File format is not recognized. Either the file is corrupted or it's an old format");
				}
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						MessageDialog.openWarning(getViewSite().getShell(), "Error closing the file", 
								"Unable to close the file: " + fileName);
					}
				}
			}
		}
	}

	public void goNorth() {
		detailCanvas.goNorth();
	}

	public void goSouth() {
		detailCanvas.goSouth();
	}

	public void goEast() {
		detailCanvas.goEast();
	}

	public void goWest() {
		detailCanvas.goWest();
	}	
}