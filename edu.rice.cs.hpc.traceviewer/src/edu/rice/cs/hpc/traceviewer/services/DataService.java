package edu.rice.cs.hpc.traceviewer.services;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;


public class DataService extends AbstractSourceProvider 
{

	final static public String DATA_PROVIDER = "edu.rice.cs.hpc.traceviewer.services.DataService.data";
	final static public String DATA_UPDATE = "edu.rice.cs.hpc.traceviewer.services.DataService.update";
	
	final static public String DATA_AVAILABLE = "ENABLED";
	final static public String DATA_UNAVAILABLE = "DISABLED";
	
	private SpaceTimeDataController data;
	

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#dispose()
	 */
	public void dispose() {}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getCurrentState()
	 */
	public Map getCurrentState() {

		Map<String, Object> map = new HashMap<String, Object>(2);
		map.put(DATA_PROVIDER, getValue());
		map.put(DATA_UPDATE, data);
		
		return map;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.ISourceProvider#getProvidedSourceNames()
	 */
	public String[] getProvidedSourceNames() {

		return new String[] {DATA_PROVIDER, DATA_UPDATE};
	}
	
	/***
	 * set the updated data
	 * @param data
	 */
	public void setData( SpaceTimeDataController data ) {
		this.data = data;
		fireSourceChanged(ISources.WORKBENCH, DATA_PROVIDER, DATA_AVAILABLE);
	}
	
	/***
	 * broadcast updated data
	 */
	public void broadcastUpdate( Object updatedData ) {
		if (updatedData == null)
			fireSourceChanged(ISources.WORKBENCH, DATA_UPDATE, data);
		else
			fireSourceChanged(ISources.WORKBENCH, DATA_UPDATE, updatedData);
	}
	
	/***
	 * retrieve the current data
	 * @return
	 */
	public SpaceTimeDataController getData() {
		return data;
	}
	
	/**
	 * return if the data is available or not
	 * 
	 * @return true if the file is opened and data is available.
	 * false otherwise
	 */
	public boolean isDataAvailable() {
		return getData() != null;
	}
 
	/****
	 * retrieve the value of the data if they are available or not
	 * 
	 * @return {@link DATA_AVAILABLE} if the data is ready
	 * 	return {@link DATA_UNAVAILABLE} otherwise
	 */
	private String getValue() {
		return (data != null)? DATA_AVAILABLE : DATA_UNAVAILABLE;
	}
}
