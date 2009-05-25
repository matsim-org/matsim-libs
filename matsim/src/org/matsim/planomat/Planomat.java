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

import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.IntegerGene;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Route;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

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
	public static final double SCENARIO_DURATION = 24.0 * 3600;

	protected static enum StepThroughPlanAction {EVALUATE, WRITE_BACK}

	private final PlanomatConfigGroup config = Gbl.getConfig().planomat();
	private final int numTimeIntervals = (int) Math.pow(2, config.getLevelOfTimeResolution());
	protected final double timeIntervalSize = Planomat.SCENARIO_DURATION / numTimeIntervals;

	private final TransportMode[] possibleModes = this.config.getPossibleModes().toArray(new TransportMode[this.config.getPossibleModes().size()]);

	private final LegTravelTimeEstimator legTravelTimeEstimator;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Random seedGenerator;

	private final static Logger logger = Logger.getLogger(Planomat.class);
	private final boolean doLogging = this.config.isDoLogging();

	private PlanAnalyzeSubtours planAnalyzeSubtours = null;

	public Planomat(final LegTravelTimeEstimator legTravelTimeEstimator, final ScoringFunctionFactory scoringFunctionFactory) {

		this.legTravelTimeEstimator = legTravelTimeEstimator;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.seedGenerator = MatsimRandom.getLocalInstance();
	}

	public void run(final Plan plan) {
		if (this.doLogging) {
			logger.info("Running planomat on plan of person # " + plan.getPerson().getId().toString() + "...");
		}
		// perform subtour analysis only if mode choice on subtour basis is optimized
		// (if only times are optimized, subtour analysis is not necessary)
		if (this.possibleModes.length > 0) {
			if (this.doLogging) {
				logger.info("Running subtour analysis...");
			}
			this.planAnalyzeSubtours = new PlanAnalyzeSubtours();
			this.planAnalyzeSubtours.run(plan);
		}
		if (this.doLogging) {
			logger.info("Running subtour analysis...done.");
			logger.info("Initialization of JGAP configuration...");
		}
		Genotype population = this.initJGAP(plan, planAnalyzeSubtours);
		if (this.doLogging) {
			logger.info("Initialization of JGAP configuration...done.");
			logger.info("Running evolution...");
		}
		IChromosome fittest = this.evolveAndReturnFittest(population);
		if (this.doLogging) {
			logger.info("Running evolution...done.");
			logger.info("Writing solution back to Plan object...");
		}
		/*double score =*/ this.stepThroughPlan(StepThroughPlanAction.WRITE_BACK, fittest, plan);
		if (this.doLogging) {
			logger.info("Writing solution back to Plan object...done.");
			logger.info("Running planomat on plan of person # " + plan.getPerson().getId().toString() + "...done.");
		}
		// reset leg travel time estimator
		this.legTravelTimeEstimator.reset();
		// invalidate score information
		plan.setScore(null);
	}

//	protected EnumSet<TransportMode> getModifiedModeChoiceSet(final Plan plan) {

//	EnumSet<TransportMode> modeChoiceSet = Gbl.getConfig().planomat().getPossibleModes().clone();

//	if (!plan.getPerson().getCarAvail().equals("always")) {
//	modeChoiceSet.remove(TransportMode.car);
//	}

