/* *********************************************************************** *
 * project: org.matsim.*
 * DgLegHistogramImproved
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
package air.analysis.lhi;

import java.awt.Font;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import air.analysis.modehistogram.AbstractModeHistogram;
import air.analysis.modehistogram.ModeHistogramData;
import air.analysis.modehistogram.ModeHistogramUtils;

/**
 * Improved version of LegHistogram - no maximal time bin size - some additional data is collected as number of seats
 * standing room over time
 * 
 * @author dgrether based on the implementation of mrieser in the org.matsim project
 * 
 */
public class VehicleSeatsModeHistogramImproved extends AbstractModeHistogram implements
		VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private static final Logger log = Logger.getLogger(VehicleSeatsModeHistogramImproved.class);

	private Map<Id, VehicleDepartsAtFacilityEvent> vehDepartsEventsByVehicleId = new HashMap<Id, VehicleDepartsAtFacilityEvent>();
	private Vehicles vehicles;
	
	public VehicleSeatsModeHistogramImproved(Vehicles vehicles) {
		this(5 * 60, vehicles);
	}

	/**
	 * Creates a new LegHistogram with the specified binSize and the specified number of bins.
	 * 
	 * @param binSize
	 *          The size of a time bin in seconds.
	 * @param nofBins
	 *          The number of time bins for this analysis.
	 */
	public VehicleSeatsModeHistogramImproved(final int binSize, Vehicles vehicles) {
		super(binSize);
		this.vehicles = vehicles;
		reset(0);
	}

	@Override
	public void reset(int iteration) {
		this.vehDepartsEventsByVehicleId.clear();
		super.resetIteration(iteration);
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehDepartsEventsByVehicleId.put(event.getVehicleId(), event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		VehicleDepartsAtFacilityEvent departureEvent = this.vehDepartsEventsByVehicleId.get(event.getVehicleId());
		if (departureEvent == null){
			log.error("no departure event for vehicle :  " + event.getVehicleId() + " assuming first arrival!");
			return;
		}
		Vehicle vehicle = this.vehicles.getVehicles().get(event.getVehicleId());
		int seats = vehicle.getType().getCapacity().getSeats();
		super.increase(departureEvent.getTime(), seats, null);
		super.decrease(event.getTime(), seats, null);
		
	}
	

	/**
	 * Writes the gathered data tab-separated into a text stream.
	 * 
	 * @param stream
	 *          The data stream where to write the gathered data.
	 */
	@Override
	public void write(final PrintStream stream) {
		// data about modes, add all first
		List<String> modes = new ArrayList<String>();
		modes.addAll(this.getModeData().keySet());
		modes.remove(allModes);
		modes.add(0, allModes);

		stream.print("time");
		for (String legMode : modes) {
			stream.print("\tdepartures_" + legMode + "\tarrivals_" + legMode + "\tstuck_" + legMode
					+ "\ten-route_" + legMode);
		}
		stream.print("\n");

		Map<String, Integer> enRouteMap = new HashMap<String, Integer>();
		for (int i = this.getFirstIndex() - 2; i <= this.getLastIndex() + 2; i++) {
			stream.print(Integer.toString(i * getBinSize()));
			for (String m : modes) {
				int departures = this.getDepartures(m, i);
				int arrivals = this.getArrivals(m, i);
				int stuck = this.getAbort(m, i);
				int enRoute = ModeHistogramUtils.getNotNullInteger(enRouteMap, m);
				int modeEnRoute = enRoute + departures - arrivals - stuck;
				enRouteMap.put(m, modeEnRoute);
				stream.print("\t" + departures + "\t" + arrivals + "\t" + stuck + "\t" + modeEnRoute);
			}
			// new line
			stream.print("\n");
		}
	}

	@Override
	public JFreeChart getGraphic(final ModeHistogramData modeData, final String modeName) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("en route", false, true);
		Integer enRoute = 0;
		for (int i = this.getFirstIndex() - 2; i <= this.getLastIndex() + 2; i++) {
			int departures = this.getDepartures(modeName, i);
			int arrivals = this.getArrivals(modeName, i);
			int stuck = this.getAbort(modeName, i);
			enRoute = enRoute + departures - arrivals - stuck;
			double hour = i * this.getBinSize() / 60.0 / 60.0;
			departuresSerie.add(hour, departures);
			arrivalsSerie.add(hour, arrivals);
			onRouteSerie.add(hour, enRoute);
		}
		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart("Leg Histogram, " + modeName + ", it."
				+ this.getIteration(), "time [h]", "# vehicles", xyData, PlotOrientation.VERTICAL, true, // legend
				false, // tooltips
				false // urls
				);

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}


}
