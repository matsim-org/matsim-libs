/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePTLegs.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.christoph.matsim2030;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import playground.christoph.evacuation.pt.TransitRouterImplFactory;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;

import java.util.List;

public class CreatePTLegs {

	private static Provider<TransitRouter> transitRouterFactory;
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.global().setNumberOfThreads(8);	// for parallel population reading
		config.network().setInputFile("/data/matsim/cdobler/2030/network.xml.gz");
		config.plans().setInputFile("/data/matsim/cdobler/2030/60.plans_without_pt_routes.xml.gz");
//		config.plans().setInputFile("/data/matsim/cdobler/2030/plans_test.xml");
		config.facilities().setInputFile("/data/matsim/cdobler/2030/facilities.xml.gz");
		config.transit().setTransitScheduleFile("/data/matsim/cdobler/2030/schedule.20120117.ch-edited.xml.gz");
		config.transit().setVehiclesFile("/data/matsim/cdobler/2030/transitVehicles.ch.xml.gz");
		config.transit().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		//config.scenario().setUseKnowledge(true);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		TransitRouterNetwork routerNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReaderMatsimV1(scenario, routerNetwork).parse("/data/matsim/cdobler/2030/transitRouterNetwork_thinned.xml.gz");
		
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		
//		transitRouterFactory = new FastTransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		
		// keep only one plan per person
		for (Person person : scenario.getPopulation().getPersons().values()) {
			PersonUtils.removeUnselectedPlans(((PersonImpl) person));
		}
		
		// create pt routes
		int numThreads = 16;
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), numThreads, new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PrepareForSimOnlyPT(createRoutingAlgorithm(scenario));
			}
		});
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()
//				((ScenarioImpl) scenario).getKnowledges()).writeFileV4("/data/matsim/cdobler/2030/60.plans_with_pt_routes.xml.gz");
        ).writeFileV4("/data/matsim/cdobler/2030/60.plans_with_pt_routes_single_plan.xml.gz");
	}
		
	private static final PersonAlgorithm createRoutingAlgorithm(Scenario scenario) {
		
		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

		TripRouter tripRouter = new TripRouter();
        if ( scenario.getConfig().transit().isUseTransit() ) {
            TransitRouterWrapper routingModule = new TransitRouterWrapper(
            		transitRouterFactory.get(),
                    scenario.getTransitSchedule(),
                    scenario.getNetwork(), // use a walk router in case no PT path is found
                    DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, scenario.getPopulation().getFactory(), 
					        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) )) ;
            for (String mode : scenario.getConfig().transit().getTransitModes()) {
                // XXX one can't check for inconsistent setting here...
                // because the setting is inconsistent by default (defaults
                // set a teleportation setting for pt routing, which is overriden
                // here) (td, may 2013)
                tripRouter.setRoutingModule(mode, routingModule);
            }
        }
		return new OnlyPTPlanRouter(tripRouter, scenario.getActivityFacilities()); 
	}
	
	private static final class PrepareForSimOnlyPT extends AbstractPersonAlgorithm {

		private final PersonAlgorithm personAlgorithm;
		
		public PrepareForSimOnlyPT(PersonAlgorithm personAlgorithm) {
			this.personAlgorithm = personAlgorithm;
		}
		
		@Override
		public void run(Person person) {
			this.personAlgorithm.run(person);
		}
	}
	
	private static class OnlyPTPlanRouter implements PlanAlgorithm, PersonAlgorithm {

		private final TripRouter tripRouter;
		private final ActivityFacilities facilities;
		
		public OnlyPTPlanRouter(TripRouter tripRouter, ActivityFacilities facilities) {
			this.tripRouter = tripRouter;
			this.facilities = facilities;
		}

		@Override
		public void run(Person person) {
			for (Plan plan : person.getPlans()) {
				run(plan);
			}
		}	
		
		@Override
		public void run(final Plan plan) {
			final List<Trip> trips = TripStructureUtils.getTrips(plan, this.tripRouter.getStageActivityTypes());

			for (Trip trip : trips) {
				// handle only pt trips - other trips are up-to-date
				String mainMode = this.tripRouter.getMainModeIdentifier().identifyMainMode(trip.getTripElements());
				if (mainMode.equals(TransportMode.pt)) {
					final List<? extends PlanElement> newTrip =
							this.tripRouter.calcRoute(mainMode,
									toFacility(trip.getOriginActivity()),
									toFacility(trip.getDestinationActivity()),
									calcEndOfActivity(trip.getOriginActivity(), plan),
									plan.getPerson());
					
					TripRouter.insertTrip(plan, 
							trip.getOriginActivity(),
							newTrip,
							trip.getDestinationActivity());					
				}
			}
		}
		
		// /////////////////////////////////////////////////////////////////////////
		// helpers
		// /////////////////////////////////////////////////////////////////////////
		private Facility toFacility(final Activity act) {
			if ((act.getLinkId() == null || act.getCoord() == null)
					&& facilities != null
					&& !facilities.getFacilities().isEmpty()) {
				// use facilities only if the activity does not provides the required fields.
				return facilities.getFacilities().get( act.getFacilityId() );
			}
			return new ActivityWrapperFacility( act );
		}

		private static double calcEndOfActivity(
				final Activity activity,
				final Plan plan) {
			if (activity.getEndTime() != Time.UNDEFINED_TIME) return activity.getEndTime();

			// no sufficient information in the activity...
			// do it the long way.
			// XXX This is inefficient! Using a cache for each plan may be an option
			// (knowing that plan elements are iterated in proper sequence,
			// no need to re-examine the parts of the plan already known)
			double now = 0;

			for (PlanElement pe : plan.getPlanElements()) {
				now = updateNow( now , pe );
				if (pe == activity) return now;
			}

			throw new RuntimeException( "activity "+activity+" not found in "+plan.getPlanElements() );
		}
		
		private static double updateNow(
				final double now,
				final PlanElement pe) {
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				double endTime = act.getEndTime();
				double startTime = act.getStartTime();
				double dur = (act instanceof ActivityImpl ? act.getMaximumDuration() : Time.UNDEFINED_TIME);
				if (endTime != Time.UNDEFINED_TIME) {
					// use fromAct.endTime as time for routing
					return endTime;
				}
				else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
					// use fromAct.startTime + fromAct.duration as time for routing
					return startTime + dur;
				}
				else if (dur != Time.UNDEFINED_TIME) {
					// use last used time + fromAct.duration as time for routing
					return now + dur;
				}
				else {
					throw new RuntimeException("activity has neither end-time nor duration." + act);
				}
			}
			double tt = ((Leg) pe).getTravelTime();
			return now + (tt != Time.UNDEFINED_TIME ? tt : 0);
		}
	}
}
