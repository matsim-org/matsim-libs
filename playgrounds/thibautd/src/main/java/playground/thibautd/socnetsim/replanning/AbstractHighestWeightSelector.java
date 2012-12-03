/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractHighestWeightSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;

/**
 * Selects the plan combination with the highest (implementation specific)
 * weight.
 * @author thibautd
 */
public abstract class AbstractHighestWeightSelector implements GroupLevelPlanSelector {
	@Override
	public GroupPlans selectPlans(final ReplanningGroup group) {
		Map<Id, PersonRecord> personRecords = getPersonRecords( group );

		PlanString allocation = buildPlanString(
				personRecords,
				new ArrayList<PersonRecord>( personRecords.values() ),
				Collections.EMPTY_LIST,
				null);

		return toGroupPlans( allocation );
	}

	private GroupPlans toGroupPlans(final PlanString allocation) {
		Set<JointPlan> jointPlans = new HashSet<JointPlan>();
		List<Plan> individualPlans = new ArrayList<Plan>();
		for (PlanString curr = allocation;
				curr != null;
				curr = curr.tail) {
			if (curr.planRecord.jointPlan != null) {
				jointPlans.add( curr.planRecord.jointPlan );
			}
			else {
				individualPlans.add( curr.planRecord.plan );
			}
		}

		return new GroupPlans( jointPlans , individualPlans );
	}

	private Map<Id, PersonRecord> getPersonRecords(final ReplanningGroup group) {
		Map<Id, PersonRecord> map = new HashMap<Id, PersonRecord>();

		for (Person person : group.getPersons()) {
			List<PlanRecord> plans = new ArrayList<PlanRecord>();
			for (Plan plan : person.getPlans()) {
				plans.add( new PlanRecord(
							plan,
							JointPlanFactory.getPlanLinks().getJointPlan( plan ),
							getWeight( plan )));
			}
			map.put(
					person.getId(),
					new PersonRecord( person , plans ) );
		}

		return map;
	}

	private PlanString buildPlanString(
			final Map<Id, PersonRecord> allPersonsRecord,
			final List<PersonRecord> persons,
			final List<Id> alreadyAllocatedPersons,
			final PlanString str) {
		PersonRecord currentPerson = persons.get(0);
		List<PersonRecord> remainingPersons =
			persons.size() > 1 ?
			persons.subList( 1, persons.size() ) :
			Collections.EMPTY_LIST;

		// get a list of plans in decreasing order of maximum possible weight
		List<PlanRecord> records = new ArrayList<PlanRecord>( currentPerson.plans );
		for (PlanRecord r : records) {
			r.cachedMaximumWeight = getMaxWeightFromPersons( r , alreadyAllocatedPersons , remainingPersons );
		}
		Collections.sort(
				records,
				new Comparator<PlanRecord>() {
					@Override
					public int compare(
							final PlanRecord o1,
							final PlanRecord o2) {
						return Double.compare(
							o1.cachedMaximumWeight,
							o2.cachedMaximumWeight );
					}
				});

		// get the actual allocation, and stop when the allocation
		// is better than the maximum possible in remaining plans
		PlanString constructedString = null;

		for (PlanRecord r : records) {
			if (constructedString != null &&
					r.cachedMaximumWeight <= constructedString.getWeight()) break;

			PlanString tail = str;
			// TODO: find a better way to filter persons (should be
			// possible in PlanString)
			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			List<Id> newAllocatedPersons = new ArrayList<Id>(alreadyAllocatedPersons);
			JointPlan jointPlan = r.jointPlan ;
			if (jointPlan != null) {
				if ( contains( jointPlan , alreadyAllocatedPersons ) ) continue;
				tail = getOtherPlansAsString( r , jointPlan , allPersonsRecord , tail);
				actuallyRemainingPersons = filter( remainingPersons , jointPlan );
				newAllocatedPersons.addAll( jointPlan.getIndividualPlans().keySet() );
			}
			else {
				newAllocatedPersons.add( r.plan.getPerson().getId() );
			}

			if ( actuallyRemainingPersons.size() > 0 ) {
				constructedString = buildPlanString(
						allPersonsRecord,
						actuallyRemainingPersons,
						newAllocatedPersons,
						new PlanString( r , tail ));
			}
			else {
				constructedString = new PlanString( r , tail );
			}
		}

		return constructedString;
	}

