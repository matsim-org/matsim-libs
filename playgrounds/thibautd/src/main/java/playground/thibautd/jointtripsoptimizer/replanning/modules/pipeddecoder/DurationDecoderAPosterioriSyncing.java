/* *********************************************************************** *
 * project: org.matsim.*
 * DurationDecoderAPosterioriSyncing.java
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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.SimLegInterpretation;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

import playground.thibautd.jointtripsoptimizer.population.JointActing;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointActivity;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators.JointPlanOptimizerLegTravelTimeEstimatorFactory;
import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Decoder for durations, which takes advantage of the capability of {@link JointPlan}
 * to synchronise itself at creation.
 *
 * Drawback: time dependent travel time estimation related to the unsynchronized plan.
 *
 * meant to be quicker.
 *
 * @author thibautd
 */
public class DurationDecoderAPosterioriSyncing implements JointPlanOptimizerDimensionDecoder {
	private static final Logger log =
		Logger.getLogger(DurationDecoder.class);

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

	/**
	 * initializes a decoder, which can be used on any modification of the parameter plan.
	 * This constructor initializes the relation between activities and genes.
	 */
	public DurationDecoderAPosterioriSyncing(
			final JointPlan plan,
			final JointReplanningConfigGroup configGroup,
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
	 * Returns a plan corresponding to the chromosome.
	 */
	@Override
	public JointPlan decode(
			final IChromosome chromosome,
			final JointPlan inputPlan) {

		Map<Id, PlanImpl> constructedIndividualPlans = new HashMap<Id, PlanImpl>();
		Id currentId = null;
		PlanImpl individualPlan = null;

		this.plan = inputPlan;

		resetInternalState();
		//this.initializeLegEstimators(this.plan);

		for (Person individual : plan.getClique().getMembers().values()) {
			individualPlan = new PlanImpl(individual);
			currentId = individual.getId();
			constructedIndividualPlans.put(currentId, individualPlan);
			plan(chromosome, currentId, individualPlan);
		}

		return new JointPlan(
				this.plan.getClique(),
				constructedIndividualPlans,
				false, // do not add at individual level
				true, //do not synchronize at creation
				this.plan.getScoresAggregatorFactory());
	}

	private void resetInternalState() {
		this.readyJointLegs.clear();
		this.driverLegs.clear();
		//this.legTTEstimators.clear();
	}

	private final void plan(
			final IChromosome chromosome,
			final Id id,
			final PlanImpl constructedPlan) {
		List<PlanElement> planElements = this.individualPlanElements.get(id);
		PlanElement currentElement;
		LegTravelTimeEstimator currentLegTTEstimator =
			this.legTTEstimators.get(id);
		Integer geneIndex;
		int indexInChromosome = 0;
		double now = 0d;
		double duration;
		double travelTime = 0d;

		JointActivity origin;
		JointActivity destination;
		JointActivity currentAct;
		JointLeg currentLeg;


		for (int i=0; i < planElements.size(); i++) {
			currentElement = planElements.get(i);
			if (currentElement instanceof JointActivity) {
				geneIndex = this.genesIndices.get(id).get(indexInChromosome);
				currentAct = (JointActivity) currentElement;

				if (!(isPickUp(currentAct) || isDropOff(currentAct))) {
					duration = getDuration(
							chromosome,
							geneIndex,
							now,
							travelTime);
					indexInChromosome++;
				}
				else {
					duration = 0d;
				}

				constructedPlan.addActivity(createActivity(
							currentAct, now, duration));
				now += duration;

			}
			else {
				origin = (JointActivity) planElements.get(i - 1);
				destination = (JointActivity) planElements.get(i + 1);
				currentLeg = createLeg(
						currentLegTTEstimator,
						origin,
						destination,
						i,
						now,
						(JointLeg) currentElement);
				constructedPlan.addLeg(currentLeg);
				now += currentLeg.getTravelTime();
			}
		}
	}

	private final JointActivity createActivity(
			final JointActivity act,
			final double now,
			final double duration) {
		JointActivity newAct = new JointActivity(act);
		newAct.setMaximumDuration(duration);
		newAct.setEndTime(now + duration);

		return newAct;
	}

	private JointLeg createLeg(
			final LegTravelTimeEstimator legTTEstimator,
			final Activity origin,
			final Activity destination,
			final int indexInPlan,
			final double now,
			final JointLeg leg) {
		JointLeg output;
		if (!(leg.getMode().equals(JointActingTypes.PASSENGER))) {
			output = new JointLeg(legTTEstimator.getNewLeg(
				leg.getMode(),
				origin,
				destination,
				indexInPlan,
				now), leg);
		}
		else {
			output = new JointLeg(leg);
		}

		//necessary with the fixed route operator
		try {
			output.getRoute().setTravelTime(output.getTravelTime());
		} catch (NullPointerException e) {
			// can occur for passenger legs.
		}

		output.setDepartureTime(now);
		output.setArrivalTime(now + output.getTravelTime());

		return output;
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
}

