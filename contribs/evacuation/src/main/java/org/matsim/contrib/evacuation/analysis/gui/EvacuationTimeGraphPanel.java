/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.evacuation.analysis.gui;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.matsim.core.utils.collections.Tuple;

public class EvacuationTimeGraphPanel extends AbstractDataPanel {

	private static final long serialVersionUID = 1L;
	private ChartPanel chartPanel;

	// TODO: GRAPH graph;

	public EvacuationTimeGraphPanel(int width, int height) {
		this.setPanelSize(width, height);
		drawDataPanel();
	}

	@Override
	public void drawDataPanel() {
		// if data is not set yet: do nothing
		if (data == null)
			return;

		List<Tuple<Double, Integer>> arrivalTimes = data.getArrivalTimes(); 
		int arrivalTimeCount = arrivalTimes.size(); 
													

		double[] xs = new double[arrivalTimeCount];
		double[] ys = new double[arrivalTimeCount];

		TimeSeriesCollection dataset = new TimeSeriesCollection();
		TimeSeries timeSeries = new TimeSeries("evacuation time");

		for (int i = 0; i < arrivalTimeCount; i++) {
			xs[i] = 1000 * 60 * 60 * 23 + arrivalTimes.get(i).getFirst() * 1000;
			ys[i] = arrivalTimes.get(i).getSecond()/data.getSampleSize();
			timeSeries.add(new Second(new Date((long) xs[i])), ys[i]);
		}

		dataset.addSeries(timeSeries);

		JFreeChart freeChart = ChartFactory.createTimeSeriesChart("evacuation time", "time (hh:mm:ss)", "agents", dataset, false, false, false);

		XYPlot plot = (XYPlot) freeChart.getPlot();
		((DateAxis) (plot.getDomainAxis())).setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));

		freeChart.setAntiAlias(true);

		if (chartPanel != null) {
			chartPanel.setChart(freeChart);
			chartPanel.repaint();
			return;
		} else {
			chartPanel = new ChartPanel(freeChart);
			chartPanel.setPreferredSize(new Dimension(this.width, this.height));

			this.add(chartPanel);
			this.validate();
			this.setSize(this.width, this.height);
		}

	}

}
