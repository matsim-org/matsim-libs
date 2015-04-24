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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.utils.EditRoutes;

/**
 * @author nagel
 *
 */
class KNWithinDayMobsimListener2 implements MobsimBeforeSimStepListener {
    
	private static final Logger log = Logger.getLogger("dummy");
	
	private TripRouter tripRouter;
	private Scenario scenario;

	private Netsim mobsim;

	KNWithinDayMobsimListener2(TripRouter tripRouter) {
		this.tripRouter = tripRouter;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		
		this.mobsim = (Netsim) event.getQueueSimulation() ;
		this.scenario = mobsim.getScenario();

		Collection<MobsimAgent> agentsToReplan = getAgentsToReplan(mobsim); 
				
		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma);
		}
	}
	
	private static List<MobsimAgent> getAgentsToReplan(Netsim mobsim ) {

		List<MobsimAgent> set = new ArrayList<MobsimAgent>();

		final double now = mobsim.getSimTimer().getTimeOfDay();
		if ( now < 8.*3600. || Math.floor(now) % 10 != 0 ) {
			return set;
		}
		
		// find agents that are en-route (more interesting case)
		for (NetsimLink link:mobsim.getNetsimNetwork().getNetsimLinks().values()){
			for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
				MobsimDriverAgent agent=vehicle.getDriver();
//				System.out.println(agent.getId());
				if ( KNBangBang.replanningLinkIds.contains( agent.getCurrentLinkId() ) ) {
					System.out.println("found agent");
					set.add(agent);
				}
			}
		}
		
		return set;
	}

	private boolean doReplanning(MobsimAgent agent ) {
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

		// ---
		
		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
		ArrayList<Id<Link>> previousLinkIds = new ArrayList<>(((NetworkRoute) leg.getRoute()).getLinkIds());
		
		Id<Link> currentLinkId = agent.getCurrentLinkId() ;
		
		boolean flag = false ;
		for ( Id<Link> linkId : previousLinkIds ) {
			if ( KNBangBang.replanningLinkIds.contains( currentLinkId ) ) {
				flag = true ;
			}
			if ( flag ) {
				System.out.println( linkId ) ;
			}
		}
		
		
		// new Route for current Leg.
//		final Id<Link> destinationLinkId = ((Activity) plan.getPlanElements().get(planElementsIndex+1)).getLinkId();
//		EditRoutes.relocateCurrentRoute(agent, destinationLinkId, now, scenario.getNetwork(), tripRouter);
//		ArrayList<Id<Link>> currentLinkIds = new ArrayList<>(((NetworkRoute) leg.getRoute()).getLinkIds());
//
//		if ( !Arrays.deepEquals(previousLinkIds.toArray(), currentLinkIds.toArray()) ) {
//			log.warn("modified route");
//			this.scenario.getPopulation().getPersonAttributes().putAttribute( agent.getId().toString(), "marker", true ) ;
//		}

		// ---
		
		// finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(agent);
//		WithinDayAgentUtils.rescheduleActivityEnd(agent, mobsim);
		
		return true;
	}


}
