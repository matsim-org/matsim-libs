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
package playground.thibautd.socnetsim.framework.replanning.removers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import playground.ivt.utils.MapUtils;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;

import java.util.*;

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
	private final Random random;
	private final int maxPlansPerComposition;
	private final int maxPlansPerAgent;

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
		boolean somethingDone = false;

		// check condition per composition
		final int maxRank = maxRank( group );
		for ( Person person : group.getPersons() ) {
			final PlansPerComposition plansPerComposition = getPlansPerComposition( jointPlans , person );

			// too many individual plans?
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
					jointPlans.removeJointPlan( toRemove );
					somethingDone = true;
				}
			}
		}

		// now that maxima per composition are processed, check whether
		// there are still agents with too many plans. This is mainly to
		// avoid memory consumption getting out of control, but the number should
		// be large, as this way of doing is problematic (it does not consider conflicts
		// at all).
		// randomize order to avoid the existence of "dictators" (agent for which their
		// worst plan is always removed, whatever the ranking of the others is)
		final List<Person> persons = new ArrayList<Person>( group.getPersons() );
		Collections.shuffle( persons , random );
		for ( Person person : persons ) {
			if ( person.getPlans().size() > maxPlansPerAgent ) {
				assert person.getPlans().size() == maxPlansPerAgent + 1;
				final Plan toRemove = getWorstNonUniquelyIndividualPlan( person.getPlans() , jointPlans );
				final JointPlan jpToRemove = jointPlans.getJointPlan( toRemove );

				if ( jpToRemove != null ) {
					for ( Plan p : jpToRemove.getIndividualPlans().values() ) {
						((PersonImpl) p.getPerson()).removePlan( p );
					}
					jointPlans.removeJointPlan( jpToRemove );
				}
				else {
					((PersonImpl) person).removePlan( toRemove );
				}

				somethingDone = true;
			}
		}

		return somethingDone;
	}

	private static Plan getWorstNonUniquelyIndividualPlan(
			final List<? extends Plan> plans,
			final JointPlans jointPlans) {
		boolean worstIsIndividual = false;
		Plan worst = null;
		Plan secondWorst = null;

		int nIndividualPlans = 0;
		for ( Plan plan : plans ) {
			final JointPlan jp = jointPlans.getJointPlan( plan );

			if ( jp == null ) nIndividualPlans++;

			if ( worst == null || plan.getScore() < worst.getScore() ) {
				secondWorst = worst;
				worst = plan;
				worstIsIndividual = jp == null;
			}
			else if ( secondWorst == null || plan.getScore() < secondWorst.getScore() ) {
				secondWorst = plan;
			}
		}

		// if there is only one individual plan, do not consider it "worse"
		assert nIndividualPlans > 0;
		return ( nIndividualPlans == 1 && worstIsIndividual ) ? secondWorst : worst;
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
		private final Collection<Plan> individualPlans = new ArrayList<Plan>();
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

