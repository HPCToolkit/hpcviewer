package edu.rice.cs.hpc.data.experiment.extdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.rice.cs.hpc.data.experiment.ExperimentWithoutMetrics;
import edu.rice.cs.hpc.data.util.MergeDataFiles;

/*****
 * 
 * Class to print the content of hpctrace file.
 * If the trace file is not merged, it will be merged automatically
 * 
 * To run the program:
 * 
 *   java TracePointer database_directory [thread_to_print]
 *   
 ****/
public class TracePrinter 
{

	public static void main(String[] args) {
		
		if (args == null || args.length < 1) {
			System.out.println("Syntax: java TracePrinter <database_directory>  [thread_to_print]");
			return;
		}
		
		// ------------------------------------------------------------------------
		// create experiment object by reading the experiment.xml and extract 
		// some information concerning hpctraces
		// we don't need this step, but it's best practice
		// ------------------------------------------------------------------------
		
		final ExperimentWithoutMetrics experiment = new ExperimentWithoutMetrics();
		try {
			experiment.open(new File(args[0]), null);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		final TraceAttribute trAttribute = experiment.getTraceAttribute();		

		// ------------------------------------------------------------------------
		// open, read and merge (if necessary) hpctrace files
		// ------------------------------------------------------------------------

		final FileDB2 fileDB = new FileDB2();
		try {
			String filename = getTraceFile(args[0]);
			fileDB.open(filename, trAttribute.dbHeaderSize, TraceReader.RECORD_SIZE);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		if (args.length == 1) {
			printSummary(fileDB);
			return;
		}
		for(int i=1; i<args.length; i++) {
			final String []ranks = fileDB.getRankLabels();
			for(int j=0; j<ranks.length; j++) {
				if (args[i].compareTo(ranks[j]) == 0) {
					try {
						printTrace(j, fileDB);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
				}
			}
		}
	}

	
	private static void printTrace(int rank, FileDB2 fileDB) throws IOException {
		TraceReader reader = new TraceReader(fileDB);
		long numRecords = reader.getNumberOfRecords(rank);
		
		TraceRecord prevRecord = reader.getData(rank, 0);
		
		for(long i=1; i<numRecords; i++) {
			TraceRecord newRecord = reader.getData(rank, i);
			long delta = newRecord.timestamp - prevRecord.timestamp;
			System.out.println(prevRecord.timestamp + " , " + prevRecord.cpId + " (" + delta + " ns)");

			prevRecord = newRecord;
		}
	}
	
	private static void printSummary(FileDB2 fileDB) {
		final String []ranks = fileDB.getRankLabels();
		if (ranks == null) return;
		
		System.out.println("\nParallelism level: " + fileDB.getParallelismLevel());
		System.out.println("Rank: ");
		int i = 0;
		for(String rank : ranks) {
			long minLoc = fileDB.getMinLoc(i);
			long maxLoc = fileDB.getMaxLoc(i);
			long numBytes = maxLoc - minLoc;
			System.out.println("\t" + rank + " [" + minLoc + " , " + maxLoc + " ] : " + numBytes);
			i++;
		}
	}
	
	
	private static String getTraceFile(String directory) throws FileNotFoundException, IOException {

		final String outputFile = directory
				+ File.separatorChar + "experiment.mt";
		
		File dirFile = new File(directory);
		final MergeDataFiles.MergeDataAttribute att = MergeDataFiles
				.merge(dirFile, "*.hpctrace", outputFile,
						null);
		
		if (att != MergeDataFiles.MergeDataAttribute.FAIL_NO_DATA) {
			File fileTrace = new File(outputFile);
			if (fileTrace.length() > 56) {
				return fileTrace.getAbsolutePath();
			}
		}
		System.err
				.println("Error: trace file(s) does not exist or fail to open "
						+ outputFile);
		return null;
	}
}
