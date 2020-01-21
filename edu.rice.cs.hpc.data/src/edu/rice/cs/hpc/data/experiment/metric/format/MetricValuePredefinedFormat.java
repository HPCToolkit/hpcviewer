package edu.rice.cs.hpc.data.experiment.metric.format;

import java.util.Formatter;

import edu.rice.cs.hpc.data.experiment.metric.MetricValue;


/**********************************************************
 * 
 * @author laksonoadhianto
 *
 **********************************************************/
public class MetricValuePredefinedFormat implements IMetricValueFormat {
	private String format;
	
	public MetricValuePredefinedFormat(String sFormat) {
		this.format = sFormat;
	}
	
	
	public String format(MetricValue value) {
		try {
			Formatter format_str = new Formatter();
			format_str = format_str.format(format, MetricValue.getValue(value));

			if (format_str != null) {
				String fmt = format_str.toString();
				format_str.close();
				
				return fmt;
			}

		} catch (java.util.IllegalFormatConversionException e) {
			System.err.println("Illegal format conversion: " + format.toString() + "\tFrom value: " + MetricValue.getValue(value));
			e.printStackTrace();
		}
		return "";
	}

	/****
	 * get the custom format
	 * 
	 * @return the format
	 */
	public String getFormat() {
		return format;
	}
}
