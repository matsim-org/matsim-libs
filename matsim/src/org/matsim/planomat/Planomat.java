/* *********************************************************************** *
 * project: org.matsim.*
 * PlanOptimizeTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat;

import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.StockRandomGenerator;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.population.routes.Route;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.utils.misc.Time;

/**
 * The "heart" of the planomat external strategy module:
 * Optimize a the departure times and activity durations
 * of a given <code>Plan</code>
 * <ul>
 * <li> according to a <code>ScoringFunction</code>
 * <li> with respect to time-of-day dependent travel costs as perceived
 *   by a <code>LegtravelTimeEstimator</code>.
 * </ul>
 * @author meisterk
 *
 */
public class Planomat implements PlanAlgorithm {

	/**
	 * Maximum possible activity duration. Serves as upper limit for double encoding of activity durations in GA plan chromosome. 
	 */
	protected static final double MAX_ACTIVITY_DURATION = 24.0 * 3600;
	protected static final int NUM_TIME_INTERVALS = (int) Math.pow(2, Gbl.getConfig().planomat().getLevelOfTimeResolution());
	protected static final double TIME_INTERVAL_SIZE = Planomat.MAX_ACTIVITY_DURATION / Planomat.NUM_TIME_INTERVALS;

	private LegTravelTimeEstimator legTravelTimeEstimator = null;
	private ScoringFunctionFactory scoringFunctionFactory = null;
	private Random seedGenerator = null;

	private final static Logger logger = Logger.getLogger(Planomat.class);

	public Planomat(final LegTravelTimeEstimator legTravelTimeEstimator, final ScoringFunctionFactory scoringFunctionFactory) {

		this.legTravelTimeEstimator = legTravelTimeEstimator;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.seedGenerator = MatsimRandom.getLocalInstance();

	}

	public void run(final Plan plan) {

//		// distinguish for optimization tools
//		String optiToolboxName = Gbl.getConfig().planomat().getOptimizationToolbox();
//		if (optiToolboxName.equals(PlanomatConfigGroup.OPTIMIZATION_TOOLBOX_JGAP)) {

		// perform subtour analysis only if mode choice on subtour basis is optimized
		// (if only times are optimized, subtour analysis is not necessary)
		PlanAnalyzeSubtours planAnalyzeSubtours = null;
		if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
			planAnalyzeSubtours = new PlanAnalyzeSubtours();
			planAnalyzeSubtours.run(plan);
		}

		PlanomatJGAPConfiguration jgapConfiguration = new PlanomatJGAPConfiguration(plan, planAnalyzeSubtours);

		// JGAP random number generator is initialized for each run
		// but use a random number as seed so every run will draw a different, but deterministic sequence of random numbers
		long seed = this.seedGenerator.nextLong();
		((StockRandomGenerator) jgapConfiguration.getRandomGenerator()).setSeed( seed );

		IChromosome sampleChromosome = this.initSampleChromosome(plan, planAnalyzeSubtours, jgapConfiguration);
		try {
			jgapConfiguration.setSampleChromosome(sampleChromosome);
		} catch (InvalidConfigurationException e1) {
			e1.printStackTrace();
		}
		ScoringFunction sf = this.scoringFunctionFactory.getNewScoringFunction(plan);
		PlanomatFitnessFunctionWrapper fitnessFunction = new PlanomatFitnessFunctionWrapper( 
				sf, 
				plan, 
				this.legTravelTimeEstimator, 
				planAnalyzeSubtours );
		Genotype population = null;
		try {
			jgapConfiguration.setFitnessFunction( fitnessFunction );
			population = Genotype.randomInitialGenotype( jgapConfiguration );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
		IChromosome fittest = null;
		String logMessage = null;
		for (int i = 0; i < Gbl.getConfig().planomat().getJgapMaxGenerations(); i++) {
			population.evolve();
			if (Gbl.getConfig().planomat().isDoLogging()) {
				fittest = population.getFittestChromosome();
				logMessage = "Generation #" + Integer.toString(i) + " : Max: " + fittest.getFitnessValue();
				logger.info(logMessage);
			}
		}
		fittest = population.getFittestChromosome();
		this.writeChromosome2Plan(fittest, plan, planAnalyzeSubtours );

	}

