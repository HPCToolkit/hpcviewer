package edu.rice.cs.hpc.traceviewer.data.util;

import java.util.Iterator;
import java.util.Map.Entry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import edu.rice.cs.hpc.common.util.AliasMap;
import edu.rice.cs.hpc.common.util.ProcedureClassData;
import edu.rice.cs.hpc.data.util.Util;


/***
 * 
 * Class to manage map between a procedure and its class
 * For instance, we want to class all MPI_* into mpi class, 
 * 	the get() method will then return all MPI functions into mpi
 *
 */
public class ProcedureClassMap extends AliasMap<String,ProcedureClassData> {

	static public final String CLASS_IDLE = "idle";
	static private final String FILENAME = "proc-class.map";
	private final Display display;
	
	public ProcedureClassMap(Display display) {
		this.display = display;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.common.util.ProcedureMap#getFilename()
	 */
	public String getFilename() {
		IPath path;
		if (Platform.isRunning()) {
			path = Platform.getLocation().makeAbsolute();			
			return path.append(FILENAME).makeAbsolute().toString();
		} else {
			return FILENAME;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.common.util.ProcedureMap#initDefault()
	 */
	public void initDefault() {

		Color COLOR_GRAY  = null;
		Color COLOR_WHITE = null;
		
		if (display != null) {
			COLOR_GRAY  = display.getSystemColor(SWT.COLOR_GRAY);
			COLOR_WHITE = display.getSystemColor(SWT.COLOR_WHITE); 
		}

		this.put("GPU_IDLE", 			  CLASS_IDLE, COLOR_GRAY);
		this.put("cudaEventSynchronize",  CLASS_IDLE, COLOR_GRAY);
		this.put("cudaStreamSynchronize", CLASS_IDLE, COLOR_GRAY);
		this.put("cudaDeviceSynchronize", CLASS_IDLE, COLOR_GRAY);
		this.put("cudaThreadSynchronize", CLASS_IDLE, COLOR_GRAY);
		this.put("cuStreamSynchronize",   CLASS_IDLE, COLOR_GRAY);
		this.put("cuEventSynchronize", 	  CLASS_IDLE, COLOR_GRAY);
		this.put("cuCtxSynchronize", 	  CLASS_IDLE, COLOR_GRAY);

		this.put("<no thread>", 		  CLASS_IDLE, COLOR_WHITE);
		this.put("<no activity>", 		  CLASS_IDLE, COLOR_WHITE);
	}
	
	public Object[] getEntrySet() {
		checkData();
		return data.entrySet().toArray();
	}

	public ProcedureClassData get(String key) {
		checkData();
		Iterator<Entry<String, ProcedureClassData>> iterator = data.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ProcedureClassData> entry = iterator.next();
			
			// convert glob pattern into regular expression
			//entry.getKey().replace("*", ".*").replace("?", ".?");
			String glob = Util.convertGlobToRegex(entry.getKey()); 
			if (key.equals(glob) || key.matches(glob)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public void put(String key, String val, Color image) {
		if (image != null)
		put(key,new ProcedureClassData(val,image));
	}

	public void put(String key, String val, RGB rgb) {
		put(key,new ProcedureClassData(val,rgb));
	}

	public ProcedureClassData remove(String key) {
		return data.remove(key);
	}
	

}
