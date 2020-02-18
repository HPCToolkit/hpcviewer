package edu.rice.cs.hpc.data.experiment.scope.visitors;

import java.util.*;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.AlienScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.FileScope;
import edu.rice.cs.hpc.data.experiment.scope.GroupScope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.LoadModuleScope;
import edu.rice.cs.hpc.data.experiment.scope.LoopScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.ScopeVisitType;
import edu.rice.cs.hpc.data.experiment.scope.StatementRangeScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope.ProcedureType;
import edu.rice.cs.hpc.data.experiment.scope.filters.ExclusiveOnlyMetricPropagationFilter;
import edu.rice.cs.hpc.data.experiment.scope.filters.InclusiveOnlyMetricPropagationFilter;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;


/*************************************************************************************************
 * Class to create Flat tree based on calling context tree
 * 
 * REMARK: THIS CODE IS NOT COMPATIBLE WITH OLD DATABASE !!!
 *  
 *
 *************************************************************************************************/

public class FlatViewScopeVisitor implements IScopeVisitor 
{
	final static private char SEPARATOR_ID = ':';
	
	private Hashtable<Integer, LoadModuleScope> htFlatLoadModuleScope;
	private Hashtable<String, FileScope> htFlatFileScope;
	private HashMap<String, FlatScopeInfo> htFlatScope;
	private HashMap<String, Scope[]> htFlatCostAdded;
	
	private RootScope root_ft;
	
	private InclusiveOnlyMetricPropagationFilter inclusive_filter;
	private ExclusiveOnlyMetricPropagationFilter exclusive_filter;
	
	//final private boolean debug = false;
	
	/******************************************************************
	 * Constructor
	 * @param exp: experiment
	 * @param root: the root of the tree
	 ******************************************************************/
	public FlatViewScopeVisitor( Experiment exp, RootScope root) {
		this.htFlatLoadModuleScope = new Hashtable<Integer, LoadModuleScope>();
		this.htFlatFileScope = new Hashtable<String, FileScope>();
		this.htFlatScope     = new HashMap<String, FlatScopeInfo>();
		this.htFlatCostAdded = new HashMap<String, Scope[]>();
		
		this.root_ft = root;
		
		this.inclusive_filter = new InclusiveOnlyMetricPropagationFilter( exp );
		this.exclusive_filter = new ExclusiveOnlyMetricPropagationFilter( exp );
	}
	
	
	public void visit(Scope scope, ScopeVisitType vt) 				{ }
	public void visit(RootScope scope, ScopeVisitType vt) 			{ }
	public void visit(LoadModuleScope scope, ScopeVisitType vt) 	{ }
	public void visit(FileScope scope, ScopeVisitType vt) 			{ }
	public void visit(AlienScope scope, ScopeVisitType vt) 			{ }
	public void visit(StatementRangeScope scope, ScopeVisitType vt) { }
	public void visit(GroupScope scope, ScopeVisitType vt) 			{ }

