/* *********************************************************************** *
 * project: org.matsim.*
 * XStats2Chart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.yu.utils.charts.txt2chart;

import org.matsim.core.utils.charts.XYLineChart;

import playground.yu.utils.io.SimpleReader;

/**
 * makes chart graphs derived from scorestats.txt or traveldistancestats.txt
 *
 * @author yu
 *
 */
public class XStats2Chart {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String fileBase = "../../runs-svn/run1535/1535.";
		// String fileBase = "../matsimTests/breakdown/output/";

		 String inputFile = fileBase + "traveldistancestats.txt";
//		String inputFile = fileBase + "scorestats.txt";

		String chartFile = inputFile.replace("txt", "png");
		// /////////////////////////////////////////////
		int maxIter = 346;
		// ////////////////////////////////////////////
		String avgExec, avgWorst, avgAvg, avgBest;
		double[] exec = new double[maxIter];
		double[] worst = new double[maxIter];
		double[] avg = new double[maxIter];
		double[] best = new double[maxIter];

		SimpleReader reader = new SimpleReader(inputFile);

		String line = reader.readLine();// file head
		String[] series = line.split("\t");
		avgExec = series[1];
		avgWorst = series[2];
		avgAvg = series[3];
		avgBest = series[4];

		line = reader.readLine();// first line
		while (line != null) {
			if (line.startsWith("I")) {
				line = reader.readLine();
			}
			series = line.split("\t");
			int iter = Integer.parseInt(series[0]);
			exec[iter] = Double.parseDouble(series[1]);
			worst[iter] = Double.parseDouble(series[2]);
			avg[iter] = Double.parseDouble(series[3]);
			best[iter] = Double.parseDouble(series[4]);
			line = reader.readLine();
		}

		reader.close();

		double[] xs = new double[maxIter];
		for (int i = 0; i < maxIter; i++) {
			xs[i] = i;
		}

		XYLineChart chart = null;
		String serie = "";
		if (inputFile.contains("score")) {
			chart = new XYLineChart("Score Statistics", "iteration", "score");
			serie += " score";
		} else if (inputFile.contains("distance")) {
			chart = new XYLineChart("Leg Travel Distances Statistics",
					"iteration", "avg. leg travel distance");
			serie += " plan";
		}

		chart.addSeries(avgWorst + serie, xs, worst);
		chart.addSeries(avgBest + serie, xs, best);
		chart.addSeries(avgAvg + serie, xs, avg);
		chart.addSeries(avgExec, xs, exec);
		chart.addMatsimLogo();
		chart.saveAsPng(chartFile, 1024, 768);
	}
}
