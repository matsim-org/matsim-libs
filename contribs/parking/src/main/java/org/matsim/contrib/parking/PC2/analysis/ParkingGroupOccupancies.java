/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.PC2.analysis;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.events.handler.BasicEventHandler;

public abstract class ParkingGroupOccupancies implements BasicEventHandler {

	IntegerValueHashMap<String> parkingGroupOccupency;
	HashMap<String, XYSeries> parkingGroupOccupancySeries;
	XYSeriesCollection xySeriesCollection;

	@Override
	public void reset(int iteration) {
		parkingGroupOccupency = new IntegerValueHashMap<String>();
		parkingGroupOccupancySeries = new HashMap<String, XYSeries>();
		xySeriesCollection = new XYSeriesCollection();
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE) || event.getEventType().equalsIgnoreCase(ParkingDepartureEvent.EVENT_TYPE)) {
			Id<PC2Parking> parkingId = Id.create(event.getAttributes().get(ParkingArrivalEvent.ATTRIBUTE_PARKING_ID), PC2Parking.class);
			String groupName = getGroupName(parkingId);
			initializeGroupOccupancyIfNeeded(event, groupName);
			
			if(event.getTime()>110000){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}

			if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE)) {
				parkingGroupOccupency.increment(groupName);
			} else if (event.getEventType().equalsIgnoreCase(ParkingDepartureEvent.EVENT_TYPE)) {
				parkingGroupOccupency.decrement(groupName);
			}

			parkingGroupOccupancySeries.get(groupName).add(event.getTime() / 3600 , parkingGroupOccupency.get(groupName));
		}
	}

	private void initializeGroupOccupancyIfNeeded(Event event, String groupName) {
		if (!parkingGroupOccupancySeries.containsKey(groupName)) {
			parkingGroupOccupancySeries.put(groupName, new XYSeries(groupName));
			xySeriesCollection.addSeries(parkingGroupOccupancySeries.get(groupName));
		}
	}

	public void showPlot() {
		final LineChart lineChart = new LineChart(xySeriesCollection, "Parking Group Occupancies");
		lineChart.pack();
		RefineryUtilities.centerFrameOnScreen(lineChart);
		lineChart.setVisible(true);
	}

	public void savePlot(String fileName) {
		final JFreeChart chart = LineChart.createChart(xySeriesCollection, "Parking Group Occupancies");
		try {
			ChartUtilities.saveChartAsPNG(new File(fileName), chart,  800, 600, null, true, 9);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public abstract String getGroupName(Id parkingId);

	private static class LineChart extends ApplicationFrame {

		private String title;
		private JFreeChart chart;

		public LineChart(XYDataset dataset, String title) {

			super(title);
			this.title = title;

			setChart(createChart(dataset,title));
			final ChartPanel chartPanel = new ChartPanel(getChart());
			chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
			setContentPane(chartPanel);
		}

		public static JFreeChart createChart(final XYDataset dataset, String title) {

			final JFreeChart chart = ChartFactory.createXYLineChart(title, "time [h]", "# of parked vehicles", dataset, PlotOrientation.VERTICAL, true, true,
					false);

			chart.setBackgroundPaint(Color.white);

			final XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.lightGray);
			plot.setDomainGridlinePaint(Color.white);
			plot.setRangeGridlinePaint(Color.white);

			final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
			renderer.setSeriesLinesVisible(0, false);
			renderer.setSeriesShapesVisible(1, false);
			plot.setRenderer(renderer);

			final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
			rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

			return chart;

		}

		public JFreeChart getChart() {
			return chart;
		}

		public void setChart(JFreeChart chart) {
			this.chart = chart;
		}

	}

}
