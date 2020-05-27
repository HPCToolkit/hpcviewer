package edu.rice.cs.hpc.data.experiment.xml;


import java.io.File;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.ExperimentConfiguration;
import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.experiment.scope.AlienScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScope;
import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeType;
import edu.rice.cs.hpc.data.experiment.scope.FileScope;
import edu.rice.cs.hpc.data.experiment.scope.LineScope;
import edu.rice.cs.hpc.data.experiment.scope.LoadModuleScope;
import edu.rice.cs.hpc.data.experiment.scope.LoopScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope;
import edu.rice.cs.hpc.data.experiment.scope.ProcedureScope.ProcedureType;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.experiment.scope.StatementRangeScope;
import edu.rice.cs.hpc.data.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.experiment.xml.Token2.TokenXML;
import edu.rice.cs.hpc.data.trace.TraceAttribute;
import edu.rice.cs.hpc.data.util.Dialogs;
import edu.rice.cs.hpc.data.util.IUserData;


/****
 * 
 * Base class to build experiment class without metrics associated
 * The base class is ideal for using CCT only like hpctraceviewer
 * 
 * @see ExperimentBuilder2 for building experiment with metrics
 */
public class BaseExperimentBuilder extends Builder {

	private final static boolean KEEP_OLD_DATABASE_COMPATIBILITY = false;
	
	protected final static String ATTRIBUTE_LINE		= "l";
	protected final static String ATTRIBUTE_NAME 		= "n";
	protected final static String ATTRIBUTE_FILENAME 	= "f";
	protected final static String ATTRIBUTE_VALUE 		= "v";
	protected final static String ATTRIBUTE_ID 			= "i";
	protected final static String ATTRIBUTE_TYPE 		= "t";
	protected final static String ATTRIBUTE_ALIEN 		= "a";
	
	private final static String PROCEDURE_UNKNOWN = "unknown procedure";

	/** The default name for the experiment, in case none is found by the parser. */
	protected String defaultName;

	/** The parsed configuration. */
	protected ExperimentConfiguration configuration;

	/** Master root scope, invisible **/
	protected Scope rootScope;
	/*** root scope of the view ***/
	protected RootScope viewRootScope;

	/** The experiment to own parsed objects. */
	protected BaseExperiment experiment;

	/** A stack to keep track of scope nesting while parsing. */
	protected Stack<Scope> scopeStack;

	/** The current source file while parsing. */
	protected Stack<SourceFile> srcFileStack;
	
	private Stack<RootScope> rootStack;

	private boolean csviewer;
	
	final private IUserData<String, String> userData;

	protected Token2.TokenXML previousToken = TokenXML.T_INVALID_ELEMENT_NAME;
	protected Token2.TokenXML elemInfoState = TokenXML.T_INVALID_ELEMENT_NAME;

	//--------------------------------------------------------------------------------------
	private HashMap<Integer, String> 			hashProcedureTable;
	private HashMap<Integer, LoadModuleScope> 	hashLoadModuleTable;
	private HashMap<Integer, SourceFile> 		hashSourceFileTable;
	private HashMap<Integer, Scope> 			hashCallSiteTable;
	
	private HashMap<Integer /*id*/, Integer /*status*/>			statusProcedureMap;

	private int min_cctid = Integer.MAX_VALUE;
	private int max_cctid = Integer.MIN_VALUE;

	private int current_cs_id = Integer.MAX_VALUE - 1;

	private boolean removeInvisibleProcedure = false;
	
	//=============================================================
	
	/*************************************************************************
	 *	Creates a ExperimentBuilder.
	 *
	 *	Parsed objects are added to the experiment as soon as possible, but
	 *	all the parsed objects of a given kind must be added at once. The
	 *	builder keeps parsed objects on lists until a whole set can be added
	 *	to the experiment.
	 *	<p>
	 *	Because of the way <code>Metric</code>s are implemented, the metric
	 *	objects must be added to the experiment before any scope objects are
	 *	constructed. Scopes need to know how many metrics the experiment has,
	 *	and find out by asking the experiment.
	 *
	 *  @param experiment: experiment class
	 *  @param defaultName: the default name of the experiment
	 *
	 ************************************************************************/
	public BaseExperimentBuilder(BaseExperiment experiment, String defaultName, IUserData<String, String> userData) {
		super();
		this.csviewer = false;
		//viewRootScope = new RootScope[3];
		
		hashProcedureTable  = new HashMap<Integer, String>();
		hashLoadModuleTable = new HashMap<Integer, LoadModuleScope>();
		hashSourceFileTable = new HashMap<Integer, SourceFile>();
		hashCallSiteTable   = new HashMap<Integer, Scope>();
		statusProcedureMap  = new HashMap<Integer, Integer>();

		// parse action data structures
		this.scopeStack   = new Stack<Scope>();
		this.rootStack    = new Stack<RootScope>();
		this.srcFileStack = new Stack<SourceFile>();
		
		this.srcFileStack.push(null); // mimic old behavior

		// creation arguments
		this.experiment = experiment;
		this.defaultName = defaultName;
		
		this.userData = userData;
	}
	
	
	protected boolean isCallingContextTree() {
		return this.csviewer;
	}
	
