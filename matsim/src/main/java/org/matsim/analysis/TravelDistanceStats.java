/* *********************************************************************** *
 * project: org.matsim.*
 * TravelDistanceStats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * 	<li>average of the average leg distance per plan</li>
 * </ul>
 *
 * Is used by the standard Controler and fed the "really executed" "plans" which
 * are generated from Events during the simulation and which are also used by the scoring.
 * But you can also use it on other kinds of plans from your own code.
 *
 * @author anhorni, michaz, jbischoff
 */

public class TravelDistanceStats {

	private static final int MAX_ITERATIONS_IN_GRAPH = 5000;

	private final ControllerConfigGroup controllerConfigGroup;
	private BufferedWriter out;
	private final String legStatsPngName;
	private final String tripStatsPngName;
	private final String delimiter;

	private Map<Integer, DoubleSummaryStatistics> legStats;
	private Map<Integer, DoubleSummaryStatistics> tripStats;

	private final static Logger log = LogManager.getLogger(TravelDistanceStats.class);

	@Inject
	TravelDistanceStats(ControllerConfigGroup controllerConfigGroup, OutputDirectoryHierarchy controlerIO, GlobalConfigGroup globalConfig) {
		this(controllerConfigGroup, controlerIO.getOutputFilename("traveldistancestats"),
				controlerIO.getOutputFilename("traveldistancestats") + "legs",
				controlerIO.getOutputFilename("traveldistancestats") + "trips",
				globalConfig.getDefaultDelimiter());
	}

	private TravelDistanceStats(ControllerConfigGroup controllerConfigGroup, String travelDistanceStatsFileName, String legStatsPngName,
								String tripStatsPngName, String delimiter) {
		this.controllerConfigGroup = controllerConfigGroup;
		this.legStatsPngName = legStatsPngName;
		this.tripStatsPngName = tripStatsPngName;
		this.delimiter = delimiter;
		initStats(controllerConfigGroup);
		initWriter(travelDistanceStatsFileName);
	}

