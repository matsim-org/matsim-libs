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

package playground.ivt.analysis;

import com.google.inject.Singleton;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.analysis.LegHistogram;
import org.matsim.core.utils.io.UncheckedIOException;

import javax.media.jai.Histogram;
import java.awt.*;
import java.io.File;
import java.io.IOException;

@Singleton
public class ActivityHistogramChart {
    static JFreeChart getGraphic(final ActivityHistogram.DataFrame dataFrame, final String type, int iteration) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries startsSerie = new XYSeries("starts", false, true);
		final XYSeries endsSerie = new XYSeries("ends", false, true);
		final XYSeries inActSerie = new XYSeries("in act", false, true);
		int inAct = 0;
		for (int i = 0; i < dataFrame.countsStart.length; i++) {
			inAct = inAct + dataFrame.countsStart[i] - dataFrame.countsEnd[i];
			double hour = i*dataFrame.binSize / 60.0 / 60.0;
			startsSerie.add(hour, dataFrame.countsStart[i]);
			endsSerie.add(hour, dataFrame.countsEnd[i]);
			inActSerie.add(hour, inAct);
		}

		xyData.addSeries(startsSerie);
		xyData.addSeries(endsSerie);
		xyData.addSeries(inActSerie);

        final JFreeChart chart = ChartFactory.createXYStepChart(
                "Activity Histogram, " + type + ", it." + iteration,
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
	 * en route of all legs/trips with the specified transportation mode to the
	 * specified file.
	 *
	 * @param actHistogram
     * @param filename
	 * @param actType
	 *
	 */
	public static void writeGraphic(ActivityHistogram actHistogram, final String filename, final String actType) {
		try {
            ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(actHistogram.getDataForType(actType), actType, actHistogram.getIteration()), 1024, 768);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