	public void visit(CallSiteScope scope, ScopeVisitType vt) 		{ 
		add(scope,vt, true, false); 
	}
	public void visit(LineScope scope, ScopeVisitType vt) 			{ 
		add(scope,vt, true, true); 
	}
	public void visit(LoopScope scope, ScopeVisitType vt) 			{
		add(scope,vt, true, false); 
	}
	public void visit(ProcedureScope scope, ScopeVisitType vt) 		{
		if (isPseudoProcedure(scope))
			return;
		add(scope,vt, true, false); 
	}

	
	/******************************************************************
	 * Create or add a flat scope based on the scope from CCT
	 * @param scope
	 * @param vt
	 * @param add_inclusive: flag if an inclusive cost had to be combined in flat/module scope
	 * @param add_exclusive: flag if an exclusive cost had to be combined in flat/module scope
	 ******************************************************************/
	private void add( Scope scope, ScopeVisitType vt, boolean add_inclusive, boolean add_exclusive ) {
		
		String id = this.getID(scope); 

		if (vt == ScopeVisitType.PreVisit ) {
			//--------------------------------------------------------------------------
			// Pre-visit
			//--------------------------------------------------------------------------
			Scope flat_info[] = this.htFlatCostAdded.get( id );
			if (flat_info != null) {
				this.htFlatCostAdded.remove(id);
			}

			FlatScopeInfo objFlat = this.getFlatCounterPart(scope, scope, id);
			
			//--------------------------------------------------------------------------
			// Aggregating metrics to load module and flat scope
			// Notes: this is not correct for Derived incremental metrics
			//--------------------------------------------------------------------------
			if (objFlat != null) {
				addCostIfNecessary(id, objFlat.flat_lm, scope, add_inclusive, add_exclusive);
				addCostIfNecessary(id, objFlat.flat_file, scope, add_inclusive, add_exclusive);

				//--------------------------------------------------------------------------
				// For call site, we need also to create its procedure scope
				//--------------------------------------------------------------------------
				if (scope instanceof CallSiteScope) {
					ProcedureScope proc_cct_s = ((CallSiteScope) scope).getProcedureScope();
					this.getFlatCounterPart(proc_cct_s, scope, id);
				}
			}

		} else {
			
			//--------------------------------------------------------------------------
			// Post visit
			//--------------------------------------------------------------------------
			Scope flat_info[] = this.htFlatCostAdded.get( id );
			if (flat_info != null)
				for (int i=0; i<flat_info.length; i++) {
					if (flat_info[i] != null) {
						flat_info[i].decrementCounter();
					}
				}
		}
	}
	
	
	/****************************************************************************
	 * Get the flat counterpart of the scope cct:
	 * - check if the flat counter part already exist
	 * -- if not, create a new one
	 * - get the flat file counter part exists
	 * 
	 * @param scopeCCT
	 * @return
	 ****************************************************************************/
	private FlatScopeInfo getFlatScope( Scope cct_s, String cct_id ) {
		//-----------------------------------------------------------------------------
		// get the flat scope
		//-----------------------------------------------------------------------------
		String id = getID(cct_s);
		FlatScopeInfo flat_info_s = this.htFlatScope.get( id );
		
		if (flat_info_s == null) {

			//-----------------------------------------------------------------------------
			// Initialize the flat scope
			//-----------------------------------------------------------------------------
			flat_info_s = new FlatScopeInfo();
			
			//-----------------------------------------------------------------------------
			// finding enclosing procedure of this cct scope:
			// if it is a call site, then the file and the module can be found in the scope
			// for others, we need to find the enclosing procedure iteratively
			//-----------------------------------------------------------------------------
			ProcedureScope proc_cct_s;
			if (cct_s instanceof CallSiteScope) {
				proc_cct_s = ((CallSiteScope)cct_s).getProcedureScope();
			} else {
				proc_cct_s = findEnclosingProcedure(cct_s);
			}

			// if the procedure is a fake procedure, do not convert it to flat tree node
			// Careful: Some inline functions are fake procedures (like macros).
			//  we want this inline procedures to be converted to flat tree regardless 
			//  they are fake procedure.
			if (proc_cct_s == null || isPseudoProcedure(proc_cct_s)) {
				//throw new RuntimeException("Cannot find the enclosing procedure for " + cct_s);
				return null;
			}
			
			//-----------------------------------------------------------------------------
			// Initialize the flat scope of this cct
			//-----------------------------------------------------------------------------
			flat_info_s.flat_s = cct_s.duplicate();
			flat_info_s.flat_s.setRootScope(root_ft);
			
			//-----------------------------------------------------------------------------
			// save the info into hashtable
			//-----------------------------------------------------------------------------
			this.htFlatScope.put( id, flat_info_s);

			//-----------------------------------------------------------------------------
			// for inline macro, we don't need to attach the file and load module
			// an inline macro node will be attached directly to its parent
			//-----------------------------------------------------------------------------
			if (isInlineMacro(flat_info_s.flat_s)) {
				return flat_info_s;
			}
			
			//-----------------------------------------------------------------------------
			// Initialize the load module scope
			//-----------------------------------------------------------------------------
			flat_info_s.flat_lm = this.createFlatModuleScope(proc_cct_s);

			//-----------------------------------------------------------------------------
			// Initialize the flat file scope
			//-----------------------------------------------------------------------------
			flat_info_s.flat_file = this.createFlatFileScope(proc_cct_s, flat_info_s.flat_lm);
			
			//-----------------------------------------------------------------------------
			// Attach the scope to the file if it is a procedure
			//-----------------------------------------------------------------------------
			if (flat_info_s.flat_s instanceof ProcedureScope) {
				this.addToTree(flat_info_s.flat_file, flat_info_s.flat_s);
			}
		}
		
		return flat_info_s;
	}
	
