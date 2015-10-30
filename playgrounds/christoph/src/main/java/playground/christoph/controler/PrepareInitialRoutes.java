/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareInitialRoutes.java
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

package playground.christoph.controler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.facilities.Facility;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;

/**
 * Prepare initial plans for within-day initial routes creation. There, routes are
 * created on-the-fly during the first iteration. However, the Controler create routes
 * for all persons with no routes. Therefore, this class creates dummy routes for them.
 * 
 * This is only a performance optimization. The results will not change if the controler
 * creates routes for those persons which are then overwritten by the within-day code.
 *
 * Persons that already have valid routes are ignored!
 * 
 * @author cdobler
 */
public class PrepareInitialRoutes {
	
	private final Scenario scenario;
	
	private Set<String> dummyModes;

	/**
	 * @param args two Strings: config file and output population file
	 */
	public static void main(String[] args) {
		if (args.length == 2) {
			Config config = ConfigUtils.loadConfig(args[0]);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			new PrepareInitialRoutes(scenario).run();
			new PopulationWriter(scenario.getPopulation()).write(args[1]);
		}
	}
	
	public PrepareInitialRoutes(Scenario scenario) {
		this.scenario = scenario;
		
		// by default dummy routes are created only for car trips
		this.dummyModes = CollectionUtils.stringToSet(TransportMode.car);
	}
	
	public Set<String> getDummyModes() {
		return Collections.unmodifiableSet(this.dummyModes);
	}
	
	/**
	 * 
	 * @param dummyModes set of modes for which dummy routes are created
	 */
	public void setDummyModes(Set<String> dummyModes) {
		this.dummyModes.clear();
		this.dummyModes.addAll(dummyModes);
	}
	
	public void run() {
		prepareForSim(this.scenario, this.dummyModes);
	}
	
	/*
	 * If agent's routes are not included in their initial plans, the Controler would
	 * create them. This is not necessary, since they are created using real time
	 * travel times during the first iteration. Therefore, we create only dummy routes
	 * which saves time.
	 */
	private void prepareForSim(final Scenario scenario, Set<String> dummyModes) {
		
		Config config = scenario.getConfig();

		Provider<TripRouter> defaultTripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(scenario);
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		Provider<TripRouter> tripRouterFactory = new WithinDayInitialRoutesTripRouterFactory(defaultTripRouterFactory, dummyModes,
				populationFactory, routeFactory, scenario.getNetwork());
				
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutilityFactory travelDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);

		final PlanRouterProvider planRouterProvider = new PlanRouterProvider(tripRouterFactory, routingContext);
		
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(planRouterProvider.getPlanAlgorithm(), scenario);
			}
		});
	}
	
	private static class WithinDayInitialRoutesTripRouterFactory implements Provider<TripRouter> {

		private final Provider<TripRouter> tripRouterFactory;
		private final Set<String> dummyModes;
		private final PopulationFactory populationFactory;
		private final ModeRouteFactory routeFactory;
		private final Network network;
		
		public WithinDayInitialRoutesTripRouterFactory(Provider<TripRouter> tripRouterFactory, Set<String> dummyModes,
				PopulationFactory populationFactory, ModeRouteFactory routeFactory, Network network) {
			this.tripRouterFactory = tripRouterFactory;
			this.dummyModes = dummyModes;
			this.populationFactory = populationFactory;
			this.routeFactory = routeFactory;
			this.network = network;
		}
		
		@Override
		public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
			TripRouter tripRouter = tripRouterFactory.get();

			// replace routing modules for dummy modes
			for (String mode : dummyModes) {
				RoutingModule routingModule = new WithinDayInitialRoutesRoutingModule(mode, this.populationFactory, 
						this.routeFactory, this.network);
				tripRouter.setRoutingModule(mode, routingModule);
			}
			
			return tripRouter;
		}
	}
	
	private static class WithinDayInitialRoutesRoutingModule implements RoutingModule {

		private final String mode;
		private final PopulationFactory populationFactory;
		private final ModeRouteFactory routeFactory;
		private final Network network;
		
		public WithinDayInitialRoutesRoutingModule(String mode, PopulationFactory populationFactory, ModeRouteFactory routeFactory,
				Network network) {
			this.mode = mode;
			this.populationFactory = populationFactory;
			this.routeFactory = routeFactory;
			this.network = network;
		}
		
		@Override
		public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
			
			Leg newLeg = this.populationFactory.createLeg(mode);
			newLeg.setDepartureTime(departureTime);
			newLeg.setTravelTime(0.0);	// we do not know the travel time
			
			Route route = this.routeFactory.createRoute(mode, fromFacility.getLinkId(), toFacility.getLinkId());
			
			/*
			 * Workaround for network routes:
			 * Due to a recent change in the code, the mobility simulation calls 
			 * agent.chooseNextLinkId() when an agent starts a new leg. However, at this point,
			 * the agent does not know its next link and therefore is removed from the simulation,
			 * if the leg does not end on the same link again. To avoid this, we insert a dummy link
			 * which is replaced when the agent actually updates its route.
			 */
			if (route instanceof NetworkRoute) {
				if (!fromFacility.getLinkId().equals(toFacility.getLinkId())) {
					
					Link fromLink = this.network.getLinks().get(fromFacility.getLinkId());
					Node toNode = fromLink.getToNode();
					
					List<Id<Link>> links = new ArrayList<Id<Link>>();
					// add the first possible outlink to the list
					for (Link link : toNode.getOutLinks().values()) {
						links.add(link.getId());
						break;
					}
					
					((NetworkRoute) route).setLinkIds(fromFacility.getLinkId(), links, toFacility.getLinkId());
				}				
			}
							
			newLeg.setRoute(route);

			return Arrays.asList(newLeg);
		}

		@Override
		public StageActivityTypes getStageActivityTypes() {
			return EmptyStageActivityTypes.INSTANCE;
		}
		
	}
	
	private static class PlanRouterProvider {

		private final Provider<TripRouter> tripRouterFactory;
		private final RoutingContext routingContext;
		
		public PlanRouterProvider(Provider<TripRouter> tripRouterFactory, RoutingContext routingContext) {
			this.tripRouterFactory = tripRouterFactory;
			this.routingContext = routingContext;
			
		}
		
		public PlanAlgorithm getPlanAlgorithm() {
			return new PlanRouter(this.tripRouterFactory.get());
		}
	}
}
