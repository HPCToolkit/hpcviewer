package edu.rice.cs.hpc.traceviewer.data.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.common.ui.Util;
import edu.rice.cs.hpc.common.util.ProcedureClassData;
import edu.rice.cs.hpc.traceviewer.data.util.ProcedureClassMap;

/**************************************************************
 * A data structure designed to hold all the name-color pairs
 * needed for the actual drawing.
 **************************************************************/
public class ColorTable 
{
	static final public int COLOR_ICON_SIZE = 8;
	
	static private final int MAX_NUM_DIFFERENT_COLORS = 512;
	static private final int COLOR_MIN = 16;
	static private final int COLOR_MAX = 200 - COLOR_MIN;
	static private final long RANDOM_SEED = 612543231L;
	
	/**The display this ColorTable uses to generate the random colors.*/
	final private Display display;

	/** user defined color */
	final private ProcedureClassMap classMap;
	
	final private Random random_generator;

	// data members

	private ColorImagePair IMAGE_WHITE;
	final private	HashMap<String, ColorImagePair> colorMatcher;

	/**Creates a new ColorTable with Display _display.*/
	public ColorTable()
	{
		display = Util.getActiveShell().getDisplay();
		
		// rework the color assignment to use a single random number stream
		random_generator = new Random((long)RANDOM_SEED);

		// initialize the procedure-color map (user-defined color)
		classMap = new ProcedureClassMap(display);
		
		colorMatcher = new HashMap<String, ColorTable.ColorImagePair>();
		
		initializeWhiteColor();
	}
	
	/**
	 * Dispose the allocated resources
	 */
	public void dispose() {
		for (ColorImagePair pair: colorMatcher.values()) {
			if (pair != null) pair.dispose();
		}
		
		colorMatcher.clear();
		classMap.clear();
	}
	
	/**
	 * Returns the color in the colorMatcher that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Color getColor(String name)
	{
		ColorImagePair cip = createColorImagePair(name);
		if (cip == null) {
			System.err.println(name + ": has null color");
		}
		return cip.getColor();
	}
	
	/**
	 * returns the image that corresponds to the name's class
	 * @param name
	 * @return
	 */
	public Image getImage(String name) 
	{
		ColorImagePair cip = createColorImagePair(name); 
		if (cip == null) {
			System.err.println(name + ": has null image");
		}
		return cip.getImage();
	}
	
	public void setColor(List<String> listProcedure) {
		
		ColorImagePair []values = null;
		int i = 0;
		
		for (String proc: listProcedure) {
			
			ColorImagePair pair = null;
			
			if (i<MAX_NUM_DIFFERENT_COLORS) {
				pair = createColorImagePair(proc);
				i++;
			} else {
				
				if (values == null) {
					values = new ColorImagePair[listProcedure.size()];
					colorMatcher.values().toArray(values);
				}
				int index = random_generator.nextInt(MAX_NUM_DIFFERENT_COLORS-1);
				pair = values[index];
			}
			colorMatcher.put(proc, pair);
		}
	}
	
	/***********************************************************************
	 * create an image based on the color
	 * the caller is responsible to free the image
	 * 
	 * @param display
	 * @param color
	 * @return an image (to be freed)
	 ***********************************************************************/
	static public Image createImage(Display display, RGB color) {
		PaletteData palette = new PaletteData(new RGB[] {color} );
		ImageData imgData = new ImageData(COLOR_ICON_SIZE, COLOR_ICON_SIZE, 1, palette);
		Image image = new Image(display, imgData);
		return image;
	}
	
	
	/************************************************************************
	 * create a pair of color and image based on the procedure name
	 * 
	 * @param procName
	 * @return ColorImagePair
	 ************************************************************************/
	private ColorImagePair createColorImagePair(String procName)
	{
		ColorImagePair cip = colorMatcher.get(procName);
		
		if (cip != null) {
			return cip;
		}			
		
		RGB rgb = getProcedureColor( procName, COLOR_MIN, COLOR_MAX, random_generator );
		Color c = new Color(display, rgb);
		Image i = createImage(display, rgb);
		cip = new ColorImagePair(c, i);
		
		colorMatcher.put(procName, cip);
		
		return cip;
	}
	
	/***********************************************************************
	 * retrieve color for a procedure. If the procedure has been assigned to
	 * 	a color, we'll return the allocated color, otherwise, create a new one
	 * 	randomly.
	 * 
	 * @param name name of the procedure
	 * @param colorMin minimum integer value
	 * @param colorMax maximum integer value
	 * @param r random integer
	 * 
	 * @return RGB
	 ***********************************************************************/
	private RGB getProcedureColor( String name, int colorMin, int colorMax, Random r ) {
		
		// if the name matches, we return the user-defined color
		// otherwise, we randomly create a color for this name
		
		ProcedureClassData value = this.classMap.get(name);
		final RGB rgb;
		if (value != null)
			rgb = value.getRGB();
		else 
			rgb = new RGB(	colorMin + r.nextInt(colorMax), 
							colorMin + r.nextInt(colorMax), 
							colorMin + r.nextInt(colorMax));
		return rgb;
	}

	/************************************************************************
	 * Initialize the predefined-value of white color
	 * 
	 * If the white color value is not initialize, we create a new one
	 * Otherwise, do nothing.
	 ************************************************************************/
	private void initializeWhiteColor() {
		if (IMAGE_WHITE == null || IMAGE_WHITE.getImage().isDisposed()) {
			// create our own white color so we can dispose later, instead of disposing
			//	Eclipse's white color
			final RGB rgb_white = display.getSystemColor(SWT.COLOR_WHITE).getRGB();
			final Color col_white = new Color(display, rgb_white);
			final Image img_white = createImage(display, rgb_white);
			
			IMAGE_WHITE = new ColorImagePair(col_white, img_white );
			
			colorMatcher.put(CallPath.NULL_FUNCTION, IMAGE_WHITE);
		}
	}
	
	
	/************************************************************************
	 * class to pair color and image
	 * @author laksonoadhianto
	 *
	 ************************************************************************/
	private class ColorImagePair {
		private Color color;
		private Image image;
		
		/****
		 * create a color-image pair
		 * @param Color color
		 * @param Image image
		 */
		ColorImagePair(Color color, Image image) {
			// create an empty image filled with color c
			this.image = image;
			this.color = color;
		}
		
		/***
		 * get the color 
		 * @return Color
		 */
		public Color getColor() {
			return this.color;
		}
		
		/***
		 * get the image
		 * @return Image
		 */
		public Image getImage() {
			return this.image;
		}
		
		public void dispose() {
			this.color.dispose();
			this.image.dispose();
		}
	}
}