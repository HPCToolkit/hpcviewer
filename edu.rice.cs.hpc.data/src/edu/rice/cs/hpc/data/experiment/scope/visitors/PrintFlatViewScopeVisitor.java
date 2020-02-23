package edu.rice.cs.hpc.data.experiment.scope.visitors;

import java.io.PrintStream;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
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
import edu.rice.cs.hpc.data.experiment.source.SourceFile;
import edu.rice.cs.hpc.data.experiment.xml.PrintFileXML;


/****************************************************************************************
 * 
 * @author laksonoadhianto
 *
 ****************************************************************************************/
public class PrintFlatViewScopeVisitor implements IScopeVisitor {
	static private StringBuffer indent;

	private Experiment objExperiment;
	private PrintStream objOutputStream;

	
	public PrintFlatViewScopeVisitor(Experiment experiment, PrintStream stream) {
		this.objExperiment = experiment;
		this.objOutputStream = stream;
		indent = new StringBuffer();
	}
	
	//----------------------------------------------------
	// visitor pattern instantiations for each Scope type
	//----------------------------------------------------

	public void visit(Scope scope, ScopeVisitType vt) { print(scope, "u", vt, false, false); }
	public void visit(RootScope scope, ScopeVisitType vt) { 
		if (vt == ScopeVisitType.PreVisit) printMetrics(scope); 
	}
	public void visit(LoadModuleScope scope, ScopeVisitType vt) { print(scope, "LM", vt, true, false); }
	public void visit(FileScope scope, ScopeVisitType vt) { print(scope, "F", vt, true, false); }
	public void visit(ProcedureScope scope, ScopeVisitType vt) { print(scope, "P", vt, true, true); }
	public void visit(AlienScope scope, ScopeVisitType vt) { print(scope, "A", vt, true, true); }
	public void visit(LoopScope scope, ScopeVisitType vt) { print(scope, "L", vt, false, true); }
	public void visit(LineScope scope, ScopeVisitType vt) { print(scope, "S", vt, false, true); }
	public void visit(StatementRangeScope scope, ScopeVisitType vt) { print(scope, "S", vt, false, true); }
	public void visit(CallSiteScope scope, ScopeVisitType vt) { printCallSite(scope, vt);	}
	public void visit(GroupScope scope, ScopeVisitType vt) { print(scope, "G", vt, false, true); }

	/**-------------------------------------------------------------------------------**
	 * Print the scope information into XML tag
	 * @param scope
	 * @param initial
	 * @param vt
	 * @param name
	 * @param line
	 **-------------------------------------------------------------------------------**/
	private void print(Scope scope, String initial, ScopeVisitType vt, 
			boolean name, boolean line) {
		if (vt == ScopeVisitType.PreVisit) {
			
			//--------------------------------------------------
			// print the scope tag, attributes and values
			//--------------------------------------------------
			Scope objScopeToPrint = scope;
			if ( (scope instanceof CallSiteScope) && initial.equals("PF") ) 
				objScopeToPrint = ((CallSiteScope) scope).getProcedureScope();
			
			this.printScopeTag(objScopeToPrint, initial, name, line);

			//--------------------------------------------------
			// print the metric values of this scope, except callsite
			//--------------------------------------------------
			if (initial.charAt(0) != 'C')
				this.printMetrics(scope);
			
			//--------------------------------------------------
			// increment the indentation for future usage
			//--------------------------------------------------
			indent.append(' ');
			
		} else {
			
			indent.deleteCharAt(0);
			this.objOutputStream.println(indent + "</" + initial + ">" );
			
		}
	}
	
	
	private void printCallSite( CallSiteScope scope, ScopeVisitType vt) {
		if (vt == ScopeVisitType.PreVisit)  {
			print(scope, "C", vt, false, true);
			//-------------------------------------------------------------------
			// a call site contains information of line scope AND procedure scope.
			// we need to generate both
			//-------------------------------------------------------------------
			print(scope, "PF", vt, true, true); 

		} else {
			print(scope, "PF", vt, true, false); 
			print(scope, "C", vt, true, false);
			
		}
	}
	
	/**-------------------------------------------------------------------------------**
	 * Print the tag of the scope, including its attributes
	 * @param objScopeToPrint
	 * @param initial
	 * @param name
	 * @param line
	 **-------------------------------------------------------------------------------**/
	private void printScopeTag(Scope objScopeToPrint, String initial, boolean name, boolean line) {
		this.objOutputStream.print(indent + "<" + initial);
		PrintFileXML.printAttribute(objOutputStream, "i", objScopeToPrint.getFlatIndex());
		
		if (name) {
			final String scopeName;
			// if the scope is a file, we have to print the original path from hpcprof
			if (objScopeToPrint instanceof FileScope) 
			{
				SourceFile srcFile = ((FileScope)objScopeToPrint).getSourceFile();
				if (srcFile != null && srcFile.getFilename()!= null)
					scopeName = srcFile.getFilename().toString();
				else
					scopeName = objScopeToPrint.getName();
			} else {
				scopeName = objScopeToPrint.getName();
			}
			// 2010.12.17 Ashay addition:
			// Escape the "name" so that we conform to a valid XML syntax
			String sName = scopeName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
			PrintFileXML.printAttribute(objOutputStream, "n", sName);
		}
		
		if (line) {
			Scope linescope = objScopeToPrint;
			if ( (objScopeToPrint instanceof CallSiteScope) && initial.equals("C")) {
				// for call site, we just need to get the line number from its line scope
				linescope = ((CallSiteScope) objScopeToPrint).getLineScope();
			}
			// the original data decrement the line number by 1,
			// 	in return we need to increment again
			int line1 = linescope.getFirstLineNumber() + 1;
			int line2 = linescope.getLastLineNumber() + 1;
			if (line1 == line2)
				PrintFileXML.printAttribute(objOutputStream, "l", line1);
			else 
				PrintFileXML.printAttribute(objOutputStream, "l", line1 + "-" + line2);
		}
		
		if (objScopeToPrint instanceof AlienScope) {
			SourceFile objFile = objScopeToPrint.getSourceFile();
			if (objFile != null)
				PrintFileXML.printAttribute(objOutputStream, "f", objFile.getFilename());
		}
		this.objOutputStream.println(">" );
	}
	
	/***-------------------------------------------------------------------------------**
	 * 
	 * @param scope
	 ***-------------------------------------------------------------------------------**/
	private void printMetrics(Scope scope) {

		int nbMetrics = objExperiment.getMetricCount();
		for (int i=0; i<nbMetrics; i++) {
			MetricValue value = scope.getMetricValue(i);
			if (MetricValue.isAvailable(value)) {
				BaseMetric m = objExperiment.getMetric(i);
				this.objOutputStream.print(indent + "<M");
				PrintFileXML.printAttribute(this.objOutputStream, "n", m.getIndex());
				PrintFileXML.printAttribute(this.objOutputStream, "v", MetricValue.getValue(value));
				this.objOutputStream.print("/>");
			}
		}
		this.objOutputStream.println();
	}
}
