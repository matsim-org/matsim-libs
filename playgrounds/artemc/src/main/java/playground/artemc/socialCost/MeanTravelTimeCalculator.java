/* *********************************************************************** *
 * project: org.matsim.*
 * MeanTravelTimeCalculator.java
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

package playground.artemc.socialCost;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Calculates the mean travel times split up into the different leg modes.
 * 
 * @author cdobler
 */
public class MeanTravelTimeCalculator implements PersonArrivalEventHandler, PersonDepartureEventHandler,
PersonStuckEventHandler, IterationEndsListener, ShutdownListener  {

	protected Scenario scenario;
	protected Set<String> transportModes;

	protected Map<Id, Double> currentLegs;
	protected Map<String, List<Double>> legs;
	protected Map<String, List<Double>> previousTravelTimes;	// mean travel times from previous iterations
	
	private BufferedWriter out;

	public MeanTravelTimeCalculator(Scenario scenario, Set<String> transportModes) {
		this.scenario = scenario;
		this.transportModes = transportModes;

		init();
	}

	private void init() {
		currentLegs = new HashMap<Id, Double>();
		legs = new TreeMap<String, List<Double>>();
		previousTravelTimes = new TreeMap<String, List<Double>>();
		out = IOUtils.getBufferedWriter(scenario.getConfig().controler().getOutputDirectory()+"/meanTravelTimes.txt");

		for (String string : transportModes) {
			legs.put(string, new ArrayList<Double>());
			previousTravelTimes.put(string, new ArrayList<Double>());
		}
		
		try {
			out.write("ITERATION\tavg");
			for (String transportMode : transportModes) {
				out.write("MEAN_TT_"+transportMode+"\t");
			}
			out.write("\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double departureTime = currentLegs.get(event.getPersonId());
		double travelTime = event.getTime() - departureTime;

		String transportMode = event.getLegMode();
		if (!transportModes.contains(transportMode)) return;
		legs.get(transportMode).add(travelTime);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		currentLegs.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		currentLegs.remove(event.getPersonId());
	}

	@Override
	public void reset(int iteration) {
		currentLegs.clear();
		for (List<Double> list : legs.values()) list.clear();				
	}

	private void calculateMeanTravelTimes() {

		for (String transportMode : transportModes) {
			List<Double> legTravelTimes = legs.get(transportMode); 
			double sumLegTravelTimes = 0.0;
			int legCounter = legTravelTimes.size();

			for (double legTravelTime : legTravelTimes) {
				sumLegTravelTimes = sumLegTravelTimes + legTravelTime;
			}
			previousTravelTimes.get(transportMode).add(sumLegTravelTimes / legCounter);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		// We have a dataset for each iteration. We start count with 0, therefore we add 1.
		int dataLength = event.getIteration() + 1;

		// calculate the mean Travel Times for the given Transport Modes
		calculateMeanTravelTimes();

		String fileName = null;

	
		MeanTravelTimeWriter writer = new MeanTravelTimeWriter(event.getIteration());

		try {
			out.write( event.getIteration()+"\t");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		// write leg travel times graphs
		for (String transportMode : transportModes) {
			List<Double> meanTravelTimes = previousTravelTimes.get(transportMode);
			try {
				out.write(meanTravelTimes.get(dataLength-1)+"\t");
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			double[] data = new double[dataLength];
			for (int i = 0; i < data.length; i++) data[i] = meanTravelTimes.get(i);
			fileName = event.getServices().getControlerIO().getOutputFilename("meanTravelTime_" + transportMode + ".png");
			writer.writeGraphic(fileName, transportMode, data);
		}
		
		try {
			out.write("\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// write single graph with all transport modes
		String[] names = new String[transportModes.size() + 1];
		double[][] data = new double[transportModes.size() + 1][dataLength];

		int index = 1;
		for (String transportMode : transportModes) {
			List<Double> meanTravelTimes = previousTravelTimes.get(transportMode);
			for (int i = 0; i < dataLength; i++) data[index][i] = meanTravelTimes.get(i);
			index++;
		}

		for (int i = 0; i < dataLength; i++) {
			double sum = 0.0;
			for (int j = 1; j <= transportModes.size(); j++) {
				sum = sum + data[j][i];
			}
			data[0][i] = sum / transportModes.size();
		}

		names[0] = "all transport modes";
		int i = 1;
		for (String legMode : transportModes) {
			names[i] = legMode;
			i++;
		}

		fileName = event.getServices().getControlerIO().getOutputFilename("meanTravelTime_comparison.png");
		writer.writeGraphic(fileName, names, data);
	}
	
	@Override
	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
