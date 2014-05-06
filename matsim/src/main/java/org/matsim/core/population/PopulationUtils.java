/* *********************************************************************** *
 * project: matsim
 * PopulationUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.population;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;

/**
 * @author nagel
 */
public final class PopulationUtils {
	/**
	 * Is a namespace, so don't instantiate:
	 */
	private PopulationUtils() {}
	
	public static Population createPopulation( Config config ) {
		return new PopulationImpl( config ) ; 
	}
	public static Population createPopulation( Config config, Network network ) {
		// yy here, the "network" lookup could be pushed into the create method: createRoute(...,network) ;
		return new PopulationImpl( config, network ) ;
	}
	
	public static Leg unmodifiableLeg( Leg leg ) {
		return new UnmodifiableLeg( leg ) ;
	}
	public static class UnmodifiableLeg implements Leg {
		private final Leg delegate ;
		public UnmodifiableLeg( Leg leg ) {
			this.delegate = leg ;
		}
		@Override
		public String getMode() {
			return this.delegate.getMode() ;
		}

		@Override
		public void setMode(String mode) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Route getRoute() {
			// route should be unmodifiable. kai
			return this.delegate.getRoute() ;
		}

		@Override
		public void setRoute(Route route) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public double getDepartureTime() {
			return this.delegate.getDepartureTime() ;
		}

		@Override
		public void setDepartureTime(double seconds) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public double getTravelTime() {
			return this.delegate.getTravelTime() ;
		}

		@Override
		public void setTravelTime(double seconds) {
			throw new UnsupportedOperationException() ;
		}
		
	}
	
	public static Activity unmodifiableActivity( Activity act ) {
		return new UnmodifiableActivity( act ) ;
	}
	public static class UnmodifiableActivity implements Activity {
		private final Activity delegate ;
		public UnmodifiableActivity( Activity act ) {
			this.delegate = act ;
		}

		@Override
		public double getEndTime() {
			return this.delegate.getEndTime() ;
		}

		@Override
		public void setEndTime(double seconds) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public String getType() {
			return this.delegate.getType() ;
		}

		@Override
		public void setType(String type) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Coord getCoord() {
			return this.delegate.getCoord() ;
		}

		@Override
		public double getStartTime() {
			return this.delegate.getStartTime() ;
		}

		@Override
		public void setStartTime(double seconds) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public double getMaximumDuration() {
			return this.delegate.getMaximumDuration() ;
		}

		@Override
		public void setMaximumDuration(double seconds) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Id getLinkId() {
			return this.delegate.getLinkId() ;
		}

		@Override
		public Id getFacilityId() {
			return this.delegate.getFacilityId() ;
		}
	}

	/**
	 * The idea of this method is to mirror the concept of Collections.unmodifiableXxx( xxx ) .
	 * At this point, the protection does not go to the end, i.e. PlanElements themselves can
	 * still be modified.  kai, nov'10
	 * <p/>
	 * @author nagel
	 */
	public static Plan unmodifiablePlan(Plan plan) {
		return new UnmodifiablePlan(plan);
	}

	public static class UnmodifiablePlan implements Plan {
		private final Plan delegate;
		private final List<PlanElement> unmodifiablePlanElements;
		
		public UnmodifiablePlan( Plan plan ) {
			this.delegate = plan;
			List<PlanElement> tmp = new ArrayList<PlanElement>() ;
			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Leg ) {
					tmp.add( unmodifiableLeg( (Leg) pe ) ) ;
				}
			}
			
