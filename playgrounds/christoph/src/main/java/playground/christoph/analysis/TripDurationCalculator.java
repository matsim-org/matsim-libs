/* *********************************************************************** *
 * project: org.matsim.*
 * TripDurationCalculator.java
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

package playground.christoph.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * Calculates:
 * - average leg travel time per mode
 * - average leg travel time over all modes
 * 
 * @author cdobler
 */
public class TripDurationCalculator implements AgentDepartureEventHandler, AgentArrivalEventHandler,
		StartupListener, IterationEndsListener, ShutdownListener {

	private final BufferedWriter writer;
	
	private final Set<String> sortedModes;
	private final Map<Id, Double> departureTimes = new HashMap<Id, Double>();
	private final Map<String, List<Double>> legTravelTimes = new HashMap<String, List<Double>>();
	
	private final String fileName;
	private final boolean createGraph;
	
	private double[][] history;
	private int minIteration;
	
	public TripDurationCalculator(String fileName, Set<String> modes, boolean createGraph) {

		this.fileName = fileName;
		this.createGraph = createGraph;
		
		this.writer = IOUtils.getBufferedWriter(fileName + ".txt");
		
		sortedModes = new TreeSet<String>(modes);
		try {
			this.writer.write("ITERATION");
			for (String mode : sortedModes) {
				this.writer.write("\t");
				this.writer.write(mode.toUpperCase());
				
				this.legTravelTimes.put(mode, new ArrayList<Double>());
			}
			this.writer.write("\t");
			this.writer.write("OVERALL");
			this.writer.write("\n");
			
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	@Override
	public void reset(int iteration) {
		for(List<Double> modeTravelTime : legTravelTimes.values()) {
			modeTravelTime.clear();
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		
		Double departureTime = departureTimes.get(event.getPersonId());
		if (departureTime == null) throw new RuntimeException("No departure time for agent " + event.getPersonId() + " was found!");
		
		double travelTime = event.getTime() - departureTime;
		String mode = event.getLegMode();
		List<Double> modeTravelTimes = legTravelTimes.get(mode);
		modeTravelTimes.add(travelTime);
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		departureTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.minIteration = controler.getFirstIteration();
		int maxIter = controler.getLastIteration();
		int iterations = maxIter - this.minIteration;
		this.history = new double[this.sortedModes.size() + 1][iterations + 1];
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			int iteration = event.getIteration();
			this.writer.write(String.valueOf(iteration));
			int index = iteration - this.minIteration;
			
			int i = 0;
			double overallTrips = 0;
			double overallTravelTime = 0.0;
			for (String mode : sortedModes) {
				List<Double> modeTravelTimes = legTravelTimes.get(mode);
				double sumTravelTimes = 0.0;
				for (double travelTime : modeTravelTimes) sumTravelTimes += travelTime;
				
				overallTrips += modeTravelTimes.size();
				overallTravelTime += sumTravelTimes;

				double averageTravelTime = sumTravelTimes / modeTravelTimes.size();
				this.history[i][index] = averageTravelTime;
				
				this.writer.write("\t");
				this.writer.write(String.valueOf(averageTravelTime));
				
				i++;
			}
			
			double averageTravelTime = overallTravelTime / overallTrips;
			this.history[sortedModes.size()][index] = averageTravelTime;
			
			this.writer.write("\t");
			this.writer.write(String.valueOf(averageTravelTime));
			this.writer.write("\n");

			this.writer.flush();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		if (this.createGraph && event.getIteration() != this.minIteration) {
			int index = event.getIteration() - this.minIteration;

			// create chart when data of more than one iteration is available.
			XYLineChart chart = new XYLineChart("Average Leg Travel Times Statistics", "iteration", "time");
			
			double[] iterations = new double[index + 1];
			for (int i = 0; i <= index; i++) {
				iterations[i] = i + this.minIteration;
			}
			
			double[] values = new double[index + 1];
			int i = 0;
			for (String mode : sortedModes) {
				System.arraycopy(this.history[i], 0, values, 0, index + 1);
				chart.addSeries(mode, iterations, values);
				i++;
			}
			System.arraycopy(this.history[i], 0, values, 0, index + 1);
			chart.addSeries("overall", iterations, values);
			
			chart.addMatsimLogo();
			chart.saveAsPng(this.fileName + ".png", 800, 600);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			this.writer.flush();
			this.writer.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

}
