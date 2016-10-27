/* *********************************************************************** *
 * project: org.matsim.*
 * MyWithinDayMobsimListener.java
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

package playground.ikaddoura.incidents.incidentWithinDayReplanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.utils.EditRoutes;

import com.google.inject.Inject;

import playground.ikaddoura.incidents.NetworkChangeEventsUtils;

/**
 * @author nagel, ikaddoura
 *
 */
class IncidentBestRouteMobsimListener implements MobsimBeforeSimStepListener, IterationStartsListener {

	private static final Logger log = Logger.getLogger(IncidentBestRouteMobsimListener.class);
	
	private final Scenario scenario;
	private final int withinDayReplanInterval = 3600;
	
	private Set<Id<Person>> withinDayReplanningAgents = new HashSet<>();
	private EditRoutes editRoutes;
	private int counter;
	
	@Inject
	IncidentBestRouteMobsimListener(
			Scenario scenario,
			LeastCostPathCalculatorFactory pathAlgoFactory,
			TravelTime travelTime,
			Map<String, TravelDisutilityFactory> travelDisutilityFactories
			) {
		
		this.scenario = scenario;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car).createTravelDisutility( travelTime ) ;
		LeastCostPathCalculator pathAlgo = pathAlgoFactory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime) ;

		this.editRoutes = new EditRoutes(scenario.getNetwork(), pathAlgo, scenario.getPopulation().getFactory());
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		Collection<MobsimAgent> agentsToReplan = getAgentsToReplan( (Netsim) event.getQueueSimulation() ); 
		
		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma, (Netsim) event.getQueueSimulation());
		}		
		if (agentsToReplan.size() > 0) {
			log.info("Number of modified routes at time " + Time.writeTime(event.getSimulationTime(), Time.TIMEFORMAT_HHMMSS) + ": " + counter);
		}

	}

	private List<MobsimAgent> getAgentsToReplan(Netsim mobsim) {

		List<MobsimAgent> agentsToReplan = new ArrayList<MobsimAgent>();

		final double now = mobsim.getSimTimer().getTimeOfDay();
		
		
		
		if ( Math.floor(now) % withinDayReplanInterval == 0 ) {

			counter = 0;
//			log.info("Within-day replanning at time " + now);
			
			for ( Id<Link> linkId : this.scenario.getNetwork().getLinks().keySet() ) {
				NetsimLink link = mobsim.getNetsimNetwork().getNetsimLink( linkId ) ;
				for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
					MobsimDriverAgent agent = vehicle.getDriver();
					
					if (withinDayReplanningAgents.contains(agent.getId())) {
						agentsToReplan.add(agent);
					}
				}
			}			
		}
		
		return agentsToReplan;
	}

	private boolean doReplanning(MobsimAgent agent, Netsim netsim ) {
		double now = netsim.getSimTimer().getTimeOfDay() ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ; 

		if (plan == null) {
			log.info( " we don't have a modifiable plan; returning ... ") ;
			return false;
		}
		if ( !(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Leg) ) {
			log.info( "agent not on leg; returning ... ") ;
			return false ;
		}
		if (!((Leg) WithinDayAgentUtils.getCurrentPlanElement(agent)).getMode().equals(TransportMode.car)) {
			log.info( "not a car leg; can only replan car legs; returning ... ") ;
			return false;
		}

		final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
		
		List<Id<Link>> oldLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ; // forces a copy, which I need later
		
		final int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ;
		editRoutes.replanCurrentLegRoute(leg, plan.getPerson(), currentLinkIndex, now ) ;
		
		ArrayList<Id<Link>> currentLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ;
		if ( !Arrays.deepEquals(oldLinkIds.toArray(), currentLinkIds.toArray()) ) {
			counter++;
//			log.warn("modified route");
//			log.warn("old route: " + oldLinkIds.toString());
//			log.warn("new route: " + currentLinkIds.toString());
		}

		WithinDayAgentUtils.resetCaches(agent);

		return true;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		log.info("Iteration starts. Computing the agents to be considered for within-day replanning...");
		
		withinDayReplanningAgents.clear();
		
		Set<Id<Link>> incidentLinkIds = NetworkChangeEventsUtils.getIncidentLinksFromNetworkChangeEventsFile(scenario);
		withinDayReplanningAgents.addAll(NetworkChangeEventsUtils.getPersonIDsOfAgentsDrivingAlongSpecificLinks(scenario, incidentLinkIds));
		
		if (withinDayReplanningAgents.isEmpty()) {
			log.warn("No agent considered for replanning.");
		}
	}

}

