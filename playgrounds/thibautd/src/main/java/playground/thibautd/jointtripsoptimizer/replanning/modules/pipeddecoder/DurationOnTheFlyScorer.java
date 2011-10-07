/* *********************************************************************** *
 * project: org.matsim.*
 * DurationOnTheFlyScorer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.pipeddecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jgap.IChromosome;
import org.jgap.impl.DoubleGene;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

import playground.thibautd.jointtripsoptimizer.population.JointActing;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Uses the same algorithm as {@link DurationDecoder}, but scores directly the
 * plan rather than creating it.
 * This allows to save time, by both avoiding numerous intanciations an diminuishing
 * the number of loops necessary to carry out scoring.
 *
 * @author thibautd
 */
public class DurationOnTheFlyScorer implements FinalScorer {
	private static final Logger log =
		Logger.getLogger(DurationOnTheFlyScorer.class);

	/**
	 * lists the indices of the genes in the chromosome relative to
	 * each individual.
	 */
	private final Map<Id, List<Integer>> genesIndices =
		new HashMap<Id, List<Integer>>();

	//TODO: import from config group
	private static final double MIN_DURATION = 0d;
	private static final double PU_DURATION = 0d;
	private static final double DO_DURATION = 0d;
	private static final double DAY_DURATION = 24*3600d;

	private JointPlan plan;
	// stocks the plan elements of the UNTOGGLED plan.
	// to use with the legTTEst ONLY
	private final Map<Id, List<PlanElement>> individualPlanElements =
		new HashMap<Id, List<PlanElement>>();
	private final Map<Id, LegTravelTimeEstimator> legTTEstimators =
		new HashMap<Id, LegTravelTimeEstimator>();

	/**
	 * links shared legs with their soonest beginning time (understood as the
	 * arrival time of the agent to the pick-up).
	 */
	private final Map<JointLeg, Double> readyJointLegs =
		new HashMap<JointLeg, Double>();

	/**
	 * relates passenger shared rides to the related driver trip, if already
	 * scheduled.
	 */
	private final Map<JointLeg, JointLeg> driverLegs =
		new HashMap<JointLeg, JointLeg>();

	private final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private	final PlansCalcRoute routingAlgorithm;
	private	final Network network;
	private final int nMembers;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final JointReplanningConfigGroup configGroup;

	/**
	 * initializes a decoder, which can be used on any modification of the
	 * {@link JointPlan} passed as a parameter.
	 * This constructor initializes the relation between activities and genes.
	 */
	public DurationOnTheFlyScorer(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
			final ScoringFunctionFactory scoringFunctionFactory,
			final JointPlanOptimizerLegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			final PlansCalcRoute routingAlgorithm,
			final Network network,
			final int numJointEpisodes,
			final int numEpisodes,
			final int nMembers) {
		//initialisation of final fields
		this.legTravelTimeEstimatorFactory = legTravelTimeEstimatorFactory;
		this.routingAlgorithm = routingAlgorithm;
		this.network = network;
		this.nMembers = nMembers;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.configGroup = configGroup;

		//construction
		//Map<PlanElement,Integer> alreadyDetermined = new HashMap<PlanElement,Integer>();
		List<Activity> lastActivities = plan.getLastActivities();
		//TODO: less hard-coded chromosome structure
		int currentDurationGene = numJointEpisodes;
		Integer indexToAdd;

		// initialize the geneIndices structure
		for (Id id : plan.getClique().getMembers().keySet()) {
			this.genesIndices.put(id, new ArrayList<Integer>());
		}

		// construction of the list of relative genes.
		for (PlanElement pe : plan.getPlanElements()) {
			//do not consider legs
			if (pe instanceof JointLeg) {
				continue;
			}

			// associate to duration genes (only for non-joint-trip related activities)
			if ( !isPickUp(pe) && !isDropOff(pe) ) {
				if (!lastActivities.contains(pe)) {
					indexToAdd = currentDurationGene;
					currentDurationGene++;
				}
				else {
					indexToAdd = null;
				}
				// remember the association
				this.genesIndices.get( ((JointActing) pe).getPerson().getId() ).add(
						indexToAdd);
			}
		}

		this.initializeLegEstimators(plan, configGroup.getSimLegInterpretation());
	}

