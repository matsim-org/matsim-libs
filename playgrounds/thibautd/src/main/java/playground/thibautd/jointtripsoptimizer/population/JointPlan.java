/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlan.java
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
package playground.thibautd.jointtripsoptimizer.population;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;

import playground.thibautd.jointtripsoptimizer.scoring.HomogeneousScoreAggregatorFactory;
import playground.thibautd.jointtripsoptimizer.scoring.ScoresAggregator;
import playground.thibautd.jointtripsoptimizer.scoring.ScoresAggregatorFactory;

/**
 * class for handling synchronized plans.
 * It implements the plan interface to be compatible with the StrategyManager.
 * @author thibautd
 */
public class JointPlan implements Plan {
	private static final Logger log =
		Logger.getLogger(JointPlan.class);


	// durations for syncing:
	private final static double pickUpDuration = 0d;
	// this will also be used for DO
	private final static double minimalDuration = 1d;

	private final Map<Id,Plan> individualPlans = new HashMap<Id,Plan>();
	/**
	 * for robust resolution of links between activities.
	 */
	private final Map<IdLeg, JointLeg> legsMap = new HashMap<IdLeg, JointLeg>();
	/**
	 * true if the individual plans are maintained at the individual level.
	 */
	private final boolean setAtIndividualLevel;

	private final Clique clique;

	private ScoresAggregator aggregator = null;
	// for replanning modules to be able to replicate aggregator
	private final ScoresAggregatorFactory aggregatorFactory;
	private final String individualPlanType;

	//private Id currentIndividual = null;
	//private Iterator<Id> individualsIterator;

	/**
	 * Creates a joint plan from individual plans.
	 * The plans are added at the individual level.
	 * equivalent to JointPlan(clique, plans, true, true).
	 */
	public JointPlan(
			final Clique clique,
			final Map<Id, ? extends Plan> plans) {
		this(clique, plans, true);
	}

	/**
	 * equivalent to JointPlan(clique, plans, addAtIndividualLevel, true)
	 */
	public JointPlan(
			final Clique clique,
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel) {
		this(clique, plans, addAtIndividualLevel, true);
	}


	/**
	 * Initilizes with an {@link HomogeneousScoreAggregatorFactory}
	 */
	public JointPlan(
			final Clique clique,
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel,
			final boolean toSynchronize) {
		this(clique, plans, addAtIndividualLevel, toSynchronize, new HomogeneousScoreAggregatorFactory());
	}

	/**
	 * Creates a joint plan from individual plans.
	 * Two individual trips to be shared must have their Pick-Up activity type set
	 * to 'pu_i', where i is an integer which identifies the joint trip.
	 * @param addAtIndividualLevel if true, the plans are added to the Person's plans.
	 * set to false for a temporary plan (in a replanning for example).
	 * @param toSynchronize if true, the activity durations will be modified so
	 * that the joint activities are simultaneous (not implemented yet)
	 */
	//TODO: separate in several helpers (too messy)
	public JointPlan(
			final Clique clique,
			final Map<Id, ? extends Plan> plans,
			final boolean addAtIndividualLevel,
			final boolean toSynchronize,
			final ScoresAggregatorFactory aggregatorFactory) {
		this.setAtIndividualLevel = addAtIndividualLevel;
		PlanImpl currentPlan;
		Plan currentImportedPlan;
		this.clique = clique;

		// in the plan file, pu activities are numbered. If two pick ups have
		// the same number in the same joint plan, the following legs are
		// considered joint.
		// This structure "accumulates" the legs to join during the construction,
		// in order to be able to link all related genes.
		Map<String, List<JointLeg>> toLink = new HashMap<String, List<JointLeg>>();
		String actType;
		String currentJointEpisodeId = null;
		JointLeg currentLeg;
		JointActivity currentActivity;

		//TODO: check for consistency (referenced IDs, etc)
		for (Id id: plans.keySet()) {
			currentPlan = new PlanImpl(this.clique.getMembers().get(id));
			currentImportedPlan = plans.get(id);

			for (PlanElement pe : currentImportedPlan.getPlanElements()) {
				if (pe instanceof Activity) {
					currentActivity = new JointActivity((Activity) pe, 
							this.clique.getMembers().get(id));
					actType = currentActivity.getType();

					if (actType.matches(JointActingTypes.PICK_UP_REGEXP)) {
						// the next leg will be to associate with this id
						currentJointEpisodeId =
							actType.split(JointActingTypes.PICK_UP_SPLIT_EXPR)[1];
						currentActivity.setType(JointActingTypes.PICK_UP);
					}

					currentPlan.addActivity(currentActivity);
				}
				else {
					currentLeg = new JointLeg((Leg) pe,
							(Person) this.clique.getMembers().get(id));

					if (currentJointEpisodeId != null) {
						// this leg is a shared leg, remember this.
						if (!toLink.containsKey(currentJointEpisodeId)) {
							toLink.put(currentJointEpisodeId, new ArrayList<JointLeg>());
						}

						toLink.get(currentJointEpisodeId).add(currentLeg);
						currentJointEpisodeId = null;
					}

					currentPlan.addLeg(currentLeg);
				}
			}
			this.individualPlans.put(id, currentPlan);

			currentPlan.setScore(currentImportedPlan.getScore());

			if (addAtIndividualLevel) {
				this.clique.getMembers().get(id).addPlan(currentPlan);
			}
		}

		// create the links that where encoded in the activity types names
		for (List<JointLeg> legsToLink : toLink.values()) {
			for (JointLeg leg : legsToLink) {
				if (leg.getMode().equals(TransportMode.car)) {
					leg.setIsDriver(true);
				}
				for (JointLeg linkedLeg : legsToLink) {
					if (leg != linkedLeg) {
						leg.addLinkedElementById(linkedLeg.getId());
					}
				}	
			}
		}

		this.constructLegsMap();
		this.individualPlanType = this.setIndividualPlanTypes();

		if (toSynchronize) {
			this.synchronize();
		}

		this.aggregatorFactory = aggregatorFactory;
		this.aggregator =
			aggregatorFactory.createScoresAggregator(this.individualPlans.values());
	}

