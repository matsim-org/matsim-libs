/* *********************************************************************** *
 * project: org.matsim.*
 * MeanTravelTimeWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.artemc.socialCost;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;

import org.jfree.chart.plot.XYPlot;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

/**
 * Writes social costs to a graph and a table.
 * 
 * @author cdobler
 */
public class SocialCostWriter {
	
	private static final String separator = "\t"; 
	private static final String lineEnd = "\n"; 
	
	protected int iteration;
	
	public SocialCostWriter(int iteration) {
		this.iteration = iteration;
	}

	public void writeTable(final String filename, double[] meanData, double[] medianData, double[] quantil25Data, double[] quantil75Data) {
	
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(filename);
			
			StringBuffer sb = new StringBuffer();
			
			// create and write header
			sb.append("mean");
			sb.append(separator);
			sb.append("median");
			sb.append(separator);
			sb.append("25% quantil");
			sb.append(separator);
			sb.append("75% quantil");
			sb.append(lineEnd);
			out.write(sb.toString());

			// write data
			for (int i = 0; i < meanData.length; i++) {
				sb = new StringBuffer();
				sb.append(meanData[i]);
				sb.append(separator);
				sb.append(medianData[i]);
				sb.append(separator);
				sb.append(quantil25Data[i]);
				sb.append(separator);
				sb.append(quantil75Data[i]);
				sb.append(lineEnd);
				out.write(sb.toString());
			}
			
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void writeGraphic(final String filename, String yAxisTitle, double[] meanData, double[] medianData, double[] quantil25Data, double[] quantil75Data) {
			ChartUtil chartUtil = getGraphic(yAxisTitle, meanData, medianData, quantil25Data, quantil75Data);
			chartUtil.saveAsPng(filename, 1024, 768);
	}

	private ChartUtil getGraphic(String yAxisTitle, double[] meanData, double[] medianData, double[] quantil25Data, double[] quantil75Data) {

		double[] iterations = new double[iteration + 1];
		for (int i = 0; i <= iteration; i++) {
			iterations[i] = i;
		}
		
		XYLineChart chart = new XYLineChart("Social Costs", "iteration", yAxisTitle);
		chart.addSeries("mean", iterations, meanData);
		chart.addSeries("median", iterations, medianData);
		chart.addSeries("25% quantil", iterations, quantil25Data);
		chart.addSeries("75% qunatil", iterations, quantil75Data);
		
		XYPlot plot = chart.getChart().getXYPlot();
		plot.getRenderer().setSeriesPaint(0, Color.red);
		plot.getRenderer().setSeriesPaint(1, Color.blue);
		plot.getRenderer().setSeriesPaint(2, Color.blue);
		plot.getRenderer().setSeriesPaint(3, Color.blue);
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
				BasicStroke.JOIN_ROUND, 1.0f, new float[] {8.0f, 4.0f}, 0.0f));
		plot.getRenderer().setSeriesStroke(3, new BasicStroke(1.0f, BasicStroke.CAP_ROUND, 
				BasicStroke.JOIN_ROUND, 1.0f, new float[] {2.0f, 4.0f}, 0.0f));
		
		chart.addMatsimLogo();
		
		return chart;
	}
}