	protected IChromosome initSampleChromosome(final Plan plan, final PlanAnalyzeSubtours planAnalyzeSubtours, final org.jgap.Configuration jgapConfiguration) {

		ArrayList<Gene> sampleGenes = new ArrayList<Gene>();
		try {

			int numActs = plan.getActsLegs().size() / 2;
			for (int ii=0; ii < numActs; ii++) {
				sampleGenes.add(new IntegerGene(jgapConfiguration, 0, Planomat.NUM_TIME_INTERVALS - 1));
			}

			if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
				for (int ii=0; ii < planAnalyzeSubtours.getNumSubtours(); ii++) {
					sampleGenes.add(new IntegerGene(jgapConfiguration, 0, Gbl.getConfig().planomat().getPossibleModes().length - 1));
				} 
			}

		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		IChromosome sampleChromosome = null;
		try {
			sampleChromosome = new Chromosome( jgapConfiguration, sampleGenes.toArray(new Gene[sampleGenes.size()]) );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return sampleChromosome;

	}

	/**
	 * Writes a JGAP chromosome back to matsim plan object.
	 *
	 * @param individual the GA individual (usually the fittest after evolution) whose values will be written back to a plan object
	 * @param plan the plan that will be altered
	 */
	protected void writeChromosome2Plan(
			final IChromosome individual, 
			final Plan plan, 
			final PlanAnalyzeSubtours planAnalyzeSubtours ) {

		Act activity = null;
		Leg leg = null;

		Route tempRoute = null;

		int max = plan.getActsLegs().size();
		double now = 0.0;

		for (int ii = 0; ii < max; ii++) {

			Object o = plan.getActsLegs().get(ii);

			if (o.getClass().equals(Act.class)) {

				activity = ((Act) o);

				// handle first activity and middle activities
				if (ii < (max - 1)) {

					activity.setStartTime(now);
					activity.setDuration((((IntegerGene) individual.getGenes()[ii / 2]).intValue() + this.seedGenerator.nextDouble()) * Planomat.TIME_INTERVAL_SIZE);
					now += activity.getDuration();
					activity.setEndTime(now);

					// handle last activity
				} else if (ii == (max - 1)) {

					// assume that there will be no delay between arrival time and activity start time
					activity.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					activity.setDuration(Time.UNDEFINED_TIME);
					activity.setEndTime(Time.UNDEFINED_TIME);

				}

			} else if (o.getClass().equals(Leg.class)) {

				leg = ((Leg) o);

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepartureTime(now);

				if (Gbl.getConfig().planomat().getPossibleModes().length > 0) {
					// set mode to result from optimization
					int subtourIndex = planAnalyzeSubtours.getSubtourIndexation()[ii / 2];
					int modeIndex = ((IntegerGene) individual.getGene(planAnalyzeSubtours.getSubtourIndexation().length + subtourIndex)).intValue();
					Mode mode = Gbl.getConfig().planomat().getPossibleModes()[modeIndex];
//					System.out.println(ii + "\t" + subtourIndex + "\t" + modeIndex + "\t" + modeName);
					leg.setMode(mode);
				} // otherwise leave modes untouched

				if (!leg.getMode().equals(BasicLeg.Mode.car)) {
					tempRoute = leg.getRoute();
				}

				// set arrival time to estimation
				Act origin = ((Act) plan.getActsLegs().get(ii - 1));
				Act destination = ((Act) plan.getActsLegs().get(ii + 1));

				double travelTimeEstimation = this.legTravelTimeEstimator.getLegTravelTimeEstimation(
						plan.getPerson().getId(),
						now,
						origin,
						destination,
						leg);

				leg.setTravelTime(travelTimeEstimation);

				if (!leg.getMode().equals(BasicLeg.Mode.car)) {
					// restore original routes, because planomat must not alter routes at all
					leg.setRoute(tempRoute);
				}
				leg.getRoute().setTravelTime(travelTimeEstimation);

				now += leg.getTravelTime();
				// set planned arrival time accordingly
				leg.setArrivalTime(now);

			}
		}

		// invalidate score information
		plan.setScore(Double.NaN);

		// reset leg travel time estimator
		this.legTravelTimeEstimator.reset();

	}

	public void setSeedGenerator(Random seedGenerator) {
		this.seedGenerator = seedGenerator;
	}

	public Random getSeedGenerator() {
		return seedGenerator;
	}

}