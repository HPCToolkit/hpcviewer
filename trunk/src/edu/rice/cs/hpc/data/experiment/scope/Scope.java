//////////////////////////////////////////////////////////////////////////
//									//
//	Scope.java							//
//									//
//	experiment.scope.Scope -- a scope in an experiment		//
//	Last edited: October 10, 2001 at 4:03 pm			//
//									//
//	(c) Copyright 2001 Rice University. All rights reserved.	//
//									//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment.scope;


import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.Metric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.ScopeVisitor;
import edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.util.*;
import edu.rice.cs.hpc.data.experiment.metric.DerivedMetric; // laks: add derived metric feature

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jface.viewers.TreeNode;


 
//////////////////////////////////////////////////////////////////////////
//	CLASS SCOPE							//
//////////////////////////////////////////////////////////////////////////

/**
 *
 * A scope in an HPCView experiment.
 *
 * FIXME: do we want to merge the functionality of Scope and Scope.Node?
 * it's kind of irritating to have the two things be distinct and having
 * objects which point at each other makes me a little uneasy.
 */


public abstract class Scope
{
static protected int idMax = 0;

/** The experiment owning this scope. */
protected Experiment experiment;

/** The source file containing this scope. */
protected SourceFile sourceFile;

/** the scope identifier */
protected int id;

/** The first line number of this scope. */
protected int firstLineNumber;

/** The last line number of this scope. */
protected int lastLineNumber;

/** The tree node connecting this scope to its parent and children. */
protected Scope.Node treeNode;

/** The metric values associated with this scope. */
protected MetricValue[] metrics;

/** The the type of scope this is. */
// Laks 2009.05.05: why I commented this var: I believe this id var is useless and we
//				still can identify the class type by using instanceof or by adaptor method 
//protected String id;

/** source citation */
protected String srcCitation;

/** special marker used for halting during debugging. */
protected boolean stop;
/**
 * FIXME: this variable is only used for the creation of callers view to count
 * 			the number of instances. To be removed in the future
 */
public int iCounter = 0;
// --------------------------

static public final int SOURCE_CODE_UNKNOWN = 0;
static public final int SOURCE_CODE_AVAILABLE = 1;
static public final int SOURCE_CODE_NOT_AVAILABLE= 2;
public int iSourceCodeAvailability = Scope.SOURCE_CODE_UNKNOWN;

//////////////////////////////////////////////////////////////////////////
//	PUBLIC CONSTANTS						//
//////////////////////////////////////////////////////////////////////////


/** The value used to indicate "no line number". */
public static final int NO_LINE_NUMBER = -169; // any negative number other than -1


//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates a Scope object with associated source line range.
 ************************************************************************/
	
public Scope(Experiment experiment, SourceFile file, int first, int last, int scopeID)
{
	// creation arguments
	this.experiment = experiment;
	this.sourceFile = file;
	this.firstLineNumber = first;
	this.lastLineNumber = last;

	// scope tree representation
	this.treeNode = new Scope.Node(this);
	//this.treeNode.setUserObject(this);
	this.stop = false;
	this.srcCitation = null;
	this.id = scopeID;
}




/*************************************************************************
 *	Creates a Scope object with associated source file.
 ************************************************************************/
	
public Scope(Experiment experiment, SourceFile file, int scopeID)
{
	this(experiment, file, Scope.NO_LINE_NUMBER, Scope.NO_LINE_NUMBER, scopeID);
}




/*************************************************************************
 *	Creates a Scope object with no associated source file.
 ************************************************************************/
	
public Scope(Experiment experiment)
{
	this(experiment, null, Scope.NO_LINE_NUMBER, Scope.NO_LINE_NUMBER, idMax);
	idMax++;
}

public int hashCode() {
	/*
	SourceFile objFile = this.getSourceFile();
	if (objFile != null) {
		return objFile.getFileID();
		//return objFile.getName().hashCode()  ^ this.firstLineNumber;
	} else {
		System.err.println("ERROR Scope hashcode: "+this.getName());
		return 0;
	} */
	return this.id;
	//return this.sourceFile.getName().hashCode() ^ this.firstLineNumber;
}


//////////////////////////////////////////////////////////////////////////
// DUPLICATION														//
//////////////////////////////////////////////////////////////////////////



/*************************************************************************
 *	Creates a Scope object with no associated source file.
 ************************************************************************/
	
public abstract Scope duplicate();



//////////////////////////////////////////////////////////////////////////
//	SCOPE DISPLAY														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the user visible name for this scope.
 *
 *	Subclasses should override this to implement useful names.
 *
 ************************************************************************/
	
public abstract String getName();



/*************************************************************************
 *	Returns the short user visible name for this scope.
 *
 *	This name is only used in tree views where the scope's name appears
 *	in context with its containing scope's name.
 *
 *	Subclasses may override this to implement better short names.
 *
 ************************************************************************/
	
public String getShortName()
{
	return this.getName();
}




/*************************************************************************
 *	Returns the tool tip for this scope.
 ************************************************************************/
	
public String getToolTip()
{
	return this.getSourceCitation();
}




/*************************************************************************
 *	Converts the scope to a <code>String</code>.
 *
 *	<p>
 *	This method is for the convenience of <code>ScopeTreeModel</code>,
 *	which passes <code>Scope</code> objects to the default tree cell
 *	renderer.
 *
 ************************************************************************/
	
public String toString()
{
	return this.getName();
}




/*************************************************************************
 *	Returns a display string describing the scope's source code location.
 ************************************************************************/
	
protected String getSourceCitation()
{
	if (this.srcCitation == null)  {
		// some scopes such as load module, doesn't have a source code file (they are binaries !!)
		// this hack will return the name of the scope instead of the citation file
		if (this.sourceFile == null) {
			return this.getName();
		}
		String cite;

		// we must display one-based line numbers
		int first1 = 1 + this.firstLineNumber;
		int last1 = 1 + this.lastLineNumber;

		if(this.firstLineNumber == Scope.NO_LINE_NUMBER)
			cite = this.getSourceFile().getName();
		else if(this.firstLineNumber == this.lastLineNumber)
			cite = this.getSourceFile().getName() + ": " + first1;
		else
			cite = this.getSourceFile().getName() + ": " + first1 + "-" + last1;
		
		srcCitation = cite.intern();
	}

	return srcCitation;
}




/*************************************************************************
 *	Returns a display string describing the scope's line number range.
 ************************************************************************/
	
protected String getLineNumberCitation()
{
	String cite;

	// we must display one-based line numbers
	int first1 = 1 + this.firstLineNumber;
	int last1  = 1 + this.lastLineNumber;

	if(this.firstLineNumber == Scope.NO_LINE_NUMBER)
		cite = "";	// TEMPORARY: is this the right thing to do?
	else if(this.firstLineNumber == this.lastLineNumber)
		cite = "line" + " " + first1;
	else
		cite = "lines" + " " + first1 + "-" + last1;

	return cite;
}




//////////////////////////////////////////////////////////////////////////
//	ACCESS TO SCOPE														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the source file of this scope.
 *
 *	<p>
 *	<em>TEMPORARY: This assumes that each scope "has" (i.e. intersects)
 *	at most one source file -- not true for discontiguous scopes.</em>
 *
 ************************************************************************/
	
public SourceFile getSourceFile()
{
	return this.sourceFile;
	//return this.experiment.getSourceFile(this.idSourceFile);
}

/*
public int getFileIndex() 
{
	return this.idSourceFile;
}
*/


/*************************************************************************
 *	Returns the first line number of this scope in its source file.
 *
 *	<p>
 *	<em>TEMPORARY: This assumes that each scope "has" (i.e. intersects)
 *	at most one source file -- not true for discontiguous scopes.</em>
 *
 ************************************************************************/
	
public int getFirstLineNumber()
{
	return this.firstLineNumber;
}




/*************************************************************************
 *	Returns the last line number of this scope in its source file.
 *
 *	<p>
 *	<em>TEMPORARY: This assumes that each scope "has" (i.e. intersects)
 *	at most one source file -- not true for discontiguous scopes.</em>
 *
 ************************************************************************/
	
public int getLastLineNumber()
{
	return this.lastLineNumber;
}




//////////////////////////////////////////////////////////////////////////
//	SCOPE HIERARCHY														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Returns the parent scope of this scope.
 ************************************************************************/
	
public Scope getParentScope()
{
	Scope.Node parent = (Scope.Node) this.treeNode.getParent();
	return ((parent != null) ? parent.getScope() : null);
}

// Laks 2009.05.05: remove unused methods
/*
public String getScopeType()
{
	return id;
}
*/

/*************************************************************************
 *	Sets the parent scope of this scope.
 ************************************************************************/
	
public void setParentScope(Scope parentScope)
{
	this.treeNode.setParent(parentScope.treeNode);
}




/*************************************************************************
 *	Returns the number of subscopes within this scope.
 ************************************************************************/
	
public int getSubscopeCount()
{
	return this.treeNode.getChildCount();
}




/*************************************************************************
 *	Returns the subscope at a given index.
 ************************************************************************/
	
public Scope getSubscope(int index)
{
	Scope.Node child = (Scope.Node) this.treeNode.getChildAt(index);
	return child.getScope();
}


/*************************************************************************
 *	Adds a subscope to the scope.
 ************************************************************************/
	
public void addSubscope(Scope subscope)
{
	this.treeNode.add(subscope.treeNode);
}

/*************************************************************************
 *	Removes a subscope of the current subscope based on index.
 ************************************************************************/
	/*
public void removeSubscope(int index)
{
	 this.treeNode.remove(index);
}
*/

/*************************************************************************
 *	Removes a subscope of the current subscope based on subscope.
 ************************************************************************/
	/*
public boolean removeSubscope(Scope subscope)
{
    int numberOfSubscopes = this.getSubscopeCount();
    int i;
    for (i=0; i< numberOfSubscopes; i++) {
	if (subscope == this.getSubscope(i)) {
	    this.treeNode.remove(i);
	    return true;
	}
    }
    return false;
}
*/

/*************************************************************************
 *	Returns the <code>Scope.Node</code> associated with this scope.
 ************************************************************************/
	
public Scope.Node getTreeNode()
{
	return this.treeNode;
}




//////////////////////////////////////////////////////////////////////////
//	ACCESS TO METRICS													//
//////////////////////////////////////////////////////////////////////////

public boolean hasMetrics() 
{
	return (metrics != null);
}

public boolean hasNonzeroMetrics() {
	if (this.hasMetrics())
		for (int i = 0; i< this.metrics.length; i++) {
			MetricValue m = this.getMetricValue(i);
			if (m != MetricValue.NONE && m.getValue() != 0.0) return true;
		}
	return false;
}

//////////////////////////////////////////////////////////////////////////
//   DEBUGGING HOOK 													//
//////////////////////////////////////////////////////////////////////////

public boolean stopHere()
{
	return stop;
}

//////////////////////////////////////////////////////////////////////////
// EXPERIMENT DATABASE 													//
//////////////////////////////////////////////////////////////////////////
public Experiment getExperiment() {
	return experiment;
}

public void setExperiment(Experiment exp) {
	this.experiment = exp;
}


//===================================================================
//						METRICS
//===================================================================

/*************************************************************************
 *	Returns the value of a given metric at this scope.
 ************************************************************************/
	
public MetricValue getMetricValue(BaseMetric metric)
{
	int index = metric.getIndex();
	MetricValue value;

	if(this.metrics != null && index < this.metrics.length)
	{
		value = this.metrics[index];

		// compute percentage if necessary
		Scope root = this.experiment.getRootScope();
		if((this != root) && (! value.isPercentAvailable()))
		{
			MetricValue total = root.getMetricValue(metric);
			if(total.isAvailable())
				value.setPercentValue(value.getValue()/total.getValue());
/*****
	                else {
                               // Don't now why (total==MetricValue.NONE) but we still have value!!!
                               // Fix it later !!!! FMZ
				value.setPercentValue(value.getValue()/total.getValue());
		}
******/
		} 

	}
	else
		value = MetricValue.NONE;

	return value;
}


/***
  overload the method to take-in the index ---FMZ
***/

public MetricValue getMetricValue(int index)
{
	MetricValue value;
        if(this.metrics != null && index < this.metrics.length)
           {
                value = this.metrics[index];
           }
        else
                value = MetricValue.NONE;

        return value;
}


/*************************************************************************
 *	Sets the value of a given metric at this scope.
 ************************************************************************/
	
public void setMetricValue(int index, MetricValue value)
{
	ensureMetricStorage();
	this.metrics[index] = value;
}

public void accumulateMetrics(Scope source, MetricValuePropagationFilter filter, int nMetrics) {
	for (int i = 0; i< nMetrics; i++) {
		this.accumulateMetric(source, i, i, filter);
	}
}

public void accumulateMetric(Scope source, int src_i, int targ_i, MetricValuePropagationFilter filter) {
	if (filter.doPropagation(source, this, src_i, targ_i)) {
		MetricValue m = source.getMetricValue(src_i);
		if (m != MetricValue.NONE && m.getValue() != 0.0) {
			this.accumulateMetricValue(targ_i, m.getValue());
		}
	}
}

/*************************************************************************
 * Laks: accumulate a metric value (used to compute aggregate value)
 * @param index
 * @param value
 ************************************************************************/
public void accumulateMetricValue(int index, double value)
{
	ensureMetricStorage();
	if (index >= this.metrics.length) return;
	MetricValue m = this.metrics[index];
	if (m == MetricValue.NONE) {
		this.metrics[index] = new MetricValue(value);
	} else {
		// TODO Could do non-additive accumulations here?
		m.setValue(m.getValue() + value);
	}
}


/*************************************************************************
 *	Makes sure that the scope object has storage for its metric values.
 ************************************************************************/
	
protected void ensureMetricStorage()
{
	if(this.metrics == null)
		this.metrics = this.makeMetricValueArray();
	// Expand if metrics not as big as experiment's (latest) metricCount
	if(this.metrics.length < this.experiment.getMetricCount()) {
		MetricValue[] newMetrics = this.makeMetricValueArray();
		for(int i=0; i<this.metrics.length; i++)
			newMetrics[i] = metrics[i];
		this.metrics = newMetrics;
	}
}




/*************************************************************************
 *	Gives the scope object storage for its metric values.
 ************************************************************************/
	
protected MetricValue[] makeMetricValueArray()
{
	int count = this.experiment.getMetricCount();
	MetricValue[] array = new MetricValue[count];
	for(int k = 0; k < count; k++)
		array[k] = MetricValue.NONE;
	return array;
}



/*************************************************************************
 * Copies defensively the metric array into a target scope
 * Used to implement duplicate() in subclasses of Scope  
 ************************************************************************/

public void copyMetrics(Scope targetScope) {
	if (this.metrics != null) {
		targetScope.ensureMetricStorage();
		for (int k=0; k<this.metrics.length && k<targetScope.metrics.length; k++) {
			MetricValue mine = null;
			MetricValue crtMetric = this.metrics[k];
			if ( crtMetric.isAvailable() && crtMetric.getValue() != 0.0) { // there is something to copy
				mine = new MetricValue();
				mine.setValue(crtMetric.getValue());

				if (crtMetric.isPercentAvailable()) {
					mine.setPercentValue(crtMetric.getPercentValue());
				} 
			} else {
				mine = MetricValue.NONE;
			}
			targetScope.metrics[k] = mine;
		}
	}
}

/*************************************************************************
 * Merge two metrics by setting the higher metrics into account
 * @param scope
 * @param filter
 *************************************************************************/
public void mergeMetric(Scope scope, MetricValuePropagationFilter filter) {
	if(scope == null || scope.metrics == null)
		return;
	ensureMetricStorage();
	for(int i=0;i<scope.metrics.length && i<this.metrics.length;i++) {
		if(filter.doPropagation(scope, this, i, i)) {
			MetricValue mTarget = scope.metrics[i];
			MetricValue mMine = this.metrics[i];
			if(mTarget.isAvailable()) {
				if( mMine.isAvailable() ) {
					// both are available, we need to find which one is bigger
					if(mMine.getValue() < mTarget.getValue()) {
						this.metrics[i] = new MetricValue(mTarget.getValue());
					}
				} else {
					this.metrics[i] = new MetricValue(mTarget.getValue());
				}
			}
		}
	}

}


//////////////////////////////////////////////////////////////////////////
//support for visitors													//
//////////////////////////////////////////////////////////////////////////

public void dfsVisitScopeTree(ScopeVisitor sv) {
	accept(sv, ScopeVisitType.PreVisit);
	int nKids = getSubscopeCount();
	for (int i=0; i< nKids; i++) {
		Scope childScope = getSubscope(i);
		childScope.dfsVisitScopeTree(sv);
	}
	accept(sv, ScopeVisitType.PostVisit);
}

public void accept(ScopeVisitor visitor, ScopeVisitType vt) {
	visitor.visit(this, vt);
}


//////////////////////////////////////////////////////////////////////////
//	INNER CLASS SCOPE.NODE												//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Class of nodes used to represent the scope tree.
 *
 *	<code>Scope.Node</code> instances provide typed access to their
 *	associated <code>Scope</code> objects and storage for per-node values
 *	needed by the user interface.
 *
 *	@see edu.rice.cs.hpcviewer.view.scope.ScopeTreeFilter
 *
 ************************************************************************/

	// @SuppressWarnings("serial")
	public static class Node extends DefaultMutableTreeNode //TreeNode
	{
		/**
		 * This public variable indicates if the node contains information about the source code file.
		 * If the boolean is true, then the filename can be retrieved from its scope
		 * @author laksono
		 */
		public boolean hasSourceCodeFile;
 		
		/** Constructs a new scope node. */
		
		/**
		 * Copy the scope into this node
		 * @param child
		 */
		public Node(Object child)
		{
			super(child);
			this.hasSourceCodeFile = false;
		}


		/** Returns the scope associated with this node. */
		public Scope getScope()
		{
			return (Scope) this.getUserObject();
		};

		public Object[] getChildren() {
			return this.children.toArray();
		}
	};
	
}
