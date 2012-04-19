/* *********************************************************************** *
 * project: org.matsim.*
 * PieChart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.matsim.core.utils.charts.ChartUtil;

/**
 * @author yu
 * 
 */
public class PieChart extends ChartUtil {
	private final DefaultPieDataset dataset;

	/**
	 * @param title
	 * @param axisLabel
	 * @param axisLabel2
	 */
	public PieChart(String title) {
		super(title, null, null);
		dataset = new DefaultPieDataset();
		chart = createChart(title, dataset);
		addDefaultFormatting();
	}

	private JFreeChart createChart(String title, DefaultPieDataset dataset) {
		JFreeChart chart = ChartFactory.createPieChart(title, // chart title
				dataset, // data
				true, // include legend
				true, false);
		return chart;
	}

	@Override
	public JFreeChart getChart() {
		return chart;
	}

	public void addSeries(final String[] titles, final double[] data) {
		if (titles.length != data.length) {
			System.err
					.println("ERROR: \"titles\" has different length as \"data\"");
		}
		double sum = 0;
		for (int i = 0; i < data.length; i++) {
			sum += data[i];
		}
		for (int i = 0; i < data.length; i++) {
			dataset.setValue(titles[i] + "\n" + data[i] + "\n"
					+ (int) (data[i] / sum * 10000.0) / 100.0 + "%", data[i]);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PieChart chart = new PieChart("TITLE");
		chart.addSeries(new String[] { "serie 1", "serie 2", "serie 3",
				"serie 4" }, new double[] { 1, 3, 5, 9 });
		chart.saveAsPng("output/PieTest.png", 800, 600);
	}

}
