/* *********************************************************************** *
 * project: org.matsim.*
 * InitialRoutesControlerListener.java
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

package playground.christoph.controler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegStartedIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

public class WithinDayInitialRoutesControlerListener implements StartupListener, IterationStartsListener {

	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifierFactory startedLegFactory;
	private DuringLegIdentifier legPerformingIdentifier;
	private DuringLegIdentifier legStartedIdentifier;
	
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	private WithinDayControlerListener withinDayControlerListener;
	
	private Scenario scenario;
		
	private boolean initialLegRerouting = true;
	private boolean duringLegRerouting = true;
	
	private double duringLegReroutingShare = 0.10;
		
	public WithinDayInitialRoutesControlerListener(Controler controler) {
		
		this.scenario = controler.getScenario();
		init(controler);
	}

	public void setDuringLegReroutingShare(double share) {
		this.duringLegReroutingShare = share;
	}
	
	public void setDuringLegReroutingEnabled(boolean enabled) {
		this.duringLegRerouting = enabled;
	}
	
	public void setInitialLegReroutingEnabled(boolean enabled) {
		this.initialLegRerouting = enabled;
	}
	
	public WithinDayControlerListener getWithinDayControlerListener() {
		return this.withinDayControlerListener;
	}
	
	private void init(Controler controler) {
		/*
		 * Change the persons' original plans. By doing so, the (hopefully) optimized
		 * routes are written to the output plans file.
		 */
		ExperimentalBasicWithindayAgent.copySelectedPlan = false;

		/*
		 * Create a WithinDayControlerListener but do NOT register it as ControlerListener.
		 * It implements the StartupListener interface as this class also does. The
		 * StartupEvent is passed over to it when this class handles the event. 
		 */
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new DijkstraFactory());
	}


	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * The withinDayControlerListener is also a StartupListener. Its notifyStartup(...)
		 * method has to be called first. There, the within-day module is initialized.
		 */
		this.withinDayControlerListener.notifyStartup(event);
		
		this.prepareForSim(event.getControler());
		
		this.initIdentifiers();
		this.initReplanners();
	}
	
	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		/*
		 * Disable Within-Day Replanning after first Iteration.
		 */
		if (event.getIteration() > 0) {
			this.withinDayControlerListener.getWithinDayEngine().doInitialReplanning(false);
			this.withinDayControlerListener.getWithinDayEngine().doDuringLegReplanning(false);
			this.withinDayControlerListener.getWithinDayEngine().doDuringActivityReplanning(false);
			
			event.getControler().getEvents().removeHandler(this.withinDayControlerListener.getTravelTimeCollector());
			this.withinDayControlerListener.getFixedOrderSimulationListener().removeSimulationListener(
					this.withinDayControlerListener.getTravelTimeCollector());
		}
	}
	
	/*
	 * If agent's routes are not included in their initial plans, the Controler would
	 * create them. This is not necessary, since they are created using real time
	 * travel times during the first iteration. Therefore, we create only dummy routes
	 * which saves time.
	 */
	private void prepareForSim(Controler controler) {
			
		final Scenario scenario = controler.getScenario();
		TripRouterFactory defaultTripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(scenario);
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		Set<String> dummyModes = CollectionUtils.stringToSet(TransportMode.car);
		TripRouterFactory tripRouterFactory = new WithinDayInitialRoutesTripRouterFactory(defaultTripRouterFactory, dummyModes,
				populationFactory, routeFactory);
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime, this.scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);

		final PlanRouterProvider planRouterProvider = new PlanRouterProvider(tripRouterFactory, routingContext);
		
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(this.scenario.getPopulation(), this.scenario.getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(planRouterProvider.getPlanAlgorithm(), scenario);
			}
		});
	}
		
	private void initIdentifiers() {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);
		
		if (initialLegRerouting || duringLegRerouting) {
			carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes);
			this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(carLegAgentsFilterFactory);
		}

		if (duringLegRerouting) {
			duringLegFactory = new LeaveLinkIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap());
			duringLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
			this.legPerformingIdentifier = duringLegFactory.createIdentifier();	
			this.legPerformingIdentifier.addAgentFilter(new ProbabilityFilterFactory(this.duringLegReroutingShare).createAgentFilter());
		}
		
		if (initialLegRerouting) {
			startedLegFactory = new LegStartedIdentifierFactory(this.withinDayControlerListener.getLinkReplanningMap());
			startedLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
			this.legStartedIdentifier = startedLegFactory.createIdentifier();			
		}
	}
	
	protected void initReplanners() {
		
		TravelDisutility travelDisutility = this.withinDayControlerListener.getTravelDisutilityFactory().createTravelDisutility(
				this.withinDayControlerListener.getTravelTimeCollector(), this.scenario.getConfig().planCalcScore()); 
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.withinDayControlerListener.getTravelTimeCollector());
		
		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		
		if (duringLegRerouting) {
			duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
					this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
			duringLegReplannerFactory.addIdentifier(this.legPerformingIdentifier);
			this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
		}
		
		if (initialLegRerouting) {
			duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
					this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
			duringLegReplannerFactory.addIdentifier(this.legStartedIdentifier);
			this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);
		}
	}
	
	private static class WithinDayInitialRoutesTripRouterFactory implements TripRouterFactory {

		private final TripRouterFactory tripRouterFactory;
		private final Set<String> dummyModes;
		private final PopulationFactory populationFactory;
		private final ModeRouteFactory routeFactory;
		
		public WithinDayInitialRoutesTripRouterFactory(TripRouterFactory tripRouterFactory, Set<String> dummyModes, 
				PopulationFactory populationFactory, ModeRouteFactory routeFactory) {
			this.tripRouterFactory = tripRouterFactory;
			this.dummyModes = dummyModes;
			this.populationFactory = populationFactory;
			this.routeFactory = routeFactory;
		}
		
		@Override
		public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
			TripRouter tripRouter = tripRouterFactory.instantiateAndConfigureTripRouter(routingContext);

			// replace routing modules for dummy modes
			for (String mode : dummyModes) {
				RoutingModule routingModule = new WithinDayInitialRoutesRoutingModule(mode, populationFactory, routeFactory);
				tripRouter.setRoutingModule(mode, routingModule);
			}				
			
			return tripRouter;
		}
	}
	
	private static class WithinDayInitialRoutesRoutingModule implements RoutingModule {

		private final String mode;
		private final PopulationFactory populationFactory;
		private final ModeRouteFactory routeFactory;
		
		public WithinDayInitialRoutesRoutingModule(String mode, PopulationFactory populationFactory, ModeRouteFactory routeFactory) {
			this.mode = mode;
			this.populationFactory = populationFactory;
			this.routeFactory = routeFactory;
		}
		
		@Override
		public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime, Person person) {
			
			Leg newLeg = this.populationFactory.createLeg(mode);
			newLeg.setDepartureTime(departureTime);
			newLeg.setTravelTime(0.0);	// we do not know the travel time
			
			Route route = this.routeFactory.createRoute(mode, fromFacility.getLinkId(), toFacility.getLinkId());
			newLeg.setRoute(route);

			return Arrays.asList(newLeg);
		}

		@Override
		public StageActivityTypes getStageActivityTypes() {
			return EmptyStageActivityTypes.INSTANCE;
		}
		
	}

	private static class PlanRouterProvider {

		private final TripRouterFactory tripRouterFactory;
		private final RoutingContext routingContext;
		
		public PlanRouterProvider(TripRouterFactory tripRouterFactory, RoutingContext routingContext) {
			this.tripRouterFactory = tripRouterFactory;
			this.routingContext = routingContext;
			
		}
		
		public PlanAlgorithm getPlanAlgorithm() {
			return new PlanRouter(this.tripRouterFactory.instantiateAndConfigureTripRouter(routingContext));
		}
	}
}
