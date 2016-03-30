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

package playground.singapore.springcalibration.run.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
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
 * @author cdobler / anhorni
 */
public class TripsDurationAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		StartupListener, IterationEndsListener, ShutdownListener {
	
	private final static Logger log = Logger.getLogger(TripsDurationAnalyzer.class);

	public static String defaultTripsFileName = "tripCounts";
	public static String defaultDurationsFileName = "tripDurations";
	
	private final Set<String> sortedModes = new TreeSet<String>();
	private final Set<Id> observedAgents;
	private final Map<Id, Double> departureTimes = new HashMap<Id, Double>();
	private final Map<String, List<Double>> legTravelTimes = new HashMap<String, List<Double>>();
	
	private String tripsFileName;
	private String durationsFileName;
	private boolean createGraphs;
	
	private BufferedWriter tripsWriter;
	private BufferedWriter durationWriter;
	
	private double[][] tripsHistory;
	private double[][] durationHistory;
	private int minIteration;
	
	/**
	 * This is how most people will probably will use this class.
	 * It has to be created an registered as ControlerListener.
	 * Then, it auto-configures itself (register as events handler,
	 * get paths to output files, ...).
	 */
	public TripsDurationAnalyzer() {
		
		this.createGraphs = true;
		
		// modes which are analyzed by default
		this.sortedModes.add(TransportMode.car);
		this.sortedModes.add(TransportMode.pt);
		this.sortedModes.add(TransportMode.walk);
		
		this.observedAgents = null;
	}
	
	public TripsDurationAnalyzer(Set<String> modes, boolean createGraphs) {
		this(modes, null, createGraphs);
	}
	
	public TripsDurationAnalyzer(Set<String> modes, Set<Id> observedAgents, boolean createGraphs) {
		
		this.sortedModes.addAll(modes);
		if (observedAgents != null) {
			// make a copy to prevent people changing the set over the iterations
			this.observedAgents = new HashSet<Id>(observedAgents);			
		} else this.observedAgents = null;
		this.createGraphs = createGraphs;
	}
	
	public void setCreateGraphs(boolean createGraphs) {
		this.createGraphs = createGraphs;
	}
	
	public Set<String> getModes() {
		return this.sortedModes;
	}
	
	@Override
	public void reset(int iteration) {
		for(List<Double> modeTravelTime : legTravelTimes.values()) {
			modeTravelTime.clear();
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (observedAgents != null && !observedAgents.contains(event.getPersonId())) return;
		
		
		double travelTime = 0.0;
		String mode = event.getLegMode();
		Double departureTime = departureTimes.remove(event.getPersonId());
		if (departureTime != null) {		
			//if (departureTime == null) throw new RuntimeException("No departure time for agent " + event.getPersonId() + " was found!");
			travelTime = event.getTime() - departureTime;
			travelTime = travelTime / 60.0;
		} else log.warn("No travel time for agent: " + event.getPersonId() + " with mode " + mode);
		
		List<Double> modeTravelTimes = legTravelTimes.get(mode);
		if (modeTravelTimes != null) modeTravelTimes.add(travelTime);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (observedAgents != null && !observedAgents.contains(event.getPersonId())) return;
		departureTimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		log.info("Starting up");
		MatsimServices controler = event.getServices();
		this.minIteration = controler.getConfig().controler().getFirstIteration();
		int maxIter = controler.getConfig().controler().getLastIteration();
		int iterations = maxIter - this.minIteration;
		this.tripsHistory = new double[this.sortedModes.size() + 1][iterations + 1];
		this.durationHistory = new double[this.sortedModes.size() + 1][iterations + 1];	
		
		this.tripsFileName = event.getServices().getControlerIO().getOutputFilename(defaultTripsFileName);
		this.durationsFileName = event.getServices().getControlerIO().getOutputFilename(defaultDurationsFileName);
		controler.getEvents().addHandler(this);

		this.tripsWriter = IOUtils.getBufferedWriter(tripsFileName + ".txt");
		this.durationWriter = IOUtils.getBufferedWriter(durationsFileName + ".txt");

		try {
			this.tripsWriter.write("ITERATION");
			this.durationWriter.write("ITERATION");
			for (String mode : sortedModes) {
				this.tripsWriter.write("\t");
				this.tripsWriter.write(mode.toUpperCase());
				this.durationWriter.write("\t");
				this.durationWriter.write(mode.toUpperCase());
				
				this.legTravelTimes.put(mode, new LinkedList<Double>());
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
			this.tripsWriter.close();
			this.durationWriter.close();
			this.tripsWriter = null;
			this.durationWriter = null;
		} catch (IOException e) {
			throw new RuntimeException(e);
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
			chart = new XYLineChart("Average Leg Travel Times Statistics for Car", "iteration", "time [min]");
			
			i = 0;
			for (String mode : sortedModes) {
				System.arraycopy(this.durationHistory[i], 0, values, 0, index + 1);
				chart.addSeries(mode, iterations, values);
				i++;
			}
			System.arraycopy(this.durationHistory[i], 0, values, 0, index + 1);
			chart.addSeries("overall", iterations, values);
			
			chart.addMatsimLogo();
			chart.saveAsPng(this.durationsFileName + ".png", 800, 600);
			
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
			if (this.tripsWriter != null) {
				this.tripsWriter.flush();
				this.tripsWriter.close();				
			}
			if (this.durationWriter != null) {
				this.durationWriter.flush();
				this.durationWriter.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