	/**
	 * If the plan is to be added a the individual level, this method attach
	 * it a type identifying all individual plans pertaining to the same joint
	 * plan.
	 *
	 * @return the type used
	 */
	private String  setIndividualPlanTypes() {
		if (this.setAtIndividualLevel) {
			String type = this.clique.getNextIndividualPlanType();

			for (Plan plan :  this.individualPlans.values()) {
				((PlanImpl) plan).setType(type);
			}

			return type;
		}
		return null;
	}

	private void synchronize() {
		Map<Id, IndividualValuesWrapper> individualValues = getIndividualValueWrappers();
		List<JointLeg> accessedLegs = new ArrayList<JointLeg>();
		
		while (notAllPlansSynchronized(individualValues)) {
			for (Id id : individualValues.keySet()) {
				examineNextActivity(id, individualValues, accessedLegs);
			}
		}
	}

	private Map<Id, IndividualValuesWrapper> getIndividualValueWrappers() {
		Map<Id, IndividualValuesWrapper> out = new HashMap<Id, IndividualValuesWrapper>();

		for (Id id : individualPlans.keySet()) {
			out.put(id, new IndividualValuesWrapper());
		}

		return out;
	}

	/**
	 * makes a <u>shallow</u> copy of the plan.
	 */
	public JointPlan(final JointPlan plan) {
		this(	plan.getClique(),
				plan.getIndividualPlans(),
				plan.setAtIndividualLevel,
				false, // just copy the plan: do not try to synchronize
				plan.getScoresAggregatorFactory());
	}

	private void constructLegsMap() {
		IdLeg currentLegId;

		this.legsMap.clear();
		for (PlanElement pe : this.getPlanElements()) {
			if (pe instanceof JointLeg) {
				((JointLeg) pe).setJointPlan(this);
				currentLegId = ((JointLeg) pe).getId();
				if (!this.legsMap.keySet().contains(currentLegId)) {
					this.legsMap.put(currentLegId, (JointLeg) pe);
				} else {
					throw new IllegalArgumentException("duplicate id found during"
							+" JointPlan construction");
				}
			}
		}
	}

	/*
	 * =========================================================================
	 * Plan interface methods
	 * =========================================================================
	 */
	/**
	 * @return the list of plan elements, for all individuals. While the plan 
	 * elements are internal references, the list is not: modifying it (by adding
	 * or removing elements) will not modify the joint plan.
	 */
	@Override
	public List<PlanElement> getPlanElements() {
		List<PlanElement> output = new ArrayList<PlanElement>();
		for (Plan plan : this.individualPlans.values()) {
			output.addAll(plan.getPlanElements());
		}
		return output;
	}


	/**
	 * Inherited from the interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void addLeg(Leg leg) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Inherited from the interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void addActivity(Activity act) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}

	@Override
	public void setScore(Double score) {
		throw new UnsupportedOperationException("JointPlan.setScore(Double) is"+
				" unsupported. The scores must be set on the individual plans.");
	}

	/**
	 * Returns the global score as defined by the score aggregator
	 */
	@Override
	public Double getScore() {
		return this.aggregator.getJointScore();
	}

