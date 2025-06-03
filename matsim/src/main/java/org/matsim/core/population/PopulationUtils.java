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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PersonRouteCheck;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.routes.*;
import org.matsim.core.population.routes.heavycompressed.HeavyCompressedNetworkRouteFactory;
import org.matsim.core.population.routes.mediumcompressed.MediumCompressedNetworkRouteFactory;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.io.*;
import java.util.*;

/**
 * @author nagel, ikaddoura
 */
public final class PopulationUtils {
	private static final Logger log = LogManager.getLogger(PopulationUtils.class);
	private static final PopulationFactory populationFactory = createPopulation(
			new PlansConfigGroup(), null, null).getFactory();

	/**
	 * @deprecated -- this is public only because it is needed in the also deprecated method {@link PlansConfigGroup#getSubpopulationAttributeName()}
	 */
	@Deprecated
	public static final String SUBPOPULATION_ATTRIBUTE_NAME = "subpopulation";


	/**
	 * Is a namespace, so don't instantiate:
	 */
	private PopulationUtils() {
	}

	/**
	 * Creates a new Population container. Population instances need a Config, because they need to know
	 * about the modes of transport.
	 *
	 * @param config the configuration which is used to create the Population.
	 * @return the new Population instance
	 */
	public static Population createPopulation(Config config) {
		return createPopulation(config, null, null);
	}

	/**
	 * Creates a new Population container. Population instances need a Config, because they need to know
	 * about the modes of transport.
	 *
	 * @param config the configuration which is used to create the Population.
	 * @param scale the scale (or sample fraction) of the population which is added as a container attribute.
	 * @return the new Population instance
	 */
	public static Population createPopulation(Config config, Double scale) {
		return createPopulation(config, null, scale);
	}

	/**
	 * Creates a new Population container which, depending on
	 * configuration, may make use of the specified Network instance to store routes
	 * more efficiently.
	 *
	 * @param config  the configuration which is used to create the Population.
	 * @param network the Network to which Plans in this Population will refer.
	 * @return the new Population instance
	 */
	public static Population createPopulation(Config config, Network network) {
		return createPopulation(config.plans(), network, null);
	}

	/**
	 * Creates a new Population container which, depending on
	 * configuration, may make use of the specified Network instance to store routes
	 * more efficiently.
	 *
	 * @param config  the configuration which is used to create the Population.
	 * @param network the Network to which Plans in this Population will refer.
	 * @param scale the scale (or sample fraction) of the population which is added as a container attribute.
	 * @return the new Population instance
	 */
	public static Population createPopulation(Config config, Network network, Double scale) {
		return createPopulation(config.plans(), network, scale);
	}

	/**
	 * Creates a new Population container which, depending on
	 * configuration, may make use of the specified Network instance to store routes
	 * more efficiently.
	 *
	 * @param plansConfigGroup  the configuration which is used to create the Population.
	 * @param network the Network to which Plans in this Population will refer.
	 * @param scale the scale (or sample fraction) of the population which is added as a container attribute.
	 * @return the new Population instance
	 */
	public static Population createPopulation(PlansConfigGroup plansConfigGroup, Network network, Double scale) {
		// yyyy my intuition would be to rather get this out of a standard scenario. kai, jun'16
		RouteFactories routeFactory = new RouteFactories();
		String networkRouteType = plansConfigGroup.getNetworkRouteType();
		RouteFactory factory;
		if (PlansConfigGroup.NetworkRouteType.LinkNetworkRoute.equals(networkRouteType)) {
			factory = new LinkNetworkRouteFactory();
		} else if (PlansConfigGroup.NetworkRouteType.MediumCompressedNetworkRoute.equals(networkRouteType) && network != null) {
			factory = new MediumCompressedNetworkRouteFactory();
		} else if (PlansConfigGroup.NetworkRouteType.HeavyCompressedNetworkRoute.equals(networkRouteType) && network != null) {
			factory = new HeavyCompressedNetworkRouteFactory(network, TransportMode.car);
		} else if (PlansConfigGroup.NetworkRouteType.CompressedNetworkRoute.equals(networkRouteType) && network != null) {
			factory = new HeavyCompressedNetworkRouteFactory(network, TransportMode.car);
		} else {
			throw new IllegalArgumentException("The type \"" + networkRouteType + "\" is not a supported type for network routes.");
		}
		routeFactory.setRouteFactory(NetworkRoute.class, factory);
        return new PopulationImpl(new PopulationFactoryImpl(routeFactory), scale);
	}

	public static Leg unmodifiableLeg(Leg leg) {
		return new UnmodifiableLeg(leg);
	}

