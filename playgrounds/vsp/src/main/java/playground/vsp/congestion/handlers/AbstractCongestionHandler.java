/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.vsp.congestion.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;

/**
 * This handler calculates delays (caused by the flow and storage capacity), identifies the causing agent(s) and throws marginal congestion events.
 * Marginal congestion events can be used for internalization.
 * 1) For each agent leaving a link a total delay is calculated as the difference of actual leaving time and the leaving time according to freespeed.
 * 2) The delay due to the flow capacity of that link is computed and marginal congestion events are thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 3) The proportion of flow delay is deducted.
 * 4) The remaining delay leads back to the storage capacity of downstream links. The marginal congestion event is thrown, indicating the affected agent, the causing agent and the delay in sec.
 * 
 * @author ikaddoura
 *
 */
public class AbstractCongestionHandler implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler,
Wait2LinkEventHandler,
PersonArrivalEventHandler {

	private final static Logger log = Logger.getLogger(AbstractCongestionHandler.class);

	private final Scenario scenario;
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id<Link>, LinkCongestionInfo>();

	Map<Id<Person>,Integer> personId2legNr = new HashMap<>() ;
	Map<Id<Person>,Integer> personId2linkNr = new HashMap<>() ;

	private Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>() ;

	AbstractCongestionHandler(Scenario scenario) {
		this.scenario = scenario;

		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			log.warn("Capacity period is other than 3600.");
		}

		// TODO: Runtime exception if parallel events handling.

		if (this.scenario.getConfig().qsim().getFlowCapFactor() != 1.0) {
			log.warn("Flow capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().qsim().getStorageCapFactor() != 1.0) {
			log.warn("Storage capacity factor unequal 1.0 is not tested.");
		}

		if (this.scenario.getConfig().transit().isUseTransit()) {
			log.warn("Mixed traffic (simulated public transport) is not tested. Vehicles may have different effective cell sizes than 7.5 meters.");
		}

	}

	@Override
	public final void reset(int iteration) {
		this.linkId2congestionInfo.clear();
		this.ptVehicleIDs.clear();
		
		//--
		this.personId2legNr.clear();
		this.personId2linkNr.clear();
	}

	@Override
	public final void handleEvent(TransitDriverStartsEvent event) {
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}

	@Override
	public final void handleEvent(PersonStuckEvent event) {
		log.warn("An agent is stucking. No garantee for right calculation of external congestion effects: " + event.toString());
	}

	@Override
	public final void handleEvent( Wait2LinkEvent event ) {
		this.vehicleId2personId.put( event.getVehicleId(), event.getPersonId() ) ;
	}
	
	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){ // car!
			LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo( event.getLinkId(), linkId2congestionInfo, scenario ) ;
			linkInfo.getPersonId2freeSpeedLeaveTime().put(event.getPersonId(), event.getTime() + 1);
			linkInfo.getPersonId2linkEnterTime().put(event.getPersonId(), event.getTime());
			
			AgentOnLinkInfo agentInfo = new AgentOnLinkInfo.Builder().setAgentId( event.getPersonId() )
					.setLinkId( event.getLinkId() ).setEnterTime( event.getTime() ).setFreeSpeedLeaveTime( event.getTime()+1. ).build();
			linkInfo.getAgentsOnLink().put( event.getPersonId(), agentInfo ) ;
		}

		//--
		final Integer cnt = this.personId2legNr.get( event.getPersonId() );
		if ( cnt == null ) {
			this.personId2legNr.put( event.getPersonId(), 0 ) ; // start counting with zero!!
		} else {
			this.personId2legNr.put( event.getPersonId(), cnt + 1 ) ; 
		}
		this.personId2linkNr.put( event.getPersonId(), 0 ) ; // start counting with zero!!
	}

	@Override
	public final void handleEvent(LinkEnterEvent event) {
		if (this.ptVehicleIDs.contains(event.getVehicleId())){
			log.warn("Public transport mode. Mixed traffic is not tested.");
		} else { // car! 
			LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo( event.getLinkId(), linkId2congestionInfo, scenario ) ;
			linkInfo.getPersonId2freeSpeedLeaveTime().put(Id.createPersonId(event.getVehicleId()), event.getTime() + linkInfo.getFreeTravelTime() + 1.0);
			linkInfo.getPersonId2linkEnterTime().put(Id.createPersonId(event.getVehicleId()), event.getTime());

			AgentOnLinkInfo agentInfo = new AgentOnLinkInfo.Builder().setAgentId( event.getPersonId() ).setLinkId( event.getLinkId() )
					.setEnterTime( event.getTime() ).setFreeSpeedLeaveTime( event.getTime()+linkInfo.getFreeTravelTime()+1. ).build();
			linkInfo.getAgentsOnLink().put( event.getPersonId(), agentInfo ) ;
		}
		// ---
		int linkNr = this.personId2linkNr.get( event.getPersonId() ) ;
		this.personId2linkNr.put( event.getPersonId(), linkNr + 1 ) ;
		
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {
		throw new RuntimeException("Not implemented. Aborting...");
		// the following should be moved to different versions
	}
	
	@Override
	public /*final*/ void handleEvent( PersonArrivalEvent event ) {
		LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo( event.getLinkId(), linkId2congestionInfo, scenario) ;
		linkInfo.getAgentsOnLink().remove( event.getPersonId() ) ;
	}
	
	final void updateFlowAndDelayQueues(double time, DelayInfo delayInfo, LinkCongestionInfo linkInfo) {
		if ( linkInfo.getDelayQueue().isEmpty() ) {
			// queue is already empty; nothing to do
		} else {
			double delay = time - delayInfo.freeSpeedLeaveTime ;
			if ( delay < 0 ) {
				linkInfo.getDelayQueue().clear() ;
			}
		}
		if (linkInfo.getFlowQueue().isEmpty() ) {
			// queue is already empty; nothing to do
		} else {
			double earliestLeaveTime = linkInfo.getLastLeaveEvent().getTime() + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			if ( time > earliestLeaveTime + 1.) {
				// yyyy is this really the correct definition?  Or should we also look at delay? kai, sep'15
				
//				// bottleneck no longer active. However, first check for combination of flow and storage delay:
//				DelayInfo causingAgentInfo = linkInfo.getFlowQueue().getLast() ;
//				double timeGap = delayInfo.freeSpeedLeaveTime - causingAgentInfo.freeSpeedLeaveTime ; 
//				if ( timeGap > linkInfo.getMarginalDelayPerLeavingVehicle_sec() ) {
//					 // (otherwise there would have been flow delay)
					
					linkInfo.getFlowQueue().clear();
//				}
			}
		}

	}

	final Map<Id<Link>, LinkCongestionInfo> getLinkId2congestionInfo() {
		return linkId2congestionInfo;
	}

	final Scenario getScenario() {
		return this.scenario ;
	}

	public List<Id<Vehicle>> getPtVehicleIDs() {
		return ptVehicleIDs;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicleId2personId() {
		return vehicleId2personId;
	}
	
	
	
}
