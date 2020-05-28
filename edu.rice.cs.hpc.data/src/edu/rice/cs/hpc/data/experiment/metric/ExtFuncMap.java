/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import com.graphbuilder.math.FuncMap;
import com.graphbuilder.math.func.Function;

/**
 * @author laksonoadhianto
 *
 */
public class ExtFuncMap extends FuncMap 
{

	/**
	 * @param caseSensitive
	 */
	public ExtFuncMap(boolean caseSensitive) {
		super(caseSensitive);
	}

	/***
	 * construct list of function specifically for hpcdata
	 * @param metrics: list of metrics
	 * @param rootscope: a root scope (any root scope will do)
	 */
	public ExtFuncMap() {
		super(false);
		this.init();
	}

	public void init() {

		StdDevFunction fctStdDev = new StdDevFunction();

		this.setFunction("stdev", fctStdDev);
		this.loadDefaultFunctions();
	}
	
	public String []getFunctionNames() {
		return super.getFunctionNames();
	}
	
	public String []getFunctionNamesWithType() {
		Function []list = getFunctions();
		String []names  = new String[list.length];
		
		for(int i=0; i<list.length; i++) {
			names[i] = list[i].toString();
		}
		return names;
	}
}
