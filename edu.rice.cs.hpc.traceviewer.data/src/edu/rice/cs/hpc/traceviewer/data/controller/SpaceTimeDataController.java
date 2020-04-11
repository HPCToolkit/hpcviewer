package edu.rice.cs.hpc.traceviewer.data.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.services.ISourceProviderService;
import edu.rice.cs.hpc.common.util.ProcedureAliasMap;
import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.experiment.extdata.IBaseData;
import edu.rice.cs.hpc.data.experiment.extdata.IFilteredData;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.trace.TraceAttribute;
import edu.rice.cs.hpc.traceviewer.data.db.ImageTraceAttributes;
import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;
import edu.rice.cs.hpc.traceviewer.data.graph.CallPath;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimeline;
import edu.rice.cs.hpc.traceviewer.data.timeline.ProcessTimelineService;


/*******************************************************************************************
 * 
 * Class to store global information concerning the database and the trace.
 * The class is designed to work for both local and remote database. Any references have to 
 * 	be addressed to the methods of this class instead of the derived class to enable
 *  transparency.
 * 
 * @author Original authors: Sinchan Banarjee, Michael France, Reed Lundrum and Philip Taffet
 * 
 * Modification:
 * - 2013 Philip: refactoring into three classes : abstract (this class), local and remote
 * - 2014.2.1 Laksono: refactoring to make it as simple as possible and avoid code redundancy
 *
 *******************************************************************************************/
public abstract class SpaceTimeDataController 
{
	protected ImageTraceAttributes attributes;
	/**
	 * The minimum beginning and maximum ending time stamp across all traces (in
	 * microseconds)).
	 */
	protected long maxEndTime, minBegTime;


	protected ProcessTimelineService ptlService = null;

	
	/** The map between the nodes and the cpid's. */
	private HashMap<Integer, CallPath> scopeMap = null;
		
	/** The maximum depth of any single CallStackSample in any trace. */
	protected int maxDepth = 0;
	
	protected ColorTable colorTable = null;
	private boolean enableMidpoint  = true;
	
	protected IBaseData dataTrace = null;
	
	final protected ExperimentWithoutMetrics exp;
	
	// nathan's data index variable
	// TODO: we need to remove this and delegate to the inherited class instead !
	private int currentDataIdx;

	/***
	 * Constructor to create a data based on File. This constructor is more suitable
	 * for local database
	 * 
	 * @param _window : SWT window
	 * @param expFile : experiment file (XML format)
	 */
	public SpaceTimeDataController(IWorkbenchWindow _window, File expFile) 
			throws InvalExperimentException, Exception 
	{			
		exp = new ExperimentWithoutMetrics();

		// possible java.lang.OutOfMemoryError exception here
		exp.open(expFile, new ProcedureAliasMap());

		init(_window);
	}
	
	/*****
	 * Constructor to create a data based on input stream, which is convenient for remote database
	 * 
	 * @param _window : SWT window
	 * @param expStream : input stream
	 * @param Name : the name of the file on the remote server
	 * @throws InvalExperimentException 
	 *****/
	public SpaceTimeDataController(IWorkbenchWindow _window, InputStream expStream, String Name) 
			throws InvalExperimentException, Exception 
	{	
		exp = new ExperimentWithoutMetrics();

		// Without metrics, so param 3 is false
		exp.open(expStream, new ProcedureAliasMap(), Name);
		
		init(_window);
	}

	public void setDataIndex(int dataIndex) 
	{
		currentDataIdx = dataIndex;
	}
	
	
	public int getDataIndex()
	{
		return currentDataIdx;
	}
	
	public void resetPredefinedColor()
	{
		colorTable.resetPredefinedColor();
	}
	
	/******
	 * Initialize the object
	 * 
	 * @param _window
	 * @throws Exception 
	 ******/
	private void init(final IWorkbenchWindow window) 
			throws InvalExperimentException 
	{	
		final Display display = Display.getDefault();
		display.syncExec(new Runnable() {

			@Override
			public void run() {
				
				// initialize color table
				colorTable = new ColorTable();
				
				// tree traversal to get the list of cpid, procedures and max depth
				TraceDataVisitor visitor = new TraceDataVisitor(colorTable);
				RootScope root = exp.getRootScope(RootScopeType.CallingContextTree);
				root.dfsVisitScopeTree(visitor);

				maxDepth   = visitor.getMaxDepth();
				scopeMap   = visitor.getMap();
				
				// attributes initialization
				attributes 	 = new ImageTraceAttributes();

				ISourceProviderService sourceProviderService = (ISourceProviderService) window.getService(ISourceProviderService.class);
				ptlService = (ProcessTimelineService) sourceProviderService.getSourceProvider(ProcessTimelineService.PROCESS_TIMELINE_PROVIDER); 
			}			
		});
		final TraceAttribute trAttribute = exp.getTraceAttribute();
		
		if (trAttribute == null) {
			throw new InvalExperimentException("Database does not contain traces: " + exp.getDefaultDirectory());
		}
		minBegTime = trAttribute.dbTimeMin;
		maxEndTime = trAttribute.dbTimeMax;

	}

	public int getMaxDepth() 
	{
		return maxDepth;
	}
	
	
	private int getCurrentlySelectedProcess()
	{
		return attributes.getPosition().process;
	}
	
