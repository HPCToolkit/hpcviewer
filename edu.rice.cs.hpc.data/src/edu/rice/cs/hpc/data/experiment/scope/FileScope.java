//////////////////////////////////////////////////////////////////////////
//																		//
//	FileScope.java														//
//																		//
//	experiment.scope.FileScope -- a source file in an experiment		//
//	Last edited: August 10, 2001 at 3:20 pm								//
//																		//
//	(c) Copyright 2001 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment.scope;

import edu.rice.cs.hpc.data.experiment.scope.visitors.IScopeVisitor;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.util.*;




//////////////////////////////////////////////////////////////////////////
//	CLASS FILE-SCOPE													//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 * A file scope in an HPCView experiment.
 *
 */


public class FileScope extends Scope
{

public static final String UNKNOWN_FILE = "<unknown file>";


//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates a FileScope.
 *
 *	The <code>lineMap</code> instance variable is not initialized here
 *	because it is computed on demand.
 *
 ************************************************************************/
	
public FileScope(RootScope root, SourceFile sourceFile, int idFile)
{
	super(root, sourceFile, idFile);
//	this.id = "FileScope";
}


public Scope duplicate() {
    return new FileScope(this.root, sourceFile, this.flat_node_index);
}

//////////////////////////////////////////////////////////////////////////
//	SCOPE DISPLAY														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the user visible name for this file scope.
 ************************************************************************/
	
public String getName()
{
	return this.sourceFile.getName();
}




/*************************************************************************
 *	Returns the tool tip for this scope.
 ************************************************************************/
	
public String getToolTip()
{
	boolean available = this.getSourceFile().isAvailable();
	return (available ? Strings.SOURCE_FILE_AVAILABLE : Strings.SOURCE_FILE_UNAVAILABLE);
}




//////////////////////////////////////////////////////////////////////////
//	ACCESS TO SCOPE														//
//////////////////////////////////////////////////////////////////////////




//////////////////////////////////////////////////////////////////////////
//support for visitors													//
//////////////////////////////////////////////////////////////////////////

public void accept(IScopeVisitor visitor, ScopeVisitType vt) {
	visitor.visit(this, vt);
}

}
