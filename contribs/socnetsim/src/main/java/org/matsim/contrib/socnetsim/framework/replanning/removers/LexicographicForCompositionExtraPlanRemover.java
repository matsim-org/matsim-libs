/* *********************************************************************** *
 * project: org.matsim.*
 * LexicograpicPerCompositionExtraPlanRemover.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.replanning.removers;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.ExtraPlanRemover;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.MapUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Considers the "maximum plans per agent" is actually a "maximum plans per
 * joint plan composition".
 *
 * If there are too many plans of a given composition, the joint plan maximizing
 * the number of first worst, then second worst, etc. plans is removed (lexicographic
 * order).
 * The first, second, etc is defined on the set of *all* plans of an agent.
 * @author thibautd
 */
public class LexicographicForCompositionExtraPlanRemover implements ExtraPlanRemover {
	private static final Logger log = Logger.getLogger( LexicographicForCompositionExtraPlanRemover.class );
	private final Random random;
	private final int maxPlansPerComposition;
	private final int maxPlansPerAgent;

	@Inject
	public LexicographicForCompositionExtraPlanRemover(
			final GroupReplanningConfigGroup config ) {
		this( config.getMaxPlansPerComposition() , config.getMaxPlansPerAgent() );
	}

	public LexicographicForCompositionExtraPlanRemover(
			final int maxPlansPerComposition,
			final int maxPlansPerAgent) {
		this.maxPlansPerComposition = maxPlansPerComposition;
		this.maxPlansPerAgent = maxPlansPerAgent;

		this.random = MatsimRandom.getLocalInstance();
	}

	@Override
	public boolean removePlansInGroup(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		if ( log.isTraceEnabled() ) log.trace( "remove plans in group of size "+group.getPersons().size() );
		boolean somethingDone = false;

		// check condition per composition
		final int maxRank = maxRank( group );
		for ( Person person : group.getPersons() ) {
			if ( person.getPlans().size() <= maxPlansPerAgent ) continue;
			final PlansPerComposition plansPerComposition = getPlansPerComposition( jointPlans , person );

			// too many individual plans?
			int rem = 0;
			while ( plansPerComposition.getIndividualPlans().size() - rem > maxPlansPerComposition ) {
				rem++;
				person.removePlan(
						identifyWorst(
								plansPerComposition.getIndividualPlans()));
				somethingDone = true;
			}
			if ( log.isTraceEnabled() && rem > 0 ) log.trace( "removed "+rem+" individual plans for person "+person.getId() );

			for ( Collection<JointPlan> structure : plansPerComposition.getJointPlansGroupedPerStructure() ) {
				rem = 0;
				int size = 0;
				while ( structure.size() - rem > maxPlansPerComposition ) {
					rem++;
					final JointPlan toRemove = identifyWorst( maxRank , structure );
					size = toRemove.getIndividualPlans().size();

					for ( Plan p : toRemove.getIndividualPlans().values() ) {
						p.getPerson().removePlan(p);
					}
					jointPlans.removeJointPlan( toRemove );
					structure.remove( toRemove );
					somethingDone = true;
				}
				if ( log.isTraceEnabled() && rem > 0 ) log.trace( "removed "+rem+" joint plans for structure of size "+size );
			}
		}

		// now that maxima per composition are processed, check whether
		// there are still agents with too many plans. This is mainly to
		// avoid memory consumption getting out of control, but the number should
		// be large, as this way of doing is problematic (it does not consider conflicts
		// at all).
		// randomize order to avoid the existence of "dictators" (agent for which their
		// worst plan is always removed, whatever the ranking of the others is)
		final List<Person> persons = new ArrayList<>( group.getPersons() );
		Collections.shuffle( persons , random );
		boolean loop = true;
		int rem = 0;
		// is useful for performance if lots of plans to remove, which is not the typical usecase...
		final Map<Id<Person>, List<? extends Plan>> sortedPlans = new HashMap<>();
		while ( loop ) {
			loop = false;
			for ( Person person : persons ) {
				if ( person.getPlans().size() > maxPlansPerAgent ) {
					if ( log.isTraceEnabled() ) log.trace( "person "+person.getId()+" has "+(person.getPlans().size() - maxPlansPerAgent)+" excedentary plans" );
					final Plan toRemove = getWorstNonUniquelyIndividualPlan( getSortedPlans( sortedPlans , person ) , jointPlans );

					rem++;
					final JointPlan jpToRemove = jointPlans.getJointPlan( toRemove );

					if ( jpToRemove != null ) {
						for ( Plan p : jpToRemove.getIndividualPlans().values() ) {
							p.getPerson().removePlan( p );
						}
						jointPlans.removeJointPlan( jpToRemove );
					}
					else {
						person.removePlan( toRemove );
					}

					somethingDone = true;
				}
				// continue looping if still too many plans
				loop = loop || person.getPlans().size() > maxPlansPerAgent;
			}
		}
		if ( log.isTraceEnabled() && rem > 0 ) log.trace( "removed "+rem+" excedentary plans" );

		return somethingDone;
	}

