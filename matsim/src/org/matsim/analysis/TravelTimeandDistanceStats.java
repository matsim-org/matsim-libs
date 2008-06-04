/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeandDistanceStats.java
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.charts.XYLineChart;
import org.matsim.utils.io.IOUtils;

/**
 * Calculates at the end of each iteration the following statistics:
 * <ul>
 * <li>average plan travel time of the selected plans</li>
 * <li>average plan travel distance of the selected plans</li>
 * <li>average leg travel time of the selected plans</li>
 * <li>average leg travel distance of the selected plans</li>
 * </ul>

 * The calculated values are written to a file, each iteration on
 * a separate line.
 *
 * @author anhorni
 */

/*
 * TODO: This is copy-paste based on "ScoreStats". Refactoring needed!
 */
public class TravelTimeandDistanceStats implements StartupListener, IterationEndsListener, ShutdownListener {

	final private static int INDEX_PLANTT = 0;
	final private static int INDEX_PLANTD = 1;
	final private static int INDEX_LEGTT = 2;
	final private static int INDEX_LEGTD = 3;

	final private Plans population;
	final private BufferedWriter out;

	private final boolean createPNG;
	private double[][] history = null;
	private int minIteration = 0;

	private final static Logger log = Logger.getLogger(TravelTimeandDistanceStats.class);

	/**
	 * Creates a new TravelTimeandDistanceStats instance.
	 *
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public TravelTimeandDistanceStats(final Plans population, final String filename, final boolean createPNG) throws FileNotFoundException, IOException {
		this.population = population;
		this.createPNG = createPNG;
		this.out = IOUtils.getBufferedWriter(filename);
		this.out.write("ITERATION\tavg. plantraveltime\tavg. plantraveldistance\tavg. legtraveltime\tavg. legtraveldistance\n");

	}

	public void notifyStartup(final StartupEvent event) {
		if (this.createPNG) {
			Controler controler = event.getControler();
			this.minIteration = controler.getFirstIteration();
			int maxIter = controler.getLastIteration();
			int iterations = maxIter - this.minIteration;
			if (iterations > 10000) {
				iterations = 1000; // limit the history size
			}
			this.history = new double[4][iterations+1];
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		double sumPlanTraveltime = 0.0;
		double sumPlanTraveldistance = 0.0;
		int sumNumberOfLegs=0;


		for (Person person : this.population.getPersons().values()) {
			PersonTimeDistanceCalculator.run(person);
			sumPlanTraveltime+=PersonTimeDistanceCalculator.getPlanTravelTime();
			sumPlanTraveldistance+=PersonTimeDistanceCalculator.getPlanTravelDistance();
			sumNumberOfLegs+=PersonTimeDistanceCalculator.getNumberOfLegs();
		}

		int nr_persons=this.population.getPersons().size();

		log.info("-- avg. plan traveltime of the selected plan: " + sumPlanTraveltime / nr_persons);
		log.info("-- avg. plan traveldistance of the selected plan: "  + sumPlanTraveldistance / nr_persons);
		log.info("-- avg. leg traveltime of the selected plan: "  + sumPlanTraveltime / (sumNumberOfLegs * nr_persons));
		log.info("-- avg. leg traveldistance of the selected plan: "  + sumPlanTraveldistance / (sumNumberOfLegs * nr_persons));


		try {
			this.out.write(event.getIteration() + "\t" + (sumPlanTraveltime / nr_persons) + "\t" +
					(sumPlanTraveldistance / nr_persons) + "\t" + (sumPlanTraveltime / (sumNumberOfLegs * nr_persons)) + "\t" +
					(sumPlanTraveldistance / (sumNumberOfLegs * nr_persons)) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_PLANTT][index] = (sumPlanTraveltime / nr_persons);
			this.history[INDEX_PLANTD][index] = (sumPlanTraveldistance / nr_persons);
			this.history[INDEX_LEGTT][index] = (sumPlanTraveltime / (sumNumberOfLegs * nr_persons));
			this.history[INDEX_LEGTD][index] = (sumPlanTraveldistance / (sumNumberOfLegs * nr_persons));

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart_time = new XYLineChart("Travel Time Statistics", "iteration", " avg time");
				double[] iterations_time = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations_time[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history[INDEX_PLANTT], 0, values, 0, index + 1);
				chart_time.addSeries("avg. plan travel time", iterations_time, values);
				System.arraycopy(this.history[INDEX_LEGTT], 0, values, 0, index + 1);
				chart_time.addSeries("avg. leg travel time", iterations_time, values);
				chart_time.addMatsimLogo();
				chart_time.saveAsPng(Controler.getOutputFilename("timestats.png"), 800, 600);


				XYLineChart chart_distance = new XYLineChart("Travel Distance Statistics", "iteration", " avg distance");
				double[] iterations_distance = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations_distance[i] = i + this.minIteration;
				}
				double[] values_distance = new double[index + 1];
				System.arraycopy(this.history[INDEX_PLANTD], 0, values_distance, 0, index + 1);
				chart_distance.addSeries("avg. plan travel distance", iterations_distance, values_distance);
				System.arraycopy(this.history[INDEX_LEGTD], 0, values_distance, 0, index + 1);
				chart_distance.addSeries("avg. leg travel distance", iterations_distance, values_distance);
				chart_distance.addMatsimLogo();
				chart_distance.saveAsPng(Controler.getOutputFilename("distancestats.png"), 800, 600);
			}
			if (index == this.history[0].length) {
				// we cannot store more information, so disable the graph feature.
				this.history = null;
			}
		}
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @return the history of scores in last iterations
	 */
	public double[][] getHistory() {
		return this.history.clone();
	}
}
