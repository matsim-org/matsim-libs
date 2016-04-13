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
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;

import playground.vsp.congestion.AgentOnLinkInfo;
import playground.vsp.congestion.DelayInfo;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * This class provides the basic functionality to calculate congestion effects which may be used for internalization.
 * One of the main tasks is to keep track of the queues on each link.
 * 
 * @author ikaddoura
 *
 */
public class CongestionHandlerBaseImpl implements CongestionHandler {

	private final static Logger log = Logger.getLogger(CongestionHandlerBaseImpl.class);

	private final Scenario scenario;
	private final EventsManager events;
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<Id<Vehicle>>();
	private final Map<Id<Link>, LinkCongestionInfo> linkId2congestionInfo = new HashMap<Id<Link>, LinkCongestionInfo>();

	private double totalDelay = 0.;
	private double delayNotInternalized_roundingErrors = 0.;
	private double totalInternalizedDelay = 0.;

	private Vehicle2DriverEventHandler Veh2DriverDelegate = new Vehicle2DriverEventHandler();

	CongestionHandlerBaseImpl(EventsManager events, Scenario scenario) {
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

		this.totalDelay = 0.;
		this.delayNotInternalized_roundingErrors = 0.;
		this.totalInternalizedDelay = 0.;
		
		Veh2DriverDelegate.reset(iteration);
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
	public final void handleEvent( VehicleEntersTrafficEvent event ) {
		Veh2DriverDelegate.handleEvent(event);
	}

	@Override
	public final void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().toString().equals(TransportMode.car.toString())){ // car!
			LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo( event.getLinkId(), linkId2congestionInfo, scenario ) ;

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

			Id<Person> driverId = Veh2DriverDelegate.getDriverOfVehicle(event.getVehicleId());
			AgentOnLinkInfo agentInfo = new AgentOnLinkInfo.Builder().setAgentId( driverId ).setLinkId( event.getLinkId() )
					.setEnterTime( event.getTime() ).setFreeSpeedLeaveTime( event.getTime()+linkInfo.getFreeTravelTime()+1. ).build();
			linkInfo.getAgentsOnLink().put( driverId, agentInfo ) ;
		}
	}

	@Override
	public final void handleEvent(LinkLeaveEvent event) {
		// yy My preference would be if we found a solution where the basic bookkeeping (e.g. update flow and
		// delay queues) is done here.  However, delegation does not allow to have custom code in between 
		// standard code (as was the case before with calculateCongestion).  We need to consider if it is possible to 
		// rather have the standard code in one go and embed it in the delegated class.  In the sense of
		// class MyCongestionHandlerImpl {
		//    ... handleEvent( LinkLeaveEvent event ) {
		//             ... // custom code
		//             delegate.handleEvent( event ) ;
		//             ... // more custom code
		//    ...
		
		// coming here ...
		
		
		Id<Person> personId = Veh2DriverDelegate.getDriverOfVehicle( event.getVehicleId() ) ;

		LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo(event.getLinkId(), this.getLinkId2congestionInfo(), scenario);

		AgentOnLinkInfo agentInfo = linkInfo.getAgentsOnLink().get( personId ) ;

		DelayInfo delayInfo = new DelayInfo.Builder( agentInfo ).setLinkLeaveTime( event.getTime() ).build() ;

		CongestionHandlerBaseImpl.updateFlowAndDelayQueues(event.getTime(), delayInfo, linkInfo );


		linkInfo.getFlowQueue().add( delayInfo ) ;

		linkInfo.memorizeLastLinkLeaveEvent( event );

		linkInfo.getAgentsOnLink().remove( personId ) ;

		// global book-keeping:
		this.totalDelay += ( event.getTime() - delayInfo.freeSpeedLeaveTime );

		
	}

	@Override
	public final void handleEvent( PersonArrivalEvent event ) {
		LinkCongestionInfo linkInfo = CongestionUtils.getOrCreateLinkInfo( event.getLinkId(), linkId2congestionInfo, scenario) ;
		linkInfo.getAgentsOnLink().remove( event.getPersonId() ) ;
	}

	public final static void updateFlowAndDelayQueues(double time, DelayInfo delayInfo, LinkCongestionInfo linkInfo) {
		
		double delay = time - delayInfo.freeSpeedLeaveTime;

		if ( delay < 1.0 ) { 
			linkInfo.getFlowQueue().clear(); 
			return ; // should be ok but not tested (*)
		} 
		
		if (linkInfo.getFlowQueue().isEmpty() ) {
			// queue is already empty; nothing to do
			
			// seems like this should never be called when (*) is used, but in practice it happens, i.e. tests fail when the above if condition
			// is commented out.  maybe initialization?  kai, apr'16
		} else {
			double earliestLeaveTimeAfterVehicleAhead = linkInfo.getLastLeaveEvent().getTime() + linkInfo.getMarginalDelayPerLeavingVehicle_sec();
			if ( time > earliestLeaveTimeAfterVehicleAhead + 1.) {
				// Vehicle is delayed by more than 1/cap.  That is, we must be spill-back delayed.
				
				double freeSpeedLeaveTimeGap = delayInfo.freeSpeedLeaveTime - linkInfo.getFlowQueue().getLast().freeSpeedLeaveTime ; 

				if(freeSpeedLeaveTimeGap < linkInfo.getMarginalDelayPerLeavingVehicle_sec()){
					// we are ALSO be flow delayed.  Therefore, we consider the bottleneck still active
					
				} else {
					// we are NOT also be flow delayed.  The bottleneck is now inactive:
					linkInfo.getFlowQueue().clear();
				}
				// yy this is what I think we would have to accept if we wanted to bring V3 and V4 under the same umbrella.
				// It would, as I discussed via skype with Ihab, probably change the old result, hopefully only slightly.
				// On second thought, however, this is not so obvious.  The Amit case (first flow then storage delayed)
				// is important, but the above condition would also move cases into the flow queue where the origin
				// of the problem clearly is somewhere else. kai, sep'15
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
	 */
	final double computeFlowCongestionAndReturnStorageDelay(double now, Id<Link> linkId, DelayInfo affectedAgentDelayInfo, double remainingDelay) {		

		LinkCongestionInfo linkInfo = this.linkId2congestionInfo.get( linkId );

		for (Iterator<DelayInfo> it = linkInfo.getFlowQueue().descendingIterator() ; remainingDelay > 0.0 && it.hasNext() ; ) {
			// Get the agent 'ahead' from the flow queue. The agents 'ahead' are considered as causing agents.
			DelayInfo causingAgentDelayInfo = it.next() ;
			if ( causingAgentDelayInfo.personId.equals( affectedAgentDelayInfo.personId ) ) {
				// not charging to yourself:
				continue ;
			}

			double allocatedDelay = Math.min(linkInfo.getMarginalDelayPerLeavingVehicle_sec(), remainingDelay);

			CongestionEvent congestionEvent = new CongestionEvent(now, "flowAndStorageCapacity", causingAgentDelayInfo.personId, 
					affectedAgentDelayInfo.personId, allocatedDelay, linkId, causingAgentDelayInfo.linkEnterTime );
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
	
	@Override
	public double getTotalDelay() {
		return this.totalDelay ;
	}

	public double getDelayNotInternalized_roundingErrors() {
		return delayNotInternalized_roundingErrors;
	}

	public void addToDelayNotInternalized_roundingErrors( double val) {
		this.delayNotInternalized_roundingErrors += val;
	}

	@Override
	public double getTotalInternalizedDelay() {
		return totalInternalizedDelay;
	}

	public void addToTotalInternalizedDelay(double val) {
		this.totalInternalizedDelay += val;
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

	public Vehicle2DriverEventHandler getVehicle2DriverEventHandler() {
		return Veh2DriverDelegate;
	}

	@Override
	public void calculateCongestion(LinkLeaveEvent event, DelayInfo delayInfo) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public double getTotalRoundingErrorDelay() {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void writeCongestionStats(String fileName) {
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Veh2DriverDelegate.handleEvent(event);
	}


}