	private SourceFile getSourceFile(String fileIdString)
	{
		SourceFile sourceFile = null;
		try {
			Integer objFileKey = Integer.parseInt(fileIdString);
			 sourceFile=hashSourceFileTable.get(objFileKey.intValue());
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return sourceFile;
	}	


	/*************************************************************************
	 *	Takes notice of the beginning of an element.
	 * @throws OldXMLFormatException 
	 ************************************************************************/

	public void beginElement(String element, String[] attributes, String[] values) 
	{
		TokenXML current = Token2.map(element);

		switch(current)
		{
		case T_HPCTOOLKIT_EXPERIMENT:
			this.do_HPCTOOLKIT(attributes, values);
			break;
		case T_HEADER:
			this.do_Header(attributes,values);	
			break;
		case T_INFO:
			this.do_Info();
			break;

		case T_SEC_CALLPATH_PROFILE:
			// we need to retrieve the profile name and the ID
			this.csviewer = true;
			this.do_TITLE(attributes, values);
			break;

		case T_SEC_FLAT_PROFILE:
			this.csviewer = false;
			this.do_TITLE(attributes, values);
			break;

			// PGM elements
		case T_SEC_FLAT_PROFILE_DATA:
		case T_CALLPATH_PROFILE_DATA:	// semi old format. some data has this kind of tag
		case T_SEC_CALLPATH_PROFILE_DATA:
			this.begin_SecData(attributes, values);	break;

			// load module dictionary
		case T_LOAD_MODULE:
			this.do_LoadModule(attributes, values);
			break;
			// file dictionary
		case T_FILE:
			this.do_File(attributes, values); break;
			
			// flat profiles
		case T_LM:
			this.begin_LM (attributes, values);	break;
		case T_F:
			this.begin_F  (attributes, values);	break;
		case T_P:
			
		case T_PR:
		case T_PF:
			this.begin_PF  (attributes, values);	break;
		case T_A:
			this.begin_A  (attributes, values);	break;
		case T_L:
			this.begin_L  (attributes, values);	break;
		case T_S:
			this.begin_S  (attributes, values);	break;

			// callstack elements
		case T_C:
			this.begin_CALLSITE(attributes,values); 
			break;
			
		case T_PROCEDURE:
			this.do_Procedure(attributes, values); break;


			// trace database
		case T_TRACE_DB_TABLE:
			this.begin_TraceDBTable(attributes, values);
			break;
			
		case T_TRACE_DB:
			this.do_TraceDB(attributes, values); break;

		// ---------------------
		// XML v. 3.0
		// ---------------------
		case T_SUMMARY_DB_FILE:
			do_DBFile(BaseExperiment.Db_File_Type.DB_SUMMARY, attributes, values);
			break;
			
		case T_TRACE_DB_FILE:
			do_DBFile(BaseExperiment.Db_File_Type.DB_TRACE, attributes, values);
			break;
			
		case T_PLOT_DB_FILE:
			do_DBFile(BaseExperiment.Db_File_Type.DB_PLOT, attributes, values);
			break;
			
		case T_THREAD_ID_FILE:
			do_DBFile(BaseExperiment.Db_File_Type.DB_THREADS, attributes, values);
			break;

			// ---------------------
			// old token from old XML
			// ---------------------
		case T_CSPROFILE:
		case T_HPCVIEWER:
			throw new java.lang.RuntimeException(new OldXMLFormatException());
			// unknown elements
			
		// ---------------------
		// Tokens to be ignored 
		// ---------------------
			
		case T_PROCEDURE_TABLE:
		case T_FILE_TABLE:
		case T_LOAD_MODULE_TABLE:
		case T_SEC_HEADER:
			break;
		
		default:
			break;
		} 
		saveTokenContext(current);
	}


	/****
	 * all children requires to register the current context
	 * 
	 * @param current
	 */
	protected void saveTokenContext(TokenXML current) 
	{
		// laks: preserve the state of the current token for the next parsing state
		this.previousToken = current;
	}
	
	/*************************************************************************
	 *	Takes notice of the ending of an element.
	 ************************************************************************/
	public void endElement(String element)
	{
		TokenXML current = Token2.map(element);
		switch(current)
		{
		case T_SEC_FLAT_PROFILE:
		case T_SEC_CALLPATH_PROFILE:
			break;

		// Data elements
		case T_CALLPATH_PROFILE_DATA:	// @deprecated: semi old format. some data has this kind of tag
		case T_SEC_FLAT_PROFILE_DATA:
		case T_SEC_CALLPATH_PROFILE_DATA:
			this.end_PGM();
			break;
		case T_LM:
			this.end_LM();
			break;
			
		case T_P:
		case T_PR:
		case T_PF:
			this.end_PF();
			break;
		case T_A:
			this.end_A();
			break;
		case T_L:
			this.end_L();
			break;
		case T_S:
			this.end_S();
			break;
		case T_C: 		
			this.end_CALLSITE();
			break;
		case T_F:
			this.end_F();
			break;
			
		case T_TRACE_DB_TABLE:
			this.end_TraceDBTable();
			break;
			
		case T_PROCEDURE_TABLE:
			break;


			// ignored elements
			// trace database
		case T_TRACE_DB:
		case T_METRIC_DB:
		case T_M:
		case T_HPCTOOLKIT_EXPERIMENT:
		case T_NAME_VALUE:
		case T_HEADER:
		case T_INFO:
		case T_METRIC_FORMULA:
		case T_SEC_HEADER:
		case T_METRIC:
		case T_PROCEDURE:
		case T_FILE_TABLE:
		case T_FILE:
		case T_LOAD_MODULE_TABLE:
		case T_LOAD_MODULE:
		default:
			break;
		} 
	}


//	------------------------------- BUILDING		---------------------------//

	/*************************************************************************
	 * Process a HPCToolkitExperiment
	 *************************************************************************/
	private void do_HPCTOOLKIT(String[] attributes, String[] values) {
		String version = null;
		for (int i=0; i<attributes.length; i++) {
			if (attributes[i].charAt(0) == 'v') {
				//version of the database
				version = values[i];
			}
		}
		this.experiment.setVersion(version);
	}

	/*************************************************************************
	 * Process a Database file name
	 *************************************************************************/
	private void do_DBFile(BaseExperiment.Db_File_Type db_type, String []attributes, String []values) {
		for (int i=0; i<attributes.length; i++) {
			if (attributes[i].charAt(0) == 'n') {
				experiment.setDBFilename(db_type, values[i]);
				return;
			}
		}
	}
	
	/*************************************************************************
	 *      Processes a TARGET element as TITLE.
	 ************************************************************************/

	private void do_Header(String[] attributes, String[] values)
	{
		this.configuration.setName(ExperimentConfiguration.NAME_EXPERIMENT, values[0]);
	}

	private void do_Info() {
		this.elemInfoState = this.previousToken;
	}

	/*************************************************************************
	 *	Processes a TITLE element.
	 ************************************************************************/

	private void do_TITLE(String[] attributes, String[] values)
	{
		// <!ATTLIST SecCallPathProfile
		//        i CDATA #REQUIRED
		//        n CDATA #REQUIRED>
		this.Assert(attributes.length == 2);
		String sTitle = "";
		if(values.length == 2) {
			sTitle = values[1];
		}
		this.configuration.setName(ExperimentConfiguration.NAME_EXPERIMENT, sTitle);
	}

	/************************************************************************
	 * <!ELEMENT LoadModule (Info?)>
    <!ATTLIST LoadModule
              i CDATA #REQUIRED
              n CDATA #REQUIRED>
	 * Example:
	 *   <LoadModule i="43497" n="/lib64/libc-2.7.so"/>
	 * @param attributes
	 * @param values
	 ************************************************************************/
	private void do_LoadModule(String[] attributes, String[] values) {
		if(values.length < 2)
			return;
		
		// We assume that the 1st attribute is always the ID and the 2nd attribute is the value
		String sValue = values[1];
		String sID = values[0];
		try {
			Integer objID = new Integer(sID);
			LoadModuleScope lmScope = new LoadModuleScope(viewRootScope, sValue, null, objID.intValue());
			this.hashLoadModuleTable.put(objID, lmScope);
		} catch (java.lang.NumberFormatException e) {
			System.err.println("Incorrect load module ID: "+sID);
		} catch (java.lang.NullPointerException e) {
			System.err.println("load module table is empty. Key: "+sID+" value: "+sValue);
		}
	}



	/*************************************************************************
	 *      Processes a FILE.
	 *          <!ELEMENT File (Info?)>
    <!ATTLIST File
              i CDATA #REQUIRED
              n CDATA #REQUIRED>
	 ************************************************************************/
	private void do_File(String[] attributes, String[] values) {
		if(values.length < 2)
			return;
		String sID = values[0];		
		try {
			final Integer objFileID = Integer.parseInt(sID);
			// just in case if there is a duplicate key in the dictionary, we need to make a test
			final SourceFile sourceFile = this.getOrCreateSourceFile(values[1], objFileID.intValue());
			
			this.hashSourceFileTable.put(objFileID, sourceFile);
			
		} catch (Exception e) {
			
		}
	}

	
	/*************************************************************************
	 *	Begins processing an LM (load module) element.
	 *	<!ATTLIST LM
                i CDATA #IMPLIED
                n CDATA #REQUIRED
                v CDATA #IMPLIED>
	 ************************************************************************/
	private void begin_LM(String[] attributes, String[] values)
	{
		// LM n="load module name"
		String name = getAttributeByName(ATTRIBUTE_NAME, attributes, values);
		String sIndex = getAttributeByName(ATTRIBUTE_ID, attributes, values);
		
		try {
			Integer objIndex = Integer.parseInt(sIndex);
			SourceFile sourceFile = this.getOrCreateSourceFile(name, objIndex.intValue());
			Scope lmScope = new LoadModuleScope(rootStack.peek(), name, sourceFile, objIndex.intValue());
			// make a new load module scope object
			this.beginScope(lmScope);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*************************************************************************
	 *	Begins processing an F (file) element.
	 *      <!ATTLIST F
                i CDATA #IMPLIED
                n CDATA #REQUIRED>
	 ************************************************************************/
	private void begin_F(String[] attributes, String[] values)

	{
		// F n="filename"
		String inode = getAttributeByName(ATTRIBUTE_ID, attributes, values);
		try {
			Integer objFileKey = Integer.parseInt(inode);
			// make a new file scope object
			SourceFile sourceFile  = this.getOrCreateSourceFile(getAttributeByName(ATTRIBUTE_NAME, attributes, values), 
					objFileKey.intValue());

			this.srcFileStack.push(sourceFile);
			Scope fileScope = new FileScope(rootStack.peek(), sourceFile, objFileKey.intValue());

			this.beginScope(fileScope);

		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

	}


	/*************************************************************************
	 *	Begins processing a PF (procedure frame) element.
	 *       <!ATTLIST Pr
                i  CDATA #IMPLIED
                s  CDATA #IMPLIED
                n  CDATA #REQUIRED
                lm CDATA #IMPLIED
                f  CDATA #IMPLIED
                l  CDATA #IMPLIED
                a  (1|0) "0"
                v  CDATA #IMPLIED>
	 ************************************************************************/
	private void begin_PF(String[] attributes, String[] values)
	{
			boolean istext  = true; 
			boolean isalien = false; 
			boolean new_cct_format = false;
			
			int cct_id  = 0, flat_id = 0;
			int firstLn = 0, lastLn  = 0;
			
			SourceFile srcFile 			  = null; // file location of this procedure
			LoadModuleScope objLoadModule = null;
			String procAttribute 		  = null;

			for(int i=0; i<attributes.length; i++) {
				if (attributes[i].equals("s")) { 
					// new database format: s is the flat ID of the procedure
					procAttribute = this.getProcedureName(values[i]);
					flat_id = Integer.parseInt(values[i]);
					if (!new_cct_format)
						// old format: cct ID = flat ID
						cct_id = flat_id;
					
				} else if (attributes[i].equals(ATTRIBUTE_ID)) {
					// id of the proc frame. needs to cross ref
					cct_id = Integer.parseInt(values[i]); 
					new_cct_format = true;
					
				} else if(attributes[i].equals(ATTRIBUTE_FILENAME)) {
					// file
					istext = true;
					try {
						Integer indexFile = Integer.parseInt(values[i]);
						srcFile = this.hashSourceFileTable.get(indexFile);
					} catch (java.lang.NumberFormatException e) {
						// in this case, either the value of "f" is invalid or it is the name of the file
						// In some old format the attribute f contains the file not in the dictionary. So 
						// 	we need to create it from here
						if (this.srcFileStack.size()==1) {
							// the first stack is null, so let start from number 1
							srcFile = this.getOrCreateSourceFile(values[i], this.srcFileStack.size()+1);
						}
					}
					
				} else if(attributes[i].equals("lm")) { 
					// load module
					try {
						// let see if the value of ln is an ID or a simple load module name
						Integer indexFile = Integer.parseInt(values[i]);
						// look at the dictionary for the name of the load module
						objLoadModule = this.hashLoadModuleTable.get(indexFile);
						if (objLoadModule == null) {
							// old database
							objLoadModule = new LoadModuleScope(rootStack.peek(), values[i], null, indexFile.intValue());
							this.hashLoadModuleTable.put(indexFile, objLoadModule);
						}
					} catch (java.lang.NumberFormatException e) {
						// old database:
						// this error means that the lm is not based on dictionary
						objLoadModule = new LoadModuleScope(rootStack.peek(), values[i], null, values[i].hashCode());
					}
				} else if (attributes[i].equals("p") ) {
					// obsolete format: p is the name of the procedure
					procAttribute = values[i];
					
				} else if(attributes[i].equals(ATTRIBUTE_NAME)) {
					// new database format: n is the flat ID of the procedure
					procAttribute = this.getProcedureName(values[i]);
					
				} else if(attributes[i].equals(ATTRIBUTE_LINE)) {
					// line number (or range)
					StatementRange objRange = new StatementRange(values[i]);
					firstLn = objRange.getFirstLine();
					lastLn = objRange.getLastLine();
				} else if(attributes[i].equals(ATTRIBUTE_TYPE)) {
					// type of the procedure frame
					// mainly used by datacentric
					
					
				} else if(attributes[i].equals(ATTRIBUTE_ALIEN)) { 
					// alien
					isalien = values[i].equals("1");
					
				} else if(attributes[i].equals(ATTRIBUTE_VALUE)) {
				}
			}
			
			if (isalien) {
				if (KEEP_OLD_DATABASE_COMPATIBILITY) {
					// in the old database, the alien's flat ID is the same as the original one
					// we want to differentiate here
					flat_id = Integer.MAX_VALUE ^ flat_id;
				}

				if (procAttribute.isEmpty()) {
					// this is a line scope
					Scope scope;
					if (firstLn == lastLn)
						scope = new LineScope(rootStack.peek(), srcFile, firstLn-1, cct_id, flat_id);
					else
						scope = new StatementRangeScope(rootStack.peek(), srcFile, 
								firstLn-1, lastLn-1, cct_id, flat_id);
					scope.setCpid(0);
					scopeStack.push(scope);

					srcFile.setIsText(istext);
					this.srcFileStack.add(srcFile);
					return;
				} else {
					// this is a procedure scope uses the handling below
				}
			}

			// FLAT PROFILE: we retrieve the source file from the previous tag
			if(srcFile == null) {
					srcFile = this.srcFileStack.peek();
			} 
			 
			srcFile.setIsText(istext);
			this.srcFileStack.add(srcFile);
			
			int feature = 0;
			Integer statusProc = statusProcedureMap.get(flat_id);
			if (statusProc != null) {
				feature = statusProc.intValue();
				if (feature == ProcedureScope.FeatureRoot) {
					RootScope datacentricRoot = new RootScope(experiment, procAttribute, RootScopeType.DatacentricTree);
					// push the new scope to the stack
					scopeStack.push(datacentricRoot);
					rootStack.push(datacentricRoot);

					experiment.setDatacentricRootScope(datacentricRoot);
					return;
				}
			}
							
			
			ProcedureScope procScope  = new ProcedureScope(rootStack.peek(), objLoadModule, srcFile, 
					firstLn-1, lastLn-1, 
					procAttribute, isalien, cct_id, flat_id, userData, feature);

			if ( (this.scopeStack.size()>1) && ( this.scopeStack.peek() instanceof LineScope)  ) {

				LineScope ls = (LineScope)this.scopeStack.pop();
				int	callsiteID = ls.getFlatIndex();
				
				if (KEEP_OLD_DATABASE_COMPATIBILITY)
				{
					//-----------------------------------------------------------------------------------------
					// In some database (especially the old ones), they have the same ID for different call sites
					// In order to keep compatibility, we need to generate our own ID hoping it doesn't interfere
					// with the ID generated by the new hpcprof :-(
					//-----------------------------------------------------------------------------------------
					callsiteID = this.getCallSiteID(ls, procScope);  
				}
				CallSiteScope csn = new CallSiteScope( ls, procScope, 
						CallSiteScopeType.CALL_TO_PROCEDURE, cct_id, callsiteID );

				// beginScope pushes csn onto the node stack and connects it with its parent
				// this is done while the ls is off the stack so the parent of csn is ls's parent. 
				// afterward, we rearrange the top of stack to tuck ls back underneath csn in case it is 
				// needed for a subsequent procedure frame that is a sibling of csn in the tree.
				this.beginScope(csn);
				ls.setParentScope(csn.getParentScope());
				CallSiteScope csn2 = (CallSiteScope) this.scopeStack.pop();
				this.scopeStack.push(ls);
				this.scopeStack.push(csn2);

				if (isalien)
					procScope.setProcedureType(ProcedureType.ProcedureInlineFunction);
				else 
					procScope.setProcedureType(ProcedureType.ProcedureNormal);
			} else {
				this.beginScope(procScope);

				// fix bug #36 (omp idle is omitted in flat view)
				// the root cause of the bug is that we do not differentiate between procedure inline macro
				//   and procedure root (such as thread root, program root and omp idle).
				// need to check if a procedure is an alien or not. Procedure root has no alien information
				
				if (isalien)
					procScope.setProcedureType(ProcedureType.ProcedureInlineMacro);
				else
					procScope.setProcedureType(ProcedureType.ProcedureRoot);
			}
	}

	/*************************************************************************
	 * Retrieve the ID of a call site.
	 * Instead of using hpcprof's flat index, we reconstruct our own ID 
	 *  (not sure if this is hpcprof's bug or not). hpcprof's flat index is not
	 *  suitable if the same function is from the same file from different load modules
	 *  In this case, hpcprof's flat index will be the same (which is incorrect) 
	 *  
	 * In normal case, the ID is the hashcode of its call site (line scope). 
	 * But, in case of there are multiple calls in one line statement, we need
	 * to generate different ID for each call sites.
	 * 
	 * @param ls
	 * @param cs
	 * @return
	 *************************************************************************/
	private int getCallSiteID ( LineScope ls, ProcedureScope cs ) {
		LoadModuleScope module = cs.getLoadModule();
		// add flat index if there are two calls in the same line
		String sName = ls.getName() + "/" + cs.getName() + "/" + ls.getFlatIndex();
		// in case of the same file and the same procedure with different module name
		// this should fix where we have ~unknown-file~ and ~unknown-procedure~ in 
		// different modules
		if (module != null) {
			sName = module.getModuleName()+ "/" + sName;
		}
		
		int scope_id = sName.hashCode();
		Scope s_old = this.hashCallSiteTable.get( Integer.valueOf(scope_id) );
		if (s_old != null) {
			if (s_old.getName().equals(cs.getName())) {
				// the same line, the same ID, the same calls
			} else {
				// the same line, different calls. We need to create a new ID
				scope_id = this.current_cs_id;
				this.hashCallSiteTable.put(Integer.valueOf(scope_id), cs);
				this.current_cs_id--;
			}
		} else {
			this.hashCallSiteTable.put(Integer.valueOf(scope_id), cs);
		}
		return scope_id;
	}
	

	
	/*************************************************************************
	 *	Begins processing a A (alien) element.
      <!ELEMENT A (A|L|S|C|M)*>      <!ATTLIST A
                i CDATA #IMPLIED
                f CDATA #IMPLIED
                n CDATA #IMPLIED
                l CDATA #IMPLIED
                v CDATA #IMPLIED>
	 ************************************************************************/

	private void begin_A(String[] attributes, String[] values)
	{
		String sIndex = null;
		String filenm = null;
		String procnm = null;
		String sLine = null;
		
		// make a new alien scope object
		for (int i=0; i<attributes.length; i++) {
			if (attributes[i].equals(ATTRIBUTE_ID)) {
				sIndex = values[i];
			} else if (attributes[i].equals(ATTRIBUTE_FILENAME)) {
				filenm = values[i];
			} else if (attributes[i].equals(ATTRIBUTE_NAME)) {
				procnm = values[i];
			} else if (attributes[i].equals(ATTRIBUTE_LINE)) {
				sLine = values[i];
			}
		}
		
		try {
			Integer objIndex = Integer.parseInt(sIndex);

			int firstLn, lastLn;
			StatementRange objRange = new StatementRange(sLine);
			firstLn = objRange.getFirstLine();
			lastLn = objRange.getLastLine();

			SourceFile sourceFile = this.getOrCreateSourceFile(filenm, objIndex.intValue());
			this.srcFileStack.push(sourceFile);

			Scope alienScope = new AlienScope(rootStack.peek(), sourceFile, filenm, procnm, firstLn-1, lastLn-1, objIndex.intValue());

			this.beginScope(alienScope);

		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
	}

	
	/*************************************************************************
	 * 
	 * @param attributes
	 * @param values
	 * <!ATTLIST Procedure
              i CDATA #REQUIRED
              n CDATA #REQUIRED>
	 *************************************************************************/
	private void do_Procedure(String[] attributes, String[] values) {
		if(values.length < 2)
			return;
		
		String sID   = null;
		String sData = null;
		
		Integer statusProc = null;

		for (int i=0; i<attributes.length; i++)
		{
			if (attributes[i].equals("i")) {
				sID = values[i];
			} else if (attributes[i].equals("n")) {
				sData = values[i];
			} else if (attributes[i].equals("f")) {
				statusProc  = Integer.parseInt(values[i]);
			}
			
		}
		try {
			Integer objID = Integer.parseInt(sID);
			this.hashProcedureTable.put(objID, sData);
			
			if (statusProc != null)
				this.statusProcedureMap.put(objID, statusProc);
			
		} catch (java.lang.NumberFormatException e) {
			e.printStackTrace();
		}
	}
	
	


	/*************************************************************************
	 *	Begins processing an L (loop) element.
	 *	<!ELEMENT L (A|L|S|C|M)*>
      <!ATTLIST L
                i CDATA #IMPLIED
                s CDATA #IMPLIED
                l CDATA #IMPLIED
                v CDATA #IMPLIED>
	 ************************************************************************/

	private void begin_L(String[] attributes, String[] values)
	{
		int cct_id = 0, flat_id = 0;
		int firstLn = 0;
		int lastLn = 0;
		SourceFile sourceFile = null;
		
		for(int i=0; i<attributes.length; i++) {
			if(attributes[i].equals("s")) {
				flat_id = Integer.parseInt(values[i]);
				if (cct_id == 0)
					cct_id = flat_id;
				
			} else if(attributes[i].equals(ATTRIBUTE_LINE)) {
				String sLine = values[i];
				StatementRange objRange = new StatementRange( sLine );
				firstLn = objRange.getFirstLine();
				lastLn = objRange.getLastLine();
			} else if (attributes[i].equals(ATTRIBUTE_FILENAME)) {
				String fileIdString = values[i];
				sourceFile = getSourceFile(fileIdString);
			} else if(attributes[i].equals(ATTRIBUTE_ID)) {
				cct_id = Integer.parseInt(values[i]);
			} 
		}
		
		if (sourceFile == null) {	
			sourceFile = this.srcFileStack.peek();
			if (this.csviewer) {
				// Use the source file of the Procedure Frame
				// NOTE: the current scope (i.e. the parent of this
				// nascent loop scope) should be either a procedure frame
				// or a loop that recursively obtained its file from the
				// procedure frame.
				Scope frameScope = this.getCurrentScope();
				//while ( !(frameScope instanceof ProcedureScope) ) {
				//  frameScope = frameScope.getParentScope();
				//}
				sourceFile = frameScope.getSourceFile();
			}
		}
		Scope loopScope = new LoopScope(rootStack.peek(), sourceFile, firstLn-1, lastLn-1, cct_id, flat_id);

		this.beginScope(loopScope);
	}

	/*************************************************************************
	 *	Finishes processing an L (loop) element.
	 ************************************************************************/

	private void end_L()
	{
		this.endScope();
	}

	/*************************************************************************
	 *	Begins processing an LN (line) element.
	 * <!ATTLIST S
                i CDATA #IMPLIED
                s CDATA #IMPLIED
                l CDATA #IMPLIED
                v CDATA #IMPLIED>
	 ************************************************************************/
	private void begin_S(String[] attributes, String[] values)
	{
		begin_S_internal( attributes,  values, false);
	}
	
	private void begin_S_internal(String[] attributes, String[] values, boolean isCallSite)
	{
		int cct_id = 0, flat_id = 0;
		// make a new statement-range scope object
		int firstLn = 0;
		int lastLn  = 0;
		int cpid = 0;

		for(int i=0; i<attributes.length; i++) {
			if(attributes[i].equals(ATTRIBUTE_LINE)) {
				firstLn = Integer.parseInt(values[i]);
				lastLn = firstLn;
				
			} else if(attributes[i].equals("s"))  {
				flat_id = Integer.parseInt(values[i]);
				if (cct_id == 0)
					cct_id = flat_id;
				
			} else if(attributes[i].equals(ATTRIBUTE_ID))  {
				cct_id = Integer.parseInt(values[i]);

			} else if(attributes[i].equals("it")) { //the cpid
				cpid = Integer.parseInt(values[i]);
			}

		}

		SourceFile srcFile = this.srcFileStack.peek();


		Scope scope;
		if( firstLn == lastLn )
			scope = new LineScope(rootStack.peek(), srcFile, firstLn-1, cct_id, flat_id);
		else
			scope = new StatementRangeScope(rootStack.peek(), srcFile, 
					firstLn-1, lastLn-1, cct_id, flat_id);

		scope.setCpid(cpid);
		if (isCallSite) {
			scopeStack.push(scope);
		} else {
			this.beginScope(scope);
		}
	}

	/*************************************************************************
	 *	Finishes processing an S (line) element.
	 ************************************************************************/

	private void end_S()
	{
		this.endScope(); 
	}


	/*************************************************************************
	 * Begin a new CALLSITE
	 * <!ATTLIST C
                i CDATA #IMPLIED
                s CDATA #IMPLIED
                l CDATA #IMPLIED
                v CDATA #IMPLIED>
	 ************************************************************************/
	private void begin_CALLSITE(String[] attributes, String[] values) {  
		this.begin_S_internal(attributes, values, true); // orig: true  
	}



	/*************************************************************************
	 * 	end a callsite.
	 ************************************************************************/
	private void end_CALLSITE() 
	{
		end_S();
	}


	private void end_PGM()
	{
		this.endScope();
	}


	


	/*************************************************************************
	 *	Finishes processing an LM (load module) element.
	 ************************************************************************/
	private void end_LM()
	{
		this.endScope();
	}



	/*************************************************************************
	 *	Finishes processing an F (file) element.
	 ************************************************************************/
	private void end_F()
	{
		this.endScope();
		this.srcFileStack.pop();
		this.Assert(srcFileStack.peek() == null); // mimic old behavior
	}


	
	
	/*************************************************************************
	 *	Finishes processing a P (procedure) element.
	 ************************************************************************/

	private void end_PF()
	{
		this.srcFileStack.pop();
		this.endScope();
	}




	/*************************************************************************
	 *	Finishes processing a A (alien) element.
	 ************************************************************************/

	private void end_A()
	{
		this.srcFileStack.pop();
		this.endScope();
	}

	
	
	//--------------------------------------------------------------------------------
	// trace database
	//--------------------------------------------------------------------------------
	private void begin_TraceDBTable(String[] attributes, String[] values) 
	{
	}

	
	private void end_TraceDBTable() 
	{
	}

	
	
	/*******
	 * handling trace db
	 * @param attributes
	 * @param values
	 */
	private void do_TraceDB(String[] attributes, String[] values)
	{
		// the database contains trace data. If the caller is hpcviewer, we don't need
		// this xml tags, and we should skip it.
		
		if (!(experiment instanceof ExperimentWithoutMetrics))
			return;
		
		TraceAttribute attribute = new TraceAttribute();
		
		attribute.dbUnitTime = TraceAttribute.PER_NANO_SECOND;
		
		// tallent: Note that the DTD currently only permits one instance of <TraceDB>
		for (int i=0; i<attributes.length; i++) {
			
			if (attributes[i].charAt(0) == 'i') {
			} else if (attributes[i].charAt(0) == 'u') {
				// unit time
				try {
					attribute.dbUnitTime = Long.parseLong(values[i]);
				} catch (Exception e) {
					attribute.dbUnitTime = TraceAttribute.PER_NANO_SECOND;
				}
				
			} else if (attributes[i].equals("db-glob")) {
				attribute.dbGlob = values[i];
				
			} else if (attributes[i].equals("db-min-time")) {
				attribute.dbTimeMin = Long.parseLong(values[i]);
				
			} else if (attributes[i].equals("db-max-time")) {
				attribute.dbTimeMax = Long.parseLong(values[i]);

			} else if (attributes[i].equals("db-header-sz")) {
				attribute.dbHeaderSize = Integer.parseInt(values[i]);
			}
		}
		((ExperimentWithoutMetrics)experiment).setTraceAttribute(attribute);
	}



	//===============================================
	// Utilities that may be used by children of the class
	//===============================================
	
	/*************************************************************************
	 *	Begins processing a profile data (program) element.
	 ************************************************************************/
	protected void begin_SecData(String[] attributes, String[] values) 
	{
		// make the root scope
		this.rootScope = new RootScope(this.experiment, "Invisible Outer Root Scope", RootScopeType.Invisible);
		this.scopeStack.push(this.rootScope);	// don't use 'beginScope'

		final String title;
		final RootScopeType rootType;
		
		if (this.csviewer) {
			title = Experiment.TITLE_TOP_DOWN_VIEW;
			rootType = RootScopeType.CallingContextTree;
		} else {
			title = Experiment.TITLE_FLAT_VIEW;
			rootType = RootScopeType.Flat;
		}
		this.viewRootScope  = new RootScope(this.experiment, title, rootType);
		beginScope(this.viewRootScope);
	}


	/************************************************************************* 
	 * treat XML attributes like a named property list; this is an alternative to a brittle
	 * position-based approach for recognizing attributes
	 *************************************************************************/
	protected String getAttributeByName(String name, String[] attributes, String[] values)
	{
		for (int i = 0; i < attributes.length; i++) 
			if (name.equals(attributes[i])) 
				return values[i];
		return null;
	}


//  ------------------------------------------------------------------- //
//	SCOPE TREE BUILDING													//
//  ------------------------------------------------------------------- //
	
	/*************************************************************************
	 * Add child node to the parent.
	 * We'll verify if the child should be invisible or not. If the child is
	 *  invisible and we need to remove invisible node, we shouldn't add the 
	 *  child to the parent
	 *  
	 * @param parent
	 * @param child
	 *************************************************************************/
	private void addChildToParent(Scope parent, Scope child) {

		if (removeInvisibleProcedure()) {
			if (child instanceof ProcedureScope) {
				if (((ProcedureScope)child).isInvisible()) {
					// don't add to the tree if the scope has to be invisible
					return;
				}
			}
		}
		parent.addSubscope(child);
		child.setParent(parent);
	}
	
	/*************************************************************************
	 *	Adds a newly parsed scope to the scope tree.
	 ************************************************************************/
	private void beginScope(Scope scope)
	{
		// add to the tree if the scope is visible
		// if the scope is not visible, we don't add to the parent, and 
		//   thus make it disappear from the tree.
		
		Scope top = getCurrentScope();
		
		addChildToParent(top, scope);
		
		// push the new scope to the stack
		scopeStack.push(scope);
		
		min_cctid = Math.min(min_cctid, scope.getCCTIndex());
		max_cctid = Math.max(max_cctid, scope.getCCTIndex());
		
		if (scope instanceof RootScope) {
			rootStack.push((RootScope)scope);
		}
	}

	
	/*************************************************************************
	 *	Ends a newly parsed scope.
	 ************************************************************************/
	protected void endScope()
	{
		try {
			Scope scope = this.scopeStack.pop();
			if (scope instanceof RootScope) {
				rootStack.pop();
			}

		} catch (java.util.EmptyStackException e) {
			System.out.println("End of stack:"+this.parser.getLineNumber());
		}
	}

	/*************************************************************************
	 *	Returns the current scope.
	 ************************************************************************/
	protected Scope getCurrentScope()
	{
		return this.scopeStack.peek();
	}

	/*************************************************************************
	 *   Get the File for a callsite 	
	 *   Using Hashtable to store the "FileSystemSourceFile" object 
	 *   for the callsite's file attribute.
	 ************************************************************************/
	protected SourceFile getOrCreateSourceFile(String fileLine, int keyFile)
	{
		SourceFile sourceFile=hashSourceFileTable.get(keyFile);
		if (sourceFile == null) {
			File filename = new File(fileLine);
			sourceFile = new FileSystemSourceFile(experiment, filename, keyFile);
			this.hashSourceFileTable.put(Integer.valueOf(keyFile), sourceFile);
		}  

		return sourceFile;
	}

	

	/*************************************************************************
	 *	Initializes the build process.
	 ************************************************************************/

	public void begin()
	{
		this.configuration = new ExperimentConfiguration();
	}


	/*************************************************************************
	 *	Takes notice of content characters within an element.
	 *
	 *	None of the elements in an hpcviewer experiment XML file have content
	 *	characters, so this method should never be called.
	 *
	 ************************************************************************/
	public void content(String s)
	{
		Dialogs.notCalled("ExperimentBuilder.content");
	}



	/*************************************************************************
	 *	Finalizes the build process.
	 ************************************************************************/

	public void end()
	{
		// bugs no 224: https://outreach.scidac.gov/tracker/index.php?func=detail&aid=224&group_id=22&atid=169
		try {
			// pop out root scope
			this.scopeStack.pop();
		} catch (EmptyStackException e) {
			System.err.println("ExperimentBuilder: no root scope !");
		}
		
		// check that input was properly nested
		if (!this.scopeStack.empty()) {
			Scope topScope = this.scopeStack.peek();
			System.out.println("Stack is not empty; remaining top scope = " + topScope.getName());
			this.error();
		}

		// copy parse results into configuration
		experiment.setConfiguration(this.configuration);
		experiment.setRootScope(this.rootScope);
		experiment.setMinMaxCCTID(min_cctid, max_cctid);
		
		// supply defaults for missing info
		if( this.configuration.getName(ExperimentConfiguration.NAME_EXPERIMENT) == null )
			this.configuration.setName(ExperimentConfiguration.NAME_EXPERIMENT, this.defaultName);
		
		if( this.configuration.getSearchPathCount() == 0 )
		{
			List<File> paths = new ArrayList<File>();
			paths.add(new File(""));
			paths.add(new File("src"));
			paths.add(new File("compile"));
			this.configuration.setSearchPaths(paths);
		}

	}

	

	
	//--------------------------------------------------------------------------------
	// Utilities
	//--------------------------------------------------------------------------------

	
	private String getProcedureName(String sProcIndex) {
		String sProcName = PROCEDURE_UNKNOWN;
		boolean hashtableExist = (this.hashProcedureTable.size()>0);
		
		if(hashtableExist) {
			try {
				Integer objProcID = Integer.parseInt(sProcIndex); 
				// get the real name of the procedure from the dictionary
				return this.hashProcedureTable.get(objProcID);
				
			} catch (java.lang.NumberFormatException e) {
				System.err.println("Warning: Procedure index doesn't exist: " + sProcIndex);
			}
		} else {
			// the database of procedure doesn't exist. This can be a flat view.
			sProcName = sProcIndex;
		}
		return sProcName;
	}
	
	/**
	 * Return true if we need to remove invisible procedures from the tree
	 * 
	 * @return
	 */
	private boolean removeInvisibleProcedure() {
		return this.removeInvisibleProcedure;
	}
	
	/**
	 * set true to remove invisible procedures (like <no activity)
	 * 
	 * @param tobeRemoved boolean true if they are to be removed. 
	 *   False otherwise.
	 */
	public void setRemoveInvisibleProcedure(boolean tobeRemoved) {
		this.removeInvisibleProcedure = tobeRemoved;
	}
	
	/*************************************************************************
	 * Class to treat a string of line or range of lines into two lines: first line and last line 
	 * @author laksonoadhianto
	 *
	 ************************************************************************/
	static private class StatementRange {
		private int firstLn;
		private int lastLn;
		
		public StatementRange(String sLine) {
			// find the range separator
			int iSeparator = sLine.indexOf('-');
			if(iSeparator > 0) {
				// separator exist, it should be a range
				this.firstLn = Integer.parseInt( sLine.substring(0,iSeparator) );
				this.lastLn = Integer.parseInt( sLine.substring(iSeparator+1) );
			} else {
				// no separator: no range
				this.firstLn = Integer.parseInt(sLine);
				this.lastLn = this.firstLn;
			}
		}
		
		public int getFirstLine( ) { return this.firstLn; }
		public int getLastLine( ) { return this.lastLn; }
	}

}
