/* *********************************************************************** *
 * project: org.matsim.*
 * Demo.java
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

package org.matsim.utils.charts;

/**
 * A simple demonstration how to use classes in this package to quickly create charts.
 *
 * @author mrieser
 */
public class Demo {

	public void createBarChart(final String filename) {
		BarChart chart = new BarChart("TITLE", "x-axis", "y-axis", new String[] {"A", "B", "C"});
		chart.addSeries("serie 1", new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries("serie 2", new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.addMatsimLogo();
		chart.saveAsPng(filename, 800, 600);
	}

	public void createLineChart(final String filename) {
		LineChart chart = new LineChart("TITLE", "x-axis", "y-axis", new String[] {"A", "B", "C"});
		chart.addSeries("serie 1", new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries("serie 2", new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}

	public void createXYLineChart(final String filename) {
		XYLineChart chart = new XYLineChart("TITLE", "x-axis", "y-axis");
		chart.addSeries("serie 1", new double[] {1.0, 2.0, 3.0, 4.0, 5.0}, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries("serie 2", new double[] {1.0, 5.0, 2.0, 4.0, 3.0}, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}

	public void createXYScatterChart(final String filename) {
		XYScatterChart chart = new XYScatterChart("TITLE", "x-axis", "y-axis");
		chart.addSeries("serie 1", new double[] {1.0, 2.0, 3.0, 4.0, 5.0}, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries("serie 2", new double[] {1.0, 5.0, 2.0, 4.0, 3.0}, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}

	public static void main(final String[] args) {
		Demo demo = new Demo();
		demo.createBarChart("./output/barchart.png");
		demo.createLineChart("./output/linechart.png");
		demo.createXYLineChart("./output/xylinechart.png");
		demo.createXYScatterChart("./output/xyscatterchart.png");
	}
}
