package org.matsim.contrib.util.chart;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

/**
 * @author michalm
 */
public class ChartSaveUtils {
	public static void saveAsPNG(JFreeChart chart, String filename, int width, int height) {
		try (var out = new BufferedOutputStream(new FileOutputStream(filename + ".png"))) {
			ChartUtils.writeChartAsPNG(out, chart, width, height);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