	/*****************************************************************
	 * Create the flat view of a load module
	 * @param proc_cct_s
	 * @return
	 *****************************************************************/
	private LoadModuleScope createFlatModuleScope(ProcedureScope proc_cct_s) {
		LoadModuleScope lm = proc_cct_s.getLoadModule();
		LoadModuleScope lm_flat_s = null;
		
		// some old database do not provide load module information
		if (lm != null)  {
			lm_flat_s = this.htFlatLoadModuleScope.get(lm.getFlatIndex());
			if (lm_flat_s == null) {
				// no load module has been created. we allocate a new one
				lm_flat_s = (LoadModuleScope) lm.duplicate();
				lm_flat_s.setRootScope(root_ft);
				// attach the load module to the root scope
				this.addToTree(root_ft, lm_flat_s);
				// store this module into our dictionary
				this.htFlatLoadModuleScope.put(lm.getFlatIndex(), lm_flat_s);
			}
		}
		return lm_flat_s;
	}
	
	/*****************************************************************
	 * generate a unique file ID which depends on the load module:
	 * unique file ID = load_module_ID + file_ID 
	 * 
	 * @param file
	 * @param lm
	 * 
	 * @return
	 *****************************************************************/
	private String getUniqueFileID(SourceFile file, LoadModuleScope lm)
	{
		String separator = String.valueOf(SEPARATOR_ID);
		return lm.getFlatIndex() + separator + file.getFileID();
	}
	
	
	/*****************************************************************
	 * Create the flat view of a file scope
	 * @param cct_s
	 * @param flat_lm
	 * @return
	 *****************************************************************/
	private FileScope createFlatFileScope(Scope cct_s, LoadModuleScope flat_lm) {
		SourceFile src_file = cct_s.getSourceFile();	
		String unique_file_id = getUniqueFileID(src_file, flat_lm);
		FileScope flat_file = this.htFlatFileScope.get( unique_file_id );
		
		//-----------------------------------------------------------------------------
		// ATTENTION: it is possible that a file can be included into more than one load module
		//-----------------------------------------------------------------------------
		if ( (flat_file == null) ){
			flat_file = createFileScope(src_file, flat_lm, unique_file_id);
			
		} else {
			
			Scope parent_lm = flat_file.getParentScope();
			if (parent_lm instanceof LoadModuleScope) {
				LoadModuleScope flat_parent_lm = (LoadModuleScope) parent_lm;

				// check if the load module the existing file is the same with the scope's load module
				if (flat_parent_lm.getFlatIndex() != flat_lm.getFlatIndex() ) {
					// the same file in different load module scope !!!
					flat_file = createFileScope(src_file, flat_lm, unique_file_id);
				}
			}
		}
		return flat_file;
	}
	
	
	/*****************************************************************
	 * Create a new file scope (this procedure will NOT check if the file already exists or not) !
	 * @param src_file
	 * @param lm_s
	 * @return
	 *****************************************************************/
	private FileScope createFileScope(SourceFile src_file, LoadModuleScope lm_s, String unique_file_id) {
		int fileID = src_file.getFileID();
		FileScope file_s =  new FileScope( this.root_ft, src_file, fileID );
		//------------------------------------------------------------------------------
		// if load module is undefined, then we attach the file scope to the root scope
		//------------------------------------------------------------------------------
		if (lm_s == null)
			this.addToTree(root_ft, file_s);
		else
			this.addToTree(lm_s, file_s);
		this.htFlatFileScope.put( unique_file_id, file_s);

		return file_s;
	}
	
	
	/*****************************************************************
	 * construct the flat view of a cct scope
	 * @param cct_s the original cct scope
	 * @param cct_s_metrics cct that metrics will be applied to the flat scope
	 * @param id original id
	 * 
	 * @return FlatScopeInfo if the flat scope can be created
	 *****************************************************************/
	private FlatScopeInfo getFlatCounterPart( Scope cct_s, Scope cct_s_metrics, String id) {
		// -----------------------------------------------------------------------------
		// Get the flat scope of the parent 	
		// -----------------------------------------------------------------------------
		Scope cct_parent_s = cct_s.getParentScope() ;
		Scope flat_enc_s = null;

		if (cct_parent_s != null) {
			if (cct_parent_s instanceof RootScope) {
				// ----------------------------------------------
				// main procedure
				// ----------------------------------------------
				flat_enc_s = null;
			} else {
				FlatScopeInfo flat_enc_info = null;
				if ( cct_parent_s instanceof CallSiteScope ) {
					// ----------------------------------------------
					// parent is a call site
					// ----------------------------------------------
					ProcedureScope proc_cct_s = ((CallSiteScope)cct_parent_s).getProcedureScope(); 
					String parent_id = getID(proc_cct_s);
					
					flat_enc_info = this.getFlatScope(proc_cct_s, parent_id);

				} else {					
					// ----------------------------------------------
					// parent is a line scope or loop scope or procedure scope
					// ----------------------------------------------
					String parent_id = getID(cct_parent_s);
					flat_enc_info = this.getFlatScope(cct_parent_s, parent_id);
				}
				if (flat_enc_info != null)
					flat_enc_s = flat_enc_info.flat_s;

			}
		}

		FlatScopeInfo objFlat = this.getFlatScope(cct_s, id);
		if (objFlat == null)
			return null;

		if (flat_enc_s != null) {
			if (!isCyclicDependency(flat_enc_s, objFlat.flat_s)) {
				// normal case: no cyclic dependency between the child and the ancestors
				this.addToTree(flat_enc_s, objFlat.flat_s);
			} else
			{	// rare case: cyclic dependency
				// TODO: we should create a new copy and attach it to the tree
				// but this will cause an issue for adding metrics and decrement counter
				// at the moment we just avoid cyclic dependency
				
				Scope copy = objFlat.flat_s.duplicate();				
				this.addToTree(flat_enc_s, copy);
			}
		}
		this.addCostIfNecessary(id, objFlat.flat_s, cct_s_metrics, true, true);
		
		return objFlat;
		
	}
	
	
	/***********************************************************
	 * Retrieve the ID given a scope
	 * a flat ID is the name of the class class concatenated by the flat ID
	 * 	(This is to force to have different ID for different classes
	 *   since hpcprof may generate the same ID for different type of scopes)
	 * for call site, we need to add the flat ID of the called procedure
	 *  (this is to ensure a flat's call site has different ID.
	 *   however, it doesn't solve if the called procedures have the same ID)
	 * @param scope
	 * @return
	 ***********************************************************/
	private String getID( Scope scope ) {
		final String id = String.valueOf(scope.getFlatIndex());
		final String class_type = scope.getClass().getSimpleName();
		StringBuffer hash_id = new StringBuffer(id);
		if (class_type != null) {
			hash_id.insert(0, class_type.substring(0, 2));
		}
		if (scope instanceof CallSiteScope)
		{
			// forcing to include procedure ID to ensure uniqueness of call site
			final int proc_id = ((CallSiteScope)scope).getProcedureScope().getFlatIndex();
			hash_id.append(SEPARATOR_ID);
			hash_id.append(proc_id);
		} else if (scope instanceof ProcedureScope) 
		{
			ProcedureScope proc_scope = (ProcedureScope) scope;
			if (proc_scope.isFalseProcedure()) {
				int linenum = proc_scope.getFirstLineNumber();
				String file_id = getUniqueFileID(proc_scope.getSourceFile(), proc_scope.getLoadModule());

				hash_id.append(SEPARATOR_ID);
				hash_id.append(file_id);
				
				hash_id.append(SEPARATOR_ID);
				hash_id.append(linenum);
			}
		}
		return hash_id.toString();
	}
	
	
	/***********************************************************
	 * Add a child as the subscope of a parent
	 * @param parent
	 * @param child
	 ***********************************************************/
	private void addToTree( Scope parent, Scope child ) {
		int nkids = parent.getSubscopeCount();
		
		//-------------------------------------------------------------------------------
		// search for the existing kids. If the kid is already added, then we don't need
		// 	to add it again
		//-------------------------------------------------------------------------------
		for (int i=0; i<nkids; i++) {
			Scope kid = parent.getSubscope(i);
			if ( this.isTheSameScope(kid, child) )
				return;
		}		
		this.addChild(parent, child);
	}
	
