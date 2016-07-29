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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.routes.CompressedNetworkRouteFactory;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author nagel, ikaddoura
 */
public final class PopulationUtils {
	private static final Logger log = Logger.getLogger( PopulationUtils.class );
	private static final PopulationFactory populationFactory = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory() ;

	/**
	 * Is a namespace, so don't instantiate:
	 */
	private PopulationUtils() {}

    /**
     *
     * Creates a new Population container. Population instances need a Config, because they need to know
     * about the modes of transport.
     *
     * @param config the configuration which is used to create the Population.
     * @return the new Population instance
     */
	public static Population createPopulation(Config config) {
		return createPopulation(config, null);
	}

    /**
     *
     * Creates a new Population container which, depending on
     * configuration, may make use of the specified Network instance to store routes
     * more efficiently.
     *
     * @param config the configuration which is used to create the Population.
     * @param network the Network to which Plans in this Population will refer.
     * @return the new Population instance
     */
	public static Population createPopulation(Config config, Network network) {
		return createPopulation(config.plans(), network);
	}

	public static Population createPopulation(PlansConfigGroup plansConfigGroup, Network network) {
		// yyyy my intuition would be to rather get this out of a standard scenario. kai, jun'16
		RouteFactories routeFactory = new RouteFactories();
		String networkRouteType = plansConfigGroup.getNetworkRouteType();
		RouteFactory factory;
		if (PlansConfigGroup.NetworkRouteType.LinkNetworkRoute.equals(networkRouteType)) {
			factory = new LinkNetworkRouteFactory();
		} else if (PlansConfigGroup.NetworkRouteType.CompressedNetworkRoute.equals(networkRouteType) && network != null) {
			factory = new CompressedNetworkRouteFactory(network);
		} else {
			throw new IllegalArgumentException("The type \"" + networkRouteType + "\" is not a supported type for network routes.");
		}
		routeFactory.setRouteFactory(NetworkRoute.class, factory);
		return new PopulationImpl(new PopulationFactoryImpl(routeFactory));
	}
	
//	public static Population createStreamingPopulation(PlansConfigGroup plansConfigGroup, Network network) {
//		// yyyy my intuition would be to rather get this out of a standard scenario. kai, jun'16
//		RouteFactories routeFactory = new RouteFactories();
//		String networkRouteType = plansConfigGroup.getNetworkRouteType();
//		RouteFactory factory;
//		if (PlansConfigGroup.NetworkRouteType.LinkNetworkRoute.equals(networkRouteType)) {
//			factory = new LinkNetworkRouteFactory();
//		} else if (PlansConfigGroup.NetworkRouteType.CompressedNetworkRoute.equals(networkRouteType) && network != null) {
//			factory = new CompressedNetworkRouteFactory(network);
//		} else {
//			throw new IllegalArgumentException("The type \"" + networkRouteType + "\" is not a supported type for network routes.");
//		}
//		routeFactory.setRouteFactory(NetworkRoute.class, factory);
//		return new Population(new PopulationFactoryImpl(routeFactory));
//	}

	public static Leg unmodifiableLeg( Leg leg ) {
		return new UnmodifiableLeg( leg ) ;
	}

	static class UnmodifiableLeg implements Leg {
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
		@Override
		public String toString() {
			return this.delegate.toString() ;
		}
	}
	
	public static Activity unmodifiableActivity( Activity act ) {
		return new UnmodifiableActivity( act ) ;
	}

	static class UnmodifiableActivity implements Activity {
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
		public Id<Link> getLinkId() {
			return this.delegate.getLinkId() ;
		}

		@Override
		public Id<ActivityFacility> getFacilityId() {
			return this.delegate.getFacilityId() ;
		}
		@Override
		public String toString() {
			return this.delegate.toString() ;
		}

		@Override
		public void setLinkId(Id<Link> id) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void setFacilityId(Id<ActivityFacility> id) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void setCoord(Coord coord) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

	}