	private void initWriter(String travelDistanceStatsFileName) {
		if (travelDistanceStatsFileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
			this.out = IOUtils.getBufferedWriter(travelDistanceStatsFileName);
		} else {
			this.out = IOUtils.getBufferedWriter(travelDistanceStatsFileName + ".csv");
		}

		try {
			writeCsvHeader();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void initStats(ControllerConfigGroup controllerConfigGroup) {
		int expectedIterations = controllerConfigGroup.getLastIteration() - controllerConfigGroup.getFirstIteration();
		this.legStats = new HashMap<>(expectedIterations+1);
		this.tripStats = new HashMap<>(expectedIterations+1);
	}

	public void addIteration(int iteration, IdMap<Person, Plan> map) {
		DoubleSummaryStatistics legStats = getLegStats(map);
		DoubleSummaryStatistics tripStats = getTripStats(map);

		log.info("-- average leg distance per plan (executed plans only): " + legStats.getAverage() + " meters");
        log.info("average leg distance per Person (executed plans only): " + legStats.getSum() / map.size() + " meters (statistic on all " + legStats.getCount() + " legs which have a finite distance)");
        log.info("-- average trip distance per plan (executed plans only): " + tripStats.getAverage() + " meters");
        log.info("average trip distance per Person (executed plans only): " + tripStats.getSum() / map.size() + " meters (statistic on all " + tripStats.getCount() + " trips which have a finite distance)");
        log.info("(TravelDistanceStats takes an average over all legs where the simulation reports travelled (network) distances");
		log.info("(and teleported legs whose route contains a distance.)");// TODO: still valid?

		this.legStats.put(iteration, legStats);
		this.tripStats.put(iteration, tripStats);

		assert this.legStats.size() == this.tripStats.size();
	}

	void writeOutput(int iteration, boolean writePngs){
		writeCsvEntry(iteration);

		if(writePngs && this.legStats.size() < MAX_ITERATIONS_IN_GRAPH){
			writePngs(iteration);
		}
	}

	private void writeCsvHeader() throws IOException {
		this.out.write("ITERATION" + this.delimiter + "avg. Average Leg distance" + this.delimiter + "avg. Average Trip distance\n");
		this.out.flush();
	}

	private void writeCsvEntry(int iteration) {
		DoubleSummaryStatistics legStats = this.legStats.get(iteration);
		DoubleSummaryStatistics tripStats = this.tripStats.get(iteration);

		try {
			this.out.write(iteration + this.delimiter + legStats.getAverage() + this.delimiter + tripStats.getAverage() + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void writePngs(int iteration){
		writeLegStatsPng(iteration);
		writeTripStatsPng(iteration);
	}

	private void writeTripStatsPng(int iteration) {
		if (iteration == controllerConfigGroup.getFirstIteration()){
			return;
		}

		// create chart when data of more than one iteration is available.
		XYLineChart chart = new XYLineChart("Trip Travel Distance Statistics", "iteration", "average of the average trip distance per plan ");
		double[] iterations = new double[iteration + 1];
		double[] values = new double[iteration + 1];

		for (int i = 0; i <= iteration; i++) {
			iterations[i] = i + controllerConfigGroup.getFirstIteration();
			values[i] = Optional.ofNullable(this.tripStats.get(i)).map(DoubleSummaryStatistics::getAverage).orElse(0.);
		}

		chart.addSeries("executed plan", iterations, values);
		chart.addMatsimLogo();
		chart.saveAsPng(this.tripStatsPngName + ".png", 800, 600);
	}

	private void writeLegStatsPng(int iteration) {
		if (iteration == controllerConfigGroup.getFirstIteration()){
			return;
		}

		// create chart when data of more than one iteration is available.
		XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "average of the average leg distance per plan ");
		double[] iterations = new double[iteration + 1];
		double[] values = new double[iteration + 1];

		for (int i = 0; i <= iteration; i++) {
			iterations[i] = i + controllerConfigGroup.getFirstIteration();
			values[i] = Optional.ofNullable(this.legStats.get(i)).map(DoubleSummaryStatistics::getAverage).orElse(0.);

		}

		chart.addSeries("executed plan", iterations, values);
		chart.addMatsimLogo();
		chart.saveAsPng(this.legStatsPngName + ".png", 800, 600);
	}

	private static DoubleSummaryStatistics getTripStats(IdMap<Person, Plan> map) {
		return map.values()
				  .parallelStream()
				  .flatMap(plan -> TripStructureUtils.getTrips(plan).stream())
				  .mapToDouble(t -> {
					  Trip trip = (Trip) t;
					  return trip.getTripElements()
								 .stream()
								 .filter(Leg.class::isInstance)
								 .collect(Collectors.summingDouble(l -> {
									 Leg leg = (Leg) l;
									 // TODO NaN handling of Collectors.summingDouble will lead to many NaNs... rethink
									 return leg.getRoute() != null ? leg.getRoute().getDistance() : Double.NaN;
								 }));
				  })
				  // the following means trips with infinite distance are silently ignored.
				  .filter(Double::isFinite)
				  .summaryStatistics();
	}

	private static DoubleSummaryStatistics getLegStats(IdMap<Person, Plan> map) {
		return map.values()
				  //TODO: This probably doesn't control how many threads parallelStream is using despite the number of threads setting in config
				  .parallelStream()
				  .flatMap(plan -> plan.getPlanElements().stream())
				  .filter(Leg.class::isInstance)
				  .mapToDouble(l -> {
					  Leg leg = (Leg) l;
					  return leg.getRoute() != null ? leg.getRoute().getDistance() : Double.NaN;
				  })
				  // the following means legs with infinite distance are ignored
				  .filter(Double::isFinite)
				  .summaryStatistics();
	}

	public void close() {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