	/*****************************************************************
	 * check if the child is also the ancestor of the parent (cyclic dependency)
	 * @param parent
	 * @param child
	 * @return boolean true if the child is the ancestor of the parent 
	 ****************************************************************/
	private boolean isCyclicDependency(Scope parent, Scope child)
	{
		Scope ancestor = parent;
		while (ancestor != null && !(ancestor instanceof RootScope))
		{
			if (ancestor == child) {
				// cyclic
				return true;
			}
			ancestor = ancestor.getParentScope();
		}
		return false;
	}
	
	/*****************************************************************
	 * check if two scopes are equal (same cct index)
	 * @param s1
	 * @param s2
	 * @return true if they are the same
	 ****************************************************************/
	private boolean isTheSameScope(Scope s1, Scope s2) {
				
		// are s1 and s2 the same class ?
		if ( s1.getClass() != s2.getClass() )
			return false;
		return (s1.getCCTIndex() == s2.getCCTIndex());
	}
	

	/*****************************************************************
	 * Check if the scope is a pseudo procedure like <root> or <thread root>
	 *  or <partial callpath>
	 * A scope is a pseudo procedure iff it's a fake procedure and it isn't an alien procedure
	 * 
	 * @param scope
	 * @return true if it's a pseudo procedure
	 ****************************************************************/
	private boolean isPseudoProcedure(ProcedureScope scope) {
		boolean result = scope.isFalseProcedure() && !scope.isAlien();
		return result;
	}

