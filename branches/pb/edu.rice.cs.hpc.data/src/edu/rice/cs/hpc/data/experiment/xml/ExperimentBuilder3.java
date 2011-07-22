/*Parses PB messages into scopes*/
package edu.rice.cs.hpc.data.experiment.xml;

import edu.rice.cs.hpc.data.experiment.*;
import edu.rice.cs.hpc.data.experiment.metric.*;
import edu.rice.cs.hpc.data.experiment.scope.*;
import edu.rice.cs.hpc.data.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.experiment.xml.Token2.TokenXML;
import edu.rice.cs.hpc.data.util.*;

import java.io.*;
import java.util.HashMap;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
// laks 2008.08.27
import java.util.EmptyStackException;
import Protobuf.*;


public class ExperimentBuilder3
{
	private HashMap<Integer,Scope> cctNodeMap;
	private HashMap<Integer,com.google.protobuf.GeneratedMessage> structTreeMap;
	protected Experiment experiment;
	protected ExperimentConfiguration configuration;
	private boolean metrics_needed;
	protected List<BaseMetric> metricList;
	protected List<MetricRaw> metricRawList;
	private enum MetricValueDesc {Raw, Final, Derived_Incr, Derived}
	final protected int maxNumberOfMetrics = 10000;
	protected HashMap <Integer, SourceFile> hashSourceFileTable = new HashMap<Integer, SourceFile>();
	private HashMap<Integer, String> hashProcedureTable;
	private HashMap<Integer, LoadModuleScope> hashLoadModuleTable;
	private Scope rootScope;
	
	//Constructor Declaration
	public ExperimentBuilder3(InputStream input,Experiment exp)throws IOException
	{
		this.cctNodeMap=new HashMap<Integer,Scope>();
		this.configuration=new ExperimentConfiguration();
		this.experiment=exp;
		this.metrics_needed=true;
		this.hashProcedureTable= new HashMap<Integer, String>();
		this.hashLoadModuleTable = new HashMap<Integer, LoadModuleScope>();
		this.structTreeMap=new HashMap<Integer,com.google.protobuf.GeneratedMessage>();
		this.experiment.setFileTable(this.hashSourceFileTable);
		this.rootScope=new RootScope(this.experiment,this.configuration.getName(),"Invisible Outer Root Scope",
				RootScopeType.Invisible);
		if(metrics_needed)
		{
			metricList=new ArrayList<BaseMetric>();
		}
		metricRawList=new ArrayList<MetricRaw>();
		try
		{
			N.Name name=N.Name.parseDelimitedFrom(input);
			this.experiment.setVersion(name.getVersion());
			this.configuration.setName(name.getName());
			Sec.SectionHeader metricMgrData=Sec.SectionHeader.parseDelimitedFrom(input);
			setMetricTable(metricMgrData.getMTable());
			if (metrics_needed)
				this.experiment.setMetrics(this.metricList);
			setMetricRawTable(metricMgrData.getMDbTable());
			if(metricMgrData.getTDbTable().getTraceDbListCount()!=0)
			{
				setTraceDB(metricMgrData.getTDbTable().getTraceDbList(0));
			}
			TreeRoot.Type nextNodeType=TreeRoot.Type.parseDelimitedFrom(input);
			final int NODE_TERMINATOR=9;
			while(nextNodeType.getType()!=NODE_TERMINATOR)
			{
				switch(nextNodeType.getType())
				{
				case 0:							//Root
					doStructRoot(input);
					break;
/*
				case 1:							//Group
					doStructGroup(input);
					break;
*/
				case 2:							//LM
					doStructLM(input);
					break;
				case 3:							//File
					doStructFile(input);
					break;
				case 4:							//Proc
					doStructProc(input);
					break;
				case 5:							//Alien
					doStructAlien(input);
					break;
				case 6:							//Stmt
					doStructStmt(input);
					break;
				case 7:							//Loop
					doStructLoop(input);
					break;
/*					
				case 8:							//Ref
					doStructRef(input);
					break;
*/
				default:
					System.out.println("This shouldn't happen");
				}
				nextNodeType=TreeRoot.Type.parseDelimitedFrom(input);
			}
			this.experiment.setFileTable(this.hashSourceFileTable);
			this.experiment.finalizeDatabase();
			CCTTreePB.GenNode cctNode=CCTTreePB.GenNode.parseDelimitedFrom(input);
			while(cctNode.getType()!=NODE_TERMINATOR)
			{
				switch(cctNode.getType())
				{
				case 0:							//Root
					doCCTRoot(cctNode);
					break;
				case 1:							//ProcFrm
					doCCTProcFrm(cctNode);
					break;
				case 2:							//Proc
					doCCTProcFrm(cctNode);
					break;
				case 3:							//Loop
					doCCTLoop(cctNode);
					break;
				case 4:							//Call
					doCCTCall(cctNode);
					break;
				case 5:							//Stmt
					doCCTStmt(cctNode);
					break;
				default:
					System.out.println("This shouldn't happen");
				}
				cctNode=CCTTreePB.GenNode.parseDelimitedFrom(input);
			}
			this.configuration.setSearchPaths(new ArrayList());
			this.experiment.setConfiguration(this.configuration);
			this.experiment.setScopes(null, this.rootScope);
			if( this.configuration.getSearchPathCount() == 0 )
			{
				List/*<File>*/ paths = new ArrayList/*<File>*/();
				paths.add(new File(""));
				paths.add(new File("src"));
				paths.add(new File("compile"));
				this.configuration.setSearchPaths(paths);
			}
		}
		catch(IOException e)
		{
			System.out.println("oops");
		}
		this.end();
		this.iterate(this.rootScope);
	}
	
	
	//just for testing
	private void iterate(Scope s)
	{
		itUp(s,0);
		for(int i=0;i<s.getChildCount();i++)
		{
			this.iterate((Scope)s.getChildAt(i));
		}
	}
	private void itUp(Scope s, int i)
	{	
		if(s!=null)
			this.itUp(s.getParentScope(),i+1);
	}
	
