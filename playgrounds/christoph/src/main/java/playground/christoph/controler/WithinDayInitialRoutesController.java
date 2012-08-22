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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.MultiModalTravelTimeWrapperFactory;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTimeFactory;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

/**
 * Creates initial routes using within-day replanning. Doing so should hopefully
 * result in a more balanced traffic load in the network than creating all routes
 * initially on an empty network. 
 * 
 * Since routes are created within-day, we ensure that all plans contain at least
 * dummy routes, otherwise PersonPrepareForSim would create them, which is not
 * necessary but very time consuming.
 *  
 * @author cdobler
 */
public class WithinDayInitialRoutesController extends WithinDayController implements StartupListener, MobsimInitializedListener {

	private DuringActivityIdentifierFactory duringActivityFactory;
	private DuringActivityIdentifier activityPerformingIdentifier;

	private DuringLegIdentifierFactory duringLegFactory;
	private DuringLegIdentifier legPerformingIdentifier;
	
	private CarLegAgentsFilterFactory carLegAgentsFilterFactory;
	
	private double duringLegReroutingShare = 0.10;
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new WithinDayInitialRoutesController(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}
		
	public WithinDayInitialRoutesController(String[] args) {
		super(args);
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	@Override
	protected void loadData() {
		super.loadData();
		
		/*
		 * Create dummy car routes, if routes are not already present.
		 */
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.getPopulation().getFactory()).getModeRouteFactory();
		DummyRoutesCreator dummyRoutesCreator = new DummyRoutesCreator(routeFactory);		
		dummyRoutesCreator.run(this.getPopulation());
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * Ensure that only a single iteration is performed.
		 */
		this.config.controler().setFirstIteration(0);
		this.config.controler().setLastIteration(0);
		
		/*
		 * Get number of threads from config file.
		 */
		int numReplanningThreads = this.config.global().getNumberOfThreads();
		
		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
//		PersonalizableTravelTimeFactory travelTimeCollectorWrapperFactory = new TravelTimeFactoryWrapper(this.getTravelTimeCollector());
		
		/*
		 * Create and initialize replanning manager and replanning maps.
		 */
		super.initReplanningManager(numReplanningThreads);
		super.getReplanningManager().setEventsManager(this.getEvents());	// set events manager to create replanning events
		super.createAndInitActivityReplanningMap();
		super.createAndInitLinkReplanningMap();
				
		// initialize Identifiers and Replanners
		this.initIdentifiers();
		this.initReplanners();
	}


	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		/*
		 * Give agents to the filter which removes agents with non-car legs.
		 */
		carLegAgentsFilterFactory.setAgents(((QSim) e.getQueueSimulation()).getAgents());
		
		/*
		 * We replace the selected plan of each agent with the executed plan which
		 * is adapted by the within day replanning modules. As a result, the adapted 
		 * plans are written after the simulation has ended to the output plans
		 * file, which in turn can be used as input file for another simulation.
		 */
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			if (agent instanceof ExperimentalBasicWithindayAgent) {
				Plan executedPlan = ((ExperimentalBasicWithindayAgent) agent).getSelectedPlan();
				PersonImpl person = (PersonImpl)((ExperimentalBasicWithindayAgent) agent).getPerson();
				person.removePlan(person.getSelectedPlan());
				person.addPlan(executedPlan);
				person.setSelectedPlan(executedPlan);
			}
		}
	}
	
	private void initIdentifiers() {
		
		/*
		 * Activity End Identifier
		 */
		carLegAgentsFilterFactory = new CarLegAgentsFilterFactory(); 
		duringActivityFactory = new ActivityEndIdentifierFactory(this.getActivityReplanningMap());
		duringActivityFactory.addAgentFilterFactory(carLegAgentsFilterFactory);
		activityPerformingIdentifier = duringActivityFactory.createIdentifier();
		
		/*
		 * During Leg Identifier
		 */		
		Set<String> duringLegRerouteTransportModes = new HashSet<String>();
		duringLegRerouteTransportModes.add(TransportMode.car);
		duringLegFactory = new LeaveLinkIdentifierFactory(this.getLinkReplanningMap(), duringLegRerouteTransportModes); 
		this.legPerformingIdentifier = duringLegFactory.createIdentifier();		
	}
	
	private void initReplanners() {
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.getPopulation().getFactory()).getModeRouteFactory();
								
		// create a copy of the MultiModalTravelTimeWrapperFactory...
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, TravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory().getTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());			
		}
		// ... and set the TravelTimeCollector for car mode
		TravelTimeCollectorFactory carTravelTimeFactory = this.getTravelTimeCollectorFactory();
		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, carTravelTimeFactory);

