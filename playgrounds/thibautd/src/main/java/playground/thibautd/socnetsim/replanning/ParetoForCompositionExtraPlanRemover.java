/* *********************************************************************** *
 * project: org.matsim.*
 * LimitedPerCompositionExtraPlanRemover.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.utils.MapUtils;

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
public class ParetoForCompositionExtraPlanRemover implements ExtraPlanRemover {
	private final int maxPlansPerComposition;

	public ParetoForCompositionExtraPlanRemover(
			final int maxPlansPerComposition) {
		this.maxPlansPerComposition = maxPlansPerComposition;
	}

	@Override
	public boolean removePlansInGroup(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		boolean somethingDone = false;

		final int maxRank = maxRank( group );
		for ( Person person : group.getPersons() ) {
			final PlansPerComposition plansPerComposition = getPlansPerComposition( jointPlans , person );

			if ( plansPerComposition.getIndividualPlans().size() > maxPlansPerComposition ) {
				assert plansPerComposition.getIndividualPlans().size() == maxPlansPerComposition + 1;
				((PersonImpl) person).removePlan( 
						identifyWorst( 
							plansPerComposition.getIndividualPlans() ) );
				somethingDone = true;
			}

			for ( Collection<JointPlan> structure : plansPerComposition.getJointPlansGroupedPerStructure() ) {
				if ( structure.size() > maxPlansPerComposition ) {
					assert structure.size() == maxPlansPerComposition + 1;
					final JointPlan toRemove = identifyWorst( maxRank , structure );

					for ( Plan p : toRemove.getIndividualPlans().values() ) {
						((PersonImpl) p.getPerson()).removePlan( p );
					}
					somethingDone = true;
				}
			}
		}

		return somethingDone;
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
		final Collection<ValuedJointPlan> coll = new ArrayList<ValuedJointPlan>();
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
			int nBetterPlans = 0;

			for ( Plan other : plan.getPerson().getPlans() ) {
				if ( other != plan && other.getScore() > plan.getScore() ) nBetterPlans++;

			}

			return nBetterPlans;
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
		private final Collection<Plan> individualPlans = new ArrayList<Plan>();
		private final Map< Set<Id> , Collection<JointPlan> > jointPlans = new LinkedHashMap< Set<Id> , Collection<JointPlan> >();

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

