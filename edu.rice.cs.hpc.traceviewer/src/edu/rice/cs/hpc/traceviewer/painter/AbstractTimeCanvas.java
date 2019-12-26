package edu.rice.cs.hpc.traceviewer.painter;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import edu.rice.cs.hpc.data.util.OSValidator;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.util.Constants;



/**********************************************************************************
 * 
 * abstract class for helper information (located on the bottom of the window)
 *
 **********************************************************************************/
public abstract class AbstractTimeCanvas
extends BufferedCanvas
implements ITraceCanvas, PaintListener
{
	/** Relates to the condition that the mouse is in.*/
	private ITraceCanvas.MouseState mouseState;
	
	/** The point at which the mouse was clicked.*/
	private Point mouseDown;
	
	/** The left/right point that you selected.*/
	private Rectangle selection;
	
	private BufferedCanvasToolTip tooltip;
	
	protected enum RegionType {Vertical, Rectangle};
	
	final private RegionType regionType;
	
	/****************
	 * Constructs a new instance of this class given its parent and a style value describing its behavior and appearance.
	 *
	 * @param composite
	 * @param style
	 ****************/
	public AbstractTimeCanvas(Composite composite, int style) {
		this(composite, style, RegionType.Vertical);
	}
	
	public AbstractTimeCanvas(Composite composite, int style, RegionType regionType) {
		super(composite);
		mouseState = ITraceCanvas.MouseState.ST_MOUSE_INIT;
		this.regionType = regionType;
	}
	
	public void init() 
	{
		if (mouseState == ITraceCanvas.MouseState.ST_MOUSE_INIT) 
		{
			addMouseListener(this);
			addMouseMoveListener(this);
			addPaintListener(this);
			
			// need to initialize mouse selection variables when we lost the focus
			//	otherwise, Eclipse will keep the variables and draw useless selected rectangles
			addFocusListener(new FocusAdapter() {
				/*
				 * (non-Javadoc)
				 * @see org.eclipse.swt.events.FocusAdapter#focusLost(org.eclipse.swt.events.FocusEvent)
				 */
				public void focusLost(FocusEvent e) {
					AbstractTimeCanvas.this.initMouseSelection();
				}
			});
			
			tooltip = new BufferedCanvasToolTip(this);
			tooltip.activate();
		}
		initMouseSelection();
		super.initBuffer();
	}
	
	/*****
	 * initialize variables 
	 */
	private void initMouseSelection()
	{
		selection = new Rectangle(0, 0, 0, 0);
		mouseState = ITraceCanvas.MouseState.ST_MOUSE_NONE;
	}
	
	@Override
	public void mouseMove(MouseEvent e) 
	{
		if (mouseState == ITraceCanvas.MouseState.ST_MOUSE_DOWN)
		{
			Point pos = new Point(e.x, e.y);
			adjustPosition(mouseDown, pos);
			
			redraw();
		}
	}

	@Override
	public void mouseDoubleClick(MouseEvent e) {}

	@Override
	public void mouseDown(MouseEvent e) 
	{
		// take into account ONLY when the button-1 is clicked and it's never been clicked before
		// the click is not right click (or modifier click on Mac)
		if (e.button == 1 && mouseState == ITraceCanvas.MouseState.ST_MOUSE_NONE 
				&& (e.stateMask & SWT.MODIFIER_MASK)==0 )
		{
			mouseState = ITraceCanvas.MouseState.ST_MOUSE_DOWN;
			mouseDown = new Point(e.x,e.y);
		}
	}

	@Override
	public void mouseUp(MouseEvent e) 
	{
		if (mouseState == ITraceCanvas.MouseState.ST_MOUSE_DOWN)
		{
			Point mouseUp = new Point(e.x,e.y);
			mouseState = ITraceCanvas.MouseState.ST_MOUSE_NONE;
			
			//difference in mouse movement < 3 constitutes a "single click"
			if(Math.abs(mouseUp.x-mouseDown.x)<3 && Math.abs(mouseUp.y-mouseDown.y)<3)
			{
				changePosition(mouseDown);
			}
			else
			{
				adjustPosition(mouseDown, mouseUp);
				changeRegion(selection);
			}
			redraw();
		}
	}

	@Override
	public void paintControl(PaintEvent event) 
	{
		super.paintControl(event);
		
 		//paints the selection currently being made
		if (mouseState==ITraceCanvas.MouseState.ST_MOUSE_DOWN)
		{
			// some Unix machines have no advanced graphic function
			// to render the alpha transformation.
			if (!OSValidator.isUnix()) 
			{
	        	event.gc.setBackground(Constants.COLOR_WHITE);
	    		event.gc.setAlpha(100);
	    		event.gc.fillRectangle( selection );
	    		event.gc.setAlpha(240);
	
			}    		
    		event.gc.setLineWidth(2);
    		event.gc.setForeground(Constants.COLOR_BLACK);
    		event.gc.drawRectangle(selection);
		}
	}
	
	
	@Override
	public void dispose () {
		if (tooltip != null)
			tooltip.deactivate();
		
		super.dispose();
	}
	
	private void adjustPosition(Point p1, Point p2) 
	{
		selection.x = Math.min(p1.x, p2.x);
		selection.width = Math.max(p1.x, p2.x) - selection.x;
		final Rectangle area   = getClientArea();

		switch(regionType) {
		case Rectangle:
			selection.y = Math.min(p1.y, p2.y);
			selection.height = Math.max(p1.y, p2.y) - selection.y;
			break;
		case Vertical:
			default:
			selection.y = 0;
			selection.height = area.height;
			break;
		}
	}
	
	
	/******************************************************************
	 * 
	 * Customized tooltip for generic time-based buffered canvas
	 *
	 ******************************************************************/

	
	static private class BufferedCanvasToolTip extends DefaultToolTip
	{
		final private AbstractTimeCanvas canvas;
		
		public BufferedCanvasToolTip(AbstractTimeCanvas canvas) {
			super(canvas);
			
			this.canvas = canvas;
		}
		
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.DefaultToolTip#getText(org.eclipse.swt.widgets.Event)
		 */
		protected String getText(Event event) {
			final Image image = canvas.getBuffer();
			if (image == null)
				return null;
			
			final ImageData imgData = image.getImageData();
			if (imgData == null)
				return null;
			
			if (event.x >= imgData.width || event.y >= imgData.height || event.x < 0 || event.y < 0)
				// corner case: when resizing is faster than rendering
				return null;
			
			int pixel = imgData.getPixel(event.x, event.y);
			final RGB rgb = imgData.palette.getRGB(pixel);
			
			ColorTable colorTable = canvas.getColorTable();
			if (colorTable == null)
				return null;
			
			String proc = canvas.getColorTable().getProcedureNameByColorHash( rgb.hashCode() );
			
			String addText = canvas.tooltipText(pixel, rgb);
			if (addText != null)
				return addText + proc;
			
			return proc;
		}
		
		@Override
		/*
		 * (non-Javadoc)
		 * @see org.eclipse.jface.window.ToolTip#getLocation(org.eclipse.swt.graphics.Point, org.eclipse.swt.widgets.Event)
		 */
		public Point getLocation(Point tipSize, Event event) {
			return canvas.toDisplay(event.x + 5, event.y - 15);
		}
		
	}

	
	/*************************
	 * function called when there's a change of mouse click position
	 * 
	 * @param point
	 *************************/
	protected abstract void changePosition(Point point);
	
	
	/***************************
	 * function called when there's a change of selected region
	 * 
	 * @param left
	 * @param right
	 ***************************/
	protected abstract void changeRegion(Rectangle region);
	
	
	protected abstract String tooltipText(int pixel, RGB rgb);
	
	protected abstract ColorTable getColorTable();
}