//		PersonalizableTravelTimeFactory timeFactory = this.getTravelTimeCollectorFactory();
		
		// add time dependent penalties to travel costs within the affected area
		TravelDisutilityFactory disutilityFactory = this.getTravelDisutilityFactory();

		LeastCostPathCalculatorFactory factory = this.getLeastCostPathCalculatorFactory();
		AbstractMultithreadedModule router = new ReplanningModule(config, network, disutilityFactory, timeFactory, factory, routeFactory);
		
		/*
		 * During Activity Replanner
		 */
		WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory;
		duringActivityReplannerFactory = new NextLegReplannerFactory(this.scenarioData, this.getReplanningManager(), router, 1.0);
		duringActivityReplannerFactory.addIdentifier(this.activityPerformingIdentifier);
		this.getReplanningManager().addDuringActivityReplannerFactory(duringActivityReplannerFactory);
		
		/*
		 * During Leg Replanner
		 */
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
		duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getReplanningManager(), router, duringLegReroutingShare);
		duringLegReplannerFactory.addIdentifier(this.legPerformingIdentifier);
		this.getReplanningManager().addDuringLegReplannerFactory(duringLegReplannerFactory);
	}
	
	private static class DummyRoutesCreator extends AbstractPersonAlgorithm implements PlanAlgorithm {

		private final ModeRouteFactory routeFactory;
		
		public DummyRoutesCreator(ModeRouteFactory routeFactory) {
			this.routeFactory = routeFactory;
		}
		
		@Override
		public void run(Plan plan) {
			for (int i = 0; i < plan.getPlanElements().size(); i++) {
				PlanElement planElement = plan.getPlanElements().get(i);
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals(TransportMode.car)) {
						if (leg.getRoute() == null) {
							Id startLinkId = ((Activity) plan.getPlanElements().get(i - 1)).getLinkId();
							Id endLinkId = ((Activity) plan.getPlanElements().get(i + 1)).getLinkId();
							leg.setRoute(routeFactory.createRoute(TransportMode.car, startLinkId, endLinkId));
						}
					}
				}
			}
		}

		@Override
		public void run(Person person) {
			for (Plan plan : person.getPlans()) {
				this.run(plan);
			}
		}
	}
	
	private static class CarLegAgentsFilterFactory implements AgentFilterFactory {

		private final Map<Id, MobsimAgent> agents = new HashMap<Id, MobsimAgent>();
			
		public void setAgents(Collection<MobsimAgent> mobsimAgents) {
			agents.clear();
			for (MobsimAgent agent : mobsimAgents) this.agents.put(agent.getId(), agent);
		}
		
		@Override
		public AgentFilter createAgentFilter() {
			return new CarLegAgentsFilter(this.agents);
		}
		
	}
	
	/*
	 * Used by agents that are going to end their current activity. If an agent's next
	 * leg's transport mode is car, the agent remains in the set, otherwise it is removed.
	 */
	private static class CarLegAgentsFilter implements AgentFilter {

		private final Map<Id, MobsimAgent> agents;
		
		public CarLegAgentsFilter(Map<Id, MobsimAgent> agents) {
			this.agents = agents;
		}
		
		@Override
		public void applyAgentFilter(Set<Id> set, double time) {
			Iterator<Id> iter = set.iterator();
			while (iter.hasNext()) {
				Id id = iter.next();
				MobsimAgent agent = agents.get(id);
				PlanAgent planAgent = (PlanAgent) agent;
				Leg nextLeg = (Leg) planAgent.getNextPlanElement();
				if (!nextLeg.getMode().equals(TransportMode.car)) iter.remove();
			}
		}
		
	}
}