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
import java.util.Iterator;
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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * This class provides the basic functionality to calculate congestion effects which may be used for internalization.
 * One of the main tasks is to keep track of the queues on each link.
 *
 * 
 * @author ikaddoura
 *
 */
public class CongestionInfoHandler implements
LinkEnterEventHandler,
LinkLeaveEventHandler,
TransitDriverStartsEventHandler,
PersonDepartureEventHandler, 
PersonStuckEventHandler,
Wait2LinkEventHandler,
PersonArrivalEventHandler {

	private final static Logger log = Logger.getLogger(CongestionInfoHandler.class);

	private final Scenario scenario;
	private final EventsManager events;
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id<Link>, LinkCongestionInfo>();

	private double delayNotInternalized_roundingErrors = 0.;
	private double totalInternalizedDelay = 0.;

	private Map<Id<Vehicle>, Id<Person>> vehicleId2personId = new HashMap<>() ;

	CongestionInfoHandler(EventsManager events, Scenario scenario) {
		this.scenario = scenario;
		this.events = events;

		if (this.scenario.getNetwork().getCapacityPeriod() != 3600.) {
			log.warn("Capacity period is other than 3600.");
		}

		if ( this.scenario.getConfig().parallelEventHandling().getNumberOfThreads()!= null) {
			log.warn("Parallel event handling is not tested. It should not work properly.");
		}

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
		
		this.delayNotInternalized_roundingErrors = 0.;
		this.totalInternalizedDelay = 0.;
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
				// bottleneck is not active anymore.
				// yyyy is this really the correct definition?  Or should we also look at delay? kai, sep'15

				// bottleneck no longer active. However, first check for combination of flow and storage delay:
				DelayInfo agentAheadDelayInfo = linkInfo.getFlowQueue().getLast() ;
				double freeSpeedLeaveTimeGap = delayInfo.freeSpeedLeaveTime - agentAheadDelayInfo.freeSpeedLeaveTime ; 
				// (otherwise there would have been flow delay)

				/* The following is to catch the possibility of agent getting delayed due to flow capacity and storage
				 * capacity respectively.
				 */
				if(freeSpeedLeaveTimeGap < linkInfo.getMarginalDelayPerLeavingVehicle_sec()){
					// Though bottleneck is not active, last leaving agent is causing delay.
				} else {
					linkInfo.getFlowQueue().clear();
				}
			}
		}

	}

	/**
	 * @param now time at which affected agent is delayed
	 * @param linkId link at which affected agent is delayed
	 * @param affectedAgentDelayInfo delayInfo of affected agent to get free speed link leave time and person id
	 * @param remainingDelay at this step, it is delay of the affected agent
	 * @return the remaining uncharged delay
	 * <p>
	 * Charging the agents that are in the flow queue.
	 * Do this step-wise comparing the freespeed leave time of two subsequent agents (agent 'ahead' and agent 'behind').
	 */
	final double computeFlowCongestionAndReturnStorageDelay(double now, Id<Link> linkId, DelayInfo affectedAgentDelayInfo, double remainingDelay) {		

		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get( linkId );

		for (Iterator<DelayInfo> it = linkInfo.getFlowQueue().descendingIterator() ; remainingDelay > 0.0 && it.hasNext() ; ) {
			// Get the agent 'ahead' from the flow queue. The agents 'ahead' are considered as causing agents.
			DelayInfo agentAheadDelayInfo = it.next() ;

			double allocatedDelay = Math.min(linkInfo.getMarginalDelayPerLeavingVehicle_sec(), remainingDelay);

			CongestionEvent congestionEvent = new CongestionEvent(now, "flowAndStorageCapacity", agentAheadDelayInfo.personId, 
					affectedAgentDelayInfo.personId, allocatedDelay, linkId, agentAheadDelayInfo.linkEnterTime );
			this.events.processEvent(congestionEvent); 

			this.totalInternalizedDelay += allocatedDelay ;

			remainingDelay = remainingDelay - allocatedDelay;

		}

		if(remainingDelay > 0. && remainingDelay <=1 ){
			// The remaining delay of up to 1 sec may result from rounding errors. The delay caused by the flow capacity sometimes varies by 1 sec.
			// Setting the remaining delay to 0 sec.
			this.delayNotInternalized_roundingErrors += remainingDelay;
			remainingDelay = 0.;
		}

		return remainingDelay;
	}
	
	public double getDelayNotInternalized_roundingErrors() {
		return delayNotInternalized_roundingErrors;
	}

	public void addToDelayNotInternalized_roundingErrors(
			double delayNotInternalized_roundingErrors) {
		this.delayNotInternalized_roundingErrors += delayNotInternalized_roundingErrors;
	}

	public double getTotalInternalizedDelay() {
		return totalInternalizedDelay;
	}

	public void addToTotalInternalizedDelay(double totalInternalizedDelay) {
		this.totalInternalizedDelay += totalInternalizedDelay;
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
