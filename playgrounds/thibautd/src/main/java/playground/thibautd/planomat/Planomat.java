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

package playground.thibautd.planomat;

import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.IntegerGene;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
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

	private final PlanomatConfigGroup planomatConfigGroup;
	private final int numTimeIntervals;
	protected final double timeIntervalSize;

	private final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final PlansCalcRoute router;
	private final Network network;

	private final Random seedGenerator;

	private final static Logger logger = Logger.getLogger(Planomat.class);
	private final boolean doLogging;

	public Planomat(
			final LegTravelTimeEstimatorFactory legTravelTimeEstimatorfactory,
			final ScoringFunctionFactory scoringFunctionFactory,
			final PlanomatConfigGroup config,
			final PlansCalcRoute router,
			final Network network) {

		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorfactory;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.planomatConfigGroup = config;
		this.router = router;
		this.network = network;

		this.numTimeIntervals = (int) Math.pow(2, this.planomatConfigGroup.getLevelOfTimeResolution());
		this.timeIntervalSize = Planomat.SCENARIO_DURATION / numTimeIntervals;
		this.doLogging = this.planomatConfigGroup.isDoLogging();

		this.seedGenerator = MatsimRandom.getLocalInstance();
	}

	@Override
	public void run(final Plan plan) {

		if (this.doLogging) {
			logger.info("Running planomat on plan of person # " + plan.getPerson().getId().toString() + "...");
		}

		LegTravelTimeEstimator legTravelTimeEstimator = this.legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				plan,
				this.planomatConfigGroup.getSimLegInterpretation(),
				this.planomatConfigGroup.getRoutingCapability(),
				this.router,
				this.network);

		// perform subtour analysis only if mode choice on subtour basis is optimized
		// (if only times are optimized, subtour analysis is not necessary)
		TreeSet<String> possibleModes = this.getPossibleModes(plan);

		PlanAnalyzeSubtours planAnalyzeSubtours = null;
		if (possibleModes.size() > 0) {
			if (this.doLogging) {
				logger.info("Running subtour analysis...");
			}
			planAnalyzeSubtours = new PlanAnalyzeSubtours();
			planAnalyzeSubtours.setTripStructureAnalysisLayer(this.planomatConfigGroup.getTripStructureAnalysisLayer());
			planAnalyzeSubtours.run(plan);
		}
		if (this.doLogging) {
			logger.info("Running subtour analysis...done.");
			logger.info("Initialization of JGAP configuration...");
		}
		// JGAP random number generator is initialized for each run
		// but use a random number as seed so every run will draw a different, but deterministic sequence of random numbers
		long seed = this.seedGenerator.nextLong();
		if (this.doLogging) {
			logger.info("agent id: " + plan.getPerson().getId() + "; JGAP seed: " + Long.toString(seed));
		}
		PlanomatJGAPConfiguration jgapConfiguration = new PlanomatJGAPConfiguration(
				plan,
				planAnalyzeSubtours,
				seed,
				this.numTimeIntervals,
				possibleModes,
				this.planomatConfigGroup);

		PlanomatFitnessFunctionWrapper fitnessFunction = new PlanomatFitnessFunctionWrapper(this, plan, planAnalyzeSubtours, possibleModes, legTravelTimeEstimator);

		Genotype population = null;
		try {
			jgapConfiguration.setFitnessFunction( fitnessFunction );
			population = Genotype.randomInitialGenotype( jgapConfiguration );
		} catch (InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}
		if (this.doLogging) {
			logger.info("Initialization of JGAP configuration...done.");
			logger.info("Running evolution...");
		}
		IChromosome fittest = this.evolveAndReturnFittest(population);
		if (this.doLogging) {
			logger.info("Running evolution...done.");
			logger.info("Writing solution back to Plan object...");
		}
		/*double score =*/ this.stepThroughPlan(StepThroughPlanAction.WRITE_BACK, fittest, plan, planAnalyzeSubtours, legTravelTimeEstimator, possibleModes);
		if (this.doLogging) {
			logger.info("Writing solution back to Plan object...done.");
			logger.info("Running planomat on plan of person # " + plan.getPerson().getId().toString() + "...done.");
		}
		// invalidate score information
		plan.setScore(null);
	}

	protected TreeSet<String> getPossibleModes(final Plan plan) {

		// remove car option for agents that have no car available
		TreeSet<String> possibleModes = new TreeSet<String>(this.planomatConfigGroup.getPossibleModes());

		String carAvail = ((PersonImpl) plan.getPerson()).getCarAvail();
		if ("never".equals(carAvail)) {
			possibleModes.remove(TransportMode.car);
		}
		return possibleModes;
	}

	private IChromosome evolveAndReturnFittest(final Genotype population) {

		for (int i = 0, n = this.planomatConfigGroup.getJgapMaxGenerations(); i < n; i++) {
			population.evolve();
		}
		return population.getFittestChromosome();
	}

	protected double stepThroughPlan(
			final StepThroughPlanAction action,
			final IChromosome individual,
			final Plan plan,
			final PlanAnalyzeSubtours planAnalyzeSubtours,
			final LegTravelTimeEstimator legTravelTimeEstimator,
			final TreeSet<String> possibleModes) {

		// TODO comment this
		double positionInTimeInterval = 0.5;

		LegImpl leg = null;
		Activity origin = null;
		Activity destination = null;

		List<? extends PlanElement> actslegs = plan.getPlanElements();
		int numLegs = actslegs.size() / 2;

		ScoringFunction scoringFunction = null;
		if (action.equals(StepThroughPlanAction.EVALUATE)) {
			scoringFunction = this.scoringFunctionFactory.createNewScoringFunction(plan);
		}
		// TODO this as a quick and dirty implementation that takes a lot of resources
		// replace activity duration encoding with double [0.0,1.0] or time slots, respectively
		int sumOfAllActDurs = 0;
		for (int geneIndex = 1; geneIndex <= numLegs; geneIndex++) {
			sumOfAllActDurs += ((IntegerGene) individual.getGenes()[geneIndex]).intValue();
		}

		double now = 0.0;
		double oldNow = 0.0;

		// solution of first gene, normalized to scenario duration, is end time of first activity
		origin = ((PlanImpl) plan).getFirstActivity();
		if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
			origin.setStartTime(now);
		}

		if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
			positionInTimeInterval = this.seedGenerator.nextDouble();
		}

		now += Math.rint(this.getEffectiveActLegTimeFrame(
				((IntegerGene) individual.getGene(0)).intValue(),
				this.numTimeIntervals,
				positionInTimeInterval));

		for (int geneIndex = 1; geneIndex <= numLegs; geneIndex++) {

			if (action.equals(StepThroughPlanAction.EVALUATE)) {
				scoringFunction.endActivity(now, origin);
			}
			now = Math.max(oldNow + 1.0, now);
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				((ActivityImpl) origin).setMaximumDuration(now - oldNow);
				origin.setEndTime(now);
			}
			///////////////////////////////////////////////////////////////////////////////////////////
			// move agent forward in time according to anticipated travel time...
			///////////////////////////////////////////////////////////////////////////////////////////
			leg = ((LegImpl) actslegs.get(geneIndex * 2 - 1));
			destination = ((PlanImpl) plan).getNextActivity(leg);

			if (action.equals(StepThroughPlanAction.EVALUATE)) {
				scoringFunction.startLeg(now, leg);
			}
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				leg.setDepartureTime(now);
			}

			String desiredMode = leg.getMode();
			if (planAnalyzeSubtours != null) {
				// set mode
				int subtourIndex = planAnalyzeSubtours.getSubtourIndexation()[geneIndex - 1];
				int modeIndex = ((IntegerGene) individual.getGene(1 + numLegs + subtourIndex)).intValue();
				desiredMode = (String) possibleModes.toArray()[modeIndex];
			} // otherwise leave modes untouched

			LegImpl newLeg = legTravelTimeEstimator.getNewLeg(
					desiredMode,
					origin,
					destination,
					((PlanImpl) plan).getActLegIndex(leg),
					now);
			// TODO here the LegImpl copy constructor should be used.
			leg.setMode(newLeg.getMode());
			leg.setRoute(newLeg.getRoute());
			double anticipatedTravelTime = Math.rint(newLeg.getTravelTime());
			now += anticipatedTravelTime;

			if (action.equals(StepThroughPlanAction.EVALUATE)) {
				scoringFunction.endLeg(now);
			}
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				leg.setTravelTime(anticipatedTravelTime);
				leg.getRoute().setTravelTime(anticipatedTravelTime);
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
			if (action.equals(StepThroughPlanAction.EVALUATE)) {
				scoringFunction.startActivity(now, destination);
			}
			if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
				destination.setStartTime(now);
			}

			if (destination != ((PlanImpl) plan).getLastActivity()) {
				if (action.equals(StepThroughPlanAction.WRITE_BACK)) {
					positionInTimeInterval = this.seedGenerator.nextDouble();
				}
				double actLegTimeFrame = this.getEffectiveActLegTimeFrame(
						((IntegerGene) individual.getGene(geneIndex)).intValue(),
						sumOfAllActDurs,
						positionInTimeInterval);

				oldNow = now;
				now += Math.rint(actLegTimeFrame - anticipatedTravelTime);

				origin = destination;
			}

		}

		if (action.equals(StepThroughPlanAction.EVALUATE)) {
			scoringFunction.finish();
			return scoringFunction.getScore();
		}

		return 0.0;
	}

	public double getEffectiveActLegTimeFrame(
			final int actDurInTimeSlots,
			final int overallTimeSlots,
			final double offsetWithinTimeSlot) {

		double normalizeBy = (((double) this.numTimeIntervals) / ((double) overallTimeSlots));

		double effectiveActDur = actDurInTimeSlots * normalizeBy;

		return (((int) effectiveActDur) + offsetWithinTimeSlot) * this.timeIntervalSize;

	}

	public Random getSeedGenerator() {
		return this.seedGenerator;
	}

}