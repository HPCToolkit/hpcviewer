package edu.rice.cs.hpc.data.experiment;

import java.io.File;
import edu.rice.cs.hpc.data.experiment.extdata.TraceAttribute;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.filter.IFilterData;
import edu.rice.cs.hpc.data.util.IUserData;

/*************************************
 * 
 * Experiment class without metrics
 *
 *************************************/
public class ExperimentWithoutMetrics extends BaseExperiment 
{
	/***** attributes of the traces ***/
	private TraceAttribute attribute;

	public void open(File fileExperiment, IUserData<String, String> userData)
			throws	Exception
	{
		super.open(fileExperiment, userData, false);
	}

	
	/******
	 * set the trace attributes (if the tracefile exist)
	 * @param _attribute
	 */
	public void setTraceAttribute(TraceAttribute _attribute) {
		this.attribute = _attribute;
	}


	/*****
	 * get the trace attributes. If the database has no traces,
	 * it return null
	 * 
	 * @return trace attributes
	 */
	public TraceAttribute getTraceAttribute() {
		return this.attribute;
	}

	/******
	 * hack : check the existence of tracefile by looking at the attribute
	 * (case for format 2.0) or the trace filename (case for format 3.0)
	 * @return
	 */
	public boolean tracefileExist() {
		boolean result = false;
		if (version.charAt(0) == '2') {
			// version 2.0
			result = (attribute != null);
		} else if (version.charAt(0) == '3') {
			// version 3.0
			result = getDbFilename(Db_File_Type.DB_TRACE) != null;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.IExperiment#duplicate()
	 */
	@Override
	public ExperimentWithoutMetrics duplicate() {

		ExperimentWithoutMetrics copy = new ExperimentWithoutMetrics();
		copy.configuration 	= configuration;
		copy.databaseRepresentation = databaseRepresentation;
		
		return copy;
	}


	@Override
	protected void filter_finalize(RootScope rootMain,
			IFilterData filter) {	}


	@Override
	protected void open_finalize() {
	}

}
