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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;

import playground.thibautd.jointtripsoptimizer.population.JointActing;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

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
	private static final double DAY_DURATION = 24*3600d;

	private final JointPlan plan;
	private final Map<Id, List<PlanElement>> individualPlanElements =
		new HashMap<Id, List<PlanElement>>();
	private final Map<Id, LegTravelTimeEstimator> legTTEstimators =
		new HashMap<Id, LegTravelTimeEstimator>();
	private final LegTravelTimeEstimator nonSharedLegsTTEstimator;
	private final PlanImpl facticePlan;

	private final boolean optimizeToggle;

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

	/**
	 * initializes a decoder, which can be used on any modification of {@param plan}.
	 * This constructor initializes the relation between activities and genes.
	 */
	public JointPlanOptimizerDecoder(
			JointPlan plan,
			JointReplanningConfigGroup configGroup,
			LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory,
			PlansCalcRoute routingAlgorithm,
			Network network,
			int numJointEpisodes,
			int numEpisodes) {

		Map<PlanElement,Integer> alreadyDetermined = new HashMap<PlanElement,Integer>();
		List<Activity> lastActivities = plan.getLastActivities();
		int currentPlannedBit = 0;
		int currentDurationGene = numJointEpisodes;
		Map<String, Integer> currentPlanElementAssociation =
			new HashMap<String, Integer>();
		LegTravelTimeEstimator currentLegTTEstimator;

		this.optimizeToggle = configGroup.getOptimizeToggle();

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
			// associate shared legs to "planned" bits
			if ((pe instanceof Leg)) {
				if ((((JointLeg) pe).getJoint())&&
						(this.optimizeToggle)) {
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
				else {
					continue;
				}
			}
			// associate to duration genes (only for non-joint-trip related activities)
			else if (( !((JointActivity) pe).getType().equals(JointActingTypes.PICK_UP) )&&
				( !((JointActivity) pe).getType().equals(JointActingTypes.DROP_OFF) )) {
				if (!lastActivities.contains(pe)) {
					currentPlanElementAssociation.put(DURATION_CHROM, currentDurationGene);
					currentDurationGene++;
				} else {
					currentPlanElementAssociation.put(DURATION_CHROM, null);
				}
				// remember the association
				this.genesIndices.get( ((JointActing) pe).getPerson().getId() ).add(
						currentPlanElementAssociation);
				currentPlanElementAssociation = new HashMap<String, Integer>();
			}
		}
		this.plan = plan;
	}

	/**
	 * Returns a plan corresponding to the chromosome.
	 */
	public JointPlan decode(IChromosome chromosome) {

		Map<Id, PlanImpl> constructedIndividualPlans = new HashMap<Id, PlanImpl>();
		Map<Id, IndividualValuesWrapper> individualValuesMap = 
			new HashMap<Id, IndividualValuesWrapper>();
		List<Id> individualsToPlan = new ArrayList<Id>();
		List<Id> toRemove = new ArrayList<Id>();
		Id currentId = null;
		PlanImpl individualPlan = null;

		resetInternalState();

		for (Person individual : plan.getClique().getMembers().values()) {
			individualPlan = new PlanImpl(individual);
			currentId = individual.getId();
			constructedIndividualPlans.put(currentId, individualPlan);
			individualsToPlan.add(currentId);
			individualValuesMap.put(currentId, new IndividualValuesWrapper());
		}

		do {
			for (Id id : individualsToPlan) {
				planNextActivity(
						chromosome,
						individualValuesMap,
						id,
						constructedIndividualPlans);
				if (individualValuesMap.get(id).getIndexInPlan()
						>= this.individualPlanElements.get(id).size()) {
					toRemove.add(id);
				}
			}
			individualsToPlan.removeAll(toRemove);
			toRemove.clear();
		} while (!individualsToPlan.isEmpty());

		return new JointPlan(this.plan.getClique(), constructedIndividualPlans, false);
	}

	private void resetInternalState() {
		this.readyJointLegs.clear();
		this.driverLegs.clear();
	}

	private final void planNextActivity(
			IChromosome chromosome,
			Map<Id, IndividualValuesWrapper> individualValuesMap,
			Id id,
			Map<Id, PlanImpl> constructedIndividualPlans) {
		//int indexInPlan = indicesInPlan.get(id);
		//int indexInChromosome = indicesInChromosome.get(id);
		IndividualValuesWrapper individualValues = individualValuesMap.get(id);
		PlanImpl constructedPlan = constructedIndividualPlans.get(id);
		List<PlanElement> planElements = this.individualPlanElements.get(id);
		PlanElement currentElement = planElements.get(individualValues.getIndexInPlan());
		LegTravelTimeEstimator currentLegTTEstimator =
			this.legTTEstimators.get(id);
		//double now = individualsNow.get(id);
		double currentDuration;
		Integer geneIndex;
		Map<String, Integer> currentGeneIndices;

		JointActivity origin;
		JointActivity destination;

		currentGeneIndices = this.genesIndices.get(id).get(individualValues.getIndexInChromosome());

		if (individualValues.getIndexInPlan()==0) {
			origin = new JointActivity((JointActivity) currentElement);
			origin.setStartTime(individualValues.getNow());
			geneIndex = this.genesIndices.get(id)
				.get(individualValues.getIndexInChromosome()).get(DURATION_CHROM);
			currentDuration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue();
			origin.setMaximumDuration(currentDuration);
			individualValues.addToNow(currentDuration);
			origin.setEndTime(individualValues.getNow());

			constructedPlan.addActivity(origin);
			individualValues.addToIndexInPlan(1);
			individualValues.addToIndexInChromosome(1);
		}
		else if (currentElement instanceof JointLeg) {
			// Assumes that a plan begins by an activity, with a strict Act-Leg alternance.
			origin = (JointActivity) planElements.get(individualValues.getIndexInPlan() - 1);
			destination = new JointActivity((JointActivity) planElements.get(individualValues.getIndexInPlan() + 1));

			if (destination.getType().equals(JointActingTypes.PICK_UP)) {
				geneIndex = currentGeneIndices.get(TOGGLE_CHROM);
				if ((this.optimizeToggle)&&
						(!((BooleanGene) chromosome.getGene(geneIndex)).booleanValue())) {
					log.warn("planing reimplacement episode. This may not be valid"
							+" when more than one passenger");
					//TODO: create reimplacement legs "on the fly".
					planReimplacementEpisode(
						//(JointLeg) currentElement,
						planElements,
						constructedPlan,
						individualValues,
						currentGeneIndices,
						chromosome);
				}
				else if (!this.readyJointLegs.containsKey(planElements.get(individualValues.getIndexInPlan() + 2))) {
					// case of an affected PU activity without access:
					// plan the access leg
					planPuAccessLeg(
							planElements,
							currentElement,
							constructedPlan,
							currentLegTTEstimator,
							individualValues,
							chromosome);
				}
			}
			else {
				// case of a leg which destination isn't a PU act
				planIndividualLegAct(
					planElements,
					currentElement,
					constructedPlan,
					currentLegTTEstimator,
					individualValues,
					currentGeneIndices,
					chromosome);
			}
		}
		else if (((JointActivity) currentElement).getType().equals(JointActingTypes.PICK_UP)) {
			if (isReadyForPlanning(planElements, individualValues.getIndexInPlan())) {
				planSharedLegAct(
						planElements,
						currentElement,
						constructedPlan,
						currentLegTTEstimator,
						individualValues,
						currentGeneIndices,
						chromosome);
			}
		}
		else {
			log.error("unexpected index: trying to plan an element which is not"+
					" a JointLeg nor a Pick-Up activity");
		}

		//update individual "now" value
		//individualsNow.put(id, now);
		//indicesInPlan.put(id, indexInPlan);
		//indicesInChromosome.put(id, indexInChromosome);
	}

	@Deprecated
	private void planReimplacementEpisode(
			//final JointLeg puAccessLeg,
			final List<PlanElement> planElements,
			final Plan constructedPlan,
			final IndividualValuesWrapper individualValues,
			final Map<String, Integer> currentGeneIndices,
			final IChromosome chromosome) {
		Integer geneIndex = currentGeneIndices.get(TOGGLE_CHROM);
		JointLeg leg;
		JointLeg sharedLeg = (JointLeg) planElements.get(individualValues.getIndexInPlan() + 2);
		double currentTravelTime;
		double currentDuration;
		JointActivity origin = (JointActivity) planElements.get(individualValues.getIndexInPlan() - 1);
		JointActivity destination = (JointActivity) planElements.get(individualValues.getIndexInPlan() + 5);

		// the joint travel isn't affected
		// go to the next non-joint leg
		leg = sharedLeg.getAssociatedIndividualLeg();
		leg = createLeg(
				this.nonSharedLegsTTEstimator,
				origin,
				destination,
				getIndexNonShared(leg),
				individualValues,
				leg);

		//leg.setDepartureTime(now);
		constructedPlan.addLeg(leg);
		currentTravelTime = leg.getTravelTime();
		individualValues.addToNow(currentTravelTime);
		//leg.setArrivalTime(now);

		// set index to the next leg
		individualValues.addToIndexInPlan(6);

		geneIndex = currentGeneIndices.get(DURATION_CHROM);
		currentDuration = getDuration(
				chromosome,
				geneIndex,
				individualValues.getNow(),
				currentTravelTime);

		destination = createActivity(
				destination,
				individualValues.getNow(),
				currentDuration);
		constructedPlan.addActivity(destination);

		individualValues.addToNow(currentDuration);
		individualValues.addToIndexInChromosome(1);
	}

	//TODO: take into account the fact that PU access legs can be shared
	private void planPuAccessLeg(
			final List<PlanElement> planElements,
			final PlanElement currentElement,
			final Plan constructedPlan,
			final LegTravelTimeEstimator legTTEstimator,
			final IndividualValuesWrapper individualValues,
			final IChromosome chromosome
			) {
		JointLeg leg = ((JointLeg) currentElement);
		JointActivity origin = (JointActivity) planElements.get(
				individualValues.getIndexInPlan() - 1);
		JointActivity destination = new JointActivity((JointActivity)
				planElements.get(individualValues.getIndexInPlan() + 1));
		double currentTravelTime;

		leg = createLeg(
				legTTEstimator,
				origin,
				destination,
				planElements.indexOf(leg),
				individualValues,
				leg);

		//leg.setDepartureTime(now);
		constructedPlan.addLeg(leg);
		currentTravelTime = leg.getTravelTime();
		individualValues.addToNow(currentTravelTime);

		// mark the PU as accessed
		this.readyJointLegs.put(
				(JointLeg) planElements.get(individualValues.getIndexInPlan() + 2),
				individualValues.getNow());

		individualValues.addToIndexInPlan(1);
		//individualValues.addToIndexInChromosome(1);
	}

	private void planIndividualLegAct(
			final List<PlanElement> planElements,
			final PlanElement currentElement,
			final Plan constructedPlan,
			final LegTravelTimeEstimator legTTEstimator,
			final IndividualValuesWrapper individualValues,
			final Map<String, Integer> currentGeneIndices,
			final IChromosome chromosome
			) {
		JointLeg leg = ((JointLeg) currentElement);
		JointActivity origin = (JointActivity) planElements.get(
				individualValues.getIndexInPlan() - 1);
		JointActivity destination = new JointActivity((JointActivity)
				planElements.get(individualValues.getIndexInPlan() + 1));
		double currentTravelTime;
		double currentDuration;
		Integer geneIndex;

		leg = createLeg(
				legTTEstimator,
				origin,
				destination,
				planElements.indexOf(leg),
				individualValues,
				leg);

		//leg.setDepartureTime(now);
		constructedPlan.addLeg(leg);
		currentTravelTime = leg.getTravelTime();
		individualValues.addToNow(currentTravelTime);
		//leg.setArrivalTime(now);

		// set index to the next leg
		individualValues.addToIndexInPlan(2);

		geneIndex = currentGeneIndices.get(DURATION_CHROM);
		currentDuration = getDuration(chromosome, geneIndex, individualValues.getNow(), currentTravelTime);
		destination.setMaximumDuration(currentDuration);
		individualValues.addToNow(currentDuration);
		destination.setEndTime(individualValues.getNow());
		constructedPlan.addActivity(destination);
		individualValues.addToIndexInChromosome(1);
	}

	//TODO: take into account that the destination of a shared ride can be a PU
	private void planSharedLegAct(
			final List<PlanElement> planElements,
			final PlanElement currentElement,
			final Plan constructedPlan,
			final LegTravelTimeEstimator legTTEstimator,
			final IndividualValuesWrapper individualValues,
			final Map<String, Integer> currentGeneIndices,
			final IChromosome chromosome) {
		JointActivity pickUp = ((JointActivity) currentElement);
		JointLeg leg = (JointLeg) planElements.get(
				individualValues.getIndexInPlan() + 1);
		JointActivity dropOff = (JointActivity) planElements.get(
				individualValues.getIndexInPlan() + 2);
		JointActivity destination = (JointActivity)
				planElements.get(individualValues.getIndexInPlan() + 4);
		double currentTravelTime = 0d;
		double currentDuration;
		Integer geneIndex;

		// /////////////////////////////////////////////////////////////////
		// plan pick-up activity
		currentDuration = getTimeToJointTrip(
				leg.getLinkedElements().values(),
				individualValues.getNow()) + PU_DURATION;
		constructedPlan.addActivity(
				createActivity(
					pickUp,
					individualValues.getNow(),
					currentDuration));

		individualValues.addToNow(currentDuration);

		// /////////////////////////////////////////////////////////////////
		// plan shared ride
		leg = (JointLeg) planElements.get(individualValues.getIndexInPlan() + 1);

		if (leg.getIsDriver()) {
			// use legttestimator
			JointLeg driverLeg = createLeg(
				legTTEstimator,
				pickUp,
				dropOff,
				planElements.indexOf(leg),
				individualValues,
				leg);
			
			updateDriverLegs(driverLeg, leg);

			leg = driverLeg;
		} else {
			// get driver trip
			leg = new JointLeg((LegImpl) this.driverLegs.get(leg), leg);
			leg.setMode(JointActingTypes.PASSENGER);
			leg.setIsDriver(false);
		}

		individualValues.addToNow(leg.getTravelTime());
		currentTravelTime += leg.getTravelTime();
		constructedPlan.addLeg(leg);

		// /////////////////////////////////////////////////////////////////
		// plan DO activity
		dropOff = createActivity(
				dropOff,
				individualValues.getNow(),
				DO_DURATION);
		individualValues.addToNow(DO_DURATION);
		currentTravelTime += DO_DURATION;
		constructedPlan.addActivity(dropOff);

		// /////////////////////////////////////////////////////////////////
		// plan egress trip
		leg = (JointLeg) planElements.get(individualValues.getIndexInPlan() + 3);
		leg = createLeg(
			legTTEstimator,
			dropOff,
			destination,
			planElements.indexOf(leg),
			individualValues,
			leg);
		individualValues.addToNow(leg.getTravelTime());
		currentTravelTime += leg.getTravelTime();
		constructedPlan.addLeg(leg);

		// /////////////////////////////////////////////////////////////////
		// plan individual activity
		geneIndex = currentGeneIndices.get(DURATION_CHROM);
		currentDuration = getDuration(
				chromosome,
				geneIndex,
				individualValues.getNow(),
				currentTravelTime);
		destination = createActivity(
				destination,
				individualValues.getNow(),
				currentDuration);
		constructedPlan.addActivity(destination);
		individualValues.addToNow(currentDuration);

		// /////////////////////////////////////////////////////////////////
		//update indices
		individualValues.addToIndexInPlan(5);
		individualValues.addToIndexInChromosome(1);
	}

	private void updateDriverLegs(JointLeg driverLeg, JointLeg legInPlan) {
		for (JointLeg leg : legInPlan.getLinkedElements().values()) {
			this.driverLegs.put(leg, driverLeg);
		}
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
		//JointActivity pickUp = (JointActivity) planElements.get(index);
		JointLeg sharedRide = (JointLeg) planElements.get(index + 1);

		boolean allRelativesArePlanned = 
			this.readyJointLegs.keySet().containsAll(sharedRide.getLinkedElements().values());
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
		newAct.setEndTime(now + duration);

		return newAct;
	}

	private JointLeg createLeg(
			LegTravelTimeEstimator legTTEstimator,
			Activity origin,
			Activity destination,
			int indexInPlan,
			IndividualValuesWrapper individualValues,
			JointLeg leg) {
		JointLeg output = new JointLeg(legTTEstimator.getNewLeg(
			leg.getMode(),
			origin,
			destination,
			indexInPlan,
			individualValues.getNow()), leg);

		//necessary with the fixed route operator
		output.getRoute().setTravelTime(output.getTravelTime());

		output.setDepartureTime(individualValues.getNow());
		output.setArrivalTime(individualValues.getNow() + output.getTravelTime());

		return output;
	}

	private final double getTimeToJointTrip(
			Collection<? extends JointLeg> linkedElements,
			double now) {
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
			IChromosome chromosome,
			Integer geneIndex,
			double now,
			double travelTime) {
		double duration = 0d;

		if (geneIndex != null) {
			duration = ((DoubleGene) chromosome.getGene(geneIndex)).doubleValue() - travelTime;
		} else {
			// case of the last activity of an individual plan
			duration = DAY_DURATION - now;
		}

		return (duration > 0 ? duration : MIN_DURATION);
	}

	/**
	 * internal class, wrapping the int indices used to step through the plans.
	 */
	class IndividualValuesWrapper {
		private int indexInPlan = 0;
		private int indexInChromosome = 0;
		private double now = 0d;

		public void addToIndexInPlan(int i) {
			indexInPlan += i;
		}

		public void addToIndexInChromosome(int i) {
			indexInChromosome += i;
		}

		public void addToNow(double d) {
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
	}

}
