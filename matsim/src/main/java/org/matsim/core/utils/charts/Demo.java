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

package org.matsim.core.utils.charts;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple demonstration how to use classes in this package to quickly create charts.
 *
 * @author mrieser
 */
public class Demo {

	private static final Logger log = LogManager.getLogger(Demo.class);

	private static final String TITLE = "TITLE";
	private static final String X_AXIS = "x-axis";
	private static final String Y_AXIS = "y-axis";
	private static final String SERIE_1 = "serie 1";
	private static final String SERIE_2 = "serie 2";


	public void createBarChart(final String filename) {
		BarChart chart = new BarChart(TITLE, X_AXIS, Y_AXIS, new String[] {"A", "B", "C"});
		chart.addSeries(SERIE_1, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries(SERIE_2, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.addMatsimLogo();
		chart.saveAsPng(filename, 800, 600);
	}

	public void createStackedBarChart(final String filename) {
		StackedBarChart chart = new StackedBarChart(TITLE, X_AXIS, Y_AXIS, new String[] {"A", "B", "C"});
		chart.addSeries(SERIE_1, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries(SERIE_2, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.addMatsimLogo();
		chart.saveAsPng(filename, 800, 600);
	}
	
	public void createLineChart(final String filename) {
		LineChart chart = new LineChart(TITLE, X_AXIS, Y_AXIS, new String[] {"A", "B", "C"});
		chart.addSeries(SERIE_1, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries(SERIE_2, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}

	public void createXYLineChart(final String filename) {
		XYLineChart chart = new XYLineChart(TITLE, X_AXIS, Y_AXIS);
		chart.addSeries(SERIE_1, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries(SERIE_2, new double[] {1.0, 5.0, 2.0, 4.0, 3.0}, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}

	public void createXYLogLogLineChart(final String filename) {
		XYLineChart chart = new XYLineChart(TITLE, X_AXIS, Y_AXIS, true);
		chart.addSeries(SERIE_1, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries(SERIE_2, new double[] {1.0, 5.0, 2.0, 4.0, 3.0}, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}


	public void createXYScatterChart(final String filename) {
		XYScatterChart chart = new XYScatterChart(TITLE, X_AXIS, Y_AXIS);
		chart.addSeries(SERIE_1, new double[] {1.0, 2.0, 3.0, 4.0, 5.0}, new double[] {1.0, 5.0, 2.0, 3.0, 4.5});
		chart.addSeries(SERIE_2, new double[] {1.0, 5.0, 2.0, 4.0, 3.0}, new double[] {2.0, 3.0, 3.0, 1.5, 4.5});
		chart.saveAsPng(filename, 800, 600);
	}

	public static void main(final String[] args) {
		log.info("start to write demo charts...");
		Demo demo = new Demo();
		demo.createBarChart("./output/barchart.png");
		demo.createStackedBarChart("./output/stackedbarchart.png");
		demo.createLineChart("./output/linechart.png");
		demo.createXYLineChart("./output/xylinechart.png");
		demo.createXYLogLogLineChart("./output/xylineloglogchart.png");
		demo.createXYScatterChart("./output/xyscatterchart.png");
		log.info("charts written to output directory at ./output/!");
	}
}
