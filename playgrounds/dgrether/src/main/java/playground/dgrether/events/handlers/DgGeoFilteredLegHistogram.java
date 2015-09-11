/* *********************************************************************** *
 * project: org.matsim.*
 * DgLegHistogram
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
package playground.dgrether.events.handlers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.events.GeospatialEventTools;


/**
 * @author dgrether
 *  based on implementation of mrieser in the org.matsim project
 *
 */
public class DgGeoFilteredLegHistogram implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler, VehicleAbortsEventHandler{

	private Map<Id<Vehicle>, LinkEnterEvent> firstTimeSeenMap;
	private Map<Id<Vehicle>, LinkLeaveEvent> lastTimeSeenMap;
	private GeospatialEventTools geospatialTools;
	private int iteration = 0;
	private final int binSizeSeconds;
	private final int nofBins;
	private ModeData allModesData = null;
	
	public DgGeoFilteredLegHistogram(Network network, CoordinateReferenceSystem networkCrs, final int binSizeSeconds, final int nofBins){
		this.geospatialTools = new GeospatialEventTools(network, networkCrs);
		this.binSizeSeconds = binSizeSeconds;
		this.nofBins = nofBins;
		this.firstTimeSeenMap = new HashMap<>();
		this.lastTimeSeenMap = new HashMap<>();
		reset(0);
	}

	public DgGeoFilteredLegHistogram(Network network, CoordinateReferenceSystem networkCrs, final int binSizeSeconds){
		this(network, networkCrs, binSizeSeconds, 30*3600/binSizeSeconds + 1);
	}

	
	@Override
	public void reset(int iteration) {
		this.iteration = iteration;
		this.allModesData = new ModeData(this.nofBins + 1);
		this.firstTimeSeenMap.clear();
		this.lastTimeSeenMap.clear();
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.geospatialTools.doNetworkAndFeaturesContainLink(event.getLinkId())){
			this.lastTimeSeenMap.put(event.getVehicleId(), event);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.geospatialTools.doNetworkAndFeaturesContainLink(event.getLinkId())){
			if (! this.firstTimeSeenMap.containsKey(event.getVehicleId())) {
				this.firstTimeSeenMap.put(event.getVehicleId(), event);
			} 
		}
	}
	
	private void handleArrivalOrStuck(Event event, Id<Vehicle> vehicleId) {
		LinkEnterEvent firstEvent = this.firstTimeSeenMap.remove(vehicleId);
		LinkLeaveEvent lastEvent = this.lastTimeSeenMap.remove(vehicleId);
		if (firstEvent != null && lastEvent != null){
			int index = getBinIndex(firstEvent.getTime());
			this.allModesData.countsDep[index]++;
			index = getBinIndex(lastEvent.getTime());
			this.allModesData.countsArr[index]++;
		}
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		this.handleArrivalOrStuck(event, event.getVehicleId());		
	}
	
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.handleArrivalOrStuck(event, event.getVehicleId());
	}

	public void addCrsFeatureTuple(Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple) {
		this.geospatialTools.addCrsFeatureTuple(cottbusFeatureTuple);
	}
	
	/**
	 * Writes the gathered data tab-separated into a text file.
	 *
	 * @param filename The name of a file where to write the gathered data.
	 */
	public void write(final String filename) {
		PrintStream stream;
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		write(stream);
		stream.close();
	}
	
	/**
	 * Writes the gathered data tab-separated into a text stream.
	 *
	 * @param stream The data stream where to write the gathered data.
	 */
	public void write(final PrintStream stream) {
		stream.print("time\ttime\tdepartures_all\tarrivals_all\tstuck_all\ten-route_all");
		stream.print("\n");
		int allEnRoute = 0;
		for (int i = 0; i < this.allModesData.countsDep.length; i++) {
			// data about all modes
			allEnRoute = allEnRoute + this.allModesData.countsDep[i] - this.allModesData.countsArr[i] - this.allModesData.countsStuck[i];
			stream.print(Time.writeTime(i*this.binSizeSeconds) + "\t" + i*this.binSizeSeconds);
			stream.print("\t" + this.allModesData.countsDep[i] + "\t" + this.allModesData.countsArr[i] + "\t" + this.allModesData.countsStuck[i] + "\t" + allEnRoute);
			// new line
			stream.print("\n");
		}
	}

	/**
	 * @return a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips
	 */
	public JFreeChart getGraphic() {
		return getGraphic(this.allModesData, "all");
	}
	
	private JFreeChart getGraphic(final ModeData modeData, final String modeName) {
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries departuresSerie = new XYSeries("departures", false, true);
		final XYSeries arrivalsSerie = new XYSeries("arrivals", false, true);
		final XYSeries onRouteSerie = new XYSeries("en route", false, true);
		int onRoute = 0;
		for (int i = 0; i < modeData.countsDep.length; i++) {
			onRoute = onRoute + modeData.countsDep[i] - modeData.countsArr[i] - modeData.countsStuck[i];
			double hour = i*this.binSizeSeconds / 60.0 / 60.0;
			departuresSerie.add(hour, modeData.countsDep[i]);
			arrivalsSerie.add(hour, modeData.countsArr[i]);
			onRouteSerie.add(hour, onRoute);
		}

		xyData.addSeries(departuresSerie);
		xyData.addSeries(arrivalsSerie);
		xyData.addSeries(onRouteSerie);

		final JFreeChart chart = ChartFactory.createXYStepChart(
        "Leg Histogram, " + modeName + ", it." + this.iteration,
        "time", "# vehicles",
        xyData,
        PlotOrientation.VERTICAL,
        true,   // legend
        false,   // tooltips
        false   // urls
    );

		XYPlot plot = chart.getXYPlot();
		plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f));
		plot.getRenderer().setSeriesStroke(1, new BasicStroke(2.0f));
		plot.getRenderer().setSeriesStroke(2, new BasicStroke(2.0f));
		plot.setBackgroundPaint(Color.white);
		plot.setRangeGridlinePaint(Color.gray);  
		plot.setDomainGridlinePaint(Color.gray);  

		
		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));
		return chart;
	}
	
	
	/**
	 * Writes a graphic showing the number of departures, arrivals and vehicles
	 * en route of all legs/trips to the specified file.
	 *
	 * @param filename
	 *
	 * @see #getGraphic()
	 */
	public void writeGraphic(final String filename) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getGraphic(), 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* private methods */

	private int getBinIndex(final double time) {
		int bin = (int)(time / this.binSizeSeconds);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}


	private static class ModeData {
		public final int[] countsDep;
		public final int[] countsArr;
		public final int[] countsStuck;

		public ModeData(final int nofBins) {
			this.countsDep = new int[nofBins];
			this.countsArr = new int[nofBins];
			this.countsStuck = new int[nofBins];
		}
	}





	
}
