package edu.rice.cs.hpc.data.experiment;

import java.io.File;
import java.io.InputStream;
import java.util.EnumMap;

import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.visitors.DisposeResourcesVisitor;
import edu.rice.cs.hpc.data.experiment.scope.visitors.FilterScopeVisitor;
import edu.rice.cs.hpc.data.filter.IFilterData;
import edu.rice.cs.hpc.data.util.IUserData;


/***
 * Base abstract experiment that handle only call path.
 * 
 * No metric is associated in this experiment
 *
 */
public abstract class BaseExperiment implements IExperiment 
{
	/*****
	 *  Enumeration for database file type
	 */
	static public enum Db_File_Type {DB_SUMMARY, DB_TRACE, DB_PLOT, DB_THREADS};
	
	static final private String []DefaultDbFilename = {"summary.db", "trace.db", "plot.db", "threads.db"};
	
	/** The experiment's configuration. */
	protected ExperimentConfiguration configuration;

	protected RootScope rootScope;
	
	protected RootScope datacentricRootScope;
	
	/** version of the database **/
	protected String version;

	protected IDatabaseRepresentation databaseRepresentation;
	
	private EnumMap<Db_File_Type, String> db_filenames;
	
	private int min_cctid, max_cctid;
	private int filterNumScopes = 0, filterStatus;
	
	
	/***
	 * the root scope of the experiment
	 * 
	 * @param the root scope
	 */
	public void setRootScope(Scope rootScope) {
		this.rootScope = (RootScope) rootScope;
	}

	
	/***
	 * retrieve the root scope.
	 * 
	 * @return the root scope
	 */
	@Override
	public Scope getRootScope() {
		return rootScope;
	}

	
	public void setDatacentricRootScope(RootScope rootScope) {
		this.datacentricRootScope = rootScope;
	}
	
	
	public RootScope getDatacentricRootScope() {
		return datacentricRootScope;
	}
	
	
	
	/******
	 * set a database filename
	 * 
	 * @param file_index : enumerate database filename {@link Db_File_Type}
	 * @param filename : the name of the file
	 */
	public void setDBFilename(Db_File_Type file_index, String filename)
	{
		if (db_filenames == null)
		{
			db_filenames = new EnumMap<Db_File_Type, String>(Db_File_Type.class);
		}
		db_filenames.put(file_index, filename);
	}
	
	/****
	 * get the database file name 
	 * @param file_index : enumerate database filename {@link Db_File_Type}
	 * @return String file name
	 */
	public String getDbFilename(Db_File_Type file_index)
	{
		if (db_filenames != null)
		{
			return db_filenames.get(file_index);
		}
		return null;
	}
	
	public int getMajorVersion()
	{
		if (this.version == null)
			return 1;
		int ip = this.version.indexOf('.');
		return Integer.parseInt(this.version.substring(0, ip));
	}

	public int getMinorVersion() 
	{
		if (version == null) return 1;
		
		int ip = version.indexOf('.');
		return Integer.parseInt(version.substring(ip+1));
	}
	

	static public String getDefaultDatabaseName(Db_File_Type type)
	{
		return DefaultDbFilename[type.ordinal()];
	}
	
	static public String getDefaultDbTraceFilename()
	{
		return getDefaultDatabaseName(Db_File_Type.DB_TRACE);
	}


	public Object[] getRootScopeChildren() {
		RootScope root = (RootScope) getRootScope();

		if (root != null)
			return root.getChildren();
		else
			return null;
	}
	
	/****
	 * Retrieve the root scope of this experiment based on the root type
	 * (cct, callers tree or flat tree)
	 * 
	 * @param type : RootScopeType, type of the root
	 * @return the root scope if the type is found, <code>null</code> otherwise
	 */
	public RootScope getRootScope(RootScopeType type)
	{
		RootScope root = (RootScope) getRootScope();
		for (int i=0; i<root.getChildCount(); i++)
		{
			if (((RootScope)root.getChildAt(i)).getType() == type)
				return (RootScope) root.getChildAt(i);
		}
		return null;
	}
	