	/**
	 * {@link getCurrentlySelectedProcess()} returns something on [begProcess,
	 * endProcess-1]. We need to map that to something on [0, numTracesShown -
	 * 1]. We use a simple linear mapping:
	 * begProcess    -> 0,
	 * endProcess-1  -> numTracesShown-1
	 */
	public int computeScaledProcess() {
		int numTracesShown = Math.min(attributes.getProcessInterval(), attributes.numPixelsV);
		int selectedProc = getCurrentlySelectedProcess();
		
		double scaledDTProcess = (((double) numTracesShown -1 )
					/ ((double) attributes.getProcessInterval() - 1) * 
					(selectedProc - attributes.getProcessBegin()));
		return (int)scaledDTProcess;
	}


	/******
	 * get the depth trace of the current "selected" process
	 *  
	 * @return ProcessTimeline
	 */
	public ProcessTimeline getCurrentDepthTrace() {
		int scaledDTProcess = computeScaledProcess();
		return  ptlService.getProcessTimeline(scaledDTProcess);
	}
	
	/******
	 * return the service provider for timelines.
	 * 
	 * @return
	 */
	public ProcessTimelineService getProcessTimelineService() {
		return ptlService;
	}
	
	public IBaseData getBaseData(){
		return dataTrace;
	}

	/******************************************************************************
	 * Returns number of processes (ProcessTimelines) held in this
	 * SpaceTimeData.
	 ******************************************************************************/
	public int getTotalTraceCount() {
		return dataTrace.getNumberOfRanks();
	}
	
	public HashMap<Integer, CallPath> getScopeMap() {
		return scopeMap;
	}

	/******************************************************************************
	 * getter/setter trace attributes
	 * @return
	 ******************************************************************************/
	
	public int getPixelHorizontal() {
		return attributes.numPixelsH;
	}
	
	public BaseExperiment getExperiment() {
		return exp;
	}

	public ImageTraceAttributes getAttributes() {
		return attributes;
	}


	public int getHeaderSize() {
		final int headerSize = exp.getTraceAttribute().dbHeaderSize;
		return headerSize;
	}

	/*************************************************************************
	 * Returns width of the spaceTimeData: The width (the last time in the
	 * ProcessTimeline) of the longest ProcessTimeline.
	 ************************************************************************/
	public long getTimeWidth() {
		return maxEndTime - minBegTime;
	}

	public long getMaxEndTime() {
		return maxEndTime;
	}

	public long getMinBegTime() {
		return minBegTime;
	}

	/**************************************************************************
	 * retrieve the unit time per second of the trace
	 * The information is from experiment.xml
	 * 
	 * @return double unit time oer second
	 **************************************************************************/
	public double getUnitTimePerSecond() 
	{
		long unit_time = TraceAttribute.PER_NANO_SECOND;
		
		if (getExperiment().getMajorVersion() == 2) {
			if (getExperiment().getMinorVersion() < 2) {
				// old version of database: always microsecond
				unit_time = 1000000;
			} else {
				// new version of database:
				// - if the measurement is from old hpcrun: microsecond
				// - if the measurement is from new hpcrun: nanosecond
				
				BaseExperiment be = getExperiment();
				ExperimentWithoutMetrics exp = (ExperimentWithoutMetrics) be;
				unit_time = exp.getTraceAttribute().dbUnitTime;
			}
		}
		return (double)unit_time;
	}

	
	public ColorTable getColorTable() {
		return colorTable;
	}

	public void dispose() {
		colorTable.dispose();
	}

	public void setEnableMidpoint(boolean enable) {
		this.enableMidpoint = enable;
	}

	public boolean isEnableMidpoint() {
		return enableMidpoint;
	}

	//see the note where this is called in FilterRanks
	public IFilteredData getFilteredBaseData() {
		if (dataTrace instanceof IFilteredData)
			return (IFilteredData) dataTrace;
		return null;
	}
	/**
	 * changing the trace data, caller needs to make sure to refresh the views
	 * @param filteredBaseData
	 */
	public void setBaseData(IFilteredData filteredBaseData) {
		dataTrace = filteredBaseData;

		int endProcess = attributes.getProcessEnd();
		int begProcess = attributes.getProcessBegin();
		
		//Snap it back into the acceptable limits.
		if (endProcess > dataTrace.getNumberOfRanks())
			endProcess  = dataTrace.getNumberOfRanks();
		
		if (begProcess >= endProcess)
			begProcess = 0;
		
		attributes.setProcess(begProcess, endProcess);
	}

	public boolean isTimelineFilled() {
		return (ptlService != null && ptlService.isFilled());
	}
	
	
	////////////////////////////////////////////////////////////////////////////////
	// Abstract methods
	////////////////////////////////////////////////////////////////////////////////
	
	/*************************************************************************
	 * Retrieve the name of the database. The name can be either the path of
	 * the directory, or the name of the profiled application, or both.
	 * <p>
	 * Ideally the name should be unique to distinguish with other databases. 
	 * 
	 * @return String: the name of the database
	 *************************************************************************/
	abstract public String getName() ;

	public abstract void closeDB();

	public abstract IFilteredData createFilteredBaseData();

	public abstract void fillTracesWithData(boolean changedBounds, int numThreadsToLaunch)
			throws IOException;
}