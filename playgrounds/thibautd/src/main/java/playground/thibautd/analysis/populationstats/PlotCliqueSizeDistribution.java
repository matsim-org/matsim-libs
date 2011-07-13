/* *********************************************************************** *
 * project: org.matsim.*
 * plotCliqueSizeDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.populationstats;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.RangeType;
import org.jfree.data.xy.AbstractIntervalXYDataset;

import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.io.IOUtils;

import playground.thibautd.utils.WrapperChartUtil;

/**
 * Counts the number of cliques of each size, and outputs a graph
 * @author thibautd
 */
public class PlotCliqueSizeDistribution {
	private static final Log log =
		LogFactory.getLog(PlotCliqueSizeDistribution.class);


	private static final String START_CLIQUE = "\\s*<clique\\s.*";
	private static final String END_CLIQUE = "\\s*</clique>.*";
	private static final String PERSON_DEF = "\\s*<person\\s.*";

	private static final String TITLE = "clique size distribution";
	private static final String XLABEL = "size";
	private static final String YLABEL = "number of cliques";

	private static final int FIRST_SIZE = 1;

	/**
	 * usage: PlotCliqueSizeDistribution cliqueFile outputDir
	 */
	public static void main(final String[] args) {
		BufferedReader reader = IOUtils.getBufferedReader(args[0]);

		List<Integer> sizes = getSizeInfo(reader);

		ChartUtil chart = getHistogram(sizes);
		chart.saveAsPng(args[1]+"/cliques_distribution.png", 800, 600);
	}

	private static List<Integer> getSizeInfo(final BufferedReader reader) {
		List<Integer> sizes = new ArrayList<Integer>();

		try {
			String line = reader.readLine();
			int count = 0;

			while (line != null) {
				//log.debug(line);
				if (line.matches(START_CLIQUE)) {
					if (count != 0) {
						throw new RuntimeException("opening a new clique before "+
								"closing the previous one.");
					}
				}
				else if (line.matches(PERSON_DEF)) {
					count++;
				}
				else if (line.matches(END_CLIQUE)) {
					sizes.add(count);
					count = 0;
				}

				line = reader.readLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return sizes;
	}

	private static ChartUtil getHistogram(final List<Integer> sizes) {
		CountDataSet dataset = new CountDataSet();
		dataset.addSeries("cliques", sizes);

		JFreeChart chart = ChartFactory.createHistogram(
				TITLE, XLABEL, YLABEL, dataset, PlotOrientation.VERTICAL,
				true, false, false);

		LogarithmicAxis axis = new LogarithmicAxis(YLABEL);
		axis.setAllowNegativesFlag(true);
		chart.getXYPlot().setRangeAxis(axis);
		axis.setLowerBound(0d);
		chart.getXYPlot().getDomainAxis().setLowerBound(0d);

		return new WrapperChartUtil(chart);
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class CountDataSet extends AbstractIntervalXYDataset {
		private static final long serialVersionUID = 1L;

		private static final double EPSILON = 0;

		private final List<double[]> seriesData = new ArrayList<double[]>();
		private final List<String> seriesNames = new ArrayList<String>();

		public void addSeries(final String key, final List<Integer> rawData) {
			seriesNames.add(key);
			seriesData.add(getHistogramCount(rawData));
		}

		private double[] getHistogramCount(final List<Integer> rawData) {
			// quick implementation, not very efficient
			double[] counts = new double[rawData.size()];
			int max = Integer.MIN_VALUE;

			for (int i=0; i < counts.length; i++) {
				//counts[i] = 0;
				counts[i] = EPSILON;
			}

			for (int size : rawData) {
				counts[size]++;
				max = Math.max(max, size);
			}

			double[] out = new double[max + 1];

			for (int i=0; i<=max; i++) {
				if (i < FIRST_SIZE) {
					out[i] = EPSILON;
				}
				else {
					out[i] = counts[i] == 0 ? -1 : counts[i];
				}
			}

			return out;
		}

		// /////////////////////////////////////////////////////////////////////
		// implemented abstract methods
		// /////////////////////////////////////////////////////////////////////
		@Override
		public Number getEndX(final int series, final int item) {
			return item + 0.5d;
		}

		@Override
		public Number getEndY(final int series, final int item) {
			return seriesData.get(series)[item];
		}

		@Override
		public Number getStartX(final int series, final int item) {
			return item - 0.5d;
		}

		@Override
		public Number getStartY(final int series, final int item) {
			return 0d;
		}

		@Override
		public int getItemCount(final int series) {
			return seriesData.get(series).length;
		}

		@Override
		public Number getX(final int series, final int item) {
			return item;
		}

		@Override
		public Number getY(final int series, final int item) {
			return seriesData.get(series)[item];
		}

		@Override
		public int getSeriesCount() {
			return seriesData.size();
		}

		@Override
		public Comparable getSeriesKey(final int series) {
			return seriesNames.get(series);
		}
	}
}

