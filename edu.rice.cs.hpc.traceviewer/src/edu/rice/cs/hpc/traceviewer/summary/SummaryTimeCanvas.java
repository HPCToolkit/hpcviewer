package edu.rice.cs.hpc.traceviewer.summary;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;
import edu.rice.cs.hpc.traceviewer.operation.ZoomOperation;
import edu.rice.cs.hpc.traceviewer.painter.AbstractTimeCanvas;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.Frame;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.db.Position;
import edu.rice.cs.hpc.traceviewer.data.util.Constants;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;

/******************************************************************
 * 
 * Canvas class for summary view
 *
 ******************************************************************/
public class SummaryTimeCanvas extends AbstractTimeCanvas 
implements IOperationHistoryListener
{	
	private SpaceTimeDataController dataTraces = null;
	private TreeMap<Integer, Integer> mapStatistics;
	private int totPixels;
	private ImageData detailData;
	private boolean needToRedraw = false;
	private ToolTip tooltip;
	
	/**********************************
	 * Construct a summary canvas without background nor scrollbar
	 * 
	 * @param composite
	 **********************************/
	public SummaryTimeCanvas(Composite composite)
    {
		super(composite, SWT.NO_BACKGROUND);
		
		OperationHistoryFactory.getOperationHistory().addOperationHistoryListener(this);
		
		// ------------------------------------------------------------------------------------------
		// setup tooltip information about the percentage of the color appointed by the mouse
		// ------------------------------------------------------------------------------------------
		tooltip = new SummaryTooltip(this) ;
		tooltip.deactivate();
	}
	
	@Override
	public void paintControl(PaintEvent event)
	{
		super.paintControl(event);
		if (needToRedraw) {
			refreshWithCondition();
			needToRedraw = false;
		}
	}
	
	/****
	 * called to be notify when the canvas is visible and needs to be activated
	 * 
	 * @param isActivated
	 */
	public void activate(boolean isActivated)
	{
		needToRedraw = isActivated;
	}

	private void refreshWithCondition()
	{
		if (getBuffer() == null){
			// ------------------------------------------------------------------------
			// ------------------------------------------------------------------------
			rebuffer(detailData);
			return;
		}
		
		// ------------------------------------------------------------------------
		// we need to avoid repainting if the size of the image buffer is not the same
		// as the image of the canvas. This case happens when the view is resize while
		// it's in hidden state, and then it turns visible. 
		// this will cause misalignment in the view
		// ------------------------------------------------------------------------
		final Rectangle r1 = getBuffer().getBounds();
		final Rectangle r2 = getClientArea();
		
		if (!(r1.height == r2.height && r1.width == r2.width))
		{
			rebuffer(detailData);
			return;
		}
	}
	
	
	
	
	/*****
	 * rebuffers the data in the summary time canvas and then asks receiver to paint it again
	 *****/
	private void rebuffer(ImageData detailData)
	{
		// store the original image data for further usage such as when the painting is needed.
		this.detailData = detailData;
		
		if (detailData == null || !isVisible())
			return;

		// ------------------------------------------------------------------------------------------
		// let use GC instead of ImageData since GC allows us to draw lines and rectangles
		// ------------------------------------------------------------------------------------------
		initBuffer();
		final int viewWidth = getBounds().width;
		final int viewHeight = getBounds().height;

		if (viewWidth == 0 || viewHeight == 0)
			return;

		final Image imageBuffer = new Image(getDisplay(), viewWidth, viewHeight);
		setBuffer(imageBuffer);
		
		GC buffer = new GC(imageBuffer);
		buffer.setBackground(Constants.COLOR_WHITE);
		buffer.fillRectangle(0, 0, viewWidth, viewHeight);
		
		float yScale = (float)viewHeight / (float)detailData.height;
		float xScale = ((float)viewWidth / (float)detailData.width);
		int xOffset = 0;

		mapStatistics = new TreeMap<Integer, Integer>();
		
		//---------------------------------------------------------------------------
		// needs to be optimized:
		// for every pixel along the width, check the pixel, group them based on color,
		//   count the amount of each group, and draw the pixel
		//---------------------------------------------------------------------------
		for (int x = 0; x < detailData.width; ++x)
		{
			//---------------------------------------------------------------------------
			// use tree map to sort the key of color map
			// without sort, it can be confusing
			//---------------------------------------------------------------------------
			TreeMap<Integer, Integer> sortedColorMap = new TreeMap<Integer, Integer>();

			for (int y = 0; y < detailData.height; ++y)
			{
				int pixelValue = detailData.getPixel(x,y);
				
				Integer count  = sortedColorMap.get(pixelValue);
				if (count != null)
					sortedColorMap.put( pixelValue , count+1 );
				else
					sortedColorMap.put( pixelValue , 1);
			}
			Set<Integer> set = sortedColorMap.keySet();
			int yOffset = viewHeight;
			
			int h = 0;
			//---------------------------------------------------------------------------
			// draw the line of a specific color with a specific length from bottom to the top
			// note: the coordinates 0,0 starts from the top-left corner !
			//---------------------------------------------------------------------------
			for (Iterator<Integer> it = set.iterator(); it.hasNext(); ) 
			{
				final Integer pixel = it.next();
				final RGB rgb = detailData.palette.getRGB(pixel);

				final Color c = new Color(getDisplay(), rgb);
				final Integer numCounts = sortedColorMap.get(pixel);
				final int height = (int) Math.ceil(numCounts * yScale);

				buffer.setBackground(c);
				
				// if this is the last color, we should draw from the current position to the end
				// this may not be the best solution, but the round-up in height variable may give
				// empty spaces if the number of colors are not a height's divisor.
				
				if (it.hasNext())
					buffer.fillRectangle(xOffset, yOffset-height, (int) Math.max(1, xScale), height);
				else {
					buffer.fillRectangle(xOffset, 0, (int) Math.max(1, xScale), viewHeight-h);
				}
				yOffset -= height;
				c.dispose();
				
				// accumulate the statistics of this pixel
				Integer val = mapStatistics.get(pixel);
				Integer acc = (val==null?  numCounts : val + numCounts);
				mapStatistics.put(pixel, acc);
				
				h += height;
			}
			xOffset = Math.round(xOffset + xScale);
		}
		totPixels = detailData.width * detailData.height;

		buffer.dispose();
		
		tooltip.activate();

		redraw();
	}
	
	/****
	 * main method to decide whether we want to create a new buffer or just to
	 * redraw the canvas
	 * 
	 * @param _detailData : new data
	 */
	private void refresh(ImageData detailData)
	{
		super.init();		
		rebuffer(detailData);
	}
	
	/********
	 * set the new database
	 * @param data
	 ********/
	public void updateData(SpaceTimeDataController data)
	{
		dataTraces = data;
		needToRedraw = true; // new data
		setVisible(true);
	}

	
	/*****
	 * get the number of pixel per time unit
	 * @return
	 */
	private double getScalePixelsPerTime()
	{
		final int viewWidth = getClientArea().width;

		return (double)viewWidth / (double)getNumTimeDisplayed();
	}
	
	/******
	 * get the time interval displayed on the canvas
	 * @return
	 */
	private long getNumTimeDisplayed()
	{
		return (dataTraces.getAttributes().getTimeInterval());
	}


	/******************************************************************
	 * 
	 * Customized tooltip for summary canvas
	 *
	 ******************************************************************/
	private class SummaryTooltip extends DefaultToolTip
	{
		public SummaryTooltip(Control control) {
			super(control);
		}

		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.DefaultToolTip#getText(org.eclipse.swt.widgets.Event)
		 */
		protected String getText(Event event) {
			if (mapStatistics != null ) 
			{
				Image img = SummaryTimeCanvas.this.getBuffer();
				if (img == null) 
					return null;
				
				ImageData imgData = img.getImageData();
				if (imgData == null) 
					return null;
				
				if (event.x >= imgData.width || event.y >= imgData.height || event.x < 0 || event.y < 0)
					// corner case: when resizing is faster than rendering
					return null;
				
				int pixel = imgData.getPixel(event.x, event.y);
				
				// ------------------------------------------------
				// get the number of counts of this pixel
				// ------------------------------------------------
				Integer stat = mapStatistics.get(pixel);
				
				if (stat != null) {
					// ------------------------------------------------
					// compute the percentage
					// ------------------------------------------------
					float percent = (float)100.0 * ((float)stat / (float) totPixels);

					if (percent > 0) {
						final String percent_str = String.format("%.2f %%", percent);
						return percent_str;
					}
				}
			}
			return null;
		}
		
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.ToolTip#getLocation(org.eclipse.swt.graphics.Point, org.eclipse.swt.widgets.Event)
		 */
		public Point getLocation(Point tipSize, Event event) {
			return SummaryTimeCanvas.this.toDisplay(event.x + 5, event.y - 15);
		}
	}
	
	//---------------------------------------------------------------------------------------
	// Override methods
	//---------------------------------------------------------------------------------------


	@Override
	public void historyNotification(final OperationHistoryEvent event) {
		// we are not interested with other operation
		if (event.getOperation().hasContext(BufferRefreshOperation.context)) {
			if (event.getEventType() == OperationHistoryEvent.DONE) {
				Debugger.printDebug(1, "STC attributes: ");
				if (isDisposed()) return;
					
				getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						BufferRefreshOperation operation = (BufferRefreshOperation) event.getOperation();
						refresh(operation.getImageData());
					}
				});
			}
		}
	}

	@Override
	protected void changePosition(Point point) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void changeRegion(Rectangle region) 
	{
		final ImageTraceAttributes attributes = dataTraces.getAttributes();
		
		long timeBegin   = attributes.getTimeBegin();
		int left = region.x;
		int right = region.width + region.x;
		
		long topLeftTime = timeBegin + (long)(left / getScalePixelsPerTime());
		long bottomRightTime = timeBegin + (long)(right / getScalePixelsPerTime());
		
		final Position position = attributes.getPosition();
		
		final Frame frame = new Frame(topLeftTime, bottomRightTime,
				attributes.getProcessBegin(), attributes.getProcessEnd(),
				attributes.getDepth(), position.time, position.process );
		try {
			TraceOperation.getOperationHistory().execute(
					new ZoomOperation("Time zoom in", frame), 
					null, null);
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}
}