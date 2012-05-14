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
package playground.thibautd.jointtrips.population;

import java.util.ArrayList;
import java.util.Collections;
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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.jointtrips.scoring.HomogeneousScoreAggregatorFactory;
import playground.thibautd.jointtrips.scoring.ScoresAggregator;
import playground.thibautd.jointtrips.scoring.ScoresAggregatorFactory;

/**
 * class for handling synchronized plans.
 * It implements the plan interface to be compatible with the StrategyManager.
 *
 * FIXME: currently, the JointPlan is responsible for "inserting" itself at
 * the individual level. This is quite messy, and should be moved at an upper level.
 * @author thibautd
 */
public class JointPlan implements Plan {
	private static final Logger log =
		Logger.getLogger(JointPlan.class);

	private final Map<Id,Plan> individualPlans = new HashMap<Id,Plan>();
	private final boolean setAtIndividualLevel;

	private final Clique clique;

	private ScoresAggregator aggregator = null;
	// for replanning modules to be able to replicate aggregator
	private final ScoresAggregatorFactory aggregatorFactory;
	//private final String individualPlanType;
	//private JointTripPossibilities jointTripPossibilities = null;

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
		this(clique, plans, addAtIndividualLevel, new HomogeneousScoreAggregatorFactory());
	}

	/**
	 * Creates a joint plan from individual plans.
	 * Two individual trips to be shared must have their Pick-Up activity type set
	 * to 'pu_i', where i is an integer which identifies the joint trip.
	 * @param clique the clique this plan pertains to
	 * @param plans the individual plans. If they consist of Joint activities, 
	 * those activities are referenced, otherwise, they are copied in a joint activity.
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
			final ScoresAggregatorFactory aggregatorFactory) {
		this.setAtIndividualLevel = addAtIndividualLevel;

		if (addAtIndividualLevel) {
			for (Person person : clique.getMembers().values()) {
				Plan plan = plans.get( person.getId() );
				if (!person.getPlans().contains( plan )) {
					person.addPlan( plan );
				}
			}
		}
		this.clique = clique;
		this.individualPlans.putAll( plans );
		this.aggregatorFactory = aggregatorFactory;
		this.aggregator =
			aggregatorFactory.createScoresAggregator(this.individualPlans.values());
	}

	/**
	 * makes a <u>shallow</u> copy of the plan.
	 */
	public JointPlan(final JointPlan plan) {
		this(	plan.getClique(),
				cloneIndividualPlans( plan ),
				plan.setAtIndividualLevel,
				plan.getScoresAggregatorFactory());
		//this.setJointTripPossibilities( plan.getJointTripPossibilities() );
	}

	private static Map<Id, Plan> cloneIndividualPlans(final JointPlan plan) {
		Map<Id , Plan> plans = new HashMap<Id, Plan>();

		for (Map.Entry<Id, Plan> indiv : plan.getIndividualPlans().entrySet()) {
			PlanImpl newPlan = new PlanImpl( indiv.getValue().getPerson() );
			newPlan.copyPlan( indiv.getValue() );
			plans.put( indiv.getKey() , newPlan );
		}
		
		return plans;
	}

	/*
	 * =========================================================================
	 * Plan interface methods
	 * =========================================================================
	 */
	/**
	 * @return the list of plan elements, for all individuals. While the plan 
	 * elements are internal references, the list is not, and is immutable.
	 */
	@Override
	public List<PlanElement> getPlanElements() {
		List<PlanElement> output = new ArrayList<PlanElement>();
		for (Plan plan : this.individualPlans.values()) {
			output.addAll(plan.getPlanElements());
		}
		return Collections.unmodifiableList(output);
	}


	/**
	 * Inherited from the interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void addLeg(final Leg leg) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Inherited from the interface, but unimplemented.
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void addActivity(final Activity act) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isSelected() {
		return this.getPerson().getSelectedPlan() == this;
	}

	@Override
	public void setScore(final Double score) {
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
	public void setPerson(final Person person) {
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

	/**
	 * returns the list of plan elements for each agent. List are immutable.
	 * @return a map linking agents Id the the list of their plan elements,
	 * in correct temporal sequence.
	 */
	public Map<Id, List<PlanElement>> getIndividualPlanElements() {
		Map<Id, List<PlanElement>> output = new TreeMap<Id, List<PlanElement>>();

		for (Map.Entry<Id, Plan> entry : this.individualPlans.entrySet()) {
			output.put(
					entry.getKey(),
					// do not make the lists unmodifiable: acting
					// on the plan element list is the only way to
					// change plan structure, andwe do not track anything,
					// so that it is safe.
					// It would be possible by getting the plan elements from the
					// individual plans anyway...
					//Collections.unmodifiableList( entry.getValue().getPlanElements() ));
					entry.getValue().getPlanElements() );
		}

		return output;
	}

	public Plan getIndividualPlan(final Person person) {
		return this.getIndividualPlan(person.getId());
	}

	public Plan getIndividualPlan(final Id id) {
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
	public void resetFromPlan(final JointPlan plan) {
		if (plan == this) {
			return;
		}

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
				//currentPlan.setType(this.individualPlanType);
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

	///**
	// * Returns a leg given its Id.
	// * used to resolve links between joint legs.
	// *
	// * @throws LinkedElementsResolutionException if the corresponding leg is not found
	// */
	//public JointLeg getLegById(final Id legId) {
	//	throw new UnsupportedOperationException( "will be removed" );
	//}

	///**
	// * Returns an act given its Id.
	// *
	// * @throws LinkedElementsResolutionException if the corresponding leg is not found
	// */
	//public JointActivity getActById(final Id actId) {
	//	throw new UnsupportedOperationException( "will be removed" );
	//}

	/**
	 */
	public String getType() {
		return "jointPlan";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+": clique="+clique+", elements="+getIndividualPlanElements()+", plans="+getIndividualPlans()+", addAtIndividualLevel="+
			setAtIndividualLevel+", isSelected="+this.isSelected();
	}

	public ScoresAggregatorFactory getScoresAggregatorFactory() {
		return this.aggregatorFactory;
	}

	//public JointTripPossibilities getJointTripPossibilities() {
	//	return jointTripPossibilities;
	//}

	///**
	// * Sets the joint trip possibilities information
	// * @param possibilities the information to set (can be null)
	// * @return the previously set possibilities information (can be null)
	// */
	//public JointTripPossibilities setJointTripPossibilities(
	//		final JointTripPossibilities possibilities) {
	//	JointTripPossibilities old = this.jointTripPossibilities;
	//	this.jointTripPossibilities = possibilities;
	//	return old;
	//}
}
