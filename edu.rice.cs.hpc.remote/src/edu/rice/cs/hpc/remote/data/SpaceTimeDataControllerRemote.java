package edu.rice.cs.hpc.remote.data;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;

import edu.rice.cs.hpc.data.experiment.ExperimentConfiguration;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.remote.data.DecompressionThread.DecompressionItemToDo;
import edu.rice.cs.hpc.remote.data.RemoteFilteredBaseData;
import edu.rice.cs.hpc.data.experiment.extdata.TraceName;
import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;



/**************************************************
 * The remote data version of the Data controller
 * 
 * @author Philip Taffet
 * 
 *************************************************/
public class SpaceTimeDataControllerRemote extends SpaceTimeDataController 
{	
	final RemoteDataRetriever dataRetriever;

	private final TraceName[]  valuesX;
	private final DataOutputStream server;
	
	private ConcurrentLinkedQueue<Integer> timelineToRender;

	public SpaceTimeDataControllerRemote(RemoteDataRetriever _dataRet, IWorkbenchWindow _window,
			InputStream expStream, String Name, int _numTraces, TraceName[] valuesX, DataOutputStream connectionToServer) 
					throws InvalExperimentException, Exception 
	{
		super(_window, expStream, Name);
		dataRetriever = _dataRet;

		this.valuesX = valuesX;
		server = connectionToServer;

		super.dataTrace = createFilteredBaseData();
	}

	
	@Override
	public IFilteredData createFilteredBaseData() {
		final int headerSize = exp.getTraceAttribute().dbHeaderSize;
		return new RemoteFilteredBaseData(valuesX, headerSize, server);
	}

	/**
	 * This performs the network request and does a small amount of processing on the reply. Namely, it does 
	 * not decompress the traces. Instead, it returns threads that will do that work when executed.
	 */
	@Override
	public void fillTracesWithData (boolean changedBounds, int numThreadsToLaunch) 
		throws IOException {
		if (changedBounds) {
			
			DecompressionThread[] workThreads = new DecompressionThread[numThreadsToLaunch];
			final int ranksExpected = Math.min(attributes.getProcessInterval(), attributes.numPixelsV);
			
			final AtomicInteger ranksRemainingToDecompress = new AtomicInteger(ranksExpected);
			ptlService.setProcessTimeline(new ProcessTimeline[ranksExpected]);
			
			// The variable workToDo needs to be accessible across different objects:
			// RemoteDataRetriever: producer
			// DecompressionThread: consumer
			final ConcurrentLinkedQueue<DecompressionItemToDo> workToDo = new ConcurrentLinkedQueue<DecompressionItemToDo>();
			timelineToRender  = new ConcurrentLinkedQueue<Integer>();

			for (int i = 0; i < workThreads.length; i++) {

				workThreads[i] = new DecompressionThread(ptlService, getScopeMap(),
						attributes, workToDo, timelineToRender, ranksRemainingToDecompress,
						new DecompressionThreadListener());
				workThreads[i].start();
			}
			

			dataRetriever.getData(attributes, getScopeMap(), workToDo);
		}
	}

	
	
	@Override
	public void dispose() {
		//closeDB();
		super.dispose();

	}
	
	@Override
	public void closeDB() {
		try {
			Debugger.printDebug(1, "Closing the connection");
			dataRetriever.closeConnection();
		} catch (IOException e) {
			System.out.println("Could not close the connection.");
		}
	}


	public ProcessTimeline getNextTrace(AtomicInteger lineNum, int totalLines, 
			ImageTraceAttributes attributes, boolean changedBounds, IProgressMonitor monitor) {
		Integer nextIndex;

		if (changedBounds) {
			int i = 0;
			
			// TODO: Should this be implemented with real locking?
			while ((nextIndex = timelineToRender.poll()) == null) {
				if (monitor.isCanceled())
					return null;
				
				//Make sure a different thread didn't get the last one while 
				//this thread was waiting:
				if (lineNum.get() >= ptlService.getNumProcessTimeline())
					return null;
				
				// check for the timeout
				if (i++ > RemoteDataRetriever.getTimeOut()) {
					throw new RuntimeException("Timeout in while waiting for data from decompression thread");
					//return null;
				}
				try {
					Thread.sleep(RemoteDataRetriever.getTimeSleep());

				} catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException("Thread is interrupted");
				}
			}
			lineNum.getAndIncrement();
		}
		else{
			nextIndex = lineNum.getAndIncrement();
			if (nextIndex >= ptlService.getNumProcessTimeline())
				return null;
		}
		return ptlService.getProcessTimeline(nextIndex.intValue());
	}

	
	private class DecompressionThreadListener implements IThreadListener
	{

		@Override
		public void notify(String msg) {
			throw new RuntimeException(msg);
			//System.err.println("Error in Decompression: " + msg);
		}
		
	}

	@Override
	public String getName() {
		return exp.getConfiguration().getName(ExperimentConfiguration.NAME_EXPERIMENT);
	}
}
