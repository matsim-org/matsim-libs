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
import org.matsim.core.config.Config;
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
import java.util.DoubleSummaryStatistics;
import java.util.Locale;
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

	private final ControllerConfigGroup controllerConfigGroup;
	private final BufferedWriter out;
	private final String legStatsPngName;
	private final String tripStatsPngName;
	private final String delimiter;

	private double[] legStatsHistory = null;
	private double[] tripStatsHistory = null;

	private final static Logger log = LogManager.getLogger(TravelDistanceStats.class);

	@Inject
	TravelDistanceStats(ControllerConfigGroup controllerConfigGroup, OutputDirectoryHierarchy controlerIO, GlobalConfigGroup globalConfig) {
		this(controllerConfigGroup, controlerIO.getOutputFilename("traveldistancestats"),
				controlerIO.getOutputFilename("traveldistancestats") + "legs",
				controlerIO.getOutputFilename("traveldistancestats") + "trips",
				controllerConfigGroup.isCreateGraphs(),
				globalConfig.getDefaultDelimiter());
	}

	/**
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the distance statistics should be visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public TravelDistanceStats(final Config config, final String filename, final boolean createPNG, final String delimiter) throws UncheckedIOException {
        this(config.controller(), filename, filename + "legs", filename + "trips", createPNG, delimiter);
    }

	private TravelDistanceStats(ControllerConfigGroup controllerConfigGroup, String travelDistanceStatsFileName,
																String legStatsPngName, String tripStatsPngName, boolean createPNG, String delimiter) {
		this.controllerConfigGroup = controllerConfigGroup;
		this.legStatsPngName = legStatsPngName;
		this.tripStatsPngName = tripStatsPngName;
		this.delimiter = delimiter;
		if (createPNG) {
			int iterations = controllerConfigGroup.getLastIteration() - controllerConfigGroup.getFirstIteration();
			if (iterations > 5000) {
				iterations = 5000; // limit the history size
			}
			this.legStatsHistory = new double[iterations+1];
			this.tripStatsHistory = new double[iterations+1];
		}
		if (travelDistanceStatsFileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
			this.out = IOUtils.getBufferedWriter(travelDistanceStatsFileName);
		} else {
			this.out = IOUtils.getBufferedWriter(travelDistanceStatsFileName + ".csv");
		}
		try {
			this.out.write("ITERATION" + this.delimiter + "avg. Average Leg distance" + this.delimiter + "avg. Average Trip distance\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void addIteration(int iteration, IdMap<Person, Plan> map) {
        DoubleSummaryStatistics legStats = map.values()
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

        DoubleSummaryStatistics tripStats = map.values()
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

        log.info("-- average leg distance per plan (executed plans only): " + legStats.getAverage() + " meters");
        log.info("average leg distance per Person (executed plans only): " + legStats.getSum() / map.size() + " meters (statistic on all " + legStats.getCount() + " legs which have a finite distance)");
        log.info("-- average trip distance per plan (executed plans only): " + tripStats.getAverage() + " meters");
        log.info("average trip distance per Person (executed plans only): " + tripStats.getSum() / map.size() + " meters (statistic on all " + tripStats.getCount() + " trips which have a finite distance)");
        log.info("(TravelDistanceStats takes an average over all legs where the simulation reports travelled (network) distances");
		log.info("(and teleported legs whose route contains a distance.)");// TODO: still valid?

		try {
			this.out.write(iteration + this.delimiter + legStats.getAverage() + this.delimiter + tripStats.getAverage() + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.legStatsHistory != null) {
			int index = iteration - controllerConfigGroup.getFirstIteration();
            this.legStatsHistory[index] = legStats.getAverage();

			if (iteration != controllerConfigGroup.getFirstIteration()) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "average of the average leg distance per plan ");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + controllerConfigGroup.getFirstIteration();
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.legStatsHistory, 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(this.legStatsPngName + ".png", 800, 600);
			}
			if (index == (this.legStatsHistory.length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.legStatsHistory = null;
			}
		}

		if (this.tripStatsHistory != null) {
			int index = iteration - controllerConfigGroup.getFirstIteration();
            this.tripStatsHistory[index] = tripStats.getAverage();

			if (iteration != controllerConfigGroup.getFirstIteration()) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Trip Travel Distance Statistics", "iteration", "average of the average trip distance per plan ");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + controllerConfigGroup.getFirstIteration();
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.tripStatsHistory, 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(this.tripStatsPngName + ".png", 800, 600);
			}
			if (index == (this.tripStatsHistory.length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.tripStatsHistory = null;
			}
		}
	}

	public void close() {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
