package edu.rice.cs.hpc.data.experiment.extdata;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import edu.rice.cs.hpc.data.util.Util;


/*****
 * Adaptation of TraceCompactor from traceviewer for more general purpose
 *  
 * Example of input files: 
 * 
 * s3d_f90.x-000002-000-a8c01d31-26093.hpctrace
 * 1.fft-000142-000-a8c0a230-23656.metric-db
 * 
 * Example of output file:
 * 
 * Single.hpctrace
 * Single.metric-db
 * 
 * @author laksono 
 *
 */
public class MergeDataFiles {
	
	private static final int PAGE_SIZE_GUESS = 4096;
	
	private static final int PROC_POS = 5;
	private static final int THREAD_POS = 4;
	
	public static String compact(File directory, String globInputFile) throws IOException {
		
		final int last_dot = globInputFile.lastIndexOf('.');
		final String suffix = globInputFile.substring(last_dot);

		final String outputFile = directory.getAbsolutePath() + File.separatorChar + "data" + suffix + ".single";
		
		final File fout = new File(outputFile);
		
		// check if the file already exists
		if (fout.canRead())
			return outputFile;
		
		FileOutputStream fos = new FileOutputStream(outputFile);
		DataOutputStream dos = new DataOutputStream(fos);
		
		//-----------------------------------------------------
		//write the header:
		//  int type (0: unknown, 1: mpi, 2: openmp, 3: hybrid, ...
		//	int num_files
		//  for all files:
		//		int proc-id, int thread-id, long offset
		//-----------------------------------------------------

		int type = 0;
		dos.writeInt(type);
		
		File[] file_metric = directory.listFiles( new Util.FileThreadsMetricFilter(globInputFile) );
		if (file_metric == null)
			return null;
		
		dos.writeInt(file_metric.length);
		
		final long num_metric_header = 2 * ThreadLevelDataFile.SIZEOF_INT;
		final long num_metric_index  = file_metric.length * (ThreadLevelDataFile.SIZEOF_LONG + 2 * ThreadLevelDataFile.SIZEOF_INT );
		long offset = num_metric_header + num_metric_index;

		//write the index
		for(int i = 0; i < file_metric.length; ++i)
		{
			//get the core number and thread number
			final String filename = file_metric[i].getName();
			final int last_pos_basic_name = filename.length() - suffix.length();
			final String basic_name = file_metric[i].getName().substring(0, last_pos_basic_name);
			String []tokens = basic_name.split("-");
			
			final int num_tokens = tokens.length;
			final int proc = Integer.parseInt(tokens[num_tokens-PROC_POS]);
			dos.writeInt(proc);
			if (proc != 0)
				type |= ThreadLevelDataFile.MULTI_PROCESSES;
			
			final int thread = Integer.parseInt(tokens[num_tokens-THREAD_POS]);
			dos.writeInt(thread);
			if (thread != 0)
				type |= ThreadLevelDataFile.MULTI_THREADING;
			

			dos.writeLong(offset);
			offset += file_metric[i].length();
		}
		
		//copy the traces
		for(int i = 0; i < file_metric.length; ++i) {
			DataInputStream dis = new DataInputStream(new FileInputStream(file_metric[i]));
			byte[] data = new byte[PAGE_SIZE_GUESS];
			
			int numRead = dis.read(data);
			while(numRead > 0) {
				dos.write(data, 0, numRead);
				numRead = dis.read(data);
			}
			dis.close();
			
		}		
		
		dos.close();
		
		// write the type of the application
		RandomAccessFile f = new RandomAccessFile(outputFile, "rw");
		f.writeInt(type);
		f.close();
		
		return outputFile;

	}
}