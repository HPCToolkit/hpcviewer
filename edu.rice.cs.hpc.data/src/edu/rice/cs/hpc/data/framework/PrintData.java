package edu.rice.cs.hpc.data.framework;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpc.data.util.Constants;
import edu.rice.cs.hpc.data.util.ScopeComparator;
import edu.rice.cs.hpc.data.util.Util;


/****************************************************************
 * 
 * Class to print the summary of a database
 * 
 * <p>
 * Usage: hpcdata.sh [-o output_file] experiment_database
 * </p>
 *
 ****************************************************************/
public class PrintData 
{
	/***
	 * The main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PrintData objApp = new PrintData();
		PrintStream objPrint = System.out;
		String sFilename;
		boolean std_output = true;
		
		//------------------------------------------------------------------------------------
		// processing the command line argument
		//------------------------------------------------------------------------------------
		if ( (args == null) || (args.length==0)) {
			System.out.println("Usage: hpcdata.sh [-o output_file] experiment_database");
			return;
		} else  {
			sFilename = args[0];
			
			for (int i=0; i<args.length; i++) {
				if (args[i].equals("-o") && (i<args.length-1)) {
					String sOutput = args[i+1];
					File f = new File(sOutput);
					if (!f.exists())
						try {
							f.createNewFile();
						} catch (IOException e1) {
							e1.printStackTrace();
							return;
						}
					try {
						FileOutputStream file = new FileOutputStream(sOutput);
						try {
							objPrint = new PrintStream( file );
							std_output = false;
							i++;
						} catch (Exception e) {
							System.err.println("Error: cannot create the file " + sOutput + ": " +e.getMessage());
							return;
						}

					
					} catch (FileNotFoundException e2) {
						System.err.println("Error: cannot open the file " + sOutput + ": " +e2.getMessage());
						e2.printStackTrace();
					}
				} else {
					sFilename = args[i];
				}
			}
		}
		
		//------------------------------------------------------------------------------------
		// open the experiment if possible
		//------------------------------------------------------------------------------------
		PrintStream print_msg;
		if (std_output)
			print_msg = System.err;
		else
			print_msg = System.out;
		
		File objFile = new File(sFilename);

		print_msg.println("Opening database " + sFilename);
		
		Experiment experiment = null;
		
		if (objFile.isDirectory()) {
			File files[] = Util.getListOfXMLFiles(sFilename);
			for (File file: files) 
			{
				// only experiment*.xml will be considered as database file
				if (file.getName().startsWith(Constants.DATABASE_FILENAME)) {
					experiment = objApp.openExperiment(print_msg, file);
					if (experiment != null)
						break;
				}
			}
		} else {
			experiment = objApp.openExperiment(objPrint, objFile);
		}
		if (experiment == null) {
			print_msg.println("Incorrect database: " + objFile.getAbsolutePath());
			return;
		}
		
		//------------------------------------------------------------------------------------
		// print information
		//------------------------------------------------------------------------------------
		
		final int MegaBytes = 1024*1024;
	    long maxMemory = Runtime.getRuntime().maxMemory()/MegaBytes;

		objPrint.println("Max memory: " + maxMemory + " MB");
		objPrint.println();
		
		//------------------------------------------------------------------------------------
		// create derivative roots: bottom-up and flat trees
		// these trees are not created automatically for the sake of memory usage
		//------------------------------------------------------------------------------------

		RootScope rootCCT = experiment.getRootScope(RootScopeType.CallingContextTree);
		
		RootScope rootFlat = experiment.getRootScope(RootScopeType.Flat);
		experiment.createFlatView(rootCCT, rootFlat);
		
		RootScope rootBottomUp = experiment.getRootScope(RootScopeType.CallerTree);
		experiment.createCallersView(rootCCT, rootBottomUp);
		
		//------------------------------------------------------------------------------------
		// print the summary
		//------------------------------------------------------------------------------------
		BaseMetric []metrics = experiment.getMetrics();
		
		Object []roots = experiment.getRootScopeChildren();
		
		for(Object root: roots) {
			objPrint.println();

			RootScope aRoot = (RootScope) root;
			objPrint.println("Summary of " + aRoot.getType());
			
			// print root CCT metrics
			printScopeAndChildren(objPrint, aRoot, metrics);
		}
	}

	
	static private void printScopeAndChildren(PrintStream objPrint, Scope scope, BaseMetric []metrics) {
		
		// print root CCT metrics
		printMetrics(objPrint, scope, metrics, "");
		
		if (!scope.hasChildren())
			return;
		
		// sort the children from the highest value to the lowest based on the first metric
		
		Object []children = scope.getChildren();
		
		ScopeComparator comparator = new ScopeComparator();
		comparator.setMetric(metrics[0]);
		comparator.setDirection(ScopeComparator.SORT_DESCENDING);
		
		Arrays.sort(children, comparator);
		
		// print the first 5 children
		for(int i=0; i<Math.min(5, children.length); i++) {
			objPrint.println();
			
			Scope child = (Scope) children[i];
			printMetrics(objPrint, child, metrics, "   ");
		}
	}
	
	/****
	 * Print metric values of a given scope
	 * 
	 * @param objPrint print stream (either file or standard output)
	 * @param scope the node scope
	 * @param metrics list of metrics
	 * @param indent output indentation
	 */
	static private void printMetrics(PrintStream objPrint, Scope scope, BaseMetric []metrics, String indent) {
		
		objPrint.println(indent + "- " + scope.getName());
		
		for(BaseMetric metric: metrics) {
			if (scope.getMetricValue(metric) == MetricValue.NONE)
				continue;
			
			objPrint.println( indent + "    " + metric.getDisplayName() + ": " + metric.getMetricTextValue(scope));
		}
	}
	
	/***
	 * Open a database if exist and valid
	 * @param objPrint  stream to print (either standard out or err)
	 * @param objFile   The XML file of the database
	 * @return experiment file if exists, null if fails.
	 */
	private Experiment openExperiment(PrintStream objPrint, File objFile) {

		try {
			Experiment experiment = new Experiment();	// prepare the experiment
			experiment.open(objFile, null, Experiment.ExperimentOpenFlag.TREE_ALL);	// parse the database

			return experiment;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}	
}