	public static void resetRoutes(final Plan plan) {
		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				((Leg) pe).setRoute(null);
			}
		}
	}

	static class UnmodifiableLeg implements Leg {
		private final Leg delegate;

		public UnmodifiableLeg(Leg leg) {
			this.delegate = leg;
		}

		@Override
		public String getMode() {
			return this.delegate.getMode();
		}

		@Override
		public void setMode(String mode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getRoutingMode() {
			return this.delegate.getRoutingMode();
		}

		@Override
		public void setRoutingMode(String routingMode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Route getRoute() {
			// route should be unmodifiable. kai
			return this.delegate.getRoute();
		}

		@Override
		public void setRoute(Route route) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalTime getDepartureTime() {
			return this.delegate.getDepartureTime();
		}

		@Override
		public void setDepartureTime(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setDepartureTimeUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalTime getTravelTime() {
			return this.delegate.getTravelTime();
		}

		@Override
		public void setTravelTime(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setTravelTimeUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return this.delegate.toString();
		}

		@Override
		public Attributes getAttributes() {
			// attributes should be made unmodifiable
			return delegate.getAttributes();
		}
	}

	public static Activity unmodifiableActivity(Activity act) {
		return new UnmodifiableActivity(act);
	}

	static class UnmodifiableActivity implements Activity {
		private final Activity delegate;

		public UnmodifiableActivity(Activity act) {
			this.delegate = act;
		}

		@Override
		public OptionalTime getEndTime() {
			return this.delegate.getEndTime();
		}

		@Override
		public void setEndTime(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setEndTimeUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getType() {
			return this.delegate.getType();
		}

		@Override
		public void setType(String type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			return this.delegate.getCoord();
		}

		@Override
		public OptionalTime getStartTime() {
			return this.delegate.getStartTime();
		}

		@Override
		public void setStartTime(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setStartTimeUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalTime getMaximumDuration() {
			return this.delegate.getMaximumDuration();
		}

		@Override
		public void setMaximumDuration(double seconds) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setMaximumDurationUndefined() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Id<Link> getLinkId() {
			return this.delegate.getLinkId();
		}

		@Override
		public Id<ActivityFacility> getFacilityId() {
			return this.delegate.getFacilityId();
		}

		@Override
		public String toString() {
			return this.delegate.toString();
		}

		@Override
		public void setLinkId(Id<Link> id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setFacilityId(Id<ActivityFacility> id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setCoord(Coord coord) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public Attributes getAttributes() {
			// attributes should be made unmodifiable
			return delegate.getAttributes();
		}
	}

	/**
	 * The idea of this method is to mirror the concept of Collections.unmodifiableXxx( xxx ) .
	 * <p></p>
	 */
	public static Plan unmodifiablePlan(Plan plan) {
		return new UnmodifiablePlan(plan);
	}

	static class UnmodifiablePlan implements Plan {
		private final Plan delegate;
		private final List<PlanElement> unmodifiablePlanElements;

		public UnmodifiablePlan(Plan plan) {
			this.delegate = plan;
			List<PlanElement> tmp = new ArrayList<>();
			for (PlanElement pe : plan.getPlanElements()) {
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
			throw new UnsupportedOperationException();
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
		public Id<Plan> getId() {
			return this.delegate.getId();
		}

		@Override
		public void setPlanId(Id<Plan> planId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getIterationCreated() {
			return this.delegate.getIterationCreated();
		}

		@Override
		public void setIterationCreated(int iteration) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getPlanMutator() {
			return this.delegate.getPlanMutator();
		}

		@Override
		public void setPlanMutator(String planMutator) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addLeg(Leg leg) {
			throw new UnsupportedOperationException();
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
			throw new UnsupportedOperationException();
		}

		@Override
		public void setScore(Double score) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Attributes getAttributes() {
			// TODO yyyy should be made unmodifiable.  kai, jan'17
			return delegate.getAttributes();
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

	private static int missingFacilityCnt = 0;

	@Deprecated // use decideOnLinkIdForActivity.  kai, sep'18
	public static Id<Link> computeLinkIdFromActivity(Activity act, ActivityFacilities facs, Config config) {
		// the following might eventually become configurable by config. kai, feb'16
		if (act.getFacilityId() == null) {
			final Id<Link> linkIdFromActivity = act.getLinkId();
			Gbl.assertNotNull(linkIdFromActivity);
			return linkIdFromActivity;
		} else {
			ActivityFacility facility = facs.getFacilities().get(act.getFacilityId());
			if (facility == null || facility.getLinkId() == null) {
				if (facility == null) {
					if (missingFacilityCnt < 10) {
						log.warn("we have a facility ID for an activity, but can't find the facility; this should not really happen. Falling back on link ID.");
						missingFacilityCnt++;
						if (missingFacilityCnt == 10) {
							log.warn(Gbl.FUTURE_SUPPRESSED);
						}
					}
				}
				final Id<Link> linkIdFromActivity = act.getLinkId();
				Gbl.assertIf(linkIdFromActivity != null);
				return linkIdFromActivity;
			} else {
				return facility.getLinkId();
			}
			// yy sorry about this mess, I am just trying to make explicit which seems to have been the logic so far implicitly.  kai, feb'16
		}
	}

	@Deprecated // use "decideOnCoord..."
	public static Coord computeCoordFromActivity(Activity act, ActivityFacilities facs, Config config) {
		return computeCoordFromActivity(act, facs, null, config);
	}

	@Deprecated // use "decideOnCoord..."
	public static Coord computeCoordFromActivity(Activity act, ActivityFacilities facs, Network network, Config config) {
		// the following might eventually become configurable by config. kai, feb'16
		if (act.getFacilityId() == null) {
			if (act.getCoord() != null) {
				return act.getCoord();
			} else {
				Gbl.assertNotNull(network);
				Link link = network.getLinks().get(act.getLinkId());
				return link.getCoord();
			}
		} else {
			Gbl.assertIf(facs != null);
			ActivityFacility facility = facs.getFacilities().get(act.getFacilityId());
			Gbl.assertIf(facility != null);
			return facility.getCoord();
		}
	}

	/**
	 * A pointer to material in TripStructureUtils
	 */
	public static List<Activity> getActivities(Plan plan, StageActivityHandling stageActivityHandling) {
		return TripStructureUtils.getActivities(plan, stageActivityHandling);
	}

	/**
	 * A pointer to material in TripStructureUtils
	 */
	public static List<Leg> getLegs(Plan plan) {
		return TripStructureUtils.getLegs(plan);
	}

	/**
	 * Notes:<ul>
	 * <li>not normalized (for the time being?)
	 * <li>does not look at times (for the time being?)
	 * </ul>
	 */
	public static double calculateSimilarity(List<Leg> legs1, List<Leg> legs2, Network network,
											 double sameModeReward, double sameRouteReward) {
		// yyyy should be made configurable somehow (i.e. possibly not a static method any more).  kai, apr'15

		// yy kwa points to:
		// Schüssler, N. and K.W. Axhausen (2009b) Accounting for similarities in destination choice modelling: A concept, paper presented at the 9th Swiss Transport Research Conference, Ascona, October 2009.
		//		und
		//  Joh, Chang-Hyeon, Theo A. Arentze and Harry J. P. Timmermans (2001).
		// A Position-Sensitive Sequence Alignment Method Illustrated for Space-Time Activity-Diary Data¹, Environment and Planning A 33(2): 313­338.

		// Mahdieh Allahviranloo has some work on activity pattern similarity (iatbr'15)

		double simil = 0.;
		Iterator<Leg> it1 = legs1.iterator();
		Iterator<Leg> it2 = legs2.iterator();
		for (; it1.hasNext() && it2.hasNext(); ) {
			Leg leg1 = it1.next();
			Leg leg2 = it2.next();
			if (leg1.getMode().equals(leg2.getMode())) {
				simil += sameModeReward;
			} else {
				continue;
				// don't look for route overlap if different mode.  Makes sense for totally different modes,
				// but maybe not so obvious for similar modes such as "car" and "ride".  kai, jul'18
			}
			// the easy way for the route is to not go along the links but just check for overlap.
			Route route1 = leg1.getRoute();
			Route route2 = leg2.getRoute();
			// currently only for network routes:
			NetworkRoute nr1, nr2;
			if (route1 instanceof NetworkRoute) {
				nr1 = (NetworkRoute) route1;
			} else {
				simil += sameModeReward;
				// ("no route" is interpreted as "same route".  One reason is that otherwise plans
				// with routes always receive higher penalties than plans without routes in the diversity
				// increasing plans remover, which clearly is not what one wants. kai, jul'18)
				continue; // next leg
			}
			if (route2 instanceof NetworkRoute) {
				nr2 = (NetworkRoute) route2;
			} else {
				simil += sameModeReward;
				continue; // next leg
			}
			simil += sameRouteReward * (RouteUtils.calculateCoverage(nr1, nr2, network) + RouteUtils.calculateCoverage(nr2, nr1, network)) / 2;
		}
		return simil;
	}

	/**
	 * Notes:<ul>
	 * <li> not normalized (for the time being?)
	 * </ul>
	 */
	public static double calculateSimilarity(List<Activity> activities1, List<Activity> activities2, double sameActivityTypePenalty,
											 double sameActivityLocationPenalty, double actTimeParameter) {
		// yyyy should be made configurable somehow (i.e. possibly not a static method any more).  kai, apr'15

		// yy kwa points to:
		// Schüssler, N. and K.W. Axhausen (2009b) Accounting for similarities in destination choice modelling: A concept, paper presented at the 9th Swiss Transport Research Conference, Ascona, October 2009.
		//		und
		//  Joh, Chang-Hyeon, Theo A. Arentze and Harry J. P. Timmermans (2001).
		// A Position-Sensitive Sequence Alignment Method Illustrated for Space-Time Activity-Diary Data¹, Environment and Planning A 33(2): 313­338.

		// Mahdieh Allahviranloo has some work on activity pattern similarity (iatbr'15)

		double simil = 0.;
		Iterator<Activity> it1 = activities1.iterator();
		Iterator<Activity> it2 = activities2.iterator();
		for (; it1.hasNext() && it2.hasNext(); ) {
			Activity act1 = it1.next();
			Activity act2 = it2.next();

			// activity type
			if (act1.getType().equals(act2.getType())) {
				simil += sameActivityTypePenalty;
			}

			// activity location
			if (act1.getCoord().equals(act2.getCoord())) {
				simil += sameActivityLocationPenalty;
			}

			// activity end times
			if (act1.getEndTime().isUndefined() && act2.getEndTime().isUndefined()) {
				// both activities have no end time, no need to compute a similarity penalty
			} else {
				// both activities have an end time, comparing the end times

				// 300/ln(2) means a penalty of 0.5 for 300 sec difference
				double delta = Math.abs(act1.getEndTime().seconds() - act2.getEndTime().seconds());
				simil += actTimeParameter * Math.exp(-delta / (300 / Math.log(2)));
			}

		}

		// a positive value is interpreted as a penalty
		return simil;
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
	 *
	 * @deprecated -- please use {@link org.matsim.core.population.routes.PopulationComparison} instead.  nkuehnel, apr'24
	 */
	@Deprecated
	public static boolean equalPopulation(final Population s1, final Population s2) {
		try {
			try (InputStream inputStream1 = openPopulationInputStream(s1); InputStream inputStream2 = openPopulationInputStream(s2)) {
				return IOUtils.isEqual(inputStream1, inputStream2);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * The InputStream which comes from this method must be properly
	 * resource-managed, i.e. always be closed.
	 * <p>
	 * Otherwise, the Thread which is opened here may stay alive.
	 */
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

	public static Activity getFirstActivityAfterLastCarLegOfDay(Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		int indexOfLastCarLegOfDay = -1;
		for (int i = planElements.size() - 1; i >= 0; i--) {
			if (planElements.get(i) instanceof Leg leg) {
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					indexOfLastCarLegOfDay = i;
					break;
				}

			}
		}

		for (int i = indexOfLastCarLegOfDay + 1; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Activity act) {
				return act;
			}
		}
		return null;
	}

	public static Activity getFirstActivityOfDayBeforeDepartingWithCar(Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		int indexOfFirstCarLegOfDay = -1;
		for (int i = 0; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Leg leg) {
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					indexOfFirstCarLegOfDay = i;
					break;
				}

			}
		}
		for (int i = indexOfFirstCarLegOfDay - 1; i >= 0; i--) {
			if (planElements.get(i) instanceof Activity act) {
				return act;
			}
		}
		return null;
	}

	public static boolean hasCarLeg(Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		for (int i = 0; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Leg leg) {
				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
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
		return populationFactory;
	}

	// --- plain factories:

	public static Plan createPlan(Person person) {
		Plan plan = getFactory().createPlan();
		plan.setPerson(person);
		return plan;
	}

	public static Plan createPlan() {
		return getFactory().createPlan();
	}

	public static Activity createActivityFromLinkId(String type, Id<Link> linkId) {
		return getFactory().createActivityFromLinkId(type, linkId);
	}

	public static Activity createInteractionActivityFromLinkId(String type, Id<Link> linkId) {
		return getFactory().createInteractionActivityFromLinkId(type, linkId);
	}

	public static Activity createActivityFromFacilityId(String type, Id<ActivityFacility> facilityId) {
		return getFactory().createActivityFromActivityFacilityId(type, facilityId);
	}

	public static Activity createInteractionActivityFromFacilityId(String type, Id<ActivityFacility> facilityId) {
		return getFactory().createInteractionActivityFromActivityFacilityId(type, facilityId);
	}

	public static Activity createActivityFromCoord(String type, Coord coord) {
		return getFactory().createActivityFromCoord(type, coord);
	}

	public static Activity createInteractionActivityFromCoord(String type, Coord coord) {
		return getFactory().createInteractionActivityFromCoord(type, coord);
	}

	public static Activity createActivityFromCoordAndLinkId(String type, Coord coord, Id<Link> linkId) {
		Activity act = getFactory().createActivityFromCoord(type, coord);
		act.setLinkId(linkId);
		return act;
	}

	public static Activity createInteractionActivityFromCoordAndLinkId(String type, Coord coord, Id<Link> linkId) {
		Activity act = getFactory().createInteractionActivityFromCoord(type, coord);
		act.setLinkId(linkId);
		return act;
	}

	public static Activity convertInteractionToStandardActivity(Activity activity) {
		if (activity instanceof InteractionActivity) {
			return createActivity(activity);
		} else {
			return activity;
		}
	}

	public static Leg createLeg(String transportMode) {
		return getFactory().createLeg(transportMode);
	}

	// createAndAdd methods:

	public static Activity createAndAddActivityFromFacilityId(Plan plan, String type, Id<ActivityFacility> facilityId) {
		Activity act = getFactory().createActivityFromActivityFacilityId(type, facilityId);
		plan.addActivity(act);
		return act;
	}

	public static Activity createAndAddActivityFromCoord(Plan plan, String type, Coord coord) {
		Activity act = getFactory().createActivityFromCoord(type, coord);
		plan.addActivity(act);
		act.setCoord(coord);
		return act;
	}

	public static Activity createAndAddActivityFromLinkId(Plan plan, String type, Id<Link> linkId) {
		Activity act = getFactory().createActivityFromLinkId(type, linkId);
		plan.addActivity(act);
		act.setLinkId(linkId);
		return act;
	}

	public static Leg createAndAddLeg(Plan plan, String mode) {
		verifyCreateLeg(plan);
		Leg leg = getFactory().createLeg(mode);
		plan.addLeg(leg);
		return leg;
	}

	private static void verifyCreateLeg(Plan plan) throws IllegalStateException {
		if (plan.getPlanElements().size() == 0) {
			throw new IllegalStateException("The order of 'acts'/'legs' is wrong in some way while trying to create a 'leg'.");
		}
	}

	public static Activity createAndAddActivity(Plan plan, String type) {
		Activity act = new ActivityImpl(type);
		// (can't do this from the factory since factory method only exists with coord or with linkId. kai, jun'16)
		plan.addActivity(act);
		return act;
	}

	public static Activity createStageActivityFromCoordLinkIdAndModePrefix(final Coord interactionCoord, final Id<Link> interactionLink, String modePrefix) {
		Activity act = createInteractionActivityFromCoordAndLinkId(ScoringConfigGroup.createStageActivityType(modePrefix), interactionCoord, interactionLink);
//		act.setMaximumDuration(0.0); // obsolete since this is hard-coded in InteractionActivity
		return act;
	}


	// --- static copy methods:

	/**
	 * loads a copy of an existing plan, but keeps the person reference
	 *
	 * @param in  a plan who's data will be loaded into this plan
	 * @param out
	 **/
	public static void copyFromTo(final Plan in, final Plan out) {
		/*
		 * By default 'false' to be backwards compatible. As a result, InteractionActivities will be converted to ActivityImpl.
		 */
		copyFromTo(in, out, false);
	}

	public static void copyFromTo(final Plan in, final Plan out, final boolean withInteractionActivities) {
		out.getPlanElements().clear();
		out.setScore(in.getScore());
		out.setType(in.getType());
		for (PlanElement pe : in.getPlanElements()) {
			if (pe instanceof Activity) {
				/*
				 * So far, we do not use the check for StageActivityTypeIdentifier.isStageActivity(...). It Would convert ActivityImpl to InteractionActivities.
				 * However, there are pieces of code in use, e.g. in the share mobility contrib, where "interaction activities" need to be modeled as ActivityImpl
				 * since their duration is != 0 or they have a defined start time.
				 */
				if (withInteractionActivities && (pe instanceof InteractionActivity /* || StageActivityTypeIdentifier.isStageActivity(((Activity) pe).getType())*/)) {
					out.getPlanElements().add(createInteractionActivity((Activity) pe));
				} else {
					out.getPlanElements().add(createActivity((Activity) pe));
				}
			} else if (pe instanceof Leg) {
				out.getPlanElements().add(createLeg((Leg) pe));
			} else {
				throw new IllegalArgumentException("unrecognized plan element type discovered");
			}
		}
		AttributesUtils.copyAttributesFromTo(in, out);
	}

	public static void copyFromTo(Leg in, Leg out) {
		out.setMode(in.getMode());
		TripStructureUtils.setRoutingMode(out, TripStructureUtils.getRoutingMode(in));
		in.getDepartureTime().ifDefinedOrElse(out::setDepartureTime, out::setDepartureTimeUndefined);
		in.getTravelTime().ifDefinedOrElse(out::setTravelTime, out::setTravelTimeUndefined);
		if (in.getRoute() != null) {
			out.setRoute(in.getRoute().clone());
		}
		AttributesUtils.copyAttributesFromTo(in, out);
	}

	public static void copyFromTo(Activity act, Activity newAct) {
		Coord coord = act.getCoord() == null ? null : new Coord(act.getCoord().getX(), act.getCoord().getY());
		// (we don't want to copy the coord ref, but rather the contents!)
		newAct.setCoord(coord);
		newAct.setType(act.getType());
		newAct.setLinkId(act.getLinkId());
		act.getStartTime().ifDefinedOrElse(newAct::setStartTime, newAct::setStartTimeUndefined);
		act.getEndTime().ifDefinedOrElse(newAct::setEndTime, newAct::setEndTimeUndefined);
		act.getMaximumDuration().ifDefinedOrElse(newAct::setMaximumDuration, newAct::setMaximumDurationUndefined);
		newAct.setFacilityId(act.getFacilityId());

		AttributesUtils.copyAttributesFromTo(act, newAct);
	}

	// --- copy factories:

	public static Activity createActivity(Activity act) {
		Activity newAct = getFactory().createActivityFromLinkId(act.getType(), act.getLinkId());

		copyFromTo(act, newAct);
		// (this ends up setting type and linkId again)

		return newAct;
	}

	public static Activity createInteractionActivity(Activity act) {
		Activity newAct = getFactory().createInteractionActivityFromLinkId(act.getType(), act.getLinkId());

		copyFromTo(act, newAct);
		// (this ends up setting type and linkId again)

		return newAct;
	}

	/**
	 * Makes a deep copy of this leg, however only when the Leg has a route which is
	 * instance of Route or BasicRoute. Other route instances are not considered.
	 * </p>
	 * <ul>
	 * <li> Is the statement about the route still correct?  kai, jun'16
	 * </ul>
	 *
	 * @param leg
	 */
	public static Leg createLeg(Leg leg) {
		Leg newLeg = createLeg(leg.getMode());

		copyFromTo(leg, newLeg);
		// (this ends up setting mode again)

		return newLeg;
	}

	// --- positional methods:

	public static Activity getFirstActivity(Plan plan) {
		return (Activity) plan.getPlanElements().get(0);
	}

	public static Activity getLastActivity(Plan plan) {
		return (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
	}

	public static Activity getNextActivity(Plan plan, Leg leg) {
		int index = getActLegIndex(plan, leg);
		if (index != -1) {
			return (Activity) plan.getPlanElements().get(index + 1);
		}
		return null;
	}

	public static int getActLegIndex(Plan plan, PlanElement pe) {
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

	public static Leg getNextLeg(Plan plan, Activity act) {
		int index = PopulationUtils.getActLegIndex(plan, act);
		if ((index < plan.getPlanElements().size() - 1) && (index != -1)) {
			return (Leg) plan.getPlanElements().get(index + 1);
		}
		return null;
	}

	public static Activity getPreviousActivity(Plan plan, Leg leg) {
		int index = PopulationUtils.getActLegIndex(plan, leg);
		if (index != -1) {
			return (Activity) plan.getPlanElements().get(index - 1);
		}
		return null;
	}

	public static Leg getPreviousLeg(Plan plan, Activity act) {
		int index = PopulationUtils.getActLegIndex(plan, act);
		if (index != -1) {
			return (Leg) plan.getPlanElements().get(index - 1);
		}
		return null;
	}

	// --- remove methods:

	/**
	 * Removes the specified leg <b>and</b> the following act, too! If the following act is not the last one,
	 * the following leg will be emptied to keep consistency (i.e. for the route)
	 *
	 * @param index
	 */
	public static void removeLeg(Plan plan, int index) {
		if ((index % 2 == 0) || (index < 1) || (index >= plan.getPlanElements().size() - 1)) {
			log.warn(plan + "[index=" + index + " is wrong. nothing removed]");
		} else {
			if (index != plan.getPlanElements().size() - 2) {
				// not the last leg
				Leg next_leg = (Leg) plan.getPlanElements().get(index + 2);
				next_leg.setDepartureTimeUndefined();
				next_leg.setTravelTimeUndefined();
				next_leg.setRoute(null);
			}
			plan.getPlanElements().remove(index + 1); // following act
			plan.getPlanElements().remove(index); // leg
		}

	}

	public static void removeActivity(Plan plan, int index) {
		if ((index % 2 != 0) || (index < 0) || (index > plan.getPlanElements().size() - 1)) {
			log.warn(plan + "[index=" + index + " is wrong. nothing removed]");
		} else if (plan.getPlanElements().size() == 1) {
			log.warn(plan + "[index=" + index + " only one act. nothing removed]");
		} else {
			if (index == 0) {
				// remove first act and first leg
				plan.getPlanElements().remove(index + 1); // following leg
				plan.getPlanElements().remove(index); // act
			} else if (index == plan.getPlanElements().size() - 1) {
				// remove last act and last leg
				plan.getPlanElements().remove(index); // act
				plan.getPlanElements().remove(index - 1); // previous leg
			} else {
				// remove an in-between act
				Leg prev_leg = (Leg) plan.getPlanElements().get(index - 1); // prev leg;
				prev_leg.setDepartureTimeUndefined();
				prev_leg.setTravelTimeUndefined();
				prev_leg.setRoute(null);

				plan.getPlanElements().remove(index + 1); // following leg
				plan.getPlanElements().remove(index); // act
			}
		}
	}

	// --- insert method(s):

	/**
	 * Inserts a leg and a following act at position <code>pos</code> into the plan.
	 *
	 * @param pos the position where to insert the leg-act-combo. acts and legs are both counted from the beginning starting at 0.
	 * @param leg the leg to insert
	 * @param act the act to insert, following the leg
	 * @throws IllegalArgumentException If the leg and act cannot be inserted at the specified position without retaining the correct order of legs and acts.
	 */
	public static void insertLegAct(Plan plan, int pos, Leg leg, Activity act) {
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

	public static void changePersonId(Person person, Id<Person> id) {
		if (person instanceof PersonImpl) {
			((PersonImpl) person).changeId(id);
		} else {
			throw new RuntimeException("wrong implementation of interface Person");
		}
	}

	public static void printPlansCount(Population population) {
		log.info(" person # " + population.getPersons().size());
	}

	public static void printPlansCount(StreamingPopulationReader reader) {
		reader.printPlansCount();
	}

	public static void writePopulation(Population population, String filename) {
		new PopulationWriter(population).write(filename);
	}

	public static Id<Link> decideOnLinkIdForActivity(Activity act, Scenario sc) {
		if (act.getFacilityId() != null) {
			final ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(act.getFacilityId());
			if (facility == null) {
				throw new RuntimeException("facility ID given but not in facilities container");
			}
			Gbl.assertNotNull(facility.getLinkId());
			return facility.getLinkId();
		}
		Gbl.assertNotNull(act.getLinkId());
		return act.getLinkId();
	}

	public static Coord decideOnCoordForActivity(Activity act, Scenario sc) {
		Id<ActivityFacility> facilityId;
		try {
			facilityId = act.getFacilityId();
		} catch (Exception ee) {
			facilityId = null;
		}
		// some people prefer throwing exceptions over using null

		if (facilityId != null) {
			final ActivityFacility facility = sc.getActivityFacilities().getFacilities().get(facilityId);
			Gbl.assertNotNull(facility);
			Gbl.assertNotNull(facility.getCoord());
			return facility.getCoord();
		}

		if (act.getCoord() != null) {
			return act.getCoord();
		}

		Gbl.assertNotNull(sc.getNetwork());
		Link link = sc.getNetwork().getLinks().get(act.getLinkId());
		Gbl.assertNotNull(link);
		Coord fromCoord = link.getFromNode().getCoord();
		Coord toCoord = link.getToNode().getCoord();
		double rel = sc.getConfig().global().getRelativePositionOfEntryExitOnLink();
		return new Coord(fromCoord.getX() + rel * (toCoord.getX() - fromCoord.getX()), fromCoord.getY() + rel * (toCoord.getY() - fromCoord.getY()));
	}

	public static void sampleDown(Population pop, double sample) {
		final Random rnd = MatsimRandom.getLocalInstance();
		log.info("population size before downsampling=" + pop.getPersons().size());
		pop.getPersons().values().removeIf(person -> rnd.nextDouble() >= sample);
		log.info("population size after downsampling=" + pop.getPersons().size());
	}

	public static void readPopulation(Population population, String filename) {
		MutableScenario scenario = ScenarioUtils.createMutableScenario(ConfigUtils.createConfig());
		scenario.setPopulation(population);
		new PopulationReader(scenario).readFile(filename);
		// (yyyy population reader uses network to retrofit some missing geo information such as route lenth.
		// In my opinion, that should be done in prepareForSim, not in the parser.  It is commented as such
		// in the PopulationReader class.  kai, nov'18)
	}

	public static Population readPopulation(String filename) {
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		readPopulation(population, filename);
		return population;
	}

	/**
	 * @deprecated -- please use {@link org.matsim.core.population.routes.PopulationComparison} instead.  nkuehnel, apr'24
	 */
	@Deprecated
	public static boolean comparePopulations(Population population1, Population population2) {
		return PopulationUtils.equalPopulation(population1, population2);
	}

	public static PopulationComparison.Result comparePopulations(String path1, String path2) {
		Population population1 = PopulationUtils.readPopulation(path1);
		Population population2 = PopulationUtils.readPopulation(path2);

		return PopulationComparison.compare(population1, population2);
	}

	// ---

	/**
	 * @deprecated -- please inline.  kai, jun'22
	 */
	public static Object getPersonAttribute(HasPlansAndId<?, ?> person, String key) {
		if (person == null) {
			return null;
		}
		// (This was originally "if person instanceof Attributable then ...".  Since HasPlansAndId now implements Attributable, this is in
		// principle always fulfilled.  However, if person==null, then person instanceof Attributable also fails.  kai, jul'22)

		return person.getAttributes().getAttribute(key);
	}

	/**
	 * @deprecated -- please inline.  kai, jun'22
	 */
	public static void putPersonAttribute(HasPlansAndId<?, ?> person, String key, Object value) {
		person.getAttributes().putAttribute(key, value);
	}

	/**
	 * @deprecated -- please inline.  kai, jun'22
	 */
	public static Object removePersonAttribute(Person person, String key) {
		return person.getAttributes().removeAttribute(key);
	}

	/**
	 * @deprecated -- this command is dangerous since it might clear some else's attributes.  Better just remove specificially the attributes that you "own".  kai, may'19
	 */
	public static void removePersonAttributes(Person person, Population population) {
		//population.getPersonAttributes().removeAllAttributesDirectly( person.getId().toString() );
		person.getAttributes().clear();
	}

	public static String getSubpopulation(HasPlansAndId<?, ?> person) {
		if (person == null) {
			return null;
		}
		// (This originally delegated to getPersonAttribute.  See comment there.  kai, jul'22)

		return (String) person.getAttributes().getAttribute(SUBPOPULATION_ATTRIBUTE_NAME);
	}

	public static void putSubpopulation(HasPlansAndId<?, ?> person, String subpopulation) {
		putPersonAttribute(person, SUBPOPULATION_ATTRIBUTE_NAME, subpopulation);
	}

	public static void removeSubpopulation(Person person) {
		person.getAttributes().removeAttribute(SUBPOPULATION_ATTRIBUTE_NAME);
	}

	public static Population getOrCreateAllPersons(Scenario scenario) {
		Population map = (Population) scenario.getScenarioElement("allpersons");
		if (map == null) {
			log.info("adding scenario element for allpersons container");
			map = new PopulationImpl(scenario.getPopulation().getFactory(), null);
			scenario.addScenarioElement("allpersons", map);
		}
		return map;
	}

	private static int tryStdCnt = 5;

	public static Person findPerson(Id<Person> personId, Scenario scenario) {
		Person person = getOrCreateAllPersons(scenario).getPersons().get(personId);
		if (person == null) {
			if (tryStdCnt > 0) {
				tryStdCnt--;
				log.info("personId=" + personId + " not in allPersons; trying standard population container ...");
				if (tryStdCnt == 0) {
					log.info(Gbl.FUTURE_SUPPRESSED);
				}
			}
			person = scenario.getPopulation().getPersons().get(personId);
		}
		if (person == null) {
			log.info("unable to find person for personId=" + personId + "; will return null");
		}
		return person;
	}

	/**
	 * Attaches vehicle types to a person, so that the router knows which vehicle to use for which mode and person.
	 *
	 * @param modeToVehicleType mode string mapped to vehicle type ids. The provided map is copied and stored as unmodifiable map.
	 */
	public static void insertVehicleTypesIntoPersonAttributes(Person person, Map<String, Id<VehicleType>> modeToVehicleType) {
		VehicleUtils.insertVehicleTypesIntoPersonAttributes(person, modeToVehicleType);
	}

	/**
	 * Attaches vehicle ids to a person, so that the router knows which vehicle to use for which mode and person.
	 *
	 * @param modeToVehicle mode string mapped to vehicle ids. The provided map is copied and stored as unmodifiable map.
	 *                      If a mode key already exists in the persons's attributes it is overridden. Otherwise, existing
	 *                      and provided values are merged into one map
	 *                      We use PersonVehicle Class in order to have a dedicated PersonVehicleAttributeConverter to/from XML
	 */
	public static void insertVehicleIdsIntoPersonAttributes(Person person, Map<String, Id<Vehicle>> modeToVehicle) {
		VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, modeToVehicle);
	}

	/**
	 * Checks if each link of a route has the mode of the respective leg. This may be the case, if network links were
	 * If the route is not a {@link NetworkRoute}, nothing is changed. If there are inconsistencies, the route is reset.
	 *
	 * @param population
	 * @param network
	 */
	public static void checkRouteModeAndReset(Population population, Network network) {
		PersonRouteCheck personRouteChecker = new PersonRouteCheck(network);
		population.getPersons().values().forEach(
			personRouteChecker::run
		);
	}
}
