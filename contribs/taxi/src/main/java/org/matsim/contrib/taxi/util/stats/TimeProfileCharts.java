/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.util.stats;

import java.awt.*;
import java.util.List;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.*;


public class TimeProfileCharts
{
    //TODO add some scaling
    public static JFreeChart chartProfile(String[] series, List<String[]> timeProfile, int interval)
    {
        XYSeriesCollection seriesCollection = new XYSeriesCollection();
        XYSeries[] seriesArray = new XYSeries[series.length];
        for (int s = 0; s < series.length; s++) {
            seriesCollection.addSeries(seriesArray[s] = new XYSeries(series[s], false, false));
        }

        for (int t = 0; t < timeProfile.size(); t++) {
            String[] timePoint = timeProfile.get(t);
            double hour = (double)t * interval / 3600;
            for (int s = 0; s < series.length; s++) {
                seriesArray[s].add(hour, Double.parseDouble(timePoint[s]), false);
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart("TimeProfile", "Time [h]", "Values",
                seriesCollection, PlotOrientation.VERTICAL, true, false, false);

        XYPlot plot = chart.getXYPlot();
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);

        NumberAxis xAxis = (NumberAxis)plot.getDomainAxis();
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        XYItemRenderer renderer = plot.getRenderer();
        for (int s = 0; s < series.length; s++) {
            renderer.setSeriesStroke(s, new BasicStroke(2));
        }

        return chart;
    }
}
