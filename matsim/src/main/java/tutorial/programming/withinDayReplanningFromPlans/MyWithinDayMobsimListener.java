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

package tutorial.programming.withinDayReplanningFromPlans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.mobsim.framework.HasPerson;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.utils.EditRoutes;

import javax.inject.Singleton;

/**
 * @author nagel
 *
 */
@Singleton
class MyWithinDayMobsimListener implements MobsimBeforeSimStepListener {
    
	private static final Logger log = Logger.getLogger("dummy");
	
	@Inject private TripRouter tripRouter;
	@Inject private Scenario scenario;
	
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;
	
	@Inject private Map<String, TravelTime> travelTimes ;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories ;

	@Inject private PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	
	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		
		Netsim mobsim = (Netsim) event.getQueueSimulation() ;

		Collection<MobsimAgent> agentsToReplan = getAgentsToReplan(mobsim); 
				
		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma, mobsim);
		}
	}
	
	private static List<MobsimAgent> getAgentsToReplan(Netsim mobsim ) {

		List<MobsimAgent> set = new ArrayList<MobsimAgent>();

		// don't do anything for most time steps:
		if (Math.floor(mobsim.getSimTimer().getTimeOfDay()) !=  22000.0) {
			return set;
		}
		
		// find agents that are en-route (more interesting case)
		for (NetsimLink link:mobsim.getNetsimNetwork().getNetsimLinks().values()){
			for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
				MobsimDriverAgent agent=vehicle.getDriver();
				System.out.println(agent.getId());
				if ( true ) { // some condition ...
					System.out.println("found agent");
					set.add(agent);
				}
			}
		}

		return set;

	}

	private boolean doReplanning(MobsimAgent agent, Netsim mobsim ) {
		double now = mobsim.getSimTimer().getTimeOfDay() ;
		
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
		
		List<PlanElement> planElements = plan.getPlanElements() ;
		final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		
		if ( !(planElements.get(planElementsIndex+1) instanceof Activity || !(planElements.get(planElementsIndex+2) instanceof Leg)) ) {
			log.error( "this version of withinday replanning cannot deal with plans where legs and acts do not alternate; returning ...") ;
			return false ;
		}

		// now the real work begins. This, as an example, changes the activity (i.e. the destination of the current leg) and then
		// re-splices the plan
		
		Id<Link> newDestinationLinkId = Id.create("22", Link.class) ;
		Activity newAct = mobsim.getScenario().getPopulation().getFactory().createActivityFromLinkId("w", newDestinationLinkId ) ;
		newAct.setMaximumDuration(3600);
		
		planElements.set( planElementsIndex+1, newAct );
		
		// =============================================================================================================
		// =============================================================================================================
		// EditRoutes at this point only works for car routes

		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		
		TravelDisutility travelDisutility = travelDisutilityFactories.get( TransportMode.car ).createTravelDisutility(travelTime) ;

		Network network = scenario.getNetwork() ;

		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime ) ;

		EditRoutes editRoutes = new EditRoutes( scenario.getNetwork(), pathCalculator, ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory() ) ;
		
		// new Route for current Leg.
		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
		final Person person = ((HasPerson) agent).getPerson();
		final Integer linkIdx = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);

//		EditRoutes.relocateCurrentLegRoute(leg, person, linkIdx, newDestinationLinkId, now, scenario.getNetwork(), tripRouter);
		editRoutes.relocateCurrentLegRoute(leg, person, linkIdx, newDestinationLinkId, now) ;
		
		// the route _from_ the modified activity also needs to be replanned:
		Leg futureLeg = (Leg) plan.getPlanElements().get(planElementsIndex + 2);
		EditRoutes.relocateFutureLegRoute(futureLeg, newDestinationLinkId, futureLeg.getRoute().getEndLinkId(), person, 
				scenario.getNetwork(), tripRouter);
		
		// =============================================================================================================
		// =============================================================================================================
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(agent);
		
		return true;
	}


}