	/****
	 * open a local database
	 * 
	 * @param fileExperiment : file of the experiment xml
	 * @param userData : map of user preferences
	 * @param need_metric : whether we need to assign metrics or not
	 * @throws Exception
	 */
	public void open(File fileExperiment, IUserData<String, String> userData, boolean need_metric)
			throws	Exception
	{
		databaseRepresentation = new LocalDatabaseRepresentation(fileExperiment, userData, need_metric);
		databaseRepresentation.open(this);
		open_finalize();
	}
	
	
	/******
	 * This method is used for opening XML from a remote machine
	 *  
	 * @param expStream : remote input stream
	 * @param userData : customized user data
	 * @param name : the remote directory
	 * @throws Exception 
	 *****/
	public void open(InputStream expStream, IUserData<String, String> userData,
		String name) throws Exception {
		databaseRepresentation = new RemoteDatabaseRepresentation(expStream, userData, name);
		databaseRepresentation.open(this);
		open_finalize();
	}

	/******
	 * Reopening and reread the database and refresh the tree.<br/>
	 * If the database has not been opened, it throws an exception.
	 * @throws Exception
	 */
	public void reopen() throws Exception
	{
		if (databaseRepresentation != null)
		{
			databaseRepresentation.open(this);
			open_finalize();
		} else {
			throw new Exception("Database has not been opened.");
		}
	}

	/******
	 * set the database version
	 * 
	 * @param v : version of the database
	 */
	public void setVersion (String v) 
	{
		this.version = v;
	}


	/*************************************************************************
	 *	Returns the name of the experiment.
	 ************************************************************************/	
	public String getName()
	{
		return configuration.getName(ExperimentConfiguration.NAME_EXPERIMENT);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	/*************************************************************************
	 *	Sets the experiment's configuration.
	 *
	 *	This method is to be called only once, during <code>Experiment.open</code>.
	 *
	 ************************************************************************/
	public void setConfiguration(ExperimentConfiguration configuration)
	{
		this.configuration = configuration;
	}

	public ExperimentConfiguration getConfiguration()
	{
		return this.configuration;
	}


	/*************************************************************************
	 *	Returns the default directory from which to resolve relative paths.
	 ************************************************************************/

	public File getDefaultDirectory()
	{
		return getXMLExperimentFile().getParentFile();
	}

	public File getXMLExperimentFile() {
		return databaseRepresentation.getFile();
	}


	/*****
	 * disposing the experiment resources.
	 */
	public void dispose()
	{
		DisposeResourcesVisitor visitor = new DisposeResourcesVisitor();
		rootScope.dfsVisitScopeTree(visitor);
		this.rootScope = null;
	}


	/*************************************************************************
	 * Filter the cct 
	 * <p>caller needs to call postprocess to ensure the callers tree and flat
	 * tree are also filtered </p>
	 * @param filter
	 *************************************************************************/
	public int filter(IFilterData filter)
	{
		// TODO :  we assume the first child is the CCT
		final RootScope rootCCT = (RootScope) rootScope.getChildAt(0);

		// duplicate and filter the cct
		FilterScopeVisitor visitor = new FilterScopeVisitor(rootCCT, filter);
		rootCCT.dfsVisitFilterScopeTree(visitor);

		if (rootCCT.getType() == RootScopeType.CallingContextTree) {
			filter_finalize(rootCCT, filter);
		}
		filterNumScopes = visitor.numberOfFilteredScopes();
		filterStatus	= visitor.getFilterStatus();
		
		return filterNumScopes;
	}

	/****
	 * return the number of matched scopes from the filter.<br/>
	 * Note that this is NOT the number of removed scopes, but the number of
	 * scopes that match the filter pattern.
	 * @return int
	 */
	public int getNumberOfFilteredScopes() {
		return filterNumScopes;
	}
	
	public int getFilterStatus() {
		return filterStatus;
	}
	
	public void setMinMaxCCTID(int min, int max)
	{
		this.min_cctid = min;
		this.max_cctid = max;
	}
	
	public int getMinCCTID()
	{
		return min_cctid;
	}
	
	public int getMaxCCTID()
	{
		return max_cctid;
	}
	
	/************************************************************************
	 * In case the experiment has a CCT, continue to create callers tree and
	 * flat tree for the finalization.
	 * 
	 * @param rootCCT
	 * @param filter
	 ************************************************************************/
	abstract protected void filter_finalize(RootScope rootMain, IFilterData filter);

	abstract protected void open_finalize();

}
 