			this.unmodifiablePlanElements = Collections.unmodifiableList(delegate.getPlanElements());
		}

		@Override
		public void addActivity(Activity act) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void addLeg(Leg leg) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return delegate.getCustomAttributes();
		}

		@Override
		public Person getPerson() {
			return delegate.getPerson();
		}

		@Override
		public List<PlanElement> getPlanElements() {
			return this.unmodifiablePlanElements;
		}

		@Override
		public Double getScore() {
			return delegate.getScore();
		}

		@Override
		public boolean isSelected() {
			return delegate.isSelected();
		}

		@Override
		public void setPerson(Person person) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void setScore(Double score) {
			throw new UnsupportedOperationException() ;
		}

	}

	/**
	 * @param population
	 * @return sorted map containing containing the persons as values and their ids as keys.
	 */
	public static SortedMap<Id, Person> getSortedPersons(final Population population) {
		return new TreeMap<Id, Person>(population.getPersons());
	}
	
	/**
	 * Sorts the person in the given population. 
	 * @param population 
	 */
	@SuppressWarnings("unchecked")
	public static void sortPersons(final Population population) {
		Map<Id, Person> map = (Map<Id, Person>) population.getPersons();
		
		if (map instanceof SortedMap) return;
		
		Map<Id, Person> treeMap = new TreeMap<Id, Person>(map);
		map.clear();
		map.putAll(treeMap);
	}

	/**
	 * Convenience method to compute (expected or planned) activity end time, depending on the different time interpretations.
	 * <p/>
	 * Design comments:<ul>
	 * <li> The whole Config is part of the argument, since it may eventually make sense to move the config parameter from VspExperimental to
	 * some more regular place.  kai, jan'13
	 * </ul>
	 */
	public static double getActivityEndTime( Activity act, double now, Config config ) {
		switch ( config.plans().getActivityDurationInterpretation() ) {
		case endTimeOnly:
			return act.getEndTime() ;
		case tryEndTimeThenDuration:
			if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
				return act.getEndTime() ;
			} else if ( act.getMaximumDuration() != Time.UNDEFINED_TIME ) {
				return now + act.getMaximumDuration() ;
			} else {
				return Time.UNDEFINED_TIME ;
			}
		case minOfDurationAndEndTime:
			return Math.min( now + act.getMaximumDuration() , act.getEndTime() ) ;
		}
		return Time.UNDEFINED_TIME ;
	}

	/**
	 * A pointer to material in TripStructureUtils
	 * 
	 * @param plan
	 * @param stageActivities
	 * @return
	 */
	public static List<Activity> getActivities( Plan plan, StageActivityTypes stageActivities ) {
		return TripStructureUtils.getActivities(plan, stageActivities ) ;
	}

	/**
	 * A pointer to material in TripStructureUtils
	 * 
	 * @param plan
	 * @return
	 */
	public static List<Leg> getLegs( Plan plan ) {
		return TripStructureUtils.getLegs( plan ) ;
	}

	/**
	 * Notes:<ul>
	 * <li>not normalized (for the time being?)
	 * <li>does not look at times (for the time being?)
	 * </ul>
	 * 
	 * @param legs1
	 * @param legs2
	 * @param network
	 * @param sameModeReward
	 * @param sameRouteReward
	 * @return
	 */
	public static double calculateSimilarity(List<Leg> legs1, List<Leg> legs2, Network network, 
			double sameModeReward, double sameRouteReward ) {
		// yy this is a bit at the limit of where a static method makes sense. kai, nov'13
		double simil = 0. ;
		Iterator<Leg> it1 = legs1.iterator();
		Iterator<Leg> it2 = legs2.iterator();
		for ( ; it1.hasNext() && it2.hasNext(); ) {
			Leg leg1 = it1.next() ;
			Leg leg2 = it2.next() ;
			if ( leg1.getMode().equals( leg2.getMode() ) ) {
				simil += sameModeReward ;
			}
			// the easy way for the route is to not go along the links but just check for overlap.
			Route route1 = leg1.getRoute() ;
			Route route2 = leg2.getRoute() ;
			// currently only for network routes:
			NetworkRoute nr1, nr2 ;
			if ( route1 instanceof NetworkRoute ) {
				nr1 = (NetworkRoute) route1 ;
			} else {
				continue ; // next leg
			}
			if ( route1 instanceof NetworkRoute ) {
				nr2 = (NetworkRoute) route2 ;
			} else {
				continue ; // next leg
			}
			simil += sameRouteReward * RouteUtils.calculateCoverage(nr1, nr2, network) ;
		}
		return simil ;
	}

	/**
	 * Notes:<ul>
	 * <li> not normalized (for the time being?)
	 * </ul>
	 * @param activities1
	 * @param activities2
	 * @param sameActivityTypeReward
	 * @param sameActivityLocationReward
	 * @param differentTimePenalty_s TODO
	 * @return
	 */
	public static double calculateSimilarity(List<Activity> activities1, List<Activity> activities2, double sameActivityTypeReward, 
			double sameActivityLocationReward, double differentTimePenalty_s ) {
		double simil = 0. ;
		Iterator<Activity> it1 = activities1.iterator() ;
		Iterator<Activity> it2 = activities2.iterator() ;
		for ( ; it1.hasNext() && it2.hasNext() ; ) {
			Activity act1 = it1.next() ;
			Activity act2 = it2.next() ;
			if ( act1.getType().equals(act2.getType() ) ) {
				simil += sameActivityTypeReward ;
			}
			if ( act1.getCoord().equals( act2.getCoord() ) ){ 
				simil += sameActivityLocationReward ;
			}
			double delta = act1.getEndTime() - act2.getEndTime() ;
			double penalty = Math.abs(delta) * differentTimePenalty_s ;
			simil -= penalty ;
			
		}
		return simil ;
	}
	
	/**
	 * Compares two Populations by serializing them to XML with the current writer
	 * and comparing their XML form byte by byte.
	 * A bit like comparing checksums of files,
	 * but regression tests won't fail just because the serialization changes.
	 * Limitation: If one of the PopulationWriters throws an Exception,
	 * this will go unnoticed (this method will just return true or false,
	 * probably false, except if both Writers have written the exact same text
	 * until the Exception happens).
	 */
	public static boolean equalPopulation(final Population s1, final Population s2) {
		try {
			InputStream inputStream1 = null;
			InputStream inputStream2 = null;
			try {
				inputStream1 = openPopulationInputStream(s1);
				inputStream2 = openPopulationInputStream(s2);
				return IOUtils.isEqual(inputStream1, inputStream2);
			} finally {
				if (inputStream1 != null) inputStream1.close();
				if (inputStream2 != null) inputStream2.close();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * The InputStream which comes from this method must be properly
	 * resource-managed, i.e. always be closed.
	 * 
	 * Otherwise, the Thread which is opened here may stay alive.
	 */
	private static InputStream openPopulationInputStream(final Population s1) {
		try {
			final PipedInputStream in = new PipedInputStream();
			final PipedOutputStream out = new PipedOutputStream(in);
			new Thread(new Runnable() {
				public void run() {
					final PopulationWriter writer = new PopulationWriter(s1);
					try {
						writer.write(out);
					} catch (UncheckedIOException e) {
						// Writer will throw an IOException when pipe is closed from the other side (like "broken pipe" in UNIX)
						// This is expected. Don't even log anything.
						// Other exceptions (from the Writer) are not caught
						// but written to the console.
					}
				}
			}).start();
			return in;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
}
