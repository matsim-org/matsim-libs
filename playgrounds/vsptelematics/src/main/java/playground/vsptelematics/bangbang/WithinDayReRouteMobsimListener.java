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

package playground.vsptelematics.bangbang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.utils.EditRoutes;

import com.google.inject.Inject;

import playground.vsptelematics.bangbang.KNAccidentScenario.MyIterationCounter;

/**
 * @author nagel
 *
 */
class WithinDayReRouteMobsimListener implements MobsimBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(WithinDayReRouteMobsimListener.class);

	private final Scenario scenario;
	private EditRoutes editRoutes ;

	private MyIterationCounter iterationCounter;
	
	@Inject
	WithinDayReRouteMobsimListener(Scenario scenario, LeastCostPathCalculatorFactory pathAlgoFactory, TravelTime travelTime,
			Map<String, TravelDisutilityFactory> travelDisutilityFactories, MyIterationCounter ic ) {
		this.scenario = scenario ;
		{
			TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car).createTravelDisutility( travelTime ) ;
			LeastCostPathCalculator pathAlgo = pathAlgoFactory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime) ;
			RouteFactories routeFactory = scenario.getPopulation().getFactory().getRouteFactories() ;
			this.editRoutes = new EditRoutes( scenario.getNetwork(), pathAlgo, routeFactory ) ;
			this.iterationCounter = ic ;
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		if ( this.iterationCounter.getIteration() >= 11 ) {
			log.warn("NOT replanning"); 
			return ;
		}
		
		
		Collection<MobsimAgent> agentsToReplan = getAgentsToReplan( (Netsim) event.getQueueSimulation() ); 
		
		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma, (Netsim) event.getQueueSimulation());
		}
	}
	
	private static int cnt2 = 0 ;

	static List<MobsimAgent> getAgentsToReplan(Netsim mobsim ) {

		List<MobsimAgent> set = new ArrayList<>();

		final double now = mobsim.getSimTimer().getTimeOfDay();
		if ( now < 8.*3600. || Math.floor(now) % 10 != 0 ) {
			return set;
		}

		// find agents that are on the "interesting" links:
		for ( Id<Link> linkId : KNAccidentScenario.replanningLinkIds ) {
			NetsimLink link = mobsim.getNetsimNetwork().getNetsimLink( linkId ) ;
			for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
				MobsimDriverAgent agent=vehicle.getDriver();
				if ( KNAccidentScenario.replanningLinkIds.contains( agent.getCurrentLinkId() ) ) {
					double proba = 1. ;
//					System.out.println("found agent");
					if ( cnt2==0 ) {
						log.warn("only replanning with proba=" + proba + "!" );
						cnt2++ ;
					}
					if ( MatsimRandom.getRandom().nextDouble() < proba ) {
						set.add(agent);
					}
				}
			}
		}

		return set;

	}
	
	private static int cnt = 0 ;

	private boolean doReplanning(MobsimAgent agent, Netsim netsim ) {
		double now = netsim.getSimTimer().getTimeOfDay() ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ; 
		
		if ( !WithinDayAgentUtils.isReplannableCarLeg(agent) ) {
			return false ;
		}

		final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		// ---

		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
		
		// for vis:
		List<Id<Link>> oldLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ; // forces a copy, which I need later

		// "real" action:
		final int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);
		editRoutes.replanCurrentLegRoute(leg, plan.getPerson(), currentLinkIndex, now ) ;
		
		// for vis:
		ArrayList<Id<Link>> currentLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ;
		if ( !Arrays.deepEquals(oldLinkIds.toArray(), currentLinkIds.toArray()) ) {
			if ( cnt < 10 ) {
				log.warn("modified route");
				cnt++ ;
			}
			this.scenario.getPopulation().getPersonAttributes().putAttribute( agent.getId().toString(), "marker", true ) ;
		}

		// ---

		// finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(agent);

		return true;
	}

}

