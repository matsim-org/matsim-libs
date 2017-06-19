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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author nagel
 *
 */
public class ManualDetour implements MobsimBeforeSimStepListener {

	public static final Logger log = Logger.getLogger("dummy");

	private final Scenario scenario;

	private List<Id<Link>> alternativeLinks;

	private Id<Link> returnId = Id.createLinkId("4706699_26662459_26662476");

	private class AvoidAccidentTravelTimeAndDisutility implements TravelTime, TravelDisutility {
		FreespeedTravelTimeAndDisutility delegate = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		@Override public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			if ( KNAccidentScenario.accidentLinkId.equals( link.getId() ) ) {
				return 1e13 ;
			} else {
				return delegate.getLinkTravelDisutility(link, time, person, vehicle);
			}
		}
		@Override public double getLinkMinimumTravelDisutility(Link link) {
			if ( KNAccidentScenario.accidentLinkId.equals( link.getId() ) ) {
				return 1e13 ;
			} else {
				return delegate.getLinkMinimumTravelDisutility(link);
			}
		}
		@Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return delegate.getLinkTravelTime(link, time, person, vehicle);
		}
	}

	@Inject
	ManualDetour(Scenario scenario, LeastCostPathCalculatorFactory pathAlgoFactory ) {
		this.scenario = scenario ;

		AvoidAccidentTravelTimeAndDisutility fff = new AvoidAccidentTravelTimeAndDisutility() ;
		alternativeLinks = computeRouteLinkIds(pathAlgoFactory, fff, fff) ;

	}

	private List<Id<Link>> computeRouteLinkIds(LeastCostPathCalculatorFactory pathAlgoFactory, TravelTime tt,
			TravelDisutility td) {
		// it feels like one should have the following infrastructure as fields.  However, since it is only called once, at initialization,
		// can as well leave it local here. kai, apr'16

		LeastCostPathCalculator routeAlgo = pathAlgoFactory.createPathCalculator( scenario.getNetwork(), td, tt);
		final RoutingModule routingModule = DefaultRoutingModules.createPureNetworkRouter(TransportMode.car, scenario.getPopulation().getFactory(), 
				scenario.getNetwork(), routeAlgo);
		// (yy could probably use the "computer science" algorithm directly in the above. kai, apr'16)

		List<Id<Link>> links = null ;
		for ( Id<Link> currentId : KNAccidentScenario.replanningLinkIds ) {
			Facility<ActivityFacility> fromFacility = new LinkWrapperFacility(scenario.getNetwork().getLinks().get(currentId));
			Facility<ActivityFacility> toFacility = new LinkWrapperFacility(scenario.getNetwork().getLinks().get(returnId));

			final Leg leg = (Leg) routingModule.calcRoute(fromFacility, toFacility, 0, null).get(0);
			links = ((NetworkRoute)leg.getRoute()).getLinkIds() ;
		}
		return links;
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
//		double now = event.getSimulationTime() ;
//		double duration = 60 ;
//		if (                                                              now < 8.*3600+10*60 ) return ;
//		if ( 8.*3600 + 10*60 + duration < now && now < 8.*3600+20*60 ) return ;
//		if ( 8.*3600 + 20*60 + duration < now && now < 8.*3600+30*60 ) return ;
//		if ( 8.*3600 + 30*60 + duration < now && now < 8.*3600+40*60 ) return ;
//		if ( 8.*3600 + 40*60 + duration < now && now < 8.*3600+50*60 ) return ;
//		if ( 8.*3600 + 50*60 + duration < now  ) return ;

		Collection<MobsimAgent> agentsToReplan = WithinDayReRouteMobsimListener.getAgentsToReplan( (Netsim) event.getQueueSimulation() ); 

		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma);
		}
	}

	private boolean doReplanning(MobsimAgent agent ) {

		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ; 

		if ( !WithinDayAgentUtils.isReplannableCarLeg(agent) ) {
			return false ;
		}

		final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		// ---

		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
		final NetworkRoute oldRoute = (NetworkRoute) leg.getRoute();
		List<Id<Link>> oldLinkIds = new ArrayList<>( oldRoute.getLinkIds() ) ;
		// the above forces a copy, which I need later

		final int idx = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);

		if ( oldRoute.getLinkIds().contains( this.returnId ) ) {
			List<Id<Link>> copy = new ArrayList<>( oldRoute.getLinkIds() ) ;
			while (  !copy.get( idx ).equals( this.returnId )  ) {
				copy.remove( idx ) ;
			}
			copy.addAll( idx, this.alternativeLinks ) ;
			final RouteFactories modeRouteFactory = this.scenario.getPopulation().getFactory().getRouteFactories();
			NetworkRoute newRoute = modeRouteFactory.createRoute( NetworkRoute.class, oldRoute.getStartLinkId(), oldRoute.getEndLinkId()) ;
			newRoute.setLinkIds( oldRoute.getStartLinkId(), copy, oldRoute.getEndLinkId() );
			leg.setRoute(newRoute);
		}

		ArrayList<Id<Link>> currentLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ;
		if ( !Arrays.deepEquals(oldLinkIds.toArray(), currentLinkIds.toArray()) ) {
			log.warn("modified route");
			this.scenario.getPopulation().getPersonAttributes().putAttribute( agent.getId().toString(), "marker", true ) ;
		}

		// ---

		// finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(agent);

		return true;
	}

}

