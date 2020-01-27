/**
 * 
 */
package edu.rice.cs.hpc.data.experiment.metric;

import edu.rice.cs.hpc.data.experiment.metric.BaseMetric.AnnotationType;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric.VisibilityType;

/**
 * @author laksonoadhianto
 *
 */
public class TestMetricFormat {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Metric baseMetric = new Metric("sn", "nn", "dn",
				VisibilityType.SHOW, null, AnnotationType.PERCENT, "", 0, MetricType.INCLUSIVE, 1);
		
		// test 1: 9.999 should be displayed as 1.0e+01 
		MetricValue mv = new MetricValue(9.999, .999);
		// test 2: 0.999 should be displayed as 9.99e-1
		MetricValue mv2 = new MetricValue(0.992, .009);
		MetricValue mv3 = new MetricValue(9.55, 0.0009);
		MetricValue mv4 = new MetricValue(9.5, -0.000005);
		MetricValue mv5 = new MetricValue(0.9999, -0.000005);
		MetricValue mv6 = new MetricValue(0.945, -0.000005);
		MetricValue mv7 = new MetricValue(0.95, -0.000005);
		MetricValue mv8 = new MetricValue(0.955, -0.095005);
		MetricValue mv9 = new MetricValue(0.9992, -0.095505);
		
		System.out.println("test 1: "+MetricValue.getValue(mv) +"\t= '"+baseMetric.getDisplayFormat().format(mv) +"'");
		System.out.println("test 2: "+MetricValue.getValue(mv2)+"\t= '"+baseMetric.getDisplayFormat().format(mv2)+"'");
		System.out.println("test 3: "+MetricValue.getValue(mv3)+"\t= '"+baseMetric.getDisplayFormat().format(mv3)+"'");
		System.out.println("test 4: "+MetricValue.getValue(mv4)+"\t= '"+baseMetric.getDisplayFormat().format(mv4)+"'");
		System.out.println("test 5: "+MetricValue.getValue(mv5)+"\t= '"+baseMetric.getDisplayFormat().format(mv5)+"'");
		System.out.println("test 6: "+MetricValue.getValue(mv6)+"\t= '"+baseMetric.getDisplayFormat().format(mv6)+"'");
		System.out.println("test 7: "+MetricValue.getValue(mv7)+"\t= '"+baseMetric.getDisplayFormat().format(mv7)+"'");
		System.out.println("test 8: "+MetricValue.getValue(mv8)+"\t= '"+baseMetric.getDisplayFormat().format(mv8)+"'");
		System.out.println("test 9: "+MetricValue.getValue(mv9)+"\t= '"+baseMetric.getDisplayFormat().format(mv9)+"'");
	}

}
