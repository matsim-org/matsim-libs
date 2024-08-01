/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.analysis;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import jakarta.inject.Singleton;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author thibautd
 */
@Singleton
public class CourtesyHistogramListener  implements IterationEndsListener, IterationStartsListener {

	private final CourtesyHistogram histogram;

	private static final Logger log = LogManager.getLogger(CourtesyHistogramListener.class);
	private final OutputDirectoryHierarchy controlerIO;

    @Inject
    CourtesyHistogramListener(CourtesyHistogram histogram, OutputDirectoryHierarchy controlerIO) {
        this.controlerIO = controlerIO;
		this.histogram = histogram;
	}

	@Override
	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.histogram.reset(event.getIteration());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.histogram.write(controlerIO.getIterationFilename(event.getIteration(), "courtesyHistogram.txt"));

		int createGraphsInterval = event.getServices().getConfig().controller().getCreateGraphsInterval();
		boolean createGraphs = createGraphsInterval >0 && event.getIteration() % createGraphsInterval == 0;
		if (createGraphs) {
			for ( String type : histogram.getDataFrames().keySet() ) {
				writeGraphic(
						this.histogram,
						type,
						controlerIO.getIterationFilename(event.getIteration(), "courtesyHistogram_"+type+".png"));
			}
		}
	}

	static JFreeChart getGraphic(final CourtesyHistogram.DataFrame dataFrame, int iteration, String actType) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries helloSeries = new XYSeries("hello", false, true);
		final XYSeries goodbyeSerie = new XYSeries("goodbye", false, true);
		final XYSeries togetherSerie = new XYSeries("pairs together", false, true);
		int together = 0;
		for (int i = 0; i < dataFrame.countsHello.length; i++) {
			together = together + dataFrame.countsHello[i] - dataFrame.countsGoodbye[i];
			double hour = i*dataFrame.binSize / 60.0 / 60.0;
			helloSeries.add(hour, dataFrame.countsHello[i]);
			goodbyeSerie.add(hour, dataFrame.countsGoodbye[i]);
			togetherSerie.add(hour, together);
		}

		xyData.addSeries(helloSeries);
		xyData.addSeries(goodbyeSerie);
		xyData.addSeries(togetherSerie);

        final JFreeChart chart = ChartFactory.createXYStepChart(
				"Courtesy Statistics," +
				"actType "+actType+
				" it." + iteration,
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

	public static void writeGraphic(CourtesyHistogram courtesyHistogram, final String actType, final String filename) {
		try {
            ChartUtils.saveChartAsPNG(
					new File(filename),
					getGraphic(
							courtesyHistogram.getDataFrames().get( actType ),
							courtesyHistogram.getIteration(),
							actType), 1024, 768);
		} catch (IOException e) {
            throw new UncheckedIOException(e);
		}
	}
}
