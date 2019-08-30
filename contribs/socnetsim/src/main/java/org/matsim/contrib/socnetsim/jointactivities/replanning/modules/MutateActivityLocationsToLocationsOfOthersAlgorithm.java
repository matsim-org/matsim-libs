/* *********************************************************************** *
 * project: org.matsim.*
 * MutateActivityLocationsToLocationsOfOthersAlgorithm.java
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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.facilities.Facility;

import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.utils.ObjectPool;

/**
 * @author thibautd
 * @deprecated quick and dirty for experiments. Not a long-term solution.
 */
@Deprecated
public class MutateActivityLocationsToLocationsOfOthersAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final Random random;
	private final ChoiceSet choiceSet;

	public MutateActivityLocationsToLocationsOfOthersAlgorithm(
			final ChoiceSet choiceSet,
			final Random random) {
		this.random = random;
		this.choiceSet = choiceSet;
	}

	@Override
	public void run(final GroupPlans plans) {
		final List<Facility> groupChoiceSet = choiceSet.getGroupChoiceSet( plans );
		for ( Plan plan : plans.getAllIndividualPlans() ) {
			for ( Activity act : TripStructureUtils.getActivities( plan , StageActivityHandling.ExcludeStageActivities ) ) {
				assert act.getType().equals( choiceSet.type );
				final Facility choice = groupChoiceSet.get( random.nextInt( groupChoiceSet.size() ) );
				((Activity) act).setCoord( choice.getCoord() );
				((Activity) act).setLinkId( choice.getLinkId() );
				if ( choice instanceof Identifiable ) {
					( (Activity) act ).setFacilityId( ((Identifiable)choice).getId() );
				} else {
					throw new RuntimeException( Facility.FACILITY_NO_LONGER_IDENTIFIABLE ) ;
				}
			}
		}
	}

	private static class BasicFacility implements Facility, Identifiable {
		private final Coord coord;
		private final Id id;
		private final Id link;

		public BasicFacility(
				final Activity act) {
			this.coord = act.getCoord();
			this.id = act.getFacilityId();
			this.link = act.getLinkId();
		}

		@Override
		public Coord getCoord() {
			return coord;
		}

		@Override
		public Id getId() {
			return id;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return null;
		}

		@Override
		public Id getLinkId() {
			return link;
		}

		@Override
		public boolean equals( final Object other ) {
			return other instanceof BasicFacility &&
				areEqual( ((BasicFacility) other).coord , coord ) &&
				areEqual( ((BasicFacility) other).id , id ) &&
				areEqual( ((BasicFacility) other).link , link );
		}

		private static boolean areEqual(final Object o1, final Object o2) {
			return o1 == null ? o2 == null : o1.equals( o2 );
		}

		@Override
		public int hashCode() {
			return (coord != null ? coord.hashCode() : 0) +
				(id != null ? id.hashCode() : 0) +
				(coord != null ? coord.hashCode() : 0);
		}
	}

	public static class ChoiceSet {
		private final Map<Id, Set<Facility>> choiceSetPerPerson;
		private final String type;
		// TODO: This looks awkward. Not clear whether we can really assume that this in practice really identified only activity 
		// types which end on "interaction" or whether we now miss some. In socnetsim all stage activity types are renamed to end
		// on "interaction"
//		private final StageActivityTypes filter = new StageActivityTypes() {
//			@Override
//			public boolean isStageActivity(final String t) {
//				return !t.equals( type );
//			}
//		};

		public ChoiceSet(
				final Population population,
				final String type) {
			this.type = type;

			// XXX this is ugly! Long term solution would be to use Andreas LC
			final Map<Id, Set<Facility>> map = new HashMap<Id, Set<Facility>>();
			final ObjectPool<Facility> pool = new ObjectPool<Facility>();
			for ( Person person : population.getPersons().values() ) {
				final Set<Facility> facilities = new HashSet<Facility>();
				map.put( person.getId() , facilities );
				for ( Plan plan : person.getPlans() ) {
					for ( Activity act : TripStructureUtils.getActivities( plan , StageActivityHandling.ExcludeStageActivities ) ) {
						assert act.getType().equals( type );
						facilities.add(
								pool.getPooledInstance( new BasicFacility( act ) ) );
					}
				}
			}
			this.choiceSetPerPerson = Collections.unmodifiableMap( map );
		}

		private List<Facility> getGroupChoiceSet(final GroupPlans plans) {
			final Set<Facility> facilities = new HashSet<Facility>();

			for ( Plan p : plans.getAllIndividualPlans() ) {
				facilities.addAll( choiceSetPerPerson.get( p.getPerson().getId() ) );
			}

			final List<Facility> list = new ArrayList<Facility>( facilities );
			// for determinism
			Collections.sort( list , new Comparator<Facility>() {
				@Override
				public int compare(final Facility o1, final Facility o2) {
					if ( o1 instanceof Identifiable && o2 instanceof Identifiable ) {
						return ((Identifiable) o1).getId().compareTo( ((Identifiable) o2).getId() );
					} else {
						throw new RuntimeException( Facility.FACILITY_NO_LONGER_IDENTIFIABLE ) ;
					}
				}
			});
			return list;
		}
	}
}

