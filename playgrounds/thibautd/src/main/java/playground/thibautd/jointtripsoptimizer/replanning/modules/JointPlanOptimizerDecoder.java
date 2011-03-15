/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerDecoder.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.impl.DoubleGene;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.jointtripsoptimizer.population.JointActing;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Transforms a genotype into a JointPlan.
 * @author thibautd
 */
public class JointPlanOptimizerDecoder {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerDecoder.class);

	// Map that associates the following structure to each person in the clique:
	// -a list of maps (one per activity in the individal'l plan)
	// -each map relates types of genes (planned, duration) and gene indices.
	private final Map<Id, List<Map<String, Integer>>> genesIndices =
		new HashMap<Id, List<Map<String, Integer>>>();

	// field names
	private static final String TOGGLE_CHROM = "toggle";
	private static final String DURATION_CHROM = "duration";

	//TODO: import from config group
	private static final double MIN_DURATION = 0d;
	private static final double PU_DURATION = 0d;
	private static final double DO_DURATION = 0d;

	private final JointPlan plan;
	private final Map<Id, List<PlanElement>> individualPlanElements =
		new HashMap<Id, List<PlanElement>>();
	private final Map<Id, LegTravelTimeEstimator> legTTEstimators =
		new HashMap<Id, LegTravelTimeEstimator>();
	private final LegTravelTimeEstimator nonSharedLegsTTEstimator;
	private final PlanImpl facticePlan;

	/**
	 * links PU activities with their soonest beginning time (understood as the
	 * arrival time of the agent).
	 */
	private final Map<JointActivity, Double> readyPickUps =
		new HashMap<JointActivity, Double>();
	/**
	 * relates passenger shared rides to the related driver trip, if already
	 * scheduled.
	 */
	private final Map<JointLeg, JointLeg> driverLegs =
		new HashMap<JointLeg, JointLeg>();

	/**
	 * initializes a decoder, which can be used on any modification of {@param plan}.
	 * This constructor initializes the relation between activities and genes.
	 */
	public JointPlanOptimizerDecoder(
			JointPlan plan,
			LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			PlansCalcRoute routingAlgorithm,
			Network network,
			int numJointEpisodes,
			int numEpisodes) {

		Map<PlanElement,Integer> alreadyDetermined = new HashMap<PlanElement,Integer>();
		int currentPlannedBit = 0;
		int currentDurationGene = numJointEpisodes;
		Map<String, Integer> currentPlanElementAssociation = null;
		LegTravelTimeEstimator currentLegTTEstimator;

		// initialize the geneIndices structure
		for (Id id : plan.getClique().getMembers().keySet()) {
			this.genesIndices.put(id, new ArrayList<Map<String, Integer>>());

			currentLegTTEstimator = legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
					plan.getIndividualPlan(id),
					//TODO: pass it by the config group
					PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
					// PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible,
					PlanomatConfigGroup.RoutingCapability.fixedRoute,
					routingAlgorithm,
					network);
			this.legTTEstimators.put(id, currentLegTTEstimator);

			this.individualPlanElements.put(id,
					plan.getIndividualPlan(id).getPlanElements());
		}

		this.facticePlan = createFacticePlan(plan);
		nonSharedLegsTTEstimator = legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				this.facticePlan,
				//TODO: pass it by the config group
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
				// PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible,
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				routingAlgorithm,
				network);

		// construction of the list of relative genes.
		for (PlanElement pe : plan.getPlanElements()) {
			currentPlanElementAssociation = new HashMap<String, Integer>();

			// only consider activities
			if (pe instanceof Leg) {
				continue;
			}

			// associate PU activities to "planned" bits
			// if (((JointActing) pe).getJoint()) {
			if (((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP)) {
				if (!alreadyDetermined.containsKey(pe)) {
					// associate a "toggle" bit
					currentPlanElementAssociation.put(TOGGLE_CHROM, currentPlannedBit);
					// remember the bit's position for linked activities
					for (JointActing linked : 
							((JointActing) pe).getLinkedElements().values()) {
						alreadyDetermined.put((PlanElement) linked, currentPlannedBit);
					}
					currentPlannedBit++;
				} else {
					currentPlanElementAssociation.put(TOGGLE_CHROM, alreadyDetermined.get(pe));
				}
			}
			// associate to duration genes (only for non-joint-trip related activities)
			else if ( !((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF) ) {
				currentPlanElementAssociation.put(DURATION_CHROM, currentDurationGene);
				currentDurationGene++;
			}

			// remember the association
			this.genesIndices.get( ((JointActing) pe).getPerson().getId() ).add(
					currentPlanElementAssociation);
		}
		this.plan = plan;
	}

	/**
	 * Returns a plan corresponding to the chromosome.
	 */
	public JointPlan decode(IChromosome chromosome) {

		Map<Id, PlanImpl> constructedIndividualPlans = new HashMap<Id, PlanImpl>();

		List<Id> individualsToPlan = new ArrayList<Id>();
		List<Id> toRemove = new ArrayList<Id>();
		Id currentId = null;
		PlanImpl individualPlan = null;
		Map<PlanElement, Double> plannedPickUps = new HashMap<PlanElement, Double>();
		Map<Id, Integer> indicesInPlan = new HashMap<Id, Integer>();
		Map<Id, Integer> indicesInChromosome = new HashMap<Id, Integer>();
		Map<Id, Double> individualsNow = new HashMap<Id, Double>();

		for (Person individual : plan.getClique().getMembers().values()) {
			individualPlan = new PlanImpl(individual);
			currentId = individual.getId();
			constructedIndividualPlans.put(currentId, individualPlan);
			individualsToPlan.add(currentId);
			indicesInPlan.put(currentId, 0);
			indicesInChromosome.put(currentId, 0);
			individualsNow.put(currentId, 0d);
		}

		do {
			for (Id id : individualsToPlan) {
				planNextActivity(
						chromosome,
						indicesInPlan,
						indicesInChromosome,
						id,
						constructedIndividualPlans,
						individualsNow,
						plannedPickUps);
				if (indicesInPlan.get(id).equals(this.individualPlanElements.get(id).size())) {
					toRemove.add(id);
				}
			}
			//for (Id id : toRemove) {
			//		individualsToPlan.remove(id);
			//}
			individualsToPlan.removeAll(toRemove);
			toRemove.clear();
		} while (!individualsToPlan.isEmpty());

		return new JointPlan(this.plan.getClique(), constructedIndividualPlans);
	}

	//TODO: refactoring (make separate private methods for planing of activities)
	//make shorter, less redundant and clearer
	//make methods to retrieve information from data structures and updating indices
	//at the same time
	private final void planNextActivity(
			IChromosome chromosome,
			Map<Id, Integer> indicesInPlan,
			Map<Id, Integer> indicesInChromosome,
			Id id,
			Map<Id, PlanImpl> constructedIndividualPlans,
			Map<Id, Double> individualsNow,
			Map<PlanElement, Double> plannedPickUps) {
		int indexInPlan = indicesInPlan.get(id);
		int indexInChromosome = indicesInChromosome.get(id);
		PlanImpl constructedPlan = constructedIndividualPlans.get(id);
		List<PlanElement> planElements = this.individualPlanElements.get(id);
		PlanElement currentElement = planElements.get(indexInPlan);
		LegTravelTimeEstimator currentLegTTEstimator =
			this.legTTEstimators.get(id);
		double now = individualsNow.get(id);
		double currentDuration;
		double actualDuration;
		double currentTravelTime;
		int geneIndex;
		Map<String, Integer> currentGeneIndices;

		JointActivity origin;
		JointActivity destination;
		JointActivity dropOff;
		JointLeg leg;

		if (indexInPlan==0) {
			origin = new JointActivity((JointActivity) currentElement);
			origin.setStartTime(now);
			geneIndex = this.genesIndices.get(id).get(indexInChromosome).get(DURATION_CHROM);
			currentDuration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue();
			origin.setMaximumDuration(currentDuration);
			now += currentDuration;
			origin.setEndTime(now);

			constructedPlan.addActivity(origin);
			indexInPlan++;
			indexInChromosome++;
		}
		else if (currentElement instanceof JointLeg) {
			// Assumes that a plan begins by an activity, with a strict Act-Leg alternance.
			origin = (JointActivity) planElements.get(indexInPlan - 1);
			destination = new JointActivity((JointActivity) planElements.get(indexInPlan + 1));
			currentGeneIndices = this.genesIndices.get(id).get(indexInChromosome);
			indexInChromosome++;

			if (destination.getType().equals(JointActingTypes.PICK_UP)) {
				geneIndex = currentGeneIndices.get(TOGGLE_CHROM);
				if (!((BooleanGene) chromosome.getGene(geneIndex)).booleanValue()) {
					// the joint travel isn't affected
					// go to the next non-joint leg
					leg = ((JointLeg) currentElement).getAssociatedIndividualLeg();
					leg = new JointLeg(this.nonSharedLegsTTEstimator.getNewLeg(
							leg.getMode(),
							origin,
							destination,
							getIndexNonShared(leg),
							now), leg);

					//leg.setDepartureTime(now);
					constructedPlan.addLeg(leg);
					currentTravelTime = leg.getTravelTime();
					now += currentTravelTime;
					//leg.setArrivalTime(now);


					// set index to the next leg
					indexInPlan += 6;

					geneIndex = currentGeneIndices.get(DURATION_CHROM);
					currentDuration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue();
					actualDuration = actualDuration(currentTravelTime, currentDuration);

					destination = createActivity(
							(JointActivity) planElements.get(indexInPlan - 1),
							now,
							actualDuration);
					constructedPlan.addActivity(destination);

					now += actualDuration;
				}
				else if (!this.readyPickUps.containsKey(destination)) {
					// case of an affected PU activity without access:
					// plan the access leg
					leg = ((JointLeg) currentElement);
					leg = new JointLeg(currentLegTTEstimator.getNewLeg(
							leg.getMode(),
							origin,
							destination,
							planElements.indexOf(leg),
							now), leg);

					//leg.setDepartureTime(now);
					constructedPlan.addLeg(leg);
					currentTravelTime = leg.getTravelTime();
					now += currentTravelTime;

					// mark the PU as accessed
					this.readyPickUps.put(destination, now);

					indexInPlan += 1;
				}
			}
			else {
				// case of a leg which destination isn't a PU act
				leg = ((JointLeg) currentElement);
				leg = new JointLeg(currentLegTTEstimator.getNewLeg(
						leg.getMode(),
						origin,
						destination,
						planElements.indexOf(leg),
						now), leg);

				//leg.setDepartureTime(now);
				constructedPlan.addLeg(leg);
				currentTravelTime = leg.getTravelTime();
				now += currentTravelTime;
				//leg.setArrivalTime(now);

				// set index to the next leg
				indexInPlan += 2;

				geneIndex = currentGeneIndices.get(DURATION_CHROM);
				currentDuration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue();
				actualDuration = actualDuration(currentTravelTime, currentDuration);
				destination.setMaximumDuration(actualDuration);
				now += actualDuration;
				destination.setEndTime(now);
				constructedPlan.addActivity(destination);
			}
		}
		else if (((JointActivity) currentElement).getType().equals(JointActingTypes.PICK_UP)&&
				(isReadyForPlanning(planElements, indexInPlan))) {
			origin = (JointActivity) currentElement;
			dropOff = (JointActivity) planElements.get(indexInPlan + 2);
			destination = (JointActivity) planElements.get(indexInPlan + 4);
			currentTravelTime = 0d;

			// /////////////////////////////////////////////////////////////////
			// plan pick-up activity
			currentDuration = getTimeToJointTrip(
					origin.getLinkedElements().values(),
					now) + PU_DURATION;
			constructedPlan.addActivity(createActivity(origin, now, currentDuration));

			now += currentDuration;

			// /////////////////////////////////////////////////////////////////
			// plan shared ride
			leg = (JointLeg) planElements.get(indexInPlan + 1);

			if (leg.getIsDriver()) {
				// use legttestimator
				leg = new JointLeg(currentLegTTEstimator.getNewLeg(
					leg.getMode(),
					origin,
					dropOff,
					planElements.indexOf(leg),
					now), leg);
			} else {
				// get driver trip
				leg = new JointLeg(this.driverLegs.get(leg));
				leg.setMode(JointActingTypes.PASSENGER);
				leg.setIsDriver(false);
			}

			now += leg.getTravelTime();
			currentTravelTime += leg.getTravelTime();
			constructedPlan.addLeg(leg);

			// /////////////////////////////////////////////////////////////////
			// plan DO activity
			dropOff = createActivity(dropOff, now, DO_DURATION);
			now += DO_DURATION;
			currentTravelTime += DO_DURATION;
			constructedPlan.addActivity(dropOff);

			// /////////////////////////////////////////////////////////////////
			// plan egress trip
			leg = (JointLeg) planElements.get(indexInPlan + 3);
			leg = new JointLeg(currentLegTTEstimator.getNewLeg(
				leg.getMode(),
				dropOff,
				destination,
				planElements.indexOf(leg),
				now), leg);
			now += leg.getTravelTime();
			currentTravelTime += leg.getTravelTime();
			constructedPlan.addLeg(leg);

			// /////////////////////////////////////////////////////////////////
			// plan individual activity
			currentGeneIndices = this.genesIndices.get(id).get(indexInChromosome);
			geneIndex = currentGeneIndices.get(DURATION_CHROM);
			currentDuration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue();
			actualDuration = actualDuration(currentTravelTime, currentDuration);
			destination = createActivity(destination, now, actualDuration);
			constructedPlan.addActivity(destination);
			now += actualDuration;

			// /////////////////////////////////////////////////////////////////
			//update indices
			indexInPlan += 5;
			indexInChromosome++;
		}
		else {
			log.error("unexpected index: trying to plan an element which is not"+
					" a JointLeg nor a Pick-Up activity");
		}

		//update individual "now" value
		individualsNow.put(id, now);
		indicesInPlan.put(id, indexInPlan);
		indicesInChromosome.put(id, indexInChromosome);
	}

	private static final double actualDuration(double travelTime, double duration) {
		double actualDuration = duration - travelTime;
		return (actualDuration > 0 ? actualDuration : MIN_DURATION);
	}

	/**
	 * Creates a "plan" containing only references to the individuals "reimplacement"
	 * legs, for compatibility with the planomat travel time estimators.
	 * Assumes a strict act-leg alternance in the individual plans.
	 */
	private static final PlanImpl createFacticePlan(JointPlan realPlan) {
		//Array prefered over list as is allows to access by indices more
		//efficiently
		PlanElement[] planElements = new PlanElement[1];
		PlanImpl outputPlan = new PlanImpl();
		int i = 1;

		for (Plan individualPlan : realPlan.getIndividualPlans().values()) {
			planElements = individualPlan.getPlanElements().toArray(planElements);
			while (i < planElements.length) {
				if (((JointLeg) planElements[i]).getJoint()) {
					outputPlan.addLeg(((JointLeg) planElements[i]).getAssociatedIndividualLeg());
					// jump directly to the next leg which is not associated to this
					// joint trip (reminder: a Joint trip is a sequence accessLeg-PU-
					// sharedLeg-DO-egressLeg)
					i += 6;
				} else {
					//jump to the next leg
					i += 2;
				}
			}
		}

		return outputPlan;
	}

	private final int getIndexNonShared(Leg leg) {
		return this.facticePlan.getActLegIndex(leg);
	}

	/**
	 * Determines if a pick-up activity is ready for planning, that is:
	 * - all related access trips are planned;
	 * - the current individual is the driver, or the driver trip is planned
	 */
	private final boolean isReadyForPlanning(List<PlanElement> planElements, int index) {
		JointActivity pickUp = (JointActivity) planElements.get(index);
		JointLeg sharedRide = (JointLeg) planElements.get(index + 1);

		boolean allRelativesArePlanned = 
			this.readyPickUps.keySet().containsAll(pickUp.getLinkedElements().values());
		boolean isDriver = sharedRide.getIsDriver();
		boolean driverIsPlanned = this.driverLegs.containsKey(sharedRide);
		
		return (allRelativesArePlanned && (isDriver || driverIsPlanned));
	}

	private final JointActivity createActivity(
			JointActivity act,
			double now,
			double duration) {
		JointActivity newAct = new JointActivity(act);
		newAct.setMaximumDuration(duration);
		act.setEndTime(now + duration);

		return newAct;
	}

	private final double getTimeToJointTrip(
			Collection<? extends JointActing> linkedElements,
			double now) {
		// the min start time should not be before now
		double max = now;
		double currentTime;

		for (JointActing act : linkedElements) {
			currentTime = this.readyPickUps.get(act);
			if (max < currentTime) {
				max = currentTime;
			}
		}
		return max - now;
	}

}