	/**
	 * The idea of this method is to mirror the concept of Collections.unmodifiableXxx( xxx ) .
	 * <p/>
	 */
	public static Plan unmodifiablePlan(Plan plan) {
		return new UnmodifiablePlan(plan);
	}

	static class UnmodifiablePlan implements Plan {
		private final Plan delegate;
		private final List<PlanElement> unmodifiablePlanElements;
		
		public UnmodifiablePlan( Plan plan ) {
			this.delegate = plan;
			List<PlanElement> tmp = new ArrayList<>() ;
			for ( PlanElement pe : plan.getPlanElements() ) {
				if (pe instanceof Activity) {
                    tmp.add(unmodifiableActivity((Activity) pe));
                } else if (pe instanceof Leg) {
					tmp.add(unmodifiableLeg((Leg) pe));
				}
			}
			this.unmodifiablePlanElements = Collections.unmodifiableList(tmp);
		}

		@Override
		public void addActivity(Activity act) {
			throw new UnsupportedOperationException() ;
		}

        @Override
        public String getType() {
            return delegate.getType();
        }

        @Override
        public void setType(String type) {
            throw new UnsupportedOperationException();
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
		public void setPerson(Person person) {
			throw new UnsupportedOperationException() ;
		}

		@Override
		public void setScore(Double score) {
			throw new UnsupportedOperationException() ;
		}
	}

	/**
	 * @return sorted map containing containing the persons as values and their ids as keys.
	 */
	public static SortedMap<Id<Person>, Person> getSortedPersons(final Population population) {
		return new TreeMap<>(population.getPersons());
	}
	
	/**
	 * Sorts the persons in the given population.
	 */
	@SuppressWarnings("unchecked")
	public static void sortPersons(final Population population) {
		Map<Id<Person>, Person> map = (Map<Id<Person>, Person>) population.getPersons();
		
		if (map instanceof SortedMap) return;
		
		Map<Id<Person>, Person> treeMap = new TreeMap<>(map);
		map.clear();
		map.putAll(treeMap);
	}

	/**
	 * Computes the (expected or planned) activity end time, depending on the configured time interpretation.
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
		default:
			break ;
		}
		return Time.UNDEFINED_TIME ;
	}

	private static int missingFacilityCnt = 0 ;
	/**
	 * @param config  
	 */
	public static Id<Link> computeLinkIdFromActivity( Activity act, ActivityFacilities facs, Config config ) {
		// the following might eventually become configurable by config. kai, feb'16
		if ( act.getFacilityId()==null ) {
			final Id<Link> linkIdFromActivity = act.getLinkId();
			Gbl.assertNotNull( linkIdFromActivity );
			return linkIdFromActivity ;
		} else {
			ActivityFacility facility = facs.getFacilities().get( act.getFacilityId() ) ;
			if ( facility==null || facility.getLinkId()==null ) {
				if ( facility==null ) {
					if ( missingFacilityCnt < 10 ) {
						log.warn("we have a facility ID for an activity, but can't find the facility; this should not really happen. Falling back on link ID.") ;
						missingFacilityCnt++ ;
						if ( missingFacilityCnt==10 ) {
							log.warn( Gbl.FUTURE_SUPPRESSED ) ;
						}
					}
				}
				final Id<Link> linkIdFromActivity = act.getLinkId();
				Gbl.assertIf( linkIdFromActivity!=null );
				return linkIdFromActivity ;
			} else {
				return facility.getLinkId() ;
			} 
			// yy sorry about this mess, I am just trying to make explicit which seems to have been the logic so far implicitly.  kai, feb'16
		}
	}

	/**
	 * @param config  
	 */
	public static Coord computeCoordFromActivity( Activity act, ActivityFacilities facs, Config config ) {
		// the following might eventually become configurable by config. kai, feb'16
		if ( act.getFacilityId()==null ) {
			return act.getCoord() ; // if not available, fall back on coord of link?
		} else {
			Gbl.assertIf( facs!=null ) ;
			ActivityFacility facility = facs.getFacilities().get( act.getFacilityId() ) ;
			Gbl.assertIf( facility!=null );
			return facility.getCoord() ;
		}
	}
	