	private static List<PersonRecord> filter(
			final List<PersonRecord> toFilter,
			final JointPlan jointPlan) {
		List<PersonRecord> newList = new ArrayList<PersonRecord>();

		for (PersonRecord r : toFilter) {
			if (!jointPlan.getIndividualPlans().containsKey( r.person.getId() )) {
				newList.add( r );
			}
		}

		return newList;
	}

	private static PlanString getOtherPlansAsString(
			final PlanRecord r,
			final JointPlan jointPlan,
			final Map<Id, PersonRecord> allPersonsRecords,
			final PlanString additionalTail) {
		PlanRecord head = null;
		PlanString tail = additionalTail;

		for (Plan p : jointPlan.getIndividualPlans().values()) {
			if (p == r.plan) continue;

			tail = new PlanString( head , tail );
			head = allPersonsRecords.get( p.getPerson().getId() ).getRecord( p );
		}

		return new PlanString( head , tail );
	}

	/**
	 * Gets the maximum plan weight that can be obtained from the
	 * plans of remainingPersons, given the alradySelected has been
	 * selected, and that planToSelect is about to be selected.
	 */
	private static double getMaxWeightFromPersons(
			final PlanRecord planToSelect,
			// the joint plans linking to persons with a plan
			// already selected cannot be selected
			final List<Id> personsSelected,
			final List<PersonRecord> remainingPersons) {
		double score = planToSelect.weight;

		// if the plan to select is a joint plan,
		// we know exactly what plan to get the score from.
		final JointPlan jointPlanToSelect = planToSelect.jointPlan;

		List<PersonRecord> personsWithChoice = remainingPersons;
		if (jointPlanToSelect != null) {
			personsWithChoice = new ArrayList<PersonRecord>();

			Map<Id, Plan> plans = jointPlanToSelect.getIndividualPlans();
			for (PersonRecord record : remainingPersons) {
				Plan plan = plans.get( record.person.getId() );
				if ( plan != null ) {
					PlanRecord planRecord = record.getRecord( plan );
					score += planRecord.weight;
				}
				else {
					personsWithChoice.add( record );
				}
			}
		}

		for (PersonRecord record : remainingPersons) {
			for (PlanRecord plan : record.plans) {
				// the plans are sorted by decreasing weight:
				// consider the first valid plan
				if (plan.jointPlan == null || contains( plan.jointPlan , personsSelected )) {
					score += plan.weight;
					break;
				}
			}
			throw new RuntimeException( "no valid plan for record "+record );
		}

		return score;
	}

	private static boolean contains(
			final JointPlan jp,
			final List<Id> personsSelected) {
		for (Id id : personsSelected) {
			if (jp.getIndividualPlans().containsKey( id )) return true;
		}
		return false;
	}

	private static List<Id> getPersonIds(final PlanString s) {
		List<Id> ids = new ArrayList<Id>();
		ids.add( s.planRecord.plan.getPerson().getId() );
		
		for (PlanString tail = s.tail;
				tail != null;
				tail = tail.tail) {
			ids.add( tail.planRecord.plan.getPerson().getId() );
		}

		return ids;
	}

	public abstract double getWeight(final Plan indivPlan);

	private static class PlanString {
		public final PlanRecord planRecord;
		public final PlanString tail;

		public PlanString(
				final PlanRecord head,
				final PlanString tail) {
			this.planRecord = head;
			this.tail = tail;
		}

		public double getWeight() {
			return (planRecord == null ? Double.NEGATIVE_INFINITY : planRecord.weight)
				+ (tail == null ? 0 : tail.getWeight());
		}
	}

	private static class PersonRecord {
		final Person person;
		final List<PlanRecord> plans;

		public PersonRecord(
				final Person person,
				final List<PlanRecord> plans) {
			this.person = person;
			this.plans = plans;
			Collections.sort(
					plans,
					new Comparator<PlanRecord>() {
						@Override
						public int compare(
								final PlanRecord o1,
								final PlanRecord o2) {
							return Double.compare( o1.weight , o2.weight );
						}
					});
		}

		public PlanRecord getRecord( final Plan plan ) {
			for (PlanRecord r : plans) {
				if (r.plan == plan) return r;
			}
			throw new IllegalArgumentException();
		}

	}

	private static class PlanRecord {
		final Plan plan;
		/**
		 * The joint plan to which pertains the individual plan,
		 * if any.
		 */
		final JointPlan jointPlan;
		final double weight;
		double cachedMaximumWeight = Double.NaN;

		public PlanRecord(
				final Plan plan,
				final JointPlan jointPlan,
				final double weight) {
			this.plan = plan;
			this.jointPlan = jointPlan;
			this.weight = weight;
		}
	}
}

