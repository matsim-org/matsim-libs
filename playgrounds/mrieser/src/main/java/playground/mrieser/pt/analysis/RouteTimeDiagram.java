/* *********************************************************************** *
 * project: org.matsim.*
 * RouteTimeDiagram.java
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

package playground.mrieser.pt.analysis;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;

/**
 * Collects data to create Route-Time-Diagrams based on the actual simulation.
 * A Route-Time-Diagram shows the position along one transit route of one or
 * more vehicles over the lapse of time.
 *
 * @author mrieser
 */
public class RouteTimeDiagram implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	/**
	 * Map containing for each vehicle a list of positions, stored as StopFacility Ids and the time.
	 */
	private final Map<Id, List<Tuple<Id, Double>>> positions = new HashMap<Id, List<Tuple<Id, Double>>>();

	public void handleEvent(final VehicleArrivesAtFacilityEvent event) {
		List<Tuple<Id, Double>> list = this.positions.get(event.getVehicleId());
		if (list == null) {
			list = new ArrayList<Tuple<Id, Double>>();
			this.positions.put(event.getVehicleId(), list);
		}
		list.add(new Tuple<Id, Double>(event.getFacilityId(), Double.valueOf(event.getTime())));
	}

	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		List<Tuple<Id, Double>> list = this.positions.get(event.getVehicleId());
		if (list == null) {
			list = new ArrayList<Tuple<Id, Double>>();
			this.positions.put(event.getVehicleId(), list);
		}
		list.add(new Tuple<Id, Double>(event.getFacilityId(), Double.valueOf(event.getTime())));
	}

	public void reset(final int iteration) {
		this.positions.clear();
	}

	public void writeData() {
		for (List<Tuple<Id, Double>> list : this.positions.values()) {
			for (Tuple<Id, Double> info : list) {
				System.out.println(info.getFirst().toString() + "\t" + info.getSecond().toString());
			}
			System.out.println();
		}
	}

	public void createGraph(final String filename, final TransitRoute route) {

		HashMap<Id, Integer> stopIndex = new HashMap<Id, Integer>();
		int idx = 0;
		for (TransitRouteStop stop : route.getStops()) {
			stopIndex.put(stop.getStopFacility().getId(), idx);
			idx++;
		}

		HashSet<Id> vehicles = new HashSet<Id>();
		for (Departure dep : route.getDepartures().values()) {
			vehicles.add(dep.getVehicleId());
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		int numSeries = 0;
		double earliestTime = Double.POSITIVE_INFINITY;
		double latestTime = Double.NEGATIVE_INFINITY;

		for (Map.Entry<Id, List<Tuple<Id, Double>>> entry : this.positions.entrySet()) {
			if (vehicles.contains(entry.getKey())) {
				XYSeries series = new XYSeries("t", false, true);
				for (Tuple<Id, Double> pos : entry.getValue()) {
					Integer stopIdx = stopIndex.get(pos.getFirst());
					if (stopIdx != null) {
						double time = pos.getSecond().doubleValue();
						series.add(stopIdx.intValue(), time);
						if (time < earliestTime) {
							earliestTime = time;
						}
						if (time > latestTime) {
							latestTime = time;
						}
					}
				}
				dataset.addSeries(series);
				numSeries++;

			}
		}

		JFreeChart c = ChartFactory.createXYLineChart("Route-Time Diagram, Route = " + route.getId(), "stops", "time",
				dataset, PlotOrientation.VERTICAL,
				false, // legend?
				false, // tooltips?
				false // URLs?
				);
		c.setBackgroundPaint(new Color(1.0f, 1.0f, 1.0f, 1.0f));

		XYPlot p  = (XYPlot) c.getPlot();

		p.getRangeAxis().setInverted(true);
		p.getRangeAxis().setRange(earliestTime, latestTime);
		XYItemRenderer renderer = p.getRenderer();
		for (int i = 0; i < numSeries; i++) {
			renderer.setSeriesPaint(i, Color.black);
		}

		try {
			ChartUtilities.saveChartAsPNG(new File(filename), c, 1024, 768, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