	/**
	 * A pointer to material in TripStructureUtils
	 *
	 */
	public static List<Activity> getActivities( Plan plan, StageActivityTypes stageActivities ) {
		return TripStructureUtils.getActivities(plan, stageActivities ) ;
	}

	/**
	 * A pointer to material in TripStructureUtils
	 *
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
	 */
	public static double calculateSimilarity(List<Leg> legs1, List<Leg> legs2, Network network, 
			double sameModeReward, double sameRouteReward ) {
		// yyyy should be made configurable somehow (i.e. possibly not a static method any more).  kai, apr'15

		// yy kwa points to: 
		// Schüssler, N. and K.W. Axhausen (2009b) Accounting for similarities in destination choice modelling: A concept, paper presented at the 9th Swiss Transport Research Conference, Ascona, October 2009.
		//		und 
		//  Joh, Chang-Hyeon, Theo A. Arentze and Harry J. P. Timmermans (2001). 
		// A Position-Sensitive Sequence Alignment Method Illustrated for Space-Time Activity-Diary Data¹, Environment and Planning A 33(2): 313­338.

		// Mahdieh Allahviranloo has some work on activity pattern similarity (iatbr'15)

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
			if ( route2 instanceof NetworkRoute ) {
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
	 */
	public static double calculateSimilarity(List<Activity> activities1, List<Activity> activities2, double sameActivityTypePenalty, 
			double sameActivityLocationPenalty, double actTimeParameter ) {
		// yyyy should be made configurable somehow (i.e. possibly not a static method any more).  kai, apr'15

		// yy kwa points to: 
		// Schüssler, N. and K.W. Axhausen (2009b) Accounting for similarities in destination choice modelling: A concept, paper presented at the 9th Swiss Transport Research Conference, Ascona, October 2009.
		//		und 
		//  Joh, Chang-Hyeon, Theo A. Arentze and Harry J. P. Timmermans (2001). 
		// A Position-Sensitive Sequence Alignment Method Illustrated for Space-Time Activity-Diary Data¹, Environment and Planning A 33(2): 313­338.
		
		// Mahdieh Allahviranloo has some work on activity pattern similarity (iatbr'15)

		double simil = 0. ;
		Iterator<Activity> it1 = activities1.iterator() ;
		Iterator<Activity> it2 = activities2.iterator() ;
		for ( ; it1.hasNext() && it2.hasNext() ; ) {
			Activity act1 = it1.next() ;
			Activity act2 = it2.next() ;
			
			// activity type
			if ( act1.getType().equals(act2.getType() ) ) {
				simil += sameActivityTypePenalty ;
			}
			
			// activity location
			if ( act1.getCoord().equals( act2.getCoord() ) ){ 
				simil += sameActivityLocationPenalty ;
			}
			
			// activity end times
			if ( Double.isInfinite( act1.getEndTime() ) && Double.isInfinite( act2.getEndTime() ) ){
				// both activities have no end time, no need to compute a similarity penalty
			} else {
				// both activities have an end time, comparing the end times
				
				// 300/ln(2) means a penalty of 0.5 for 300 sec difference
				double delta = Math.abs(act1.getEndTime() - act2.getEndTime()) ;
				simil += actTimeParameter * Math.exp( - delta/(300/Math.log(2)) ) ;
			}
			
		}
		
		// a positive value is interpreted as a penalty
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
			@SuppressWarnings("resource")
			InputStream inputStream1 = null;
			@SuppressWarnings("resource")
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
	@SuppressWarnings("resource")
	private static InputStream openPopulationInputStream(final Population s1) {
		try {
			final PipedInputStream in = new PipedInputStream();
			final PipedOutputStream out = new PipedOutputStream(in);
			new Thread(new Runnable() {
				@Override
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

	public static Activity getFirstActivityAfterLastCarLegOfDay(Plan plan){
		List<PlanElement> planElements = plan.getPlanElements();
		int indexOfLastCarLegOfDay=-1;
		for (int i=planElements.size()-1;i>=0;i--){
			if (planElements.get(i) instanceof Leg){
				Leg leg = (Leg) planElements.get(i);
	
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
					indexOfLastCarLegOfDay=i;
					break;
				}
	
			}
		}
	
		for (int i=indexOfLastCarLegOfDay+1;i<planElements.size();i++){
			if (planElements.get(i) instanceof Activity){
				return (Activity) planElements.get(i);
			}
		}
		return null;
	}

	public static Activity getFirstActivityOfDayBeforeDepartingWithCar(Plan plan){
		List<PlanElement> planElements = plan.getPlanElements();
		int indexOfFirstCarLegOfDay=-1;
		for (int i=0;i<planElements.size();i++){
			if (planElements.get(i) instanceof Leg){
				Leg leg= (Leg) planElements.get(i);
	
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
					indexOfFirstCarLegOfDay=i;
					break;
				}
	
			}
		}
		for (int i=indexOfFirstCarLegOfDay-1;i>=0;i--){
			if (planElements.get(i) instanceof Activity){
				return (Activity) planElements.get(i);
			}
		}
		return null;
	}

	public static boolean hasCarLeg(Plan plan){
		List<PlanElement> planElements = plan.getPlanElements();
		for (int i=0;i<planElements.size();i++){
			if (planElements.get(i) instanceof Leg){
				Leg Leg= (Leg) planElements.get(i);
	
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return true;
				}
	
			}
		}
		return false;
	}
	public static PopulationFactory getFactory() {
		// to make this private, would have to get rid of things like getFactory().createPerson(..) .
		// But I am not sure if this really makes a lot of sense, because this static default factory is easier to change into
		// pop.getFactory() than anothing else.  kai, jun'16
		
//		Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() ) ;
//		return scenario.getPopulation().getFactory() ;
		// the above is too slow. kai, jun'16
		
//		return new PopulationFactoryImpl( new RouteFactoryImpl() ) ;
		return populationFactory ;
	}
	
	// --- plain factories: 

	public static Plan createPlan(Person person) {
		Plan plan = getFactory().createPlan() ;
		plan.setPerson(person);
		return plan ;
	}

	public static Plan createPlan() {
		return getFactory().createPlan() ;
	}

	public static Activity createActivityFromLinkId(String type, Id<Link> linkId) {
		return getFactory().createActivityFromLinkId(type, linkId) ;
	}

	public static Activity createActivityFromCoord(String type, Coord coord) {
		return getFactory().createActivityFromCoord(type, coord) ;
	}

	public static Activity createActivityFromCoordAndLinkId(String type, Coord coord, Id<Link> linkId) {
		Activity act = getFactory().createActivityFromCoord(type, coord) ;
		act.setLinkId(linkId);
		return act ;
	}
	
	public static Leg createLeg(String transportMode) {
		return getFactory().createLeg(transportMode) ;
	}

	// createAndAdd methods:
	
	public static Activity createAndAddActivityFromCoord( Plan plan, String type, Coord coord ) {
		Activity act = getFactory().createActivityFromCoord(type, coord) ;
		plan.addActivity(act);
		act.setCoord(coord);
		return act ;
	}
	public static Activity createAndAddActivityFromLinkId( Plan plan, String type, Id<Link> linkId ) {
		Activity act = getFactory().createActivityFromLinkId(type, linkId) ;
		plan.addActivity(act);
		act.setLinkId(linkId);
		return act ;
	}

	public static Leg createAndAddLeg(Plan plan, String mode) {
		verifyCreateLeg( plan ) ;
		Leg leg = getFactory().createLeg(mode) ;
		plan.addLeg( leg );
		return leg ;
	}
	private static void verifyCreateLeg(Plan plan) throws IllegalStateException {
		if (plan.getPlanElements().size() == 0) {
			throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
		}
	}

	public static Activity createAndAddActivity(Plan plan, String type) {
		Activity act = new ActivityImpl( type ) ;
		// (can't do this from the factory since factory method only exists with coord or with linkId. kai, jun'16)
		plan.addActivity(act);
		return act ;
	}
	

	// --- static copy methods:

	/** loads a copy of an existing plan, but keeps the person reference
	 * 
	 * @param in a plan who's data will be loaded into this plan
	 * @param out 
	 **/
	public static void copyFromTo(final Plan in, Plan out) {
		out.getPlanElements().clear();
		out.setScore(in.getScore());
		out.setType(in.getType());
		for (PlanElement pe : in.getPlanElements()) {
			if (pe instanceof Activity) {
				out.getPlanElements().add(createActivity((Activity) pe));
			} else if (pe instanceof Leg) {
				out.getPlanElements().add( createLeg( (Leg) pe ) ) ;
			} else {
				throw new IllegalArgumentException("unrecognized plan element type discovered");
			}
		}
	}

	public static void copyFromTo(Leg in, Leg out) {
		out.setMode( in.getMode() );
		out.setDepartureTime(in.getDepartureTime());
		out.setTravelTime(in.getTravelTime());
		if (in.getRoute() != null) {
			out.setRoute(in.getRoute().clone());
		}
	}
	
	public static void copyFromTo(Activity act, Activity newAct) {
		Coord coord = act.getCoord() == null ? null : new Coord(act.getCoord().getX(), act.getCoord().getY());
		// (we don't want to copy the coord ref, but rather the contents!)
		newAct.setCoord(coord);
		newAct.setType( act.getType() );
		newAct.setLinkId(act.getLinkId());
		newAct.setStartTime(act.getStartTime());
		newAct.setEndTime(act.getEndTime());
		newAct.setMaximumDuration(act.getMaximumDuration());
		newAct.setFacilityId(act.getFacilityId());
	}
	
	// --- copy factories:

	public static Activity createActivity(Activity act) {
		Activity newAct = getFactory().createActivityFromLinkId(act.getType(), act.getLinkId()) ;
		
		copyFromTo(act, newAct);
		// (this ends up setting type and linkId again)
		
		return newAct ;
	}


	/**
	 * Makes a deep copy of this leg, however only when the Leg has a route which is
	 * instance of Route or BasicRoute. Other route instances are not considered. 
	 * </p>
	 * <ul>
	 * <li> Is the statement about the route still correct?  kai, jun'16
	 * </ul>
	 * @param leg
	 */
	public static Leg createLeg(Leg leg) {
		Leg newLeg = createLeg( leg.getMode() ) ;

		copyFromTo( leg, newLeg ) ;
		// (this ends up setting mode again)

		return newLeg ;
	}
	
	// --- positional methods:

	public static Activity getFirstActivity(Plan plan) {
		return (Activity) plan.getPlanElements().get(0);
	}
	public static Activity getLastActivity( Plan plan ) {
		return (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
	}
	public static Activity getNextActivity( Plan plan, Leg leg ) {
		int index = getActLegIndex(plan,leg);
		if (index != -1) {
			return (Activity) plan.getPlanElements().get(index+1);
		}
		return null;
	}
	public static int getActLegIndex( Plan plan , PlanElement pe ) {
		if ((pe instanceof Leg) || (pe instanceof Activity)) {
			for (int i = 0; i < plan.getPlanElements().size(); i++) {
				if (plan.getPlanElements().get(i).equals(pe)) {
					return i;
				}
			}
			return -1;
		}
		throw new IllegalArgumentException("Method call only valid with a Leg or Act instance as parameter!");
	}
	public static Leg getNextLeg( Plan plan, Activity act ) {
		int index = PopulationUtils.getActLegIndex(plan, act);
		if ((index < plan.getPlanElements().size() - 1) && (index != -1)) {
			return (Leg) plan.getPlanElements().get(index+1);
		}
		return null;
	}
	public static Activity getPreviousActivity( Plan plan, Leg leg ) {
		int index = PopulationUtils.getActLegIndex(plan, leg);
		if (index != -1) {
			return (Activity) plan.getPlanElements().get(index-1);
		}
		return null;
	}
	public static Leg getPreviousLeg( Plan plan, Activity act ) {
		int index = PopulationUtils.getActLegIndex(plan, act);
		if (index != -1) {
			return (Leg) plan.getPlanElements().get(index-1);
		}
		return null;
	}
	
	// --- remove methods:

	/**
	 * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
	 * the following leg will be emptied to keep consistency (i.e. for the route)
	 * @param index
	 */
	public static void removeLeg( Plan plan, int index) {
		if ((index % 2 == 0) || (index < 1) || (index >= plan.getPlanElements().size()-1)) {
			log.warn(plan + "[index=" + index +" is wrong. nothing removed]");
		}
		else {
			if (index != plan.getPlanElements().size()-2) {
				// not the last leg
				Leg next_leg = (Leg)plan.getPlanElements().get(index+2);
				next_leg.setDepartureTime(Time.UNDEFINED_TIME);
				next_leg.setTravelTime(Time.UNDEFINED_TIME);
				next_leg.setRoute(null);
			}
			plan.getPlanElements().remove(index+1); // following act
			plan.getPlanElements().remove(index); // leg
		}

	}
	public static void removeActivity( Plan plan, int index ) {
		if ((index % 2 != 0) || (index < 0) || (index > plan.getPlanElements().size()-1)) {
			log.warn(plan + "[index=" + index +" is wrong. nothing removed]");
		}
		else if (plan.getPlanElements().size() == 1) {
			log.warn(plan + "[index=" + index +" only one act. nothing removed]");
		}
		else {
			if (index == 0) {
				// remove first act and first leg
				plan.getPlanElements().remove(index+1); // following leg
				plan.getPlanElements().remove(index); // act
			}
			else if (index == plan.getPlanElements().size()-1) {
				// remove last act and last leg
				plan.getPlanElements().remove(index); // act
				plan.getPlanElements().remove(index-1); // previous leg
			}
			else {
				// remove an in-between act
				Leg prev_leg = (Leg)plan.getPlanElements().get(index-1); // prev leg;
				prev_leg.setDepartureTime(Time.UNDEFINED_TIME);
				prev_leg.setTravelTime(Time.UNDEFINED_TIME);
				prev_leg.setRoute(null);

				plan.getPlanElements().remove(index+1); // following leg
				plan.getPlanElements().remove(index); // act
			}
		}
	}
	
	// --- insert method(s):
	
	/**
	 * Inserts a leg and a following act at position <code>pos</code> into the plan.
	 * @param pos the position where to insert the leg-act-combo. acts and legs are both counted from the beginning starting at 0.
	 * @param leg the leg to insert
	 * @param act the act to insert, following the leg
	 *
	 * @throws IllegalArgumentException If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
	 */
	public static void insertLegAct( Plan plan, int pos, Leg leg, Activity act ) {
		if (pos < plan.getPlanElements().size()) {
			Object o = plan.getPlanElements().get(pos);
			if (!(o instanceof Leg)) {
				throw new IllegalArgumentException("Position to insert leg and act is not valid (act instead of leg at position).");
			}
		} else if (pos > plan.getPlanElements().size()) {
			throw new IllegalArgumentException("Position to insert leg and act is not valid.");
		}
		plan.getPlanElements().add(pos, act);
		plan.getPlanElements().add(pos, leg);
	}

		public static void changePersonId( Person person, Id<Person> id ) {
			if ( person instanceof PersonImpl ) {
				((PersonImpl) person).changeId(id);
			} else {
				throw new RuntimeException("wrong implementation of interface Person") ;
			}
		}

		public static void printPlansCount( Population population ) {
			log.info(" person # " + population.getPersons().size() );
		}

		public static void printPlansCount(StreamingPopulationReader reader) {
			reader.printPlansCount() ;
		}
}