	/******************************************************************
	 * check if the scope is an inlined macro
	 * @param scope
	 * @return true if it is an inlined macro
	 *****************************************************************/
	private boolean isInlineMacro(Scope scope) {
		if (!(scope instanceof ProcedureScope))
			return false;
		
		ProcedureScope ps = (ProcedureScope) scope;
		return ps.getProcedureType() == ProcedureType.ProcedureInlineMacro;
	}
	
	/******************************************************************
	 * add child to the parent
	 * 
	 * @param parent
	 * @param child
	 ****************************************************************/
	private void addChild(Scope parent, Scope child) {
		parent.addSubscope(child);
		child.setParentScope(parent);
	}
	
	
	/***********************************************************
	 * Iteratively finding an enclosing procedure of a CCT scope
	 * @param cct_s
	 * @return
	 ***********************************************************/
	private ProcedureScope findEnclosingProcedure(Scope cct_s)
	{
		if (cct_s instanceof ProcedureScope) 
			return (ProcedureScope) cct_s;
		Scope parent = cct_s.getParentScope();
		while(parent != null) {
			if (parent instanceof CallSiteScope) {
				ProcedureScope proc = ((CallSiteScope) parent).getProcedureScope();
				/*if (!proc.isAlien()) */return proc;
			}
			if (parent instanceof ProcedureScope) {
				ProcedureScope proc = (ProcedureScope) parent;
				if (!proc.isAlien())
					return proc;
			}
			if (parent instanceof RootScope) return null;
			parent = parent.getParentScope();
		}
		return null;
	}

	
	/***********************************************************
	 * check if a scope has been assigned as the outermost instance
	 * @param scope
	 * @return
	 ***********************************************************/
	private boolean isOutermostInstance(Scope scope) {
		return scope.getCounter() == 1;
	}

	
	/***********************************************************
	 * add the cost of the cct into the flat scope if "necessary"
	 * Necessary means: add the inclusive cost if the cct scope if the outermost scope
	 * @param flat_s
	 * @param cct_s
	 ***********************************************************/
	private void addCostIfNecessary( String objCode, Scope flat_s, Scope cct_s, boolean add_inclusive, boolean add_exclusive ) {
		if (flat_s == null)
			return;
		
		flat_s.incrementCounter();
			
		if (isOutermostInstance(flat_s)) {
			if (add_inclusive)
				flat_s.combine(cct_s, inclusive_filter);
		}
		if (add_exclusive) {
			if (flat_s instanceof CallSiteScope && cct_s instanceof CallSiteScope) {
				CallSiteScope cs_scope = (CallSiteScope) cct_s;
				flat_s.combine(cs_scope.getLineScope(), exclusive_filter);
			} else {
				flat_s.combine(cct_s, exclusive_filter);
			}
		}
		//-----------------------------------------------------------------------
		// store the flat scopes that have been updated  
		//-----------------------------------------------------------------------
		Scope arr_new_scopes[]; 
		Scope scope_added[] = htFlatCostAdded.get( objCode );
		if (scope_added != null) {
			int nb_scopes = scope_added.length;
			arr_new_scopes = new Scope[nb_scopes+1];
			System.arraycopy(scope_added, 0, arr_new_scopes, 0, nb_scopes);
			arr_new_scopes[nb_scopes] = flat_s;
		} else {
			arr_new_scopes = new Scope[1];
			arr_new_scopes[0] = flat_s;
		}
		htFlatCostAdded.put(objCode, arr_new_scopes);
	}

	
	/*************************************************************************
	 * Each scope in the flat view has to be linked with 3 enclosing scopes:
	 * - load module
	 * - file
	 * - procedure
	 * @author laksonoadhianto
	 *************************************************************************/
	private class FlatScopeInfo {
		LoadModuleScope flat_lm;
		FileScope flat_file;
		Scope flat_s;
	}
}
