/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LegHistogramChart.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.analysis;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class LegHistogramChart {
    static JFreeChart getGraphic(final LegHistogram.DataFrame dataFrame, final String mode, int iteration) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("en route", false, true);
		int onRoute = 0;
		for (int i = 0; i < dataFrame.countsDep.length; i++) {
			onRoute = onRoute + dataFrame.countsDep[i] - dataFrame.countsArr[i] - dataFrame.countsStuck[i];
			double hour = i*dataFrame.binSize / 60.0 / 60.0;
			departuresSerie.add(hour, dataFrame.countsDep[i]);
			arrivalsSerie.add(hour, dataFrame.countsArr[i]);
			onRouteSerie.add(hour, onRoute);
		}

		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

        final JFreeChart chart = ChartFactory.createXYStepChart(
                "Leg Histogram, " + mode + ", it." + iteration,
                "time", "# persons",
                xyData,
                PlotOrientation.VERTICAL,
                true,   // legend
                false,   // tooltips
                false   // urls
        );

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));

		plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
		plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.gray);
		plot.setDomainGridlinePaint(Color.gray);

		return chart;
	}

    /**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips to the specified file.
	 *
	 * @param legHistogram
     * @param filename
	 *
	 */
	public static void writeGraphic(LegHistogram legHistogram, final String filename) {
		try {
            ChartUtils.saveChartAsPNG(new File(filename), getGraphic(legHistogram.getAllModesData(), "all", legHistogram.getIteration()), 1024, 768);
		} catch (IOException e) {
            throw new UncheckedIOException(e);
		}
	}

    /**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips with the specified transportation mode to the
	 * specified file.
	 *
	 * @param legHistogram
     * @param filename
	 * @param legMode
	 *
	 */
	public static void writeGraphic(LegHistogram legHistogram, final String filename, final String legMode) {
		try {
			ChartUtils.saveChartAsPNG(new File(filename), getGraphic(legHistogram.getDataForMode(legMode), legMode, legHistogram.getIteration()), 1024, 768);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