//	return modeChoiceSet;
//	}

	private synchronized Genotype initJGAP(final Plan plan, final PlanAnalyzeSubtours planAnalyzeSubtours) {

		Genotype population = null;

		// JGAP random number generator is initialized for each run
		// but use a random number as seed so every run will draw a different, but deterministic sequence of random numbers
		long seed = this.seedGenerator.nextLong();
		if (this.doLogging) {
			logger.info("agent id: " + plan.getPerson().getId() + "; JGAP seed: " + Long.toString(seed));
		}
		PlanomatJGAPConfiguration jgapConfiguration = new PlanomatJGAPConfiguration(plan, planAnalyzeSubtours, seed);

		IChromosome sampleChromosome = this.initSampleChromosome(plan, planAnalyzeSubtours, jgapConfiguration);
		try {
			jgapConfiguration.setSampleChromosome(sampleChromosome);
		} catch (InvalidConfigurationException e1) {
			e1.printStackTrace();
		}

		PlanomatFitnessFunctionWrapper fitnessFunction = new PlanomatFitnessFunctionWrapper(this, plan);		

		try {
			jgapConfiguration.setFitnessFunction( fitnessFunction );
			population = Genotype.randomInitialGenotype( jgapConfiguration );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return population;
	}

	private IChromosome evolveAndReturnFittest(final Genotype population) {

//		IChromosome fittest = null;
//		String logMessage = null;
		for (int i = 0, n = this.config.getJgapMaxGenerations(); i < n; i++) {
			population.evolve();
//			if (Gbl.getConfig().planomat().isDoLogging()) {
//			fittest = population.getFittestChromosome();
//			logMessage = "Generation #" + Integer.toString(i) + " : Max: " + fittest.getFitnessValue();
//			logger.info(logMessage);
//			}
		}
		return population.getFittestChromosome();

	}

	protected IChromosome initSampleChromosome(final Plan plan, final PlanAnalyzeSubtours planAnalyzeSubtours, final org.jgap.Configuration jgapConfiguration) {

		int numActs = plan.getPlanElements().size() / 2;
		int numSubtours = 0;
		if (this.possibleModes.length > 0) {
			numSubtours = planAnalyzeSubtours.getNumSubtours();
		}
		Gene[] sampleGenes = new Gene[1 + numActs + numSubtours];

		try {
			// first integer gene for the start time of the plan
			sampleGenes[0] = new IntegerGene(jgapConfiguration, 0, this.numTimeIntervals - 1);
			// one integer gene for each activity duration
			for (int ii=0; ii < numActs; ii++) {
				sampleGenes[1 + ii] = new IntegerGene(jgapConfiguration, 0, this.numTimeIntervals - 1);
			}
			// one integer gene for the mode of each subtour
			for (int ii=0; ii < numSubtours; ii++) {
				sampleGenes[1 + numActs + ii] = new IntegerGene(jgapConfiguration, 0, this.possibleModes.length - 1);
			}
		} catch (InvalidConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		

		IChromosome sampleChromosome = null;
		try {
			sampleChromosome = new Chromosome( jgapConfiguration, sampleGenes );
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		return sampleChromosome;

	}

	protected double stepThroughPlan(final StepThroughPlanAction action, final IChromosome individual, final Plan plan) {

		// TODO comment this
		double positionInTimeInterval = 0.5;

		ScoringFunction scoringFunction = null;
		scoringFunction = this.scoringFunctionFactory.getNewScoringFunction(plan);

		Route tempRoute = null;
		Leg leg = null;
		Activity origin = null;
		Activity destination = null;

		List<? extends BasicPlanElement> actslegs = plan.getPlanElements();
		int numLegs = actslegs.size() / 2;

		// TODO this as a quick and dirty implementation that takes a lot of resources
		// replace activity duration encoding with double [0.0,1.0] or time slots, respectively
		int sumOfAllActDurs = 0;
		for (int geneIndex = 1; geneIndex <= numLegs; geneIndex++) {
			sumOfAllActDurs += ((IntegerGene) individual.getGenes()[geneIndex]).intValue();
		}

		double now = 0.0;
		double oldNow = 0.0;

		// solution of first gene, normalized to scenario duration, is end time of first activity 
		origin = plan.getFirstActivity();
		if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
			origin.setStartTime(now);
		}

		if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
			positionInTimeInterval = this.seedGenerator.nextDouble();
		}
//		double activityDuration = Math.rint(this.getEffectiveActLegTimeFrame(
//				((IntegerGene) individual.getGene(0)).intValue(), 
//				this.numTimeIntervals, 
//				positionInTimeInterval));
//		now += activityDuration;

		now += Math.rint(this.getEffectiveActLegTimeFrame(
				((IntegerGene) individual.getGene(0)).intValue(), 
				this.numTimeIntervals, 
				positionInTimeInterval));
		
		for (int geneIndex = 1; geneIndex <= numLegs; geneIndex++) {

			scoringFunction.endActivity(now);
			now = Math.max(oldNow + 1.0, now);
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				origin.setDuration(now - oldNow);
				origin.setEndTime(now);
			}
			///////////////////////////////////////////////////////////////////////////////////////////
			// move agent forward in time according to anticipated travel time...
			///////////////////////////////////////////////////////////////////////////////////////////
			leg = ((Leg) actslegs.get(geneIndex * 2 - 1));
			destination = plan.getNextActivity(leg);

			scoringFunction.startLeg(now, null);
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				leg.setDepartureTime(now);
			}

			if (possibleModes.length > 0) {
				// set mode
				int subtourIndex = this.planAnalyzeSubtours.getSubtourIndexation()[geneIndex - 1];
				int modeIndex = ((IntegerGene) individual.getGene(1 + numLegs + subtourIndex)).intValue();
				leg.setMode(possibleModes[modeIndex]);
			} // otherwise leave modes untouched

			// save original route
			if (!leg.getMode().equals(TransportMode.car)) {
				tempRoute = leg.getRoute();
			}

			double anticipatedTravelTime = Math.rint(this.legTravelTimeEstimator.getLegTravelTimeEstimation(
					plan.getPerson().getId(),
					now,
					origin,
					destination,
					leg));

			now += anticipatedTravelTime;

			if (!leg.getMode().equals(TransportMode.car)) {
				// recover original route
				leg.setRoute(tempRoute);
			}
			leg.getRoute().setTravelTime(anticipatedTravelTime);

			scoringFunction.endLeg(now);
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				leg.setTravelTime(anticipatedTravelTime);
				leg.setArrivalTime(now);
			}
			///////////////////////////////////////////////////////////////////////////////////////////
			// move agent forward in time according to anticipated travel time...done.
			///////////////////////////////////////////////////////////////////////////////////////////

			///////////////////////////////////////////////////////////////////////////////////////////
			// activity duration is solution of first gene, normalized to scenario duration, 
			// - minus anticipated travel time,
			// - rounded to full seconds
			// - minimum 1 second (no negative activity durations will be produced)
			///////////////////////////////////////////////////////////////////////////////////////////
			scoringFunction.startActivity(now, null);
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				destination.setStartTime(now);
			}

			if (destination != plan.getLastActivity()) {
				if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
					positionInTimeInterval = this.seedGenerator.nextDouble();
				}
				double actLegTimeFrame = this.getEffectiveActLegTimeFrame(
						((IntegerGene) individual.getGene(geneIndex)).intValue(), 
						sumOfAllActDurs, 
						positionInTimeInterval);

