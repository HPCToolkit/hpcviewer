/**
 * 
 */
package edu.rice.cs.hpc.viewer.util;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeType;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.OSValidator;

import edu.rice.cs.hpc.viewer.editor.SourceCodeEditor;
import edu.rice.cs.hpc.viewer.experiment.ExperimentView;
import edu.rice.cs.hpc.viewer.framework.Activator;
import edu.rice.cs.hpc.viewer.resources.Icons;
import edu.rice.cs.hpc.viewer.scope.AbstractBaseScopeView;
import edu.rice.cs.hpc.viewer.scope.ScopeTreeViewer;
import edu.rice.cs.hpc.viewer.window.Database;
import edu.rice.cs.hpc.viewer.window.ViewerWindow;
import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

/**
 * Class providing auxiliary utilities methods.
 * Remark: it is useless to instantiate this class since all its methods are static !
 * @author laksono
 *
 */
public class Utilities 
{
	// list of available fixed fonts.
	// the order is important: if the font in first element is available, then we'll use this one.
	
	static public final String []LIST_METRIC_FONTS = { "Courier", "Monospaced", "Courier New"};
	
	//special font for the metric columns. It supposed to be fixed font
	static public Font fontMetric;
	/* generic font for view and editor */
	static public Font fontGeneral;
	
	// special color for the top row
	static public Color COLOR_TOP;
	
	static final public String NEW_LINE = System.getProperty("line.separator");

	static final public Styler STYLE_ACTIVE_LINK = new StyledString.Styler() {

		@Override
		public void applyStyles(TextStyle textStyle) {
			ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
			textStyle.foreground = colorRegistry.get(JFacePreferences.ACTIVE_HYPERLINK_COLOR);
			textStyle.font 		 = fontGeneral;
		}
		
	};
	
	static final public Styler STYLE_INACTIVE_LINK = new StyledString.Styler() {
		
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.font = fontGeneral;
		}
	};
	
	/** font style for clickable line number in a call site */
	static final public Styler STYLE_COUNTER = new StyledString.Styler() {
		
		@Override
		public void applyStyles(TextStyle textStyle) {
			ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
			textStyle.foreground = colorRegistry.get(JFacePreferences.COUNTER_COLOR);
			textStyle.font = fontGeneral;
		}
	};
	
	/** font style for unclickable line number in a callsite */
	static final public Styler STYLE_DECORATIONS = new StyledString.Styler() {
		
		@Override
		public void applyStyles(TextStyle textStyle) {
			ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
			textStyle.foreground = colorRegistry.get(JFacePreferences.DECORATIONS_COLOR);
			textStyle.font = fontGeneral;
		}
	};

	/**
	 * Initialize the font for the metric columns (it may be different to other columns)
	 * This method has to be called first before others
	 * @param display
	 */
	static public void initFontMetric(Display display) {
		COLOR_TOP = new Color(display, 255,255,204);
		
		ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
		FontData []objFontMetric = display.getSystemFont().getFontData();
		FontData []objFontGeneric = display.getSystemFont().getFontData();
		
		objFontMetric[0].setName(getMetricFont()); 

		// get the font for metrics columns based on user preferences
		if (objPref != null) {
			// bug fix: for unknown reason, the second instance of hpcviewer cannot find the key
			//	solution: check if the key exist or not IPreferenceStore.STRING_DEFAULT_DEFAULT.equals
			String sValue = objPref.getString (PreferenceConstants.P_FONT_METRIC); 
			if ( !IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(sValue) )
				objFontMetric = PreferenceConverter.getFontDataArray(objPref, PreferenceConstants.P_FONT_METRIC);
			else 
				// bug fix: if user hasn't set the preference, we set it for him/her
				PreferenceConverter.setValue(objPref, PreferenceConstants.P_FONT_METRIC, objFontMetric);
			
			sValue = objPref.getString (PreferenceConstants.P_FONT_GENERIC);
			if ( !IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(sValue) )
				objFontGeneric = PreferenceConverter.getFontDataArray(objPref, PreferenceConstants.P_FONT_GENERIC);
			else 
				// bug fix: if user hasn't set the preference, we set it for him/her
				PreferenceConverter.setValue(objPref, PreferenceConstants.P_FONT_METRIC, objFontMetric);
		}
		// create font for general purpose (view, editor, ...)
		Utilities.fontGeneral = new Font (display, objFontGeneric);
		
		Utilities.fontMetric = new Font(display, objFontMetric);		
	}

	/**
	 * Set a new font for metric and generic view
	 * @param window
	 * @param objFontMetric
	 * @param objFontGeneric
	 */
	static public void setFontMetric(IWorkbenchWindow window, FontData objFontMetric[], FontData objFontGeneric[]) {
		FontData []myFontMetric = Utilities.fontMetric.getFontData();
		boolean isMetricFontChanged = isDifferentFontData(myFontMetric, objFontMetric);
		if (isMetricFontChanged) {
			Device device = Utilities.fontMetric.getDevice();
			Utilities.fontMetric.dispose();
			Utilities.fontMetric = new Font(device, objFontMetric);
		}
		
		FontData []myFontGeneric = Utilities.fontGeneral.getFontData();
		boolean isGenericFontChange = isDifferentFontData(myFontGeneric, objFontGeneric);
		if (isGenericFontChange) {
			Device device = Utilities.fontGeneral.getDevice();
			Utilities.fontGeneral.dispose();
			Utilities.fontGeneral = new Font(device, objFontGeneric);
		}
		
		if (isMetricFontChanged || isGenericFontChange) {
			// a font has been changed. we need to refresh the view
			resetAllViews (window);
			
			// refresh other windows too
			for (int i=0; i<ViewerWindowManager.size(); i++) {
				ViewerWindow vw = ViewerWindowManager.getViewerWindow(i);
				if (vw.getWinObj()!=window) {
					resetAllViews(vw.getWinObj());
				}
			}
			// set the fonts in the preference store to the new fonts
			// if we got here from a preference page update this was already done by the font field editor but 
			// if we got here from a tool bar button then we need the new values to be put into the preference store
			// in addition this call will fire a property changed event which other non-Rice views can listen for and
			// use it to refresh their views (without this event the SWT code throws lots of invalid argument exceptions
			// because it tries to repaint the non-Rice views using the font that the Rice code has just disposed).
			Utilities.storePreferenceFonts();
		}
	}
	
	
	/*****
	 * check if two font data are equal (name, height and style)
	 * 
	 * @param fontTarget
	 * @param fontSource
	 * @return
	 */
	static public boolean isDifferentFontData(FontData fontTarget[], FontData fontSource[]) {
		boolean isChanged = false;
		for (int i=0; i<fontTarget.length; i++) {
			if (i < fontSource.length) {
				FontData source = fontSource[i];
				// bug: if the height is not common, we just do nothing, consider everything work fine
				if (source.getHeight()<4 || source.getHeight()>99)
					return false;
				
				FontData target = fontTarget[i];
				isChanged = !( target.getName().equals(source.getName()) &&
						(target.getHeight()==source.getHeight()) && 
						(target.getStyle()==source.getStyle()) ) ;
				
				if (isChanged)
					// no need to continue the loop
					return isChanged;
			}
		}
		return isChanged;
	}
	
	/****
	 * remove all the allocated resources
	 */
	static public void dispose() {
		try {
			Utilities.fontGeneral.dispose();
			Utilities.fontMetric.dispose();
			COLOR_TOP.dispose();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/****
	 * Store the new fonts into the workspace registry
	 */
	static private void storePreferenceFonts() {
		ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
		PreferenceConverter.setValue(objPref, PreferenceConstants.P_FONT_GENERIC, Utilities.fontGeneral.getFontData());
		PreferenceConverter.setValue(objPref, PreferenceConstants.P_FONT_METRIC, Utilities.fontMetric.getFontData());
	}
	
	/**
	 * Refresh all the views 
	 * @param window: the target window
	 */
	static private void resetAllViews(IWorkbenchWindow window) {

		ViewerWindow vWin = ViewerWindowManager.getViewerWindow(window);
		if (vWin == null) {
			System.out.printf("Utilities.resetAllViews: ViewerWindow class not found\n");
			return;
		}

		final TreeItemManager objItemManager = new TreeItemManager();
		
		// first, we need to refresh the visible view
		final ArrayList<AbstractBaseScopeView> visible_view = Utilities.getTopView(window);
		if (visible_view != null && visible_view.size()>0) {
			for (AbstractBaseScopeView view : visible_view) {
				Utilities.resetView(objItemManager, view.getTreeViewer());
			}
		}
		
		// find each open database so we can reset its views
		for (Database db : vWin.getDatabases()) {
			// get the views created for our database
			if (db == null) {
				continue;		// not open just skip it
			}

			final ExperimentView ev  = db.getExperimentView(); 
			final int numViews = ev.getViewCount();
			
			// next, using helper thread to refresh other views
			window.getShell().getDisplay().asyncExec( new Runnable() {
				public void run() {
					
					// refresh all the views except the visible one 
					// we will prioritize the visible view to be refreshed first
					for(int i=0;i<numViews;i++) {
						if (!visible_view.contains( ev.getView(i))) {
							ScopeTreeViewer tree = ev.getView(i).getTreeViewer();
							
							// reset the view
							Utilities.resetView(objItemManager, tree);
						}
					}
				}
			});
		}
	}

	/****
	 * Find the first visible scope view (the view can be active or not)
	 * @return the visible view, null if there is no view
	 */
	static ArrayList<AbstractBaseScopeView> getTopView(IWorkbenchWindow window) {
		IWorkbenchPage page = window.getActivePage();
		IViewReference [] viewRefs = page.getViewReferences();
		ArrayList<AbstractBaseScopeView> listViews = new ArrayList<AbstractBaseScopeView>(viewRefs.length);
		
		for(int i=0;i<viewRefs.length;i++) {
			IWorkbenchPart part = viewRefs[i].getPart(false);
			if (page.isPartVisible(part)) {
				if (part instanceof AbstractBaseScopeView) {
					listViews.add((AbstractBaseScopeView)part);
				}
			}
		}

		return listViews;
	}
	
	static public void resetView ( TreeViewer tree )
	{
		TreeItemManager objItem = new TreeItemManager();
		resetView(objItem, tree);
	}
	
	/**
	 * refresh a particular view
	 * To save memory allocation, we ask an instance of TreeItemManager
	 * @param objItemManager
	 * @param tree
	 */
	static private void resetView ( TreeItemManager objItemManager, TreeViewer tree) {
		//resetViewRowHeight(tree);
		// save the context first
		objItemManager.saveContext(tree);
		// refresh
		tree.refresh();
		// restore the context
		objItemManager.restoreContext(tree);
	}
	
	/**
	 * activate a listener to reset Row Height for Windows only
	 * @param tree
	 */
	static public Listener listenerToResetRowHeight ( TreeViewer tree ) {
		
		final int MARGIN_FONT = 10;
		
		Tree treeItem = tree.getTree();
		
		// resize the table row height using a MeasureItem listener
		Listener measurementListener = new Listener() {
			public void handleEvent(Event event) {
				final ScopedPreferenceStore objPref = (ScopedPreferenceStore)Activator.getDefault().getPreferenceStore();
				FontData []objFontsMetric = PreferenceConverter.getFontDataArray(objPref, PreferenceConstants.P_FONT_METRIC);
				FontData []objFontsGeneric = PreferenceConverter.getFontDataArray(objPref, PreferenceConstants.P_FONT_GENERIC);
				
				// get font height (from preferences) for each font
				int objFontMetricHeight  = objFontsMetric[0].getHeight();
				int objFontGenericHeight = objFontsGeneric[0].getHeight();
				
				event.height = Math.min(objFontMetricHeight, objFontGenericHeight) + MARGIN_FONT;
			} // end handleEvent
		}; // end measurementListener
		treeItem.addListener(SWT.MeasureItem, measurementListener);
		return measurementListener;
	}
	
	/**
	 * refresh size of rows for a particular view - non Windows
	 * @param tree
	 */
	static public void resetViewRowHeight ( TreeViewer tree ) {
		if (!OSValidator.isWindows()) { 
			int saveWidth = tree.getTree().getColumn(0).getWidth();
			tree.getTree().getColumn(0).setWidth(saveWidth==0?1:0);
			tree.getTree().getColumn(0).setWidth(saveWidth);
		}
	}
	
	/**
	 * Update the font for metric pane with one single font (just take the size)
	 * @param objFontData
	 */
	static private void setFontMetric(IWorkbenchWindow window, int iFontSize) {
			FontData []myFontGeneric = Utilities.fontGeneral.getFontData();
			int iSize = myFontGeneric[0].getHeight() + iFontSize;
			myFontGeneric[0].setHeight(iSize);
			
			FontData []myFontMetric = Utilities.fontMetric.getFontData();
			iSize = myFontMetric[0].getHeight() + iFontSize;
			myFontMetric[0].setHeight(iSize);
			
			setFontMetric(window, myFontMetric, myFontGeneric);
			
	}
	
	/**
	 * Increment font size
	 * @param window
	 */
	static public void increaseFont(IWorkbenchWindow window) {
		Utilities.setFontMetric(window, +1);
	}

	
	/**
	 * Decrement font size
	 * @param window
	 */
	static public void DecreaseFont(IWorkbenchWindow window) {
		Utilities.setFontMetric(window, -1);
	}

	/**	
	 * Insert an item on the top on the tree/table with additional image if not null
	 * @param treeViewer : the tree viewer
	 * @param imgScope : the icon for the tree node
	 * @param arrText : the label of the items (started from  col 0..n-1)
	 */
	static public void insertTopRow(TreeViewer treeViewer, Image imgScope, String []arrText) {
		if(arrText == null)
			return;
    	TreeItem item = new TreeItem(treeViewer.getTree(), SWT.BOLD, 0);
    	if(imgScope != null)
    		item.setImage(0,imgScope);

    	// Laksono 2009.03.09: add background for the top row to distinguish with other scopes
    	item.setBackground(Utilities.COLOR_TOP);
    	// make monospace font for all metric columns
    	item.setFont(Utilities.fontMetric);
    	item.setFont(0, Utilities.fontGeneral); // The tree has the original font
    	// put the text on the table
    	item.setText(arrText);
    	// set the array of text as the item data 
    	// we will use this information when the table is sorted (to restore the original top row)
    	item.setData(arrText);
	}

	/**
	 * Retrieve the top row items into a list of string
	 * @param treeViewer
	 * @return
	 */
	public static String[] getTopRowItems( TreeViewer treeViewer ) {
		// for dynamic views, the table is initially empty
		if (treeViewer.getTree().getItemCount() == 0)
			return null;
		
		TreeItem item = treeViewer.getTree().getItem(0);
		String []sText= null; // have to do this to avoid error in compilation;
		if(item.getData() instanceof Scope) {
			// the table has been zoomed-out
		} else {
			// the table is in original form or flattened or zoom-in
			Object o = item.getData();
			if(o != null) {
				Object []arrObj = (Object []) o;
				if(arrObj[0] instanceof String) {
					sText = (String[]) item.getData(); 
				}
			}
		}
		return sText;
	}
	/**
	 * Return an image depending on the scope of the node.
	 * The criteria is based on ScopeTreeCellRenderer.getScopeNavButton()
	 * @param scope
	 * @return
	 */
	static public Image getScopeNavButton(Object scope) {
		if (scope instanceof RootScope)
			return Icons.getImage(Icons.Image_MetricAggregate);
		
		if (scope instanceof CallSiteScope) {
			CallSiteScope scopeCall = (CallSiteScope) scope;
        	LineScope lineScope = (scopeCall).getLineScope();
			if (((CallSiteScope) scope).getType() == CallSiteScopeType.CALL_TO_PROCEDURE) {
				if(Utilities.isFileReadable(lineScope))
					return Icons.getImage(Icons.Image_CallTo);
				else
					return Icons.getImage(Icons.Image_CallToDisabled);
			} else {
				if(Utilities.isFileReadable(lineScope))
					return Icons.getImage(Icons.Image_CallFrom);
				else
					return Icons.getImage(Icons.Image_CallFromDisabled);
			}
		}
		return null;
	}
	
	static public Image getInlineNavButton(ProcedureScope proc)
	{
		if (proc.isAlien()) {
			boolean readable = Utilities.isFileReadable(proc);
			if (readable) {
				return Icons.getImage(Icons.Image_InlineTo);
			} else {
				return Icons.getImage(Icons.Image_InlineToDisabled);
			}
		}
		return null;
	}

    /**
     * Verify if the file exist or not.
     * Remark: we will update the flag that indicates the availability of the source code
     * in the scope level. The reason is that it is less time consuming (apparently) to
     * access to the scope level instead of converting and checking into FileSystemSourceFile
     * level.
     * @param scope
     * @return true if the source is available. false otherwise
     */
    static public boolean isFileReadable(Scope scope) {
    	// check if the source code availability is already computed
    	if(scope.iSourceCodeAvailability == Scope.SOURCE_CODE_UNKNOWN) {
    		SourceFile newFile = (scope.getSourceFile());
    		if (newFile != null && !newFile.getName().isEmpty()) {
        		if( (newFile != SourceFile.NONE)
            			|| ( newFile.isAvailable() )  ) {
            			if (newFile instanceof FileSystemSourceFile) {
            				FileSystemSourceFile objFile = (FileSystemSourceFile) newFile;
            				if(objFile != null) {
            					// find the availability of the source code
            					if (objFile.isAvailable()) {
            						scope.iSourceCodeAvailability = Scope.SOURCE_CODE_AVAILABLE;
            						return true;
            					} 
            				}
            			}
            		}
    		}
    	} else
    		// the source code availability is already computed, we just reuse it
    		return (scope.iSourceCodeAvailability == Scope.SOURCE_CODE_AVAILABLE);
    	// in this level, we don't think the source code is available
		scope.iSourceCodeAvailability = Scope.SOURCE_CODE_NOT_AVAILABLE;
		return false;
    }
    
    
    /****
     * return the current active experiment. 
     * If there's only one opened database, then return the experiment
     * Otherwise, check for the database of the current active view or editor,
     * 	assuming there's one active view/editor.
     *  (return null if no part is active)
     *  
     * @param window
     * @return
     */
    static public Experiment getActiveExperiment(IWorkbenchWindow window) {

		final ViewerWindow vw = ViewerWindowManager.getViewerWindow(window);
		
		if (vw == null)
			return null;
		
		final int numDB = vw.getOpenDatabases();
		Experiment experiment = null;
		
		// try to find the current database
		if (numDB == 1)
		{
			// only one opened database
			experiment = vw.getExperiments()[0];
		}else
		{
			// multiple databases are opened:
			// need to select an experiment to show
			IWorkbenchPart part = window.getActivePage().getActivePart();
			if (part instanceof AbstractBaseScopeView)
			{
				experiment = ((AbstractBaseScopeView)part).getExperiment();
				
			} else if (part instanceof SourceCodeEditor)
			{
				experiment = ((SourceCodeEditor)part).getExperiment();
			}
		}
		return experiment;
    }
    
    /***
     * get the best way to find fixed font for metric columns
     * If no font is supported, it will return the first element in the LIST_METRIC_FONTS
     * since Eclipse requires a name. Doesn't matter if it exists or not.
     * 
     * @return fixed font
     */
    static private String getMetricFont() {
		String []availableFonts = getAvailableFonts();
		
		for (String font : LIST_METRIC_FONTS) {
			for (String availableFont : availableFonts) {
				if (font.equals(availableFont)) {
					return availableFont;
				}
			}
		}
		// we don't find any font supported by the system.
		// since we cannot return null (Eclipse doesn't like null font)
		// we just return the first font in the list. 
		// Let Eclipse find out what is the best font for us.
		return LIST_METRIC_FONTS[0];
    }
    
    /***
     * Returns the list of supported font by the system.
     * Since Eclipse 3.x doesn't support this function, we use Java AWT.
     * Once we move to Eclipse 4, we can use its theme/css function.
     *  
     * @return list of supported fonts
     */
    static private String[] getAvailableFonts() {

    	GraphicsEnvironment g=GraphicsEnvironment.getLocalGraphicsEnvironment();
    	return g.getAvailableFontFamilyNames();
    }
}
