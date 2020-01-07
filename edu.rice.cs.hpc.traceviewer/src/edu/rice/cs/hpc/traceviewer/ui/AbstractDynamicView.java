package edu.rice.cs.hpc.traceviewer.ui;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public abstract class AbstractDynamicView 
	extends ViewPart 
	implements IActiveNotification 
{

	/*****
	 * Setup the default listener
	 * The derived class is recommended to call this function after the view is instantiated
	 * such as during createPartControl
	 */
	protected void addListener()
	{
		final IViewSite  viewSite = getViewSite();
		if (viewSite == null)
			return;
		
		final String id 		  = viewSite.getId();
		final IWorkbenchPage page = viewSite.getPage();
		
		page.addPartListener( new IPartListener2() {
			
			@Override
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
			 */
			public void partVisible(IWorkbenchPartReference partRef) {
				if (id.equals(partRef.getId())) {
					active(true);
				}
			}
			
			@Override
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
			 */
			public void partHidden(IWorkbenchPartReference partRef) {
				if (id.equals(partRef.getId())) {
					active(false);
				}
			}
			
			@Override
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
			 */
			public void partClosed(IWorkbenchPartReference partRef) {
				page.removePartListener(this);				
			}
			
			@Override
			public void partOpened(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {}
			
			
			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {}
		});
	}
}
