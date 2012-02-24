//////////////////////////////////////////////////////////////////////////
//																		//
//	ExperimentConfiguration.java										//
//																		//
//	experiment.ExperimentConfiguration -- an experiment's config data	//
//	Last edited: May 18, 2001 at 2:15 pm								//
//																		//
//	(c) Copyright 2001 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment;

import java.io.File;
import java.util.List;




//////////////////////////////////////////////////////////////////////////
//	CLASS EXPERIMENT-CONFIGURATION										//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 * An object containing an HPCView experiment's configuration data.
 *
 */


public class ExperimentConfiguration extends Object
{


/** The experiment's user visible name. */
protected String name;


/** The experiment's user visible name. */
protected File[] searchPaths;




//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates an empty ExperimentConfiguration.
 ************************************************************************/
	
public ExperimentConfiguration()
{
	this.name = "<Empty Experiment>";
}




//////////////////////////////////////////////////////////////////////////
//	ACCESS TO CONFIGURATION												//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the experiment's user visible name.
 ************************************************************************/
	
public String getName()
{
	return this.name;
}




/*************************************************************************
 *	Sets the experiment's user visible name.
 ************************************************************************/
	
public void setName(String name)
{
	this.name = name;
}




/*************************************************************************
 *	Returns the number of search paths in the experiment.
 ************************************************************************/
	
public int getSearchPathCount()
{
	if (this.searchPaths != null)
		return this.searchPaths.length;
	else
		return 0;
}




/*************************************************************************
 *	Returns the search path with a given index.
 ************************************************************************/
	
public File getSearchPath(int index)
{
	return this.searchPaths[index];
}




/*************************************************************************
 *	Sets the experiment's search paths.
 ************************************************************************/
	
public void setSearchPaths(List<File> pathList)
{
	this.searchPaths = (File[]) pathList.toArray(new File[0]);
}




}







