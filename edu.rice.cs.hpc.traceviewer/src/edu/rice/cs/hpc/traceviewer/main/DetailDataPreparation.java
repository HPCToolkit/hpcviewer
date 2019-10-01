package edu.rice.cs.hpc.traceviewer.main;

import org.eclipse.swt.graphics.Color;

import edu.rice.cs.hpc.traceviewer.data.db.DataLinePainting;
import edu.rice.cs.hpc.traceviewer.data.db.DataPreparation;
import edu.rice.cs.hpc.traceviewer.data.db.TimelineDataSet;

public class DetailDataPreparation extends DataPreparation {

	private TimelineDataSet dataset;
	
	/*****
	 * Constructor for preparing data to paint on the space-time canvas
	 * 
	 * @param _colorTable
	 * @param _ptl
	 * @param _begTime
	 * @param _depth
	 * @param _height
	 * @param _pixelLength
	 * @param _usingMidpoint
	 */
	public DetailDataPreparation(DataLinePainting data) 
	{
		super(data);
		dataset = new TimelineDataSet( data.ptl.line(), data.ptl.size(), data.height);
	}

	@Override
	public void finishLine(int currSampleMidpoint, int succSampleMidpoint,
			int currDepth, Color color, int sampleCount) {

		final DetailDataVisualization data = new DetailDataVisualization(currSampleMidpoint, 
				succSampleMidpoint, currDepth, color, sampleCount);
		
		dataset.add(data);
	}

	/*****
	 * retrieve the list of data to paint
	 * @return
	 */
	public TimelineDataSet getList() {
		
		return dataset;
	}
}