	//Method Declarations
	private void doStructRoot(InputStream input)
	{
		try
		{
			TreeRoot.Root node=TreeRoot.Root.parseDelimitedFrom(input);
			this.structTreeMap.put(node.getId(),node);
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
/*	
	private void doStructGroup(InputStream input)
	{
		try
		{
			TreeRoot.Root.Group node=TreeRoot.Root.Group.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
*/	
	
	
	private void doStructLM(InputStream input)
	{
		try
		{
			TreeRoot.Root.LM node=TreeRoot.Root.LM.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
			LoadModuleScope lmScope = new LoadModuleScope(this.experiment, node.getName(), null, node.getId());
			this.hashLoadModuleTable.put(node.getId(), lmScope);
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
	private void doStructFile(InputStream input)
	{
		try
		{
			TreeRoot.Root.File node=TreeRoot.Root.File.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
			if(node.hasId()&&node.hasName()){
				SourceFile sourceFile = this.getOrCreateSourceFile(node.getName(), node.getId());
			}
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
	private void doStructProc(InputStream input)
	{
		try
		{
			TreeRoot.Root.Proc node=TreeRoot.Root.Proc.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
			if(node.hasId()&&node.hasName()){
				this.hashProcedureTable.put(node.getId(), node.getName());
			}
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
	private void doStructAlien(InputStream input)
	{
		try
		{
			TreeRoot.Root.Alien node=TreeRoot.Root.Alien.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
			if(node.hasId()&&node.hasName()){
				SourceFile sourceFile = this.getOrCreateSourceFile(node.getName(), node.getId());
			}
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
	private void doStructLoop(InputStream input)
	{
		try
		{
			TreeRoot.Root.Loop node=TreeRoot.Root.Loop.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
	private void doStructStmt(InputStream input)
	{
		try
		{
			TreeRoot.Root.Stmt node=TreeRoot.Root.Stmt.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
	
	
/*	
	private void doStructRef(InputStream input)
	{
		try
		{
			TreeRoot.Root.Ref node=TreeRoot.Root.Ref.parseDelimitedFrom(input);
			structTreeMap.put(node.getId(),node);
		}
		catch(IOException e)
		{
			System.out.println("NOOOOOOOOOO");
		}
	}
*/	
	
	
	private void doCCTRoot(CCTTreePB.GenNode node)
	{
		Scope scope  = new RootScope(this.experiment,this.configuration.getName(),"Calling Context View", RootScopeType.CallingContextTree);
		cctNodeMap.put(node.getId(),scope);
		this.rootScope.addSubscope(scope);
		scope.setParentScope(this.rootScope);
		for(int i=0;i<node.getMetricValuesCount();i++)
		{
			doMetric(node.getMetricValues(i),scope);
		}
	}
	
	
	private void doCCTProcFrm(CCTTreePB.GenNode node)
	{
		SourceFile srcf=getOrCreateSourceFile(""+node.getFile(),node.getFile());
		LoadModuleScope objLoadMod=getLoadModule(node.getStaticScopeId(),node.getParentId());
		StatementRange stmtR=new StatementRange(((TreeRoot.Root.Proc)this.structTreeMap.get(node.getStaticScopeId())).getLineRange());
		Scope scope=new ProcedureScope(this.experiment,objLoadMod,srcf,stmtR.getFirstLine()-1,
				stmtR.getLastLine()-1,getProcedureName(node.getStaticScopeId()),false,node.getId(),node.getStaticScopeId());
		cctNodeMap.put(node.getId(),scope);
		cctNodeMap.get(node.getParentId()).addSubscope(scope);
		scope.setParentScope(cctNodeMap.get(node.getParentId()));
		for(int i=0;i<node.getMetricValuesCount();i++)
		{
			doMetric(node.getMetricValues(i),scope);
		}
	}
	
	
	private void doCCTLoop(CCTTreePB.GenNode node)
	{	
		StatementRange objRange = new StatementRange(((TreeRoot.Root.Proc)structTreeMap.get
				(node.getStaticScopeId())).getLineRange());
		Scope scope=new LoopScope(this.experiment,null,objRange.getFirstLine()-1,objRange.getLastLine()-1,
				node.getId(),node.getStaticScopeId());
		cctNodeMap.put(node.getId(),scope);
		cctNodeMap.get(node.getParentId()).addSubscope(scope);
		scope.setParentScope(cctNodeMap.get(node.getParentId()));
		for(int i=0;i<node.getMetricValuesCount();i++)
		{
			doMetric(node.getMetricValues(i),scope);
		}
	}
	
	
	private void doCCTCall(CCTTreePB.GenNode node)
	{
		StatementRange objRange;
		if(structTreeMap.get(node.getStaticScopeId())!=null)
		{
			objRange = new StatementRange(((TreeRoot.Root.Stmt)structTreeMap.get
				(node.getStaticScopeId())).getLineRange());
		}
		else
		{
			objRange = new StatementRange("0");
		}	
		Scope scope=new LineScope(this.experiment,null,objRange.getFirstLine()-1,node.getId(),node.getStaticScopeId());
		Scope parentScope=cctNodeMap.get(node.getParentId());
		cctNodeMap.put(node.getId(),parentScope);
		//parentScope.addSubscope(scope);
		//scope.setParentScope(parentScope);
		for(int i=0;i<node.getMetricValuesCount();i++)
		{
			doMetric(node.getMetricValues(i),parentScope);
		}
	}
	
	
	private void doCCTStmt(CCTTreePB.GenNode node)
	{
		StatementRange objRange;
		if(structTreeMap.get(node.getStaticScopeId())!=null)
		{
			objRange = new StatementRange(((TreeRoot.Root.Stmt)structTreeMap.get
				(node.getStaticScopeId())).getLineRange());
		}
		else
		{
			objRange = new StatementRange(""+node.getLineRange());
		}
		SourceFile srcf=(SourceFile) this.hashSourceFileTable.get(node.getFile());
		Scope scope=new LineScope(this.experiment,srcf,objRange.getFirstLine()-1,node.getId(),node.getStaticScopeId());
		cctNodeMap.put(node.getId(),scope);
		cctNodeMap.get(node.getParentId()).addSubscope(scope);
		scope.setParentScope(cctNodeMap.get(node.getParentId()));
		for(int i=0;i<node.getMetricValuesCount();i++)
		{
			doMetric(node.getMetricValues(i),scope);
		}
	}
	
	
	private void setMetricTable(Sec.SectionHeader.MetricTable mTable) 
{
		if (!metrics_needed)
			return;

		for(int i=0;i<mTable.getMListCount();i++){
			this.setMetric(mTable.getMList(i));
		}
		
		int nbMetrics = this.metricList.size();
		
		for (int i=0; i<nbMetrics; i++) {
			BaseMetric objMetric = (BaseMetric) this.metricList.get(i);
			if (objMetric instanceof AggregateMetric) {
				AggregateMetric aggMetric = (AggregateMetric) objMetric;
				aggMetric.init(this.experiment);
			}
		}
	}
	
	
	private void setMetric(Sec.SectionHeader.MetricTable.Metric metricPB)
	{
		if (!metrics_needed)
			return;
		
		int nbMetrics = this.metricList.size();
		String sID = null;// = values[nID];
		int iSelf = -1;
		int partner = 0;	// 2010.06.28: new feature to add partner
		String sDisplayName = null;
		String sNativeName = null;
		boolean toShow = true, percent = true;
		MetricType objType = MetricType.EXCLUSIVE;
		boolean needPartner = metricPB.hasPartner();
		MetricValueDesc mDesc = MetricValueDesc.Raw; // by default is a raw metric
		String format = null;
		
		if(metricPB.hasId()){
			iSelf = metricPB.getId();// storing an asterisk is not supported with the current PB implementation
			sID = ""+metricPB.getId();
		}
		if(metricPB.hasName()) {
			sNativeName = metricPB.getName();
		}
		if(metricPB.hasValue()) {
			if(metricPB.getMetricFormulaListCount()==0) {
				if(metricPB.getValue()==0) {
					mDesc = MetricValueDesc.Raw;
				}
				else if(metricPB.getValue()==2) {
					mDesc = MetricValueDesc.Final;
					needPartner = false;
				}
				else {
					System.out.println("Error: ExperimentBuilder3 setMetric 1");
				}
			}
			else {
				if(metricPB.getValue()==1) {
					mDesc = MetricValueDesc.Derived_Incr;
					needPartner = false;
				}
				else if(metricPB.getValue()==2) {
					mDesc = MetricValueDesc.Derived;
				}
				else {
					System.out.println("Error: ExperimentBuilder3 setMetric 2");
				}
			}
		}
		if(metricPB.hasType()){
			if(metricPB.getType()==1){
				objType = MetricType.INCLUSIVE;
			}
			else if(metricPB.getType()==2){
				objType = MetricType.EXCLUSIVE;
			}
			else{
				System.out.println("Invalid type for metric "+metricPB.getType());
			}
		}
		//format = metricPB.getFmt(); //fmt is currently an int in PB implementation due to the fact that it was never seen to be used
		if(metricPB.hasShowPercent())
			percent =  metricPB.getShowPercent();
		if(metricPB.hasShow())
			toShow = metricPB.getShow();
		if(metricPB.hasPartner())
			partner = metricPB.getPartner();
		/*************************************************************************************
		 * The rest of this code in this method is copied directly from the do_METRIC method
		 * up until the comment at the end
		 *************************************************************************************/
		// Laks 2009.01.14: if the database is call path database, then we need
		//	to distinguish between exclusive and inclusive
		if (needPartner) {
			sDisplayName = sNativeName + " (I)";
			objType = MetricType.INCLUSIVE;
			partner = this.maxNumberOfMetrics + iSelf;
		} else {
			// this metric is not for inclusive, the display name should be the same as the native one
			sDisplayName = sNativeName;
		}
		
		// set the metric
		BaseMetric metricInc;
		switch (mDesc) {
			case Final:
				metricInc = new FinalMetric(
						String.valueOf(iSelf),			// short name
						sNativeName,			// native name
						sDisplayName, 	// display name
						toShow, format, percent, 			// displayed ? percent ?
						"",						// period (not defined at the moment)
						objType, partner);
				break;
			case Derived_Incr:
				metricInc = new AggregateMetric(sID, sDisplayName, toShow, format, percent, nbMetrics, partner, objType);
				break;
			case Raw:
			case Derived:
			default:
				metricInc = new Metric(
						String.valueOf(iSelf),			// short name
						sNativeName,			// native name
						sDisplayName, 	// display name
						toShow, format, percent, 			// displayed ? percent ?
						"",						// period (not defined at the moment)
						objType, partner);
				break;
		}

		this.metricList.add(metricInc);

		// Laks 2009.01.14: only for call path profile
		// Laks 2009.01.14: if the database is call path database, then we need
		//	to distinguish between exclusive and inclusive
		if (needPartner) {
			// set the exclusive metric
			String sSelfName = String.valueOf(partner);	// I am the partner of the inclusive metric
			// Laks 2009.02.09: bug fix for not reusing the existing inclusive display name
			String sSelfDisplayName = sNativeName + " (E)";
			Metric metricExc = new Metric(
					sSelfName,			// short name
					sSelfDisplayName,	// native name
					sSelfDisplayName, 	// display name
					toShow, format, true, 		// displayed ? percent ?
					"",					// period (not defined at the moment)
					MetricType.EXCLUSIVE, nbMetrics);
			this.metricList.add(metricExc);
		}
		/********************************************************************
		 * The code between this comment and it's pair above is from the
		 * do_METRIC method
		 ********************************************************************/
		for(int i=0;i<metricPB.getMetricFormulaListCount();i++){
			this.setMetricFormula(metricPB.getMetricFormulaList(i));
		}
	}
	
	
	private void setMetricFormula(Sec.SectionHeader.MetricTable.Metric.MetricFormula mf) 
	{
		if (!metrics_needed)
			return;
		
		int nbMetrics= this.metricList.size();
		AggregateMetric objMetric = (AggregateMetric) this.metricList.get(nbMetrics-1);
		if(mf.getType()){
			objMetric.setFormula('c', mf.getFormula());
		}
		else {
			objMetric.setFormula('f', mf.getFormula());
		}
	}
	
	
	private void setMetricRaw(Sec.SectionHeader.MetricDBTable.MetricDB metricDB)
	{
		int ID = 0;
		String title = null;
		String db_glob = null;
		int db_id = 0;
		int num_metrics = 0;
		
		if(metricDB.hasId())
			ID = metricDB.getId();
		if(metricDB.hasName())
			title = metricDB.getName();
		if(metricDB.hasDbGlob())
			db_glob = metricDB.getDbGlob();
		if(metricDB.hasDbId())
			db_id = metricDB.getDbId();
		if(metricDB.hasDbNumMetrics())
			num_metrics = metricDB.getDbNumMetrics();
		
		MetricRaw metric = new MetricRaw(ID, title, db_glob, db_id, num_metrics);
		this.metricRawList.add(metric);
	}

	
	public void setMetricRawTable(Sec.SectionHeader.MetricDBTable mDBTable)
	{
		for(int i=0;i<mDBTable.getMetricDbListCount();i++){
			this.setMetricRaw(mDBTable.getMetricDbList(i));
		}
		if (this.metricRawList != null && this.metricRawList.size()>0) {
			MetricRaw[] metrics = new MetricRaw[metricRawList.size()];
			this.metricRawList.toArray( metrics );
			this.experiment.setMetricRaw( metrics );
		}
	}

	
	public void setTraceDB(Sec.SectionHeader.TraceDBTable.TraceDB traceDB)
	{
		// tallent: Note that the DTD currently only permits one instance of <TraceDB>
		if(traceDB.hasDbMinTime()){
			experiment.trace_minBegTime = traceDB.getDbMinTime();
		}
		if(traceDB.hasDbMaxTime()){
			experiment.trace_maxEndTime = traceDB.getDbMaxTime();
		}
		//no code was created for id, db_glob or db_header_sz so as to mirror the do_TraceDB Method
	}


	protected SourceFile getOrCreateSourceFile(String fileLine, int keyFile)
	{
		SourceFile sourceFile=(SourceFile) this.hashSourceFileTable.get(keyFile);
		if (sourceFile == null) {
			File filename = new File(fileLine);
			sourceFile = new FileSystemSourceFile(experiment, filename, keyFile);
			this.hashSourceFileTable.put(Integer.valueOf(keyFile), sourceFile);
		}  

		return sourceFile;
	}


	private class StatementRange 
{
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

	
	private String getProcedureName(int sProcIndex) 
{
		String sProcName = "unknown procedure";
		boolean hashtableExist = (this.hashProcedureTable.size()>0);
		if(hashtableExist) {
			try {
				// get the real name of the procedure from the dictionary
				String sProc = this.hashProcedureTable.get(sProcIndex);
				if(sProc != null) {
					sProcName = sProc;
				}
			} catch (java.lang.NumberFormatException e) {
				System.err.println("Warning: Procedure index doesn't exist: " + sProcIndex);
			}
		} else {
			// the database of procedure doesn't exist. This can be a flat view.
			sProcName = ""+sProcIndex;
		}
		return sProcName;
	}
	
	
	private void doMetric(CCTTreePB.Metric m,Scope objCurrentScope)
	{
		if (!metrics_needed)
			return;
		
		// m n="abc" v="4.56e7"
		// add a metric value to the current scope
		String internalName = ""+m.getName();
		double actualValue  = m.getValue();
		
		BaseMetric metric = this.experiment.getMetric(internalName);
		// get the sample period
		double prd = metric.getSamplePeriod();

		// multiple by sample period 
		actualValue = /* prd * */ actualValue;
		MetricValue metricValue = new MetricValue(actualValue);
		
		objCurrentScope.setMetricValue(metric.getIndex(), metricValue);
		// update also the self metric value for calling context only
		if (metric.getMetricType() == MetricType.INCLUSIVE) {

			//----------------------------------------------------------------------------
			// Final metric (inherited from Metric) doesn't need partner. It is final.
			//----------------------------------------------------------------------------
			if (!(metric instanceof FinalMetric) && metric instanceof Metric) {
				int partner = ( (Metric) metric).getPartnerIndex();
				String selfShortName = String.valueOf(partner);

				BaseMetric selfMetric = this.experiment.getMetric(selfShortName); 
				MetricValue selfMetricValue = new MetricValue(actualValue);
				objCurrentScope.setMetricValue(selfMetric.getIndex(), selfMetricValue);  
			}
		}
	}


	public void end()
	{
		// copy parse results into configuration
		this.experiment.setConfiguration(this.configuration);

		//this.experiment.setScopes(this.scopeList, this.rootScope);
		this.experiment.setScopes(null, this.rootScope);
		if( this.configuration.getSearchPathCount() == 0 )
		{
			List<File>paths = new ArrayList<File>();
			paths.add(new File(""));
			paths.add(new File("src"));
			paths.add(new File("compile"));
			this.configuration.setSearchPaths(paths);
		}

	}

	
	private SourceFile getSourceFile(int ssid,int parentId)
	{
		SourceFile sourceFile=(SourceFile) this.hashSourceFileTable.get(ssid);
		if(sourceFile!=null)
			return sourceFile;
		Scope parent=this.cctNodeMap.get(parentId);
		if(parent==null)
			return getOrCreateSourceFile("default",ssid);
		sourceFile = (SourceFile)parent.getSourceFile();
		if(sourceFile!=null)
			return sourceFile;
		return getSourceFile(parent.getFlatIndex(),parent.getParentScope().getCCTIndex());
	}
	
	
	private LoadModuleScope getLoadModule(int ssid,int parentId)
	{
		LoadModuleScope loadModule=(LoadModuleScope) this.hashLoadModuleTable.get(ssid);
		if(loadModule!=null)
			return loadModule;
		Scope parent=this.cctNodeMap.get(parentId);
		if(parent==null)
			return new LoadModuleScope(this.experiment,""+ssid,null,ssid);
		return getLoadModule(parent.getFlatIndex(),parent.getParentScope().getCCTIndex());
	}
	
}