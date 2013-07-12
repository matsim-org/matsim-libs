/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayInitialRoutesController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.DefaultTripRouterFactoryImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegStartedIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

/**
 * Creates initial car routes during the first iteration using within-day replanning. 
 * Doing so should hopefully result in a more balanced traffic load in the network 
 * than creating all routes initially on an empty network. 
 * 
 * Since routes are created within-day, we ensure that all plans contain at least
 * dummy routes, otherwise PersonPrepareForSim would create them, which is not
 * necessary but very time consuming.
 *  
 * @author cdobler
 */
public class WithinDayInitialRoutesController extends WithinDayController implements IterationStartsListener {

	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifierFactory startedLegFactory;
	private DuringLegIdentifier legPerformingIdentifier;
	private DuringLegIdentifier legStartedIdentifier;
	
	private TransportModeFilterFactory carLegAgentsFilterFactory;
	private WithinDayInitialRoutesTripRouterFactory tripRouterFactory;
	
	private double duringLegReroutingShare = 0.10;
	
	private boolean duringLegRerouting = true;
	private boolean initialLegRerouting = true;
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: WithinDayInitialRoutesController config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new WithinDayInitialRoutesController(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
	
	public WithinDayInitialRoutesController(Scenario scenario) {
		super(scenario);
		
		init();
	}
	
	public WithinDayInitialRoutesController(String[] args) {
		super(args);
		
		init();
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
	
	private void init() {
		/*
		 * Change the persons' original plans. By doing so, the (hopefully) optimized
		 * routes are written to the output plans file.
		 */
		ExperimentalBasicWithindayAgent.copySelectedPlan = false;

		this.setNumberOfReplanningThreads( this.config.global().getNumberOfThreads());
		
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		this.setModesAnalyzedByTravelTimeCollector(analyzedModes);
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		/*
		 * Disable dummy routes creation.
		 */
		this.tripRouterFactory.setReplaceDummyModes(false);
		
		/*
		 * Disable Within-Day Replanning after first Iteration.
		 */
		if (event.getIteration() > 0) {
			this.getWithinDayEngine().doInitialReplanning(false);
			this.getWithinDayEngine().doDuringLegReplanning(false);
			this.getWithinDayEngine().doDuringActivityReplanning(false);
			
			this.events.removeHandler(this.getTravelTimeCollector());
			this.getFixedOrderSimulationListener().removeSimulationListener(this.getTravelTimeCollector());
		}
	}
	
	@Override
	protected void setUp() {
		/*
		 * The Controler initialized the LeastCostPathCalculatorFactory here, which is required
		 * by the replanners.
		 */
		super.setUp();
		
		/*
		 * Replace TripRouterFactory with WithinDayInitialRoutesTripRouterFactory which creates only
		 * dummy routes for within-day replanned modes.
		 */
		PopulationFactory populationFactory = this.getPopulation().getFactory();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		Set<String> dummyModes = CollectionUtils.stringToSet(TransportMode.car);
		tripRouterFactory = new WithinDayInitialRoutesTripRouterFactory(DefaultTripRouterFactoryImpl.createRichTripRouterFactoryImpl(this.getScenario()), dummyModes,
				populationFactory, routeFactory);
		this.setTripRouterFactory(tripRouterFactory);
	}
	
	private void initIdentifiers() {
		
		/*
		 * During Leg Identifiers
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);
		
		if (initialLegRerouting || duringLegRerouting) {
			carLegAgentsFilterFactory = new TransportModeFilterFactory(duringLegRerouteTransportModes);
			this.getMobsimListeners().add(carLegAgentsFilterFactory);			
		}

		if (duringLegRerouting) {
			duringLegFactory = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap());
			duringLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
			this.legPerformingIdentifier = duringLegFactory.createIdentifier();	
			this.legPerformingIdentifier.addAgentFilter(new ProbabilityFilterFactory(this.duringLegReroutingShare).createAgentFilter());
		}
		
		if (initialLegRerouting) {
			startedLegFactory = new LegStartedIdentifierFactory(this.getLinkReplanningMap());
			startedLegFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
			this.legStartedIdentifier = startedLegFactory.createIdentifier();			
		}
	}
	
	@Override
	protected void initReplanners(QSim sim) {
		
		this.initIdentifiers();
		
		this.initWithinDayTripRouterFactory();
		this.getWithinDayEngine().setTripRouterFactory(this.getWithinDayTripRouterFactory());
		
		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		
		if (duringLegRerouting) {
			duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine());
			duringLegReplannerFactory.addIdentifier(this.legPerformingIdentifier);
			this.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);			
		}
		
		if (initialLegRerouting) {
			duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine());
			duringLegReplannerFactory.addIdentifier(this.legStartedIdentifier);
			this.getWithinDayEngine().addDuringLegReplannerFactory(duringLegReplannerFactory);			
		}
	}
	
	private static class WithinDayInitialRoutesTripRouterFactory implements TripRouterFactory {

		private final TripRouterFactory tripRouterFactory;
		private final Set<String> dummyModes;
		private final PopulationFactory populationFactory;
		private final ModeRouteFactory routeFactory;
		
		private boolean replaceDummyModes = true;
		
		public WithinDayInitialRoutesTripRouterFactory(TripRouterFactory tripRouterFactory, Set<String> dummyModes, 
				PopulationFactory populationFactory, ModeRouteFactory routeFactory) {
			this.tripRouterFactory = tripRouterFactory;
			this.dummyModes = dummyModes;
			this.populationFactory = populationFactory;
			this.routeFactory = routeFactory;
		}
		
		public void setReplaceDummyModes(boolean replace) {
			this.replaceDummyModes = replace;
		}
		
		@Override
		public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {
			TripRouter tripRouter = tripRouterFactory.instantiateAndConfigureTripRouter(iterationContext);

			if (replaceDummyModes) {
				// replace routing modules for dummy modes
				for (String mode : dummyModes) {
					RoutingModule routingModule = new WithinDayInitialRoutesRoutingModule(mode, populationFactory, routeFactory);
					tripRouter.setRoutingModule(mode, routingModule);
				}				
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
}