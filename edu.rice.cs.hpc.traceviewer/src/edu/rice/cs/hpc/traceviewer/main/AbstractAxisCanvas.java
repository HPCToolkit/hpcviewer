package edu.rice.cs.hpc.traceviewer.main;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import edu.rice.cs.hpc.traceviewer.operation.BufferRefreshOperation;
import edu.rice.cs.hpc.traceviewer.operation.TraceOperation;

abstract public class AbstractAxisCanvas extends Canvas 
				implements PaintListener, IOperationHistoryListener
{

	public AbstractAxisCanvas(Composite parent, int style) {
		super(parent, SWT.NO_BACKGROUND | style);

		addPaintListener(this);
	}

	

	@Override
	public void setData(Object data) {
		
		if (getData() == null) {
			// just initialize once
			TraceOperation.getOperationHistory().addOperationHistoryListener(this);
		}

		super.setData(data);
		
		syncRedraw();
	}

	/***
	 * Just like normal redraw, but this method uses UI thread
	 * and wait until the thread is available
	 */
	protected void syncRedraw() {
		
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				if (getData() == null)
					return;
				
				redraw();
			}
		});
	}



	@Override
	public void historyNotification(OperationHistoryEvent event) {

		if (event.getEventType() == OperationHistoryEvent.DONE) {
			final IUndoableOperation operation = event.getOperation();

			if (operation.hasContext(BufferRefreshOperation.context)) {
				syncRedraw();
			}
		}
	}

}
