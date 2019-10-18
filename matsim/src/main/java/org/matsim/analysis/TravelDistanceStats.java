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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;
import java.util.Locale;

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

	private final ControlerConfigGroup controlerConfigGroup;
	final private BufferedWriter out;
	final private String fileName;

	private double[] history = null;

	private final static Logger log = Logger.getLogger(TravelDistanceStats.class);

	@Inject
	TravelDistanceStats(ControlerConfigGroup controlerConfigGroup, OutputDirectoryHierarchy controlerIO) {
		this(controlerConfigGroup, controlerIO.getOutputFilename("traveldistancestats"), controlerConfigGroup.isCreateGraphs());
	}

	/**
	 * @param filename including the path, excluding the file type extension
	 * @param createPNG true if in every iteration, the distance statistics should be visualized in a graph and written to disk.
	 * @throws UncheckedIOException
	 */
	public TravelDistanceStats(final Config config, final String filename, final boolean createPNG) throws UncheckedIOException {
        this(config.controler(), filename, createPNG);
    }

    private TravelDistanceStats(ControlerConfigGroup controlerConfigGroup, String filename, boolean createPNG) {
		this.controlerConfigGroup = controlerConfigGroup;
		this.fileName = filename;
		if (createPNG) {
			int iterations = controlerConfigGroup.getLastIteration() - controlerConfigGroup.getFirstIteration();
			if (iterations > 5000) {
				iterations = 5000; // limit the history size
			}
			this.history = new double[iterations+1];
		}
		if (filename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
			this.out = IOUtils.getBufferedWriter(filename);
		} else {
			this.out = IOUtils.getBufferedWriter(filename + ".txt");
		}
		try {
            this.out.write("ITERATION\tavg. Average Leg distance\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void addIteration(int iteration, IdMap<Person, Plan> map) {
        DoubleSummaryStatistics stats = map.values()
                .parallelStream()
                .flatMap(plan -> plan.getPlanElements().stream())
                .filter(Leg.class::isInstance)
                .mapToDouble(l -> {
                    Leg leg = (Leg) l;
                    return leg.getRoute() != null ? leg.getRoute().getDistance() : Double.NaN;
                })
                .filter(Double::isFinite)
                .summaryStatistics();

        log.info("-- average leg distance per plan (executed plans only): " + stats.getAverage() + " meters");
        log.info("average distance per Person (executed plans only): " + stats.getSum() / map.size() + " meters");
        log.info("(TravelDistanceStats takes an average over all legs where the simulation reports travelled (network) distances");
		log.info("(and teleported legs whose route contains a distance.)");

		try {
            this.out.write(iteration + "\t" + stats.getAverage() + "\t" + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = iteration - controlerConfigGroup.getFirstIteration();
            this.history[index] = stats.getAverage();

			if (iteration != controlerConfigGroup.getFirstIteration()) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("Leg Travel Distance Statistics", "iteration", "average of the average leg distance per plan ");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + controlerConfigGroup.getFirstIteration();
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history, 0, values, 0, index + 1);
				chart.addSeries("executed plan", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(this.fileName + ".png", 800, 600);
			}
			if (index == (this.history.length - 1)) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
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
