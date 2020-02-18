//////////////////////////////////////////////////////////////////
//																//
//	Scope.java													//
//																//
//																//
//	(c) Copyright 2015 Rice University. All rights reserved.	//
//																//
//	$LastChangedDate$		
//  $LastChangedBy$ 					//
//////////////////////////////////////////////////////////////////




package edu.rice.cs.hpc.data.experiment.scope;


import java.io.IOException;
import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.BaseExperimentWithMetrics;
import edu.rice.cs.hpc.data.experiment.metric.AggregateMetric;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric.AnnotationType;
import edu.rice.cs.hpc.data.experiment.metric.IMetricValueCollection;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.filters.MetricValuePropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.visitors.FilterScopeVisitor;
import edu.rice.cs.hpc.data.experiment.scope.visitors.IScopeVisitor;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;


 
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


public abstract class Scope extends TreeNode
	implements IMetricScope
{
//////////////////////////////////////////////////////////////////////////
//PUBLIC CONSTANTS						//
//////////////////////////////////////////////////////////////////////////


/** The value used to indicate "no line number". */
public static final int NO_LINE_NUMBER = -169; // any negative number other than -1

static public final int SOURCE_CODE_UNKNOWN = 0;
static public final int SOURCE_CODE_AVAILABLE = 1;
static public final int SOURCE_CODE_NOT_AVAILABLE= 2;

//////////////////////////////////////////////////////////////////////////
// ATTRIBUTES
//////////////////////////////////////////////////////////////////////////


protected RootScope root;

/** The source file containing this scope. */
protected SourceFile sourceFile;

/** the scope identifier */
protected int flat_node_index;

/** The first line number of this scope. */
protected int firstLineNumber;

/** The last line number of this scope. */
protected int lastLineNumber;

/** The metric values associated with this scope. */
private IMetricValueCollection metrics;

/**
 * FIXME: this variable is only used for the creation of callers view to count
 * 			the number of instances. To be removed in the future
 */
private int iCounter;

//the cpid is removed in hpcviewer, but hpctraceview still requires it in order to dfs
protected int cpid;

public int iSourceCodeAvailability = Scope.SOURCE_CODE_UNKNOWN;



//////////////////////////////////////////////////////////////////////////
//	INITIALIZATION														//
//////////////////////////////////////////////////////////////////////////




/*************************************************************************
 *	Creates a Scope object with associated source line range.
 ************************************************************************/
	
public Scope(RootScope root, SourceFile file, int first, int last, int cct_id, int flat_id)
{
	super(cct_id);
	
	// creation arguments
	this.root = root;
	this.sourceFile = file;
	this.firstLineNumber = first;
	this.lastLineNumber = last;

//	this.srcCitation = null;
	this.flat_node_index = flat_id;
	this.cpid = -1;
	this.iCounter  = 0;
}


public Scope(RootScope root, SourceFile file, int first, int last, int cct_id, int flat_id, int cpid)
{
	this(root, file, first, last, cct_id, flat_id);
	this.cpid = cpid;
}



/*************************************************************************
 *	Creates a Scope object with associated source file.
 ************************************************************************/
	
public Scope(RootScope root, SourceFile file, int scopeID)
{
	this(root, file, Scope.NO_LINE_NUMBER, Scope.NO_LINE_NUMBER, scopeID, scopeID);
}

/***
 * get the flat (static) index of the scope. Some scopes may have the same
 * flat index if they are from the same address range. 
 * @return
 */
public int getFlatIndex() {
	return this.flat_node_index;
}

/***
 * retrieve the CCT index of this scope.<br/>
 * The index is theoretically unique, so it can be used as an ID.
 * @return
 */
public int getCCTIndex() {
	return (Integer) getValue();
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

//////////////////////////////////////////////////////////////////////////
// counter														//
//////////////////////////////////////////////////////////////////////////


public void incrementCounter() {
	this.iCounter++;
}

public void decrementCounter() {
	if (this.isCounterPositif())
		this.iCounter--;
	else {
		System.err.println("Scope [" + this.getCCTIndex() + "/" + this.flat_node_index + "] "  + this.getName() + " has non-positive counter");
	}
}

public void setCounter(int counter) {
	this.iCounter = counter;
}

public int getCounter() {
	return this.iCounter;
}

public boolean isCounterPositif() {
	return this.iCounter>0;
}

public boolean isCounterZero() {
	return (this.iCounter == 0);
}

/*************************************************************************
 * Returns which processor was active
 ************************************************************************/

public int getCpid()
{
	return cpid;
}


/*************************************************************************
 *	Sets the value of the cpid
 ************************************************************************/

public void setCpid(int _cpid)
{
	this.cpid = _cpid;
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


public int hashCode() {
	return System.identityHashCode(this);
}



/*************************************************************************
 *	Returns a display string describing the scope's source code location.
 ************************************************************************/
	
protected String getSourceCitation()
{
	return getSourceCitation(sourceFile, firstLineNumber, lastLineNumber);
}


private String getSourceCitation(SourceFile sourceFile, int line1, int line2)
{

		// some scopes such as load module, doesn't have a source code file (they are binaries !!)
		// this hack will return the name of the scope instead of the citation file
		if (sourceFile == null) {
			return this.getName();
		}
		return sourceFile.getName() + ": " + this.getLineOnlyCitation(line1, line2);

}




/*************************************************************************
 *	Returns a display string describing the scope's line number range.
 ************************************************************************/
	
protected String getLineNumberCitation()
{
	return this.getLineNumberCitation(firstLineNumber, lastLineNumber);
}


private String getLineNumberCitation(int line1, int line2)
{
	String cite;

	// we must display one-based line numbers
	int first1 = 1 + line1;
	int last1  = 1 + line2;

	if(line1 == Scope.NO_LINE_NUMBER) {
		cite = "";	// TEMPORARY: is this the right thing to do?
	} else if(line1 == line2)
		cite = "line" + " " + first1;
	else
		cite = "lines" + " " + first1 + "-" + last1;

	return cite;
}


private String getLineOnlyCitation(int line1, int line2) {
	String cite;

	// we must display one-based line numbers
	int first1 = 1 + line1;
	int last1  = 1 + line2;

	if(line1 == Scope.NO_LINE_NUMBER) {
		cite = "";	// TEMPORARY: is this the right thing to do?
	} else if(line1 == line2)
		cite = String.valueOf(first1);
	else
		cite = first1 + "-" + last1;

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
}


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
	return (Scope) this.getParent();
}


/*************************************************************************
 *	Sets the parent scope of this scope.
 ************************************************************************/
	
public void setParentScope(Scope parentScope)
{
	this.setParent(parentScope);
}




/*************************************************************************
 *	Returns the number of subscopes within this scope.
 ************************************************************************/
	
public int getSubscopeCount()
{
	return this.getChildCount();
}




/*************************************************************************
 *	Returns the subscope at a given index.
 ************************************************************************/
	
public Scope getSubscope(int index)
{
	Scope child = (Scope) this.getChildAt(index);
	return child;
}


/*************************************************************************
 *	Adds a subscope to the scope.
 ************************************************************************/
	
public void addSubscope(Scope subscope)
{
	this.add(subscope);
}



//////////////////////////////////////////////////////////////////////////
// EXPERIMENT DATABASE 													//
//////////////////////////////////////////////////////////////////////////
public BaseExperiment getExperiment() {
	return root.getExperiment();
}


//////////////////////////////////////////////////////////////////////////
// Access to RootScope 													//
//////////////////////////////////////////////////////////////////////////

/*******
 * set the root scope
 * @param root
 */
public void setRootScope(RootScope root)
{
	this.root = root;
}

/****
 * get the root scope of this scope
 * @return
 */
public RootScope getRootScope()
{
	return root;
}




//////////////////////////////////////////////////////////////////////////
//ACCESS TO METRICS													//
//////////////////////////////////////////////////////////////////////////

/***************************************************************************
 * Check whether the scope has at least a metric value
 * @return
 ***************************************************************************/
private boolean hasMetrics() 
{
	ensureMetricStorage();
	return metrics.hasMetrics(this);
}

/***************************************************************************
 * check whether the scope has at least a non-zero metric value
 * @return true if the scope has at least a non-zero metric value
 ***************************************************************************/
public boolean hasNonzeroMetrics() {
	if (this.hasMetrics())
		for (int i = 0; i< metrics.size(); i++) {
			MetricValue m = this.getMetricValue(i);
			if (!MetricValue.isZero(m))
				return true;
		}
	return false;
}



/*************************************************************************
 *	Returns the value of a given metric at this scope.
 ************************************************************************/
	
public MetricValue getMetricValue(BaseMetric metric)
{
	int index = metric.getIndex();
	MetricValue value = getMetricValue(index);

	// compute percentage if necessary
	if (metric.getAnnotationType() == AnnotationType.PERCENT) {
		if(MetricValue.isAvailable(value) && (! MetricValue.isAnnotationAvailable(value)))
		{
			if (this instanceof RootScope) {
				MetricValue.setAnnotationValue(value, 1.0);
			} else {
				MetricValue total = root.getMetricValue(metric);
				if(MetricValue.isAvailable(total))
					MetricValue.setAnnotationValue(value, MetricValue.getValue(value)/MetricValue.getValue(total));
			}
		} 
	}

	return value;
}


/***************************************************************************
  overload the method to take-in the index ---FMZ
 ***************************************************************************/

public MetricValue getMetricValue(int index)
{
	ensureMetricStorage();
	MetricValue value = metrics.getValue(this, index);

    return value;
}

public MetricValue getRootMetricValue(BaseMetric metric)
{
	return getRootScope().getMetricValue(metric);
}

/*************************************************************************
 *	Sets the value of a given metric at this scope.
 ************************************************************************/
public void setMetricValue(int index, MetricValue value)
{
	ensureMetricStorage();
	metrics.setValue(index, value);
}

/*************************************************************************
 *	Add the metric cost from a source with a certain filter for all metrics
 ************************************************************************/
public void accumulateMetrics(Scope source, MetricValuePropagationFilter filter, int nMetrics) {
	for (int i = 0; i< nMetrics; i++) {
		this.accumulateMetric(source, i, i, filter);
	}
}

/*************************************************************************
 *	Add the metric cost from a source with a certain filter for a certain metric
 ************************************************************************/
public void accumulateMetric(Scope source, int src_i, int targ_i, MetricValuePropagationFilter filter) {
	if (filter.doPropagation(source, this, src_i, targ_i)) {
		MetricValue m = source.getMetricValue(src_i);
		if (m != MetricValue.NONE && Double.compare(MetricValue.getValue(m), 0.0) != 0) {
			this.accumulateMetricValue(targ_i, MetricValue.getValue(m));
		}
	}
}

/*************************************************************************
 * Laks: accumulate a metric value (used to compute aggregate value)
 * @param index
 * @param value
 ************************************************************************/
private void accumulateMetricValue(int index, double value)
{
	ensureMetricStorage();
	if (index >= metrics.size()) 
		return;

	MetricValue m = metrics.getValue(this, index);
	if (m == MetricValue.NONE) {
		MetricValue mv = new MetricValue(value);
		metrics.setValue(index, mv);
	} else {
		// TODO Could do non-additive accumulations here?
		MetricValue.setValue(m, MetricValue.getValue(m) + value);
	}
}



/***************************************************************************
 * retrieve the default metrics
 * @return
 ***************************************************************************/
public IMetricValueCollection getMetricValues() {
	// bug fix: we need to ensure that the metrics exist before giving
	// to the outside world
	ensureMetricStorage();
	return this.metrics;
}

/***************************************************************************
 * set the default metrics
 * @param values
 ***************************************************************************/
public void setMetricValues(IMetricValueCollection values) {	
	this.metrics = values;
}



/**************************************************************************
 * combining metric from source. use this function to combine metric between
 * 	different views
 * @param source
 * @param filter
 **************************************************************************/
public void combine(Scope source, MetricValuePropagationFilter filter) {
	
	final BaseExperimentWithMetrics _exp = (BaseExperimentWithMetrics) getExperiment();

	int nMetrics = _exp.getMetricCount();
	for (int i=0; i<nMetrics; i++) {
		BaseMetric metric = _exp.getMetric(i);
		if (metric instanceof AggregateMetric) {
			//--------------------------------------------------------------------
			// aggregate metric need special treatment when combining two metrics
			//--------------------------------------------------------------------
			AggregateMetric aggMetric = (AggregateMetric) metric;
			if (filter.doPropagation(source, this, i, i)) {
				aggMetric.combine(source, this);
			}
		} else {
			this.accumulateMetric(source, i, i, filter);
		}
	}
}


/**********************************************************************************
 * Safely combining metrics from another scope. 
 * This method checks if the number of metrics is the same as the number of metrics
 * 	in the experiment. If not, it generates additional metrics
 * this method is used for dynamic metrics creation such as when computing metrics
 * 	in caller view (if a new metric is added)
 * @param source
 * @param filter
 **********************************************************************************/
public void safeCombine(Scope source, MetricValuePropagationFilter filter) {
	ensureMetricStorage();
	this.combine(source, filter);
}

/*************************************************************************
 *	Makes sure that the scope object has storage for its metric values.
 ************************************************************************/
	
protected void ensureMetricStorage()
{	
	if (metrics == null)
	{
		try {
			metrics = root.getMetricValueCollection(this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


/*************************************************************************
 * Copies defensively the metric array into a target scope
 * Used to implement duplicate() in subclasses of Scope  
 ************************************************************************/

public void copyMetrics(Scope targetScope, int offset) {

	if (!this.hasMetrics())
		return;
	
	targetScope.ensureMetricStorage();
	for (int k=0; k<metrics.size() && k<targetScope.metrics.size(); k++) {
		MetricValue mine = null;
		MetricValue crtMetric = metrics.getValue(this, k);

		if ( MetricValue.isAvailable(crtMetric) && 
				Float.compare(MetricValue.getValue(crtMetric), 0.0f) != 0) { // there is something to copy
			mine = new MetricValue();
			MetricValue.setValue(mine, MetricValue.getValue(crtMetric));

			if (MetricValue.isAnnotationAvailable(crtMetric)) {
				MetricValue.setAnnotationValue(mine, MetricValue.getAnnotationValue(crtMetric));
			} 
		} else {
			mine = MetricValue.NONE;
		}
		targetScope.metrics.setValue(k+offset, mine);
	}
}



//////////////////////////////////////////////////////////////////////////
//support for visitors													//
//////////////////////////////////////////////////////////////////////////

public void dfsVisitScopeTree(IScopeVisitor sv) {
	accept(sv, ScopeVisitType.PreVisit);
	int nKids = getSubscopeCount();
	for (int i=0; i< nKids; i++) {
		Scope childScope = getSubscope(i);
		if (childScope != null)
			childScope.dfsVisitScopeTree(sv);
	}
	accept(sv, ScopeVisitType.PostVisit);
}

public void accept(IScopeVisitor visitor, ScopeVisitType vt) {
	visitor.visit(this, vt);
}

/*******
 * depth first search scope with checking whether we should go deeper or not
 * 
 * @param sv
 */
public void dfsVisitFilterScopeTree(FilterScopeVisitor sv) {
	accept(sv, ScopeVisitType.PreVisit);
	if (sv.needToContinue())
	{
		// during the process of filtering, it is possible the tree has been changed
		// and the some children may be removed. 
		// we will first retrieve the original list of children, and then investigate
		// one-by-one, even though the list of children has changed.
		
		Object []children = getChildren(); // copy the original children
		if (children != null)
		{
			for(Object child: children)
			{
				Scope scope = (Scope) child;
				scope.dfsVisitFilterScopeTree(sv);
			}
		}
	}
	accept(sv, ScopeVisitType.PostVisit);
}

@Override
/*
 * (non-Javadoc)
 * @see edu.rice.cs.hpc.data.experiment.scope.TreeNode#dispose()
 */
public void dispose()
{
	super.dispose();
	root 		= null;
	metrics 	= null;
	sourceFile  = null;
	//srcCitation		= null;
	//combinedMetrics = null;
}

}
