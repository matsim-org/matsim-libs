/* *********************************************************************** *
 * project: org.matsim.*
 * TripsAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * - number of trips per mode
 * - number of trips over all modes
 * 
 * @author cdobler
 */
public class TripsAnalyzer implements AgentDepartureEventHandler, AgentArrivalEventHandler,
		StartupListener, IterationEndsListener, ShutdownListener {

	private final BufferedWriter tripsWriter;
	private final BufferedWriter durationWriter;
	
	private final Set<String> sortedModes;
	private final Map<Id, Double> departureTimes = new HashMap<Id, Double>();
	private final Map<String, List<Double>> legTravelTimes = new HashMap<String, List<Double>>();
	
	private final String tripsFileName;
	private final String durationFileName;
	private final boolean createGraphs;
	
	private double[][] tripsHistory;
	private double[][] durationHistory;
	private int minIteration;
	
	public TripsAnalyzer(String tripsFileName, String durationFileName, Set<String> modes, boolean createGraphs) {

		this.tripsFileName = tripsFileName;
		this.durationFileName = durationFileName;
		this.createGraphs = createGraphs;
		
		this.tripsWriter = IOUtils.getBufferedWriter(tripsFileName + ".txt");
		this.durationWriter = IOUtils.getBufferedWriter(durationFileName + ".txt");
		
		sortedModes = new TreeSet<String>(modes);
		try {
			this.tripsWriter.write("ITERATION");
			this.durationWriter.write("ITERATION");
			for (String mode : sortedModes) {
				this.tripsWriter.write("\t");
				this.tripsWriter.write(mode.toUpperCase());
				this.durationWriter.write("\t");
				this.durationWriter.write(mode.toUpperCase());
				
				this.legTravelTimes.put(mode, new ArrayList<Double>());
			}
			this.tripsWriter.write("\t");
			this.tripsWriter.write("OVERALL");
			this.tripsWriter.write("\n");
			this.durationWriter.write("\t");
			this.durationWriter.write("OVERALL");
			this.durationWriter.write("\n");
			
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
		if (modeTravelTimes != null) modeTravelTimes.add(travelTime);
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
		this.tripsHistory = new double[this.sortedModes.size() + 1][iterations + 1];
		this.durationHistory = new double[this.sortedModes.size() + 1][iterations + 1];
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			int iteration = event.getIteration();
			this.tripsWriter.write(String.valueOf(iteration));
			this.durationWriter.write(String.valueOf(iteration));
			int index = iteration - this.minIteration;
			
			int i = 0;
			int overallTrips = 0;
			double overallTravelTime = 0.0;
			for (String mode : sortedModes) {
				List<Double> modeTravelTimes = legTravelTimes.get(mode);
				double sumTravelTimes = 0.0;
				for (double travelTime : modeTravelTimes) sumTravelTimes += travelTime;
				
				int modeTrips = modeTravelTimes.size();
				overallTrips += modeTrips;
				overallTravelTime += sumTravelTimes;

				double averageTravelTime = sumTravelTimes / modeTrips;
				this.tripsHistory[i][index] = modeTrips;
				this.durationHistory[i][index] = averageTravelTime;
				
				this.tripsWriter.write("\t");
				this.tripsWriter.write(String.valueOf(modeTrips));
				this.durationWriter.write("\t");
				this.durationWriter.write(String.valueOf(averageTravelTime));
				
				i++;
			}
			
			double averageTravelTime = overallTravelTime / overallTrips;
			this.tripsHistory[sortedModes.size()][index] = overallTrips;
			this.durationHistory[sortedModes.size()][index] = averageTravelTime;
			
			this.tripsWriter.write("\t");
			this.tripsWriter.write(String.valueOf(overallTrips));
			this.tripsWriter.write("\n");
			this.durationWriter.write("\t");
			this.durationWriter.write(String.valueOf(averageTravelTime));
			this.durationWriter.write("\n");

			this.tripsWriter.flush();
			this.durationWriter.flush();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		if (this.createGraphs && event.getIteration() != this.minIteration) {
			int index = event.getIteration() - this.minIteration;

			// create chart when data of more than one iteration is available.
			XYLineChart chart;
			
			double[] iterations = new double[index + 1];
			for (int i = 0; i <= index; i++) {
				iterations[i] = i + this.minIteration;
			}
			double[] values = new double[index + 1];
			
			int i;
			
			/*
			 * average leg duration
			 */
			chart = new XYLineChart("Average Leg Travel Times Statistics", "iteration", "time");
			
			i = 0;
			for (String mode : sortedModes) {
				System.arraycopy(this.durationHistory[i], 0, values, 0, index + 1);
				chart.addSeries(mode, iterations, values);
				i++;
			}
			System.arraycopy(this.durationHistory[i], 0, values, 0, index + 1);
			chart.addSeries("overall", iterations, values);
			
			chart.addMatsimLogo();
			chart.saveAsPng(this.durationFileName + ".png", 800, 600);
			
			/*
			 * number of trips
			 */
			chart = new XYLineChart("Number of Trips per Mode Statistics", "iteration", "number of trips");
			
			i = 0;
			for (String mode : sortedModes) {
				System.arraycopy(this.tripsHistory[i], 0, values, 0, index + 1);
				chart.addSeries(mode, iterations, values);
				i++;
			}
			System.arraycopy(this.tripsHistory[i], 0, values, 0, index + 1);
			chart.addSeries("overall", iterations, values);
			
			chart.addMatsimLogo();
			chart.saveAsPng(this.tripsFileName + ".png", 800, 600);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			this.tripsWriter.flush();
			this.durationWriter.flush();
			this.tripsWriter.close();
			this.durationWriter.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

}
