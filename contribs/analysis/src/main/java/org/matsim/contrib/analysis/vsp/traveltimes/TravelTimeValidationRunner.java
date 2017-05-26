/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.analysis.vsp.traveltimes;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TravelTimeValidationRunner {

	private final Network network;
	private final String eventsFile;
	private final TravelTimeValidator travelTimeValidator;
	private int numberOfTripsToValidate;
	private final Population population;
	
	
	public TravelTimeValidationRunner(Network network, Population population, String eventsFile, TravelTimeValidator travelTimeValidator,
			int numberOfTripsToValidate) {
		this.network = network;
		this.eventsFile = eventsFile;
		this.travelTimeValidator = travelTimeValidator;
		this.numberOfTripsToValidate = numberOfTripsToValidate;
		this.population = population;
	}
	
	public TravelTimeValidationRunner(Network network, Population population, String eventsFile, TravelTimeValidator travelTimeValidator) {
		this.network = network;
		this.eventsFile = eventsFile;
		this.travelTimeValidator = travelTimeValidator;
		this.numberOfTripsToValidate = Integer.MAX_VALUE;
		this.population = population;
	}
	
	public void run(){
		
		EventsManager events = EventsUtils.createEventsManager();
		CarTripsExtractor carTripsExtractor = new CarTripsExtractor(population.getPersons().keySet(), network);
		events.addHandler(carTripsExtractor);
		new MatsimEventsReader(events).readFile(eventsFile);
		List<CarTrip> carTrips = carTripsExtractor.getTrips();
		Collections.shuffle(carTrips, MatsimRandom.getRandom());
		int i = 0;
		for (CarTrip trip : carTrips){
			double validatedTravelTime = travelTimeValidator.getTravelTime(trip);
			trip.setValidatedTravelTime(validatedTravelTime);
			
			i++;
			if (i>=numberOfTripsToValidate){
				break;
			}
		}
		String fileName = eventsFile.replace(".xml.gz", "");
		writeTravelTimeValidation(fileName, carTrips);
		
		
	}
	private void writeTravelTimeValidation(String filename, List<CarTrip> trips){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename+"_trips.csv");
		XYSeriesCollection times = new XYSeriesCollection();

		XYSeries timess = new XYSeries("times", true, true);
		times.addSeries(timess);
		try {
			bw.append("agent;departureTime;fromX;fromY;toX;toY;traveltimeActual;traveltimeValidated");
			for (CarTrip trip : trips){
				if (trip.getValidatedTravelTime() != null){
					bw.newLine();
					bw.append(trip.toString());
					timess.add(trip.getActualTravelTime(), trip.getValidatedTravelTime());
				}
			}	
		
		
			bw.flush();
			bw.close();
			final JFreeChart chart2 = ChartFactory.createScatterPlot("Travel Times", "Simulated travel time [s]",
					"validated travel Time [s]", times);
			NumberAxis yAxis = (NumberAxis)((XYPlot)chart2.getPlot()).getRangeAxis();
			NumberAxis xAxis = (NumberAxis)((XYPlot)chart2.getPlot()).getDomainAxis();
			yAxis.setUpperBound(xAxis.getUpperBound());
			ChartUtilities.writeChartAsPNG(new FileOutputStream(filename+"_traveltimes" + ".png"), chart2, 1500, 1500);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	
}