	/**
	 * @return the Clique to wich the plan is affected (wrapper to getClique).
	 */
	@Override
	public Person getPerson() {
		return this.getClique();
	}

	/**
	 * Inherited from interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setPerson(Person person) {
		throw new UnsupportedOperationException("JointPlan instances can only be"
				+" associated to a clique at construction");
	}

	@Override
	public Map<String,Object> getCustomAttributes() {
		//TODO
		return null;
	}

	/*
	 * =========================================================================
	 * JointPlan specific methods
	 * =========================================================================
	 */
	public Clique getClique() {
		return this.clique;
	}

	public Map<Id, List<PlanElement>> getIndividualPlanElements() {
		Map<Id, List<PlanElement>> output = new TreeMap<Id, List<PlanElement>>();

		for (Map.Entry<Id, Plan> entry : this.individualPlans.entrySet()) {
			output.put(entry.getKey(), entry.getValue().getPlanElements());
		}

		return output;
	}

	public Plan getIndividualPlan(Person person) {
		return this.getIndividualPlan(person.getId());
	}

	public Plan getIndividualPlan(Id id) {
		return this.individualPlans.get(id);
	}

	public Map<Id,Plan> getIndividualPlans() {
		return this.individualPlans;
	}

	/**
	 * Transforms this plan so that it is identical to the argument plan.
	 * Used in the replanning module.<BR>
	 * Caution: this does NOT make a copy of the plan, but makes the internal
	 * individual plan references to be equal. This is OK in the case of the
	 * replanning module (where the argument plan is just a local instance),
	 * but could lead to strange results if the two plan are used in different
	 * places.<BR>
	 * Caution 2: if the plan is set at individual level, the types of the new
	 * individual plans are set identical to the "old" ones.
	 *
	 */
	public void resetFromPlan(JointPlan plan) {
		if (plan.getClique() != this.clique) {
			throw new UnsupportedOperationException("resetting a joint plan from"+
					" a plan of a different clique is unsupported.");
		}
		if (this.setAtIndividualLevel) {
			PlanImpl currentPlan;
			for (Person currentIndividual : this.clique.getMembers().values()) {
				// remove the corresponding plan at the individual level
				currentIndividual.getPlans().remove(
						this.individualPlans.get(currentIndividual.getId()));
				// replace it by the new plan
				currentPlan = (PlanImpl) plan.getIndividualPlan(currentIndividual);
				currentPlan.setType(this.individualPlanType);
				currentIndividual.addPlan(currentPlan);
				// set it as selected if it was the selected plan
				if (this.isSelected()) {
					// no possibility to set the selected plan with the Person interface
					((PersonImpl) currentIndividual).setSelectedPlan(
						plan.getIndividualPlan(currentIndividual));
				}
			}
		}
		this.individualPlans.clear();
		this.individualPlans.putAll(plan.individualPlans);
		// update the aggregator, so that it considers the scores of the new plans
		// in fact, without doing it, the new plans should already be considered if
		// the collection in the aggregator points towards the values collection
		// of the map, but it would become messy (and implementation dependant)
		this.aggregator = this.aggregatorFactory.createScoresAggregator(this.individualPlans.values());
		this.legsMap.clear();
		this.legsMap.putAll(plan.legsMap);
	}

	/**
	 * Sets the individual scores to null.
	 * Used in the replanning.
	 */
	public void resetScores() {
		for (Plan plan : this.individualPlans.values()) {
			plan.setScore(null);
		}
	}

	public List<Activity> getLastActivities() {
		List<Activity> output = new ArrayList<Activity>();
		List<PlanElement> currentPlanElements;

		for (Plan currentPlan : this.individualPlans.values()) {
			currentPlanElements = currentPlan.getPlanElements();
			try {
				output.add((Activity) currentPlanElements.get(currentPlanElements.size() - 1));
			} catch (ClassCastException e) {
				throw new RuntimeException("plan "+currentPlan+" does not finish by an activity.");
			}
		}

		return output;
	}

	/**
	 * Returns the a leg given its Id.
	 * used to resolve links between joint legs.
	 * for used in JointLeg only.
	 */
	JointLeg getLegById(IdLeg legId) {
		JointLeg leg = this.legsMap.get(legId);

		if (leg == null) {
			throw new RuntimeException("legs links could not be resolved");
		}

		return leg;
	}

