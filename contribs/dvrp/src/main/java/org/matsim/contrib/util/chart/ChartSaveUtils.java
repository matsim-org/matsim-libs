package org.matsim.contrib.util.chart;

import java.io.*;

import org.jfree.chart.*;

/**
 * @author michalm
 */
public class ChartSaveUtils {
	public static void saveAsPNG(JFreeChart chart, String filename, int width, int height) {
		try {
			ChartUtils.writeChartAsPNG(new FileOutputStream(filename + ".png"), chart, width, height);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