	private void initializeLegEstimators(
			final JointPlan plan,
			final SimLegInterpretation simLegInt) {
		LegTravelTimeEstimator currentLegTTEstimator;

		this.legTTEstimators.clear();
		this.individualPlanElements.clear();

		// TODO: use individual plans map instead of clique
		for (Id id : plan.getClique().getMembers().keySet()) {
			currentLegTTEstimator = legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
					plan.getIndividualPlan(id),
					simLegInt,
					PlanomatConfigGroup.RoutingCapability.fixedRoute,
					routingAlgorithm,
					network);
			this.legTTEstimators.put(id, currentLegTTEstimator);

			this.individualPlanElements.put(id,
					plan.getIndividualPlan(id).getPlanElements());
		}
	}

	/**
	 * @param chromosome the chromosome coding the plan to score
	 * @param inputPlan the partially decoded plan (ie on all dimensions
	 * except durations)
	 *
	 * @return the score of the plan
	 */
	public double score(
			final IChromosome chromosome,
			final JointPlan inputPlan) {
		//this.initializeLegEstimators(inputPlan, configGroup.getSimLegInterpretation());

		Map<Id, IndividualValuesWrapper> individualValuesMap = 
			new HashMap<Id, IndividualValuesWrapper>(nMembers);
		List<Id> individualsToPlan = new ArrayList<Id>(nMembers);
		List<Id> toRemove = new ArrayList<Id>(nMembers);
		Id currentId = null;
		Plan individualPlan = null;

		this.plan = inputPlan;

		resetInternalState();

		for (Map.Entry<Id, Plan> inputIndivPlan :
				inputPlan.getIndividualPlans().entrySet()) {
			individualPlan = inputIndivPlan.getValue();
			currentId = inputIndivPlan.getKey();
			individualsToPlan.add(currentId);
			individualValuesMap.put(
					currentId,
					new IndividualValuesWrapper(
						this.scoringFunctionFactory.createNewScoringFunction(individualPlan),
						individualPlan));
		}

		do {
			for (Id id : individualsToPlan) {
				planNextActivity(
						chromosome,
						individualValuesMap,
						id);
				if (individualValuesMap.get(id).getIndexInPlan()
						>= individualValuesMap.get(id).planElements.size()) {
					toRemove.add(id);
				}
			}
			individualsToPlan.removeAll(toRemove);
			toRemove.clear();
		} while (!individualsToPlan.isEmpty());

		double score = getScore(individualValuesMap);
		//log.debug("score: "+score);
		return score;
	}

	private void resetInternalState() {
		this.readyJointLegs.clear();
		this.driverLegs.clear();
		//this.legTTEstimators.clear();
	}

	private double getScore(final Map<Id, IndividualValuesWrapper> values) {
		ScoringFunction currentScoring;

		//for (Map.Entry<Id, Plan> indivPlan :
		//	this.plan.getIndividualPlans().entrySet()) {
		//	currentScoring = values.get(indivPlan.getKey()).scoringFunction;
		//	currentScoring.finish();
		//	indivPlan.getValue().setScore(currentScoring.getScore());
		//}
		for (IndividualValuesWrapper value : values.values()) {
			currentScoring = value.scoringFunction;
			currentScoring.finish();
			value.individualPlan.setScore(currentScoring.getScore());
		}

		return this.plan.getScore();
	}

	private final void planNextActivity(
			final IChromosome chromosome,
			final Map<Id, IndividualValuesWrapper> individualValuesMap,
			final Id id) {
		IndividualValuesWrapper individualValues = individualValuesMap.get(id);
		// CAUTION: THOSE ARE THE ELEMENTS OF THE INITIAL PLAN, POSSIBLY MODIFIED
		List<PlanElement> planElements = this.individualPlanElements.get(id);
		PlanElement currentElement =
			individualValues.planElements.get(individualValues.getIndexInPlan());
		LegTravelTimeEstimator currentLegTTEstimator =
			this.legTTEstimators.get(id);
		double currentDuration;
		Integer geneIndex;

		JointActivity origin;
		JointActivity destination;

		geneIndex = this.genesIndices.get(id).get(individualValues.getIndexInChromosome());

		if (individualValues.getIndexInPlan()==0) {
			origin = (JointActivity) currentElement;
			individualValues.scoringFunction.startActivity(individualValues.getNow(), origin);
			currentDuration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue();
			individualValues.addToNow(currentDuration);
			individualValues.scoringFunction.endActivity(individualValues.getNow(), origin);

			individualValues.addToIndexInPlan(1);
			individualValues.addToIndexInChromosome(1);
		}
		else if (currentElement instanceof JointLeg) {
			// Assumes that a plan begins by an activity, with a strict Act-Leg alternance.
			origin = (JointActivity) individualValues.planElements.get(
					individualValues.getIndexInPlan() - 1);
			destination = (JointActivity) individualValues.planElements.get(
					individualValues.getIndexInPlan() + 1);

			if (isPickUp(destination)) {
				if (!this.readyJointLegs.containsKey(
							individualValues.planElements.get(
								individualValues.getIndexInPlan() + 2))) {
					// case of an affected PU activity without access:
					// plan the access leg
					scorePuAccessLeg(
							id,
							planElements,
							currentElement,
							currentLegTTEstimator,
							individualValues,
							chromosome);
				} else {
					// for debugging only: to remove afterwards
					throw new RuntimeException("passing twice the same access leg");
				}
			} 
			else if (isDropOff(destination)) {
				throw new RuntimeException("index positionned on a shared leg");
			}
			else {
				// case of a leg which destination isn't a PU act
				scoreIndividualLegAct(
						id,
						planElements,
						currentElement,
						currentLegTTEstimator,
						individualValues,
						geneIndex,
						chromosome);
			}
		}
		else if (isPickUp(currentElement) || isDropOff(currentElement)) {
			if (isReadyForPlanning(
						individualValues.planElements,
						individualValues.getIndexInPlan())) {
				scoreSharedLegAct(
						planElements,
						id,
						currentElement,
						currentLegTTEstimator,
						individualValues,
						geneIndex,
						chromosome);
			}
		}
		else {
			log.error("unexpected index: trying to plan an element which is not"+
					" a JointLeg nor a Pick-Up activity");
		}

	}

	private void scorePuAccessLeg(
			final Id id,
			final List<PlanElement> planElements,
			final PlanElement currentElement,
			final LegTravelTimeEstimator legTTEstimator,
			final IndividualValuesWrapper individualValues,
			final IChromosome chromosome
			) {
		JointLeg leg = ((JointLeg) currentElement);
		JointActivity origin = (JointActivity) individualValues.planElements.get(
				individualValues.getIndexInPlan() - 1);
		JointActivity destination = (JointActivity)
			individualValues.planElements.get(individualValues.getIndexInPlan() + 1);
		double currentTravelTime;

		currentTravelTime = scoreLeg(
				legTTEstimator,
				id,
				origin,
				destination,
				planElements.indexOf(leg),
				individualValues,
				leg);

		individualValues.addToNow(currentTravelTime);

		// mark the PU as accessed
		this.readyJointLegs.put(
				(JointLeg) individualValues.planElements.get(
					individualValues.getIndexInPlan() + 2),
				individualValues.getNow());

		individualValues.addToIndexInPlan(1);
		individualValues.addToJointTravelTime(currentTravelTime);
	}

	private void scoreIndividualLegAct(
			final Id id,
			final List<PlanElement> planElements,
			final PlanElement currentElement,
			final LegTravelTimeEstimator legTTEstimator,
			final IndividualValuesWrapper individualValues,
			final Integer geneIndex,
			final IChromosome chromosome
			) {
		JointLeg leg = ((JointLeg) currentElement);
		JointActivity origin = (JointActivity) individualValues.planElements.get(
				individualValues.getIndexInPlan() - 1);
		JointActivity destination = (JointActivity)
				individualValues.planElements.get(individualValues.getIndexInPlan() + 1);
		double currentTravelTime;
		double currentDuration;

		currentTravelTime = scoreLeg(
				legTTEstimator,
				id,
				origin,
				destination,
				planElements.indexOf(leg),
				individualValues,
				leg);

		individualValues.addToNow(currentTravelTime);

		// set index to the next leg
		individualValues.addToIndexInPlan(2);
		individualValues.addToJointTravelTime(currentTravelTime);

		currentDuration = getDuration(
				chromosome,
				geneIndex,
				individualValues.getNow(),
				individualValues.getJointTravelTime());
		individualValues.scoringFunction.startActivity(
				individualValues.getNow(),
				destination);
		individualValues.addToNow(currentDuration);
		individualValues.scoringFunction.endActivity(individualValues.getNow(), destination);
		individualValues.addToIndexInChromosome(1);
		individualValues.resetTravelTime();
	}

	private void scoreSharedLegAct(
			final List<PlanElement> planElements,
			final Id id,
			final PlanElement currentElement,
			final LegTravelTimeEstimator legTTEstimator,
			final IndividualValuesWrapper individualValues,
			final Integer geneIndex,
			final IChromosome chromosome) {
		JointActivity origin = ((JointActivity) currentElement);
		JointLeg leg = (JointLeg) individualValues.planElements.get(
				individualValues.getIndexInPlan() + 1);
		JointActivity destination = (JointActivity) individualValues.planElements.get(
				individualValues.getIndexInPlan() + 2);
		double currentTravelTime = 0d;
		double legTravelTime;
		double currentDuration;

		// /////////////////////////////////////////////////////////////////
		// plan origin activity
		if (isPickUp(origin)) {
			currentDuration = getTimeToJointTrip(
					leg.getLinkedElements().values(),
					individualValues.getNow()) + PU_DURATION;
			individualValues.scoringFunction.startActivity(
					individualValues.getNow(),
					origin);
			individualValues.addToNow(currentDuration);
			individualValues.scoringFunction.endActivity(
					individualValues.getNow(), origin);
		}
		else if (isDropOff(origin)) {
			individualValues.scoringFunction.startActivity(
					individualValues.getNow(),
					origin);
			individualValues.addToNow(DO_DURATION);
			currentDuration = DO_DURATION;
			individualValues.scoringFunction.endActivity(
					individualValues.getNow(), origin);
		}
		else {
			throw new RuntimeException("planning of a shared ride launched "+
					"on a non-PU nor DO activity");
		}

		currentTravelTime += currentDuration;

		// /////////////////////////////////////////////////////////////////
		// plan shared ride
		leg = (JointLeg) individualValues.planElements.get(individualValues.getIndexInPlan() + 1);

		if (leg.getIsDriver()) {
			// use legttestimator
			legTravelTime = scoreLeg(
					legTTEstimator,
					id,
					origin,
					destination,
					planElements.indexOf(leg),
					individualValues,
					leg);
			
			updateDriverLegs(leg, leg);
		}
		else {
			// get driver trip
			leg = this.driverLegs.get(leg);
			legTravelTime = scoreLeg(
					legTTEstimator,
					id,
					origin,
					destination,
					planElements.indexOf(leg),
					individualValues,
					leg);
		}

		individualValues.addToNow(legTravelTime);
		currentTravelTime += legTravelTime;

		// /////////////////////////////////////////////////////////////////
		// plan destination activity
		if (isDropOff(destination)) {
			if (!((JointLeg) individualValues.planElements.get(
							individualValues.getIndexInPlan() + 3))
				.getJoint()) {
				//"terminal" DO
				individualValues.scoringFunction.startActivity(
						individualValues.getNow(), destination);
				individualValues.addToNow(DO_DURATION);
				currentTravelTime += DO_DURATION;
				individualValues.scoringFunction.endActivity(
						individualValues.getNow(), destination);

				//restart planning at the egress trip
				individualValues.addToIndexInPlan(3);
			}
			else {
				// DO "intra" -ride: restart planning from it.
				individualValues.addToIndexInPlan(2);
			}
		}
		else if (isPickUp(destination)) {
			// mark the PU as accessed
			this.readyJointLegs.put(
					(JointLeg) individualValues.planElements.get(
						individualValues.getIndexInPlan() + 3),
					individualValues.getNow());

			//restart planning at the PU
			individualValues.addToIndexInPlan(2);
		}
		else {
			throw new RuntimeException("only PU and DO activity should occur "+
					"when planning a shared ride");
		}
		individualValues.addToJointTravelTime(currentTravelTime);
	}

	private void updateDriverLegs(
			final JointLeg driverLeg,
			final JointLeg legInPlan) {
		for (JointLeg leg : legInPlan.getLinkedElements().values()) {
			this.driverLegs.put(leg, driverLeg);
		}
	}

	/**
	 * Determines if a pick-up or a drop-off followed by a shared ride is ready
	 * for planning, that is:
	 * - all related access trips are planned;
	 * - the current individual is the driver, or the driver trip is planned
	 *
	 * returns true for all other activities
	 */
	private final boolean isReadyForPlanning(
			final List<PlanElement> planElementsToPlan,
			final int index) {
		JointLeg sharedRide = (JointLeg) planElementsToPlan.get(index + 1);
		// test PUs and DOs followed by a shared ride
		boolean test = (isPickUp(planElementsToPlan.get(index)) ? true :
				(isDropOff(planElementsToPlan.get(index)) && sharedRide.getJoint()));

		if (test) {
			boolean allRelativesArePlanned = 
				this.readyJointLegs.keySet().containsAll(
						sharedRide.getLinkedElements().values());
			boolean isDriver = sharedRide.getIsDriver();
			boolean driverIsPlanned = this.driverLegs.containsKey(sharedRide);
			
			return (allRelativesArePlanned && (isDriver || driverIsPlanned));
		}

		throw new RuntimeException("ready for planning should only be checked for"+
				" shared legs");
		//return true;
	}

	/**
	 * @return the travel time.
	 */
	private double scoreLeg(
			final LegTravelTimeEstimator legTTEstimator,
			final Id id,
			final Activity origin,
			final Activity destination,
			final int indexInPlan,
			final IndividualValuesWrapper individualValues,
			final JointLeg leg) {
		double travelTime = legTTEstimator.getLegTravelTimeEstimation(
				id,
				individualValues.getNow(),
				origin,
				destination,
				leg,
				//false //ie do not modify leg
				true //ie do modify leg
				);

		individualValues.scoringFunction.startLeg(individualValues.getNow(), leg);
		individualValues.scoringFunction.endLeg(individualValues.getNow() + travelTime);

		return travelTime;
	}

	private final double getTimeToJointTrip(
			final Collection<? extends JointLeg> linkedElements,
			final double now) {
		// the min start time should not be before now
		double max = now;
		double currentTime;

		for (JointLeg leg : linkedElements) {
			currentTime = this.readyJointLegs.get(leg);
			if (max < currentTime) {
				max = currentTime;
			}
		}
		return (max - now);
	}

	/**
	 * @param now time of day after the travel
	 */
	private final double getDuration(
			final IChromosome chromosome,
			final Integer geneIndex,
			final double now,
			final double travelTime) {
		double duration = 0d;

		if (geneIndex != null) {
			duration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue() - travelTime;
		} else {
			// case of the last activity of an individual plan
			duration = DAY_DURATION - now;
		}

		return (duration > 0 ? duration : MIN_DURATION);
	}

	private boolean isPickUp(final PlanElement pe) {
		return ((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP);
	}

	private boolean isDropOff(final PlanElement pe) {
		return ((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF);
	}

	/**
	 * internal class, wrapping the int indices used to step through the plans.
	 */
	private class IndividualValuesWrapper {
		private int indexInPlan = 0;
		private int indexInChromosome = 0;
		private double now = 0d;
		public final ScoringFunction scoringFunction;
		public final List<PlanElement> planElements;
		public final Plan individualPlan;

		/**
		 * For tracking travel time in complicated joint trips
		 */
		private double jointTravelTime = 0d;

		public IndividualValuesWrapper(
				final ScoringFunction scoringFunction,
				final Plan indivPlan) {
			this.scoringFunction = scoringFunction;
			this.individualPlan = indivPlan;
			this.planElements = indivPlan.getPlanElements();
		}

		public void addToIndexInPlan(final int i) {
			indexInPlan += i;
		}

		public void addToIndexInChromosome(final int i) {
			indexInChromosome += i;
		}

		public void addToNow(final double d) {
			now += d;
		}

		public int getIndexInPlan() {
			return indexInPlan;
		}

		public int getIndexInChromosome() {
			return indexInChromosome;
		}

		public double getNow() {
			return now;
		}

		public double getJointTravelTime() {
			return this.jointTravelTime;
		}

		public void addToJointTravelTime(final double d) {
			this.jointTravelTime += d;
		}

		public void resetTravelTime() {
			this.jointTravelTime = 0d;
		}
	}

}

