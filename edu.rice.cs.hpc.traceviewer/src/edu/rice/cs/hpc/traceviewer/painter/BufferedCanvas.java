package edu.rice.cs.hpc.traceviewer.painter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;


/*************************************************************************
 * 
 * Standard abstract canvas that contains a buffered image and 
 *  context menu to save the buffered image to a file
 *
 *************************************************************************/
public abstract class BufferedCanvas extends Canvas 
	implements PaintListener, DisposeListener
{
	
	/*** buffer image for displaying the canvas ****/
	protected Image imageBuffer;

	public BufferedCanvas(Composite parent) 
	{
		this(parent, true);
	}
	
	public BufferedCanvas(Composite parent, boolean withMenu)
	{
		super(parent, SWT.NO_BACKGROUND);
		addPaintListener(this);
		addDisposeListener(this);
		
		if (withMenu)
			setContextMenus();
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
	 */
	public void paintControl(PaintEvent e) 
	{
		if (imageBuffer != null) 
		{
			final Rectangle area = getClientArea();
			final Rectangle rect = imageBuffer.getBounds();
			
			try {
				// Stretch or shrink the image automatically
				e.gc.drawImage(imageBuffer, 0, 0, rect.width, rect.height, 
						0, 0, area.width, area.height);
			}
			catch (Exception ex)
			{
				// An exception "Illegal argument" will be raised if the resize method is not "fast" enough to create the image
				//		buffer before the painting is called. Thus, it causes inconsistency between the size of the image buffer
				//		and the size of client area. 
				//		If this happens, either we wait for the creation of image buffer, or do nothing. 
				//		I prefer to do nothing because of scalability concerns.
				return;
			}
		}
	}
	
	/******
	 * initialization when a new data arrives
	 */
	protected void initBuffer() 
	{
	}
	
	/***
	 * Set a new image buffer.
	 * The old buffer will be destroyed automatically.
	 * 
	 * @param buffer
	 */
	protected void setBuffer(Image buffer)
	{
		if (imageBuffer != null && !imageBuffer.isDisposed()) {
			try {
				imageBuffer.dispose();
			} catch (Exception e) {
				System.err.println("Data race: disposing an already destroyed image.");
				e.printStackTrace();
			}
		}
		this.imageBuffer = buffer;
	}
	
	protected Image getBuffer()
	{
		return imageBuffer;
	}
	

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
	 */
	public void widgetDisposed(DisposeEvent e)
	{
		if (imageBuffer != null)
			imageBuffer.dispose();
	}
	
	/**
	 * Save the current buffer image to a file
	 * 
	 * @param filename
	 */
	public void save(String filename) 
	{
		if (filename != null && !filename.isEmpty()) 
		{
			// get image data from the buffer
			ImageData data = imageBuffer.getImageData();
			ImageLoader loader = new ImageLoader();
			loader.data = new ImageData[] {data};
			
			// save the data into a file with PNG format
			loader.save(filename, SWT.IMAGE_PNG);
		}
	}
	/*************************************************************************
	 * add context menus for the canvas
	 *************************************************************************/
	private void setContextMenus() {

		final Shell shell = getShell();
		if (shell != null) {
			
			final Action saveImage = new Action("Save image ...") {
				
				public void run() {
					
					if (imageBuffer == null)
						return;
					
					FileDialog dialog = new FileDialog(shell, SWT.SAVE);
					dialog.setText("Save image ... ");
					dialog.setFilterExtensions(new String[] {"*.png"});
					String filename = dialog.open();
					save(filename);
				}
			};
			
			// add menus to the canvas
			MenuManager mnuMgr = new MenuManager();
			Menu menu = mnuMgr.createContextMenu(this);
			mnuMgr.add(saveImage);

			setMenu(menu);
			
		} else {
			System.err.println("BC: shell is null");
		}
	}
}
