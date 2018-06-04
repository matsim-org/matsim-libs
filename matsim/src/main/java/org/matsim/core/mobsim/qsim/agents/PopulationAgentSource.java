/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import java.util.*;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;

public final class PopulationAgentSource implements AgentSource {
	private static final Logger log = Logger.getLogger( PopulationAgentSource.class );

	private final Population population;
	private final AgentFactory agentFactory;
	private final QSim qsim;
	private final Collection<String> mainModes;
	private Map<Id<Vehicle>,Id<Link>> seenVehicleIds = new HashMap<>() ;

	@Inject
	public PopulationAgentSource(Population population, AgentFactory agentFactory, QSim qsim ) {
		this.population = population;
		this.agentFactory = agentFactory;
		this.qsim = qsim;  
		this.mainModes = qsim.getScenario().getConfig().qsim().getMainModes();
	}

	@Override
	public void insertAgentsIntoMobsim() {
		for (Person p : population.getPersons().values()) {
			MobsimAgent agent = this.agentFactory.createMobsimAgentFromPerson(p);
			qsim.insertAgentIntoMobsim(agent);
		}
		for (Person p : population.getPersons().values()) {
			insertVehicles(p);
		}
	}
	
	private void insertVehicles(Person person) {
		// this is called in every iteration.  So if a route without a vehicle id is found (e.g. after mode choice),
		// then the id is generated here.  kai/amit, may'18
		
		Map<String,Id<Vehicle>> seenModes = new HashMap<>();
		for ( Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan()) ) {
			
			// only simulated modes get vehicles:
			if ( !this.mainModes.contains(leg.getMode()) ) {
				continue ;
			}
			
			// determine the vehicle ID
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			Id<Vehicle> vehicleId = null ;
			if (route != null) {
				vehicleId = route.getVehicleId();
			}
			if (vehicleId == null) {
				vehicleId = PrepareForSimImpl.createAndSetAutomaticVehicleId(person.getId(), leg.getMode(), route, qsim.getScenario().getConfig().qsim() );
			}
			
			// determine the vehicle (not the same as just its ID):

			// we have seen the mode before in the plan:
			if ( seenModes.keySet().contains( leg.getMode() ) ) {
				if (vehicleId == null && route != null) {
					vehicleId = seenModes.get(leg.getMode());
					route.setVehicleId(vehicleId);
					// yyyy what is the meaning of route==null? kai, jun'18
				}
				continue;
			}
			
			// if we are here, we haven't seen the mode before in this plan
			
			// now memorizing mode and its vehicle ID:
			seenModes.put(leg.getMode(),vehicleId);
			
			// find the vehicle from the vehicles container.  It should be there, see automatic vehicle creation in PrepareForSim.
			Vehicle vehicle = qsim.getScenario().getVehicles().getVehicles().get(vehicleId);
			Gbl.assertNotNull(vehicle);
				
			// find the link ID of where to place the vehicle:
			Id<Link> vehicleLinkId = findVehicleLink(person);
				
			// Checking if the vehicle has been seen before:
			Id<Link> result = this.seenVehicleIds.get( vehicleId ) ;
			if ( result != null ) {
				log.info( "have seen vehicle with id " + vehicleId + " before; not placing it again." );
				if ( result != vehicleLinkId ) {
					throw new RuntimeException("vehicle placement error: vehicleId=" + vehicleId +
											   "; previous placement link=" + vehicleLinkId + "; current placement link=" + result ) ;
				}
				// yyyyyy The above condition is too strict; it should be possible that one person drives
				// a car to some place, and some other person picks it up there.  However, this
				// method here is sorted by persons, not departure times, and so we don't know
				// which plan needs the vehicle first. (Looks to me that it should actually be possible
				// to resolve this.)
				
			} else {
				this.seenVehicleIds.put( vehicleId, vehicleLinkId ) ;
				qsim.createAndParkVehicleOnLink(vehicle, vehicleLinkId);
			}
		}
	}

	/**
	 *	A more careful way to decide where this agent should have its vehicles created
	 *  than to ask agent.getCurrentLinkId() after creation.
	 * @param person TODO
	 */
	private Id<Link> findVehicleLink(Person person ) {
		/* Cases that come to mind:
		 * (1) multiple persons share car located at home, but possibly brought to different place by someone else.  
		 *      This is treated by the following algo.
		 * (2) person starts day with non-car leg and has car parked somewhere else.  This is NOT treated by the following algo.
		 *      It could be treated by placing the vehicle at the beginning of the first link where it is needed, but this would not
		 *      be compatible with variant (1).
		 *  Also see comment in insertVehicles.
		 */
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				ActivityFacilities facilities = this.qsim.getScenario().getActivityFacilities() ;
				Config config = this.qsim.getScenario().getConfig() ;
				final Id<Link> activityLinkId = PopulationUtils.computeLinkIdFromActivity(activity, facilities, config ) ;
				if (activityLinkId != null) {
					return activityLinkId;
				}
			} else if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				if (leg.getRoute().getStartLinkId() != null) {
					return leg.getRoute().getStartLinkId();
				}
			}
		}
		throw new RuntimeException("Don't know where to put a vehicle for this agent.");
	}

}
