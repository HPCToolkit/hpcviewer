package edu.rice.cs.hpc.viewer.metric;

import java.io.File;
import java.io.IOException;

import edu.rice.cs.hpc.data.db.DataThread;
import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.BaseExperiment.Db_File_Type;
import edu.rice.cs.hpc.data.experiment.extdata.AbstractThreadDataCollection;

/*******************************************************************
 * 
 * Class to manage a collection of metric plot data for database v.3
 *
 *******************************************************************/
public class ThreadDataCollection3 extends AbstractThreadDataCollection
{
	private DataPlot   data_plot;
	private DataThread data_thread;
	
	@Override
	public void open(String directory) throws IOException {
		data_plot = new DataPlot();
		data_plot.open(directory + File.separatorChar + 
				BaseExperiment.getDefaultDatabaseName(Db_File_Type.DB_PLOT));
		
		data_thread = new DataThread();
		data_thread.open(directory + File.separatorChar + 
				BaseExperiment.getDefaultDatabaseName(Db_File_Type.DB_THREADS));
	}

	@Override
	public boolean isAvailable() {
		return ((data_plot != null) && (data_thread != null));
	}

	@Override
	public double[] getRankLabels() {
		int []ranks = data_thread.getParallelismRank();
		final int num_labels = ranks.length / data_thread.getParallelismLevel();
		double []labels = new double[num_labels];
		
		for(int i=0; i<num_labels; i++)
		{
			labels[i] = 0;
			for(int j=0; j<data_thread.getParallelismLevel(); j++) 
			{
				int k = i*data_thread.getParallelismLevel() + j;
				labels[i] += ranks[k] * Math.pow(10.0, j*-1); 
			}
		}
		return labels;
	}


	@Override
	public String[] getRankStringLabels() throws IOException {
		int []ranks = data_thread.getParallelismRank();
		final int num_labels = ranks.length / data_thread.getParallelismLevel();
		String []labels = new String[num_labels];
		
		for(int i=0; i<num_labels; i++)
		{
			labels[i] = "";
			for(int j=0; j<data_thread.getParallelismLevel(); j++) 
			{
				if (j>0) labels[i] += ".";
				
				int k = i*data_thread.getParallelismLevel() + j;
				labels[i] += ranks[k]; 
			}
		}
		return labels;
	}

	@Override
	public int getParallelismLevel() {
		return data_thread.getParallelismLevel();
	}

	@Override
	public String getRankTitle() {
		return data_thread.getParallelismTitle();
	}

	@Override
	public double[] getMetrics(long nodeIndex, int metricIndex, int numMetrics)
			throws IOException 
			{
		final int num_ranks 		= data_thread.getParallelismRank().length/data_thread.getParallelismLevel();
		final double []metrics		= new double[num_ranks];

		final DataPlotEntry []entry = data_plot.getPlotEntry((int) nodeIndex, metricIndex);
		// if there is no plot data in the database, we return an array of zeros
		// (assuming Java will initialize metrics[] with zeros)
		if (entry != null)
		{			
			for(DataPlotEntry e : entry)
			{
				metrics[e.tid] = e.metval;
			}
		}
		return metrics;
	}

	@Override
	public void dispose() {
		data_plot.dispose();
		try {
			data_thread.dispose();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public double[] getScopeMetrics(int thread_id, int MetricIndex,
			int numMetrics) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
