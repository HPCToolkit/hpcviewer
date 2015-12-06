package edu.rice.cs.hpc.viewer.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import edu.rice.cs.hpc.viewer.window.ViewerWindowManager;

/***
 *  
 * class to handle titles for windows, views, and editors
 *
 */
public class WindowTitle extends BaseWindowTitle {
	public enum MethodFlag { WINDOWTITLE, VIEWTITLE, EDITORTITLE};

	// The ID of the extension point
	private static final String IWINDOWTITLE_ID = "edu.rice.cs.hpc.viewer.util.windowTitle";

	private IWindowTitle extWindowTitle[] = null;
	private ExtensionSafeRunnable runnable = null;

	public WindowTitle() {
		super();
		// find all the extensions and save a list so we do not have to do this every time we want to call them.
		IConfigurationElement[] configs = Platform.getExtensionRegistry().getConfigurationElementsFor(IWINDOWTITLE_ID);
		if (configs != null && configs.length>0) {
			
			extWindowTitle = new IWindowTitle[configs.length];
			int i = 0;
			
			for (IConfigurationElement e: configs)
			{
				try {
					final Object o = e.createExecutableExtension("class");
					if (o instanceof IWindowTitle) {
						extWindowTitle[i] = ((IWindowTitle)o);
						i++;
						
						if (runnable == null)
							runnable = new ExtensionSafeRunnable();
					}
				} catch (CoreException e1) {
					e1.printStackTrace();
				}
			}
		}		
	}

	/***
	 * Reset all window, view and editor titles
	 * 
	 * @param window
	 * @param experiment: current database
	 */
	public void refreshAllTitles() {

		final IWorkbenchWindow[] wkBenchWins = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow wkBenchWin: wkBenchWins) {
			// if there are no databases open in this window, it may have been disposed so we can not update titles
			if (ViewerWindowManager.getWindowNumber(wkBenchWin) == -1) {
				continue;
			}
			// refresh this window title
			final String title = getWindowTitle(wkBenchWin);
			wkBenchWin.getShell().setText(title);
			// refresh the view titles in this window
			refreshViewTitles(wkBenchWin);
			// refresh the editor titles in this window
			refreshEditorTitles(wkBenchWin);
		}
		return;
	}
	

	/****
	 * Reset all view titles
	 * @param window
	 */
	public void refreshViewTitles(IWorkbenchWindow window) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null)
		{
			final IViewReference viewRefs[] = page.getViewReferences();
			for (IViewReference viewRef: viewRefs) {
				final IViewPart view = viewRef.getView(false);
				setTitle(window, view);
			}
		}
	}

	/***
	 * reset all editor titles
	 * 
	 * @param window
	 */
	public void refreshEditorTitles(IWorkbenchWindow window) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null)
		{
			final IEditorReference editors[] = page.getEditorReferences();
			for (IEditorReference editor: editors) {
				final IEditorPart editorPart = editor.getEditor(false);
				setEditorTitle(window, editorPart);
			}
		}
	}

	/***
	 * Get the title of a window
	 * 
	 * @param window
	 * @param experiment
	 * @return
	 */
	public String getWindowTitle(final IWorkbenchWindow window) {
		if (runnable != null) {
			if ( this.runExtension(runnable, MethodFlag.WINDOWTITLE, window, null)) {
				String windowTitle = runnable.getTitle();
				// if the extension got the window title, just return it
				if (windowTitle != null) {
					return windowTitle;
				}
			}
		}
		// either there was no extension or the extension did not handle window titles, let the super method try and set it
		return super.getWindowTitle(window);
	}

	/***
	 * Set the title of a view
	 * 
	 * @param window
	 * @param view
	 * @returns - the suggested title of the view
	 */
	public String setTitle(IWorkbenchWindow window, IViewPart view) { 
		if (runnable != null) {
			if ( this.runExtension(runnable, MethodFlag.VIEWTITLE, window, view) ) {

				String viewTitle = runnable.getTitle();
                // if the extension set the view title, just return that we are done
                if ( viewTitle != null) {
                        return viewTitle;
                }
			}
		}
		// either there was no extension or the extension did not handle this kind of view, let the super method try and set it
		return super.setTitle(window, view);
	}

	/***
	 * Set the title of an Editor
	 * 
	 * @param window
	 * @param experiment
	 * @param sTitle
	 * @returns - non-null if the title was set, null otherwise
	 */
	public String setEditorTitle(IWorkbenchWindow window, IEditorPart editorPart) { //, Experiment experiment, String sTitle) {
		if (runnable != null) {
			if ( this.runExtension(runnable, MethodFlag.EDITORTITLE, window, editorPart) ) {
				String editorTitle = runnable.getTitle();
				// if the extension set the editor title, just return that we are done
				if ( editorTitle != null) {
					return editorTitle;
				}
			}
		}
		// either there was no extension or the extension did not handle this kind of editor, let the super method try and set it
		return super.setEditorTitle(window, editorPart);
	}

	/***
	 * run all registered extensions of window title
	 * 
	 * @param run
	 * @param element
	 * @param mf
	 */
	private boolean runExtension( ISafeRunnable run, MethodFlag mf, final IWorkbenchWindow window, Object object ) {
		
		boolean isCalled = false;
		
		for (IWindowTitle ext: this.extWindowTitle) {
			if (ext != null) {
				runnable.setInfo(ext, mf, window, object);
				SafeRunner.run(run);
				isCalled = true;
			}
		}
		return isCalled;
	}

	static class ExtensionSafeRunnable implements ISafeRunnable {
		private IWindowTitle windowTitle;
		private MethodFlag mf;
		private IWorkbenchWindow window;
		private Object object;
		private String strResult;
		
		public void setInfo(IWindowTitle _windowTitle, MethodFlag _mf, final IWorkbenchWindow _window, Object _object) {
			windowTitle = _windowTitle;
			mf = _mf;
			window = _window;
			object = _object;
		}

		public void handleException(Throwable exception) {
			System.out.println("Exception in window title extension.");
		}

		public void run() throws Exception {
			if (mf == MethodFlag.WINDOWTITLE)
			{
				strResult = windowTitle.getWindowTitle(window);
				return;
			}
			if (mf == MethodFlag.VIEWTITLE)
			{
				strResult = windowTitle.setTitle(window, (IViewPart)object);
				return;
			}
			if (mf == MethodFlag.EDITORTITLE)
			{
				strResult = windowTitle.setEditorTitle(window, (IEditorPart)object);
				return;
			}
			return;
		}
		
		public String getTitle() {
			return strResult;
		}
	}
}
