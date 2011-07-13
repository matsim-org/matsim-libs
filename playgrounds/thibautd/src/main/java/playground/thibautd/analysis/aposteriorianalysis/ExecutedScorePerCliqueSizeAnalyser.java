/* *********************************************************************** *
 * project: org.matsim.*
 * ExecutedScorePerCliqueSizeAnalyser.java
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
package playground.thibautd.analysis.aposteriorianalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Allows to compare the executed plans score of a mobsim iteration with several
 * settings
 *
 * @author thibautd
 */
public class ExecutedScorePerCliqueSizeAnalyser {
	private static final Log log =
		LogFactory.getLog(ExecutedScorePerCliqueSizeAnalyser.class);


	private static final String FILE_BEGIN = "scoresStats-size-";
	private static final String FILE_END = ".txt";
	private static final ScoreFilesFilter filesFilter = new ScoreFilesFilter();
	private static final ItemComparator comparator = new ItemComparator();
	private static final String SEPARATOR = "\t";
	// column for "average executed"
	private static final int COLUMN = 1;

	private final Map<String, double[][]> series;
	private final int iteration;

	private ChartUtil chart = null;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * @param outputDirectories map linking setting name to the corresponding
	 * output path
	 * @param iteration the number of the iteration to compare
	 */
	public ExecutedScorePerCliqueSizeAnalyser(
			final Map<String, String> outputDirectories,
			final int iteration) {
		log.debug("constructing analyser");
		this.iteration = iteration;
		this.series = Collections.unmodifiableMap(getSeries(outputDirectories, iteration));
	}

	private Map<String, double[][]> getSeries(
			final Map<String, String> outputDirectories,
			final int iteration) {
		Map<String, double[][]> series = new HashMap<String, double[][]>();
		double[][] currentSeries;
		String directoryName;
		File directory;
		File[] files;
		List<Tuple<Double,Double>> seriesInConstruction;

		for (Map.Entry<String, String> entry : outputDirectories.entrySet()) {
			directoryName = entry.getValue();
			log.debug("analysing directory "+directoryName);
			currentSeries = new double[2][];
			seriesInConstruction = new ArrayList<Tuple<Double,Double>>();
			directory = new File(directoryName);

			files = directory.listFiles(filesFilter);

			for (File file : files) {
				seriesInConstruction.add(getValueForFile(
						file,
						iteration));
			}

			Collections.sort(seriesInConstruction, comparator);
			currentSeries[0] = new double[seriesInConstruction.size()];
			currentSeries[1] = new double[seriesInConstruction.size()];

			int i=0;
			for (Tuple<Double,Double> xyPair : seriesInConstruction) {
				currentSeries[0][i] = xyPair.getFirst();
				currentSeries[1][i] = xyPair.getSecond();
				i++;
			}

			series.put(entry.getKey(), currentSeries);
		}

		return series;
	}

	private Tuple<Double, Double> getValueForFile(
			final File file,
			final int iteration) {
		log.debug("getting data for file "+file.getName()+":");
		// iteration number
		double first = Double.parseDouble(
				file.getName().substring(
					FILE_BEGIN.length(),
					file.getName().length() - FILE_END.length()));
		double second = 0;

		BufferedReader reader;
		try {
			reader = IOUtils.getBufferedReader(file.getCanonicalPath());

			for (int i=0; i <= iteration; i++) {
				reader.readLine();
			}

			second = Double.parseDouble(reader.readLine().split(SEPARATOR)[COLUMN]);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.debug("   clique size: "+first);
		log.debug("   avg. executed score: "+second);

		return new Tuple<Double, Double>(first, second);
	}

	// /////////////////////////////////////////////////////////////////////////
	// public accessors
	// /////////////////////////////////////////////////////////////////////////
	public ChartUtil getChart() {
		if (chart == null) {
			chart = createChart();
		}
		return chart;
	}

	private ChartUtil createChart() {
		XYLineChart chart = new XYLineChart(
				"Average executed score per clique size, iteration "+iteration,
				"clique size",
				"average individual executed score");

		double[][] xySeries;
		for (Map.Entry<String, double[][]> entry : series.entrySet()) {
			xySeries = entry.getValue();
			chart.addSeries(entry.getKey(), xySeries[0], xySeries[1]);
		}

		chart.getChart().getXYPlot().setRenderer(
				new XYLineAndShapeRenderer(
					true, // draw lines
					true)); // draw points

		return chart;
	}

	// /////////////////////////////////////////////////////////////////////////
	// helper classes
	// /////////////////////////////////////////////////////////////////////////
	private static class ScoreFilesFilter implements FilenameFilter {
		private static final String regexp = FILE_BEGIN + ".*" + FILE_END;

		@Override
		public boolean accept(final File dir, final String fileName) {
			return fileName.matches(regexp);
		}
	}

	private static class ItemComparator implements Comparator<Tuple<Double, ? extends Object>> {

		@Override
		public int compare(final Tuple<Double, ? extends Object> o1,
				final Tuple<Double, ? extends Object> o2) {
			return Double.compare(o1.getFirst(), o2.getFirst());
		}
	}
}

