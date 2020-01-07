package edu.rice.cs.hpc.traceviewer.services;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;

import edu.rice.cs.hpc.traceviewer.data.graph.ColorTable;


/************************************************************
 * 
 * Service provider for Summary and statistic view
 *
 ************************************************************/
public class SummaryDataService extends AbstractSourceProvider 
{
	final static public String DATA_PROVIDER = "edu.rice.cs.hpc.traceviewer.services.SummaryDataService.update";
	final static public String DATA_REQUEST  = "edu.rice.cs.hpc.traceviewer.services.SummaryDataService.request";
	
	
	private PaletteData palette;
	private AbstractMap<Integer, Integer> mapPixelToCount; 
	private ColorTable colorTable; 
	private int totalPixels;

	public SummaryDataService() {
	}

	/***
	 * Broadcast messages to every views that a summary data is ready to be
	 * consumed.
	 * 
	 * @param palette
	 * @param mapPixelToCount
	 * @param colorTable
	 * @param totalPixels
	 */
	public void broadcastUpdate(			
			PaletteData palette,
			AbstractMap<Integer, Integer> mapPixelToCount, 
			ColorTable colorTable, 
			int totalPixels) {
		
		fireSourceChanged(ISources.WORKBENCH, DATA_PROVIDER, 
				new SummaryData(palette, mapPixelToCount, colorTable, totalPixels));
	}
	
	/**
	 * request summary data to be broadcasted
	 */
	public void requestData() {
		fireSourceChanged(ISources.WORKBENCH, DATA_REQUEST, null);
	}
	
	@Override
	public void dispose() {
	}

	@Override
	public Map getCurrentState() {
		Map<String, Object> map = new HashMap<String, Object>(1);
		map.put(DATA_PROVIDER, getValue());
		
		return map;
	}

	@Override
	public String[] getProvidedSourceNames() {

		return new String[] {DATA_PROVIDER, DATA_REQUEST};
	}

	private String getValue() {
		
		boolean valid = (totalPixels > 0) 		&& (palette != null) &&
						(colorTable != null)	&& (mapPixelToCount.size() > 0);
		
		if (valid) return "ENABLED";
		else return "DISABLED";
				
	}
	
	static public class SummaryData 
	{
		public PaletteData palette;
		public AbstractMap<Integer, Integer> mapPixelToCount;
		public ColorTable colorTable;
		public int totalPixels;
		
		
		public SummaryData(
				PaletteData palette,
				AbstractMap<Integer, Integer> mapPixelToCount, 
				ColorTable colorTable, 
				int totalPixels) {
			
			this.palette = palette;
			this.mapPixelToCount = mapPixelToCount;
			this.colorTable = colorTable;
			this.totalPixels = totalPixels;
		}
	}
}