	private List<? extends Plan> getSortedPlans( final Map<Id<Person>, List<? extends Plan>> sortedPlans, final Person person ) {
		if ( sortedPlans.containsKey( person.getId() ) ) return sortedPlans.get( person.getId() );
		// side effect. is it ok?
		// necessary to avoid having to remove plans here as well
		// works only if person returns internal instance
		final List<? extends Plan> plans = person.getPlans();
		Collections.sort( plans, new Comparator<Plan>() {
			@Override
			public int compare( final Plan o1, final Plan o2 ) {
				return Double.compare( o1.getScore() , o2.getScore() );
			}
		} );
		sortedPlans.put( person.getId() , plans );
		return plans;
	}

	private static Plan getWorstNonUniquelyIndividualPlan(
			final List<? extends Plan> plans,
			final JointPlans jointPlans) {
		Plan worst = null;
		Plan secondWorst = null;

		int nIndividualPlans = 0;
		for ( Plan plan : plans ) {
			final JointPlan jp = jointPlans.getJointPlan( plan );

			if ( isIndividual( jp ) ) nIndividualPlans++;

			if ( worst == null ) {
				if ( !isIndividual( jp ) || nIndividualPlans > 1 ) return plan;

				worst = plan;
			}
			else {
				secondWorst = plan;
				break;
			}
		}

		// if there is only one individual plan, do not consider it "worse"
		// assert nIndividualPlans > 0; // this assertion only makes sense if all persons have at least 1 individual plan
		return ( nIndividualPlans == 1 ) ? secondWorst : worst;
	}

	private static boolean isIndividual( final JointPlan jp ) {
		return jp == null || jp.getIndividualPlans().size() == 1;
	}

	private static int maxRank(final ReplanningGroup group) {
		int maxRank = 0;

		for ( Person p : group.getPersons() ) {
			maxRank = Math.max( maxRank , p.getPlans().size() );
		}

		return maxRank;
	}

	private static JointPlan identifyWorst(
			final int maxRank,
			final Collection<JointPlan> jointPlans) {
		final Collection<ValuedJointPlan> coll = new ArrayList<>();
		for ( JointPlan jp : jointPlans ) {
			coll.add(
					new ValuedJointPlan(
						maxRank,
						jp ) );
		}
		return Collections.min( coll ).jointPlan;
	}

	private final static Comparator<Plan> individualPlanComparator =
		new Comparator<Plan>() {
			@Override
			public int compare(
					final Plan o1,
					final Plan o2) {
				if ( o1.getScore() == null || o2.getScore() == null ) {
					throw new NullPointerException( "got plan with null score "+o1+" or "+o2 );
				}
				return Double.compare(
						o1.getScore(),
						o2.getScore() );
			}
		};
	private static Plan identifyWorst(
			final Collection<Plan> plans) {
		return Collections.min( plans , individualPlanComparator );
	}

	private static class ValuedJointPlan implements Comparable<ValuedJointPlan> {
		public final JointPlan jointPlan;
		private final int[] nPlansOfRank;

		public ValuedJointPlan(
				final int maxRank,
				final JointPlan jp) {
			this.jointPlan = jp;

			nPlansOfRank = new int[ maxRank ];
			for ( int i=0; i < nPlansOfRank.length; i++ ) {
				nPlansOfRank[ i ] = 0;
			}

			for ( Plan p : jp.getIndividualPlans().values() ) {
				nPlansOfRank[ rank( p ) ]++;
			}
		}

		private int rank(final Plan plan) {
			int nWorsePlans = 0;

			for ( Plan other : plan.getPerson().getPlans() ) {
				if ( other != plan && other.getScore() < plan.getScore() ) nWorsePlans++;

			}

			return nWorsePlans;
		}

		@Override
		public int compareTo(final ValuedJointPlan o) {
			assert nPlansOfRank.length == o.nPlansOfRank.length;

			for ( int rank = 0; rank < nPlansOfRank.length; rank++ ) {
				// less worst plans is better
				if ( nPlansOfRank[ rank ] < o.nPlansOfRank[ rank ] ) return 1;
				// more worst plans is worst
				if ( nPlansOfRank[ rank ] > o.nPlansOfRank[ rank ] ) return -1;
			}

			// all was the same
			return 0;
		}
	}

	private static final PlansPerComposition getPlansPerComposition(
			final JointPlans jointPlans,
			final Person person ) {
		final PlansPerComposition plans = new PlansPerComposition();

		for ( Plan plan : person.getPlans() ) {
			final JointPlan jp = jointPlans.getJointPlan( plan );

			if ( jp == null ) plans.addPlan( plan );
			else plans.addJointPlan( jp );
		}

		return plans;
	}

	private static class PlansPerComposition {
		private final Collection<Plan> individualPlans = new ArrayList<>();
		private final Map< Set<Id<Person>> , Collection<JointPlan> > jointPlans = new LinkedHashMap< >();

		public void addPlan( final Plan p ) {
			individualPlans.add( p );
		}

		public void addJointPlan( final JointPlan jp ) {
			MapUtils.getCollection(
					jp.getIndividualPlans().keySet(),
					jointPlans ).add( jp );
		}

		public Collection<Plan> getIndividualPlans() {
			return individualPlans;
		}

		public Collection< Collection<JointPlan> > getJointPlansGroupedPerStructure() {
			return jointPlans.values();
		}
	}
}