	/**
	 * Returns the "type" of the plan.
	 * This allows to make sure that the most "general" plan will not be removed.
	 * @return a string, corresponding to a list of the ids of the shared legs,
	 * separated by "-". No shared leg corresponds to the type "".
	 */
	public String getType() {
		String type = "";
		boolean notFirst = false;

		for (JointLeg currentJointLeg : this.legsMap.values()) {
			if (currentJointLeg.getJoint()) {
				if (notFirst) {
					type += "-";
				} else {
					notFirst = true;
				}
				type += currentJointLeg.getId();
			}
		}

		return type;
	}

	public ScoresAggregatorFactory getScoresAggregatorFactory() {
		return this.aggregatorFactory;
	}

	/*
	 * =========================================================================
	 * plan synchronization helpers
	 * =========================================================================
	 */
	private class IndividualValuesWrapper {
		public int indexInPlan = 0;
		public double now = 0d;
		public boolean isFinished = false;
	}

	private boolean notAllPlansSynchronized(
			final Map<Id, IndividualValuesWrapper> IndividualValues) {
		for (IndividualValuesWrapper value : IndividualValues.values()) {
			if (value.isFinished == false) {
				return true;
			}
		}
		return false;
	}

	/**
	 * To call iteratively to synchronize plans.
	 * It works in the following way:
	 * <ul>
	 * <li> for individual legs/activities, it just make them begin
	 * at the end of the last act/leg </li>
	 * <li> for joint legs, it makes the PU end at the latest passenger arrival </li>
	 * </ul>
	 */
	private void examineNextActivity(
			final Id id,
			final Map<Id, IndividualValuesWrapper> individualValues,
			final List<JointLeg> accessedLegs) {
		IndividualValuesWrapper currentIndividualValues = individualValues.get(id);
		PlanElement currentPlanElement;

		try {
			currentPlanElement = this.individualPlans.get(id)
				.getPlanElements().get(currentIndividualValues.indexInPlan);
		} catch (IndexOutOfBoundsException e) {
			// no more plan elements for this individual
			currentIndividualValues.isFinished = true;
			return;
		}

		if (currentPlanElement instanceof Leg) {
			JointLeg leg;
			double travelTime;

			try {
				leg = (JointLeg) currentPlanElement;
			} catch (ClassCastException e) {
				throw new RuntimeException("JointPlan contained non-JointLeg legs");
			}

			leg.setDepartureTime(currentIndividualValues.now);
			travelTime = leg.getTravelTime();
			currentIndividualValues.now += (travelTime == Time.UNDEFINED_TIME ?
					0d : travelTime);
			leg.setArrivalTime(currentIndividualValues.now);
			// ensures that passenger legs with new times will be copied again.
			leg.routeToCopy();

			currentIndividualValues.indexInPlan++;

			return;
		}

		Activity act = (Activity) currentPlanElement;

		if (!act.getType().equals(JointActingTypes.PICK_UP)) {
			double endTime = act.getEndTime();
			if (endTime == Time.UNDEFINED_TIME) {
				endTime = currentIndividualValues.now + act.getMaximumDuration();
			}

			act.setStartTime(currentIndividualValues.now);
			currentIndividualValues.now =
				currentIndividualValues.now < endTime ?
				endTime :
				currentIndividualValues.now + minimalDuration;
			act.setEndTime(currentIndividualValues.now);

			currentIndividualValues.indexInPlan++;
		}
		else {
			// test if linked legs are planned
			JointLeg sharedRide = (JointLeg) this.individualPlans.get(id)
				.getPlanElements().get(currentIndividualValues.indexInPlan + 1);
			Map<Id, JointLeg> linkedLegs = sharedRide.getLinkedElements();
			if (accessedLegs.containsAll(linkedLegs.values())) {
				// for other legs to be aware
				accessedLegs.add(sharedRide);
				// if yes, compute duration and set activity
				double soonestStartTime = currentIndividualValues.now;
				for (JointLeg leg : linkedLegs.values()) {
					soonestStartTime = Math.max(
							soonestStartTime,
							leg.getDepartureTime());
				}
				act.setStartTime(currentIndividualValues.now);
				currentIndividualValues.now = soonestStartTime;
				act.setEndTime(currentIndividualValues.now);
				sharedRide.setDepartureTime(currentIndividualValues.now);
				currentIndividualValues.indexInPlan++;
			}
			// else, add joint leg as accessed 
			else {
				accessedLegs.add(sharedRide);
				sharedRide.setDepartureTime(
						currentIndividualValues.now + pickUpDuration);
			}
		}
	}
}