//				activityDuration = Math.rint(actLegTimeFrame - anticipatedTravelTime);
//				oldNow = now;
//				now += activityDuration;
				
				oldNow = now;
				now += Math.rint(actLegTimeFrame - anticipatedTravelTime);
				
				origin = destination;
			}

		}

		scoringFunction.finish();
//		logger.info("score: " + scoringFunction.getScore());
		return scoringFunction.getScore();

	}

	private double normalizeBy;
	private double effectiveActDur;

	public double getEffectiveActLegTimeFrame(
			final int actDurInTimeSlots, 
			final int overallTimeSlots, 
			final double offsetWithinTimeSlot) {

//		logger.info("overallTimeSlots = " + Integer.toString(overallTimeSlots));

		this.normalizeBy = (((double) this.numTimeIntervals) / ((double) overallTimeSlots));

//		logger.info("normalizeBy = " + Double.toString(normalizeBy));

		this.effectiveActDur = actDurInTimeSlots * this.normalizeBy;

//		logger.info("actDurInTimeSlots = " + Integer.toString(actDurInTimeSlots));
//		logger.info("effectiveActDur = " + Double.toString(effectiveActDur));
//		logger.info("offsetWithinTimeSlot = " + Double.toString(offsetWithinTimeSlot));

//		logger.info("effectiveActLegTimeFrame = " + Double.toString(effectiveActLegTimeFrame));
//		logger.info("");

		return (((int) this.effectiveActDur) + offsetWithinTimeSlot) * this.timeIntervalSize;

	}

	public Random getSeedGenerator() {
		return this.seedGenerator;
	}

}