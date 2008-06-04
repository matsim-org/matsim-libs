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
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.controler.Controler;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.plans.Leg;
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
	 * Creates a new ScoreStats instance.
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
		double sumLegTraveltime = 0.0;
		double sumLegTraveldistance = 0.0;


		for (Person person : this.population.getPersons().values()) {
			sumPlanTraveltime+=this.getTravelTime(person);
			sumPlanTraveldistance+=this.getTravelDist(person);
			sumLegTraveltime+=this.getTravelTime(person)/this.getNumberOfTrips(person);
			sumLegTraveldistance+=this.getTravelDist(person)/this.getNumberOfTrips(person);
		}

		int nr_persons=this.population.getPersons().size();

		log.info("-- avg. plan traveltime of the selected plan: " + sumPlanTraveltime / nr_persons);
		log.info("-- avg. plan traveldistance of the selected plan: "  + sumPlanTraveldistance / nr_persons);
		log.info("-- avg. leg traveltime of the selected plan: "  + sumLegTraveltime / nr_persons);
		log.info("-- avg. leg traveldistance of the selected plan: "  + sumLegTraveldistance / nr_persons);


		try {
			this.out.write(event.getIteration() + "\t" + (sumPlanTraveltime / nr_persons) + "\t" +
					(sumPlanTraveldistance / nr_persons) + "\t" + (sumLegTraveltime / nr_persons) + "\t" +
					(sumLegTraveldistance / nr_persons) + "\n");
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.history != null) {
			int index = event.getIteration() - this.minIteration;
			this.history[INDEX_PLANTT][index] = (sumPlanTraveltime / nr_persons);
			this.history[INDEX_PLANTD][index] = (sumPlanTraveldistance / nr_persons);
			this.history[INDEX_LEGTT][index] = (sumLegTraveltime / nr_persons);
			this.history[INDEX_LEGTD][index] = (sumLegTraveldistance / nr_persons);

			if (event.getIteration() != this.minIteration) {
				// create chart when data of more than one iteration is available.
				XYLineChart chart = new XYLineChart("TT and TD Statistics", "iteration", " avg time");
				double[] iterations = new double[index + 1];
				for (int i = 0; i <= index; i++) {
					iterations[i] = i + this.minIteration;
				}
				double[] values = new double[index + 1];
				System.arraycopy(this.history[INDEX_PLANTT], 0, values, 0, index + 1);
				chart.addSeries("avg. plan travel time", iterations, values);
				System.arraycopy(this.history[INDEX_PLANTD], 0, values, 0, index + 1);
				chart.addSeries("avg. plan travel distance score", iterations, values);
				System.arraycopy(this.history[INDEX_LEGTT], 0, values, 0, index + 1);
				chart.addSeries("avg. leg travel time", iterations, values);
				System.arraycopy(this.history[INDEX_LEGTD], 0, values, 0, index + 1);
				chart.addSeries("avg. leg travel distance", iterations, values);
				chart.addMatsimLogo();
				chart.saveAsPng(Controler.getOutputFilename("timedistancestats.png"), 800, 600);
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

	private double getTravelTime(final Person person) {

		double travelTime=0.0;
		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			final Leg leg = (Leg)leg_it.next();
			travelTime+=leg.getTravTime();
		}
		return travelTime;
	}

	private double getTravelDist(final Person person) {

		double travelDist=0.0;
		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();

		while (leg_it.hasNext()) {
			final Leg leg = (Leg)leg_it.next();
			travelDist+=leg.getRoute().getDist();
		}
		return travelDist;
	}

	private int getNumberOfTrips(final Person person) {

		int numberOfLegs=0;
		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			leg_it.next();
			numberOfLegs++;
		}
		return numberOfLegs;
	}
}
