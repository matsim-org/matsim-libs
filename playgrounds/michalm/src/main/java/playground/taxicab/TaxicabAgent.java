/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.taxicab;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.*;
import org.matsim.core.mobsim.qsim.interfaces.*;

/**
 * @author nagel
 *
 */
public class TaxicabAgent implements MobsimDriverAgent, DispatcherTaxiRequestEventHandler {
	
	public static TaxicabAgent insertTaxicabAgent(Netsim simulation) {
		return new TaxicabAgent(simulation);
	}

	private static Id NO_LINK = new IdImpl("nolink"); 
	
	private Netsim netsim ;
	private Scenario sc ;
	private Id currentLinkId;
	private MobsimVehicle vehicle;
	private Id destinationLinkId = NO_LINK ; 
	private Id expectedPassengerId;
	private MobsimAgent currentPassenger ;
	private double activityEndTime;
	
	private MobsimAgent.State state ;
	@Override
	public MobsimAgent.State getState() {
		return this.state ;
	}
	


	private TaxicabAgent(Netsim simulation) {
		netsim = simulation ;
		sc = netsim.getScenario();
		
		// I want to get some random link id in order to initialize the position:
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			currentLinkId = link.getId() ;
		}
		
		this.activityEndTime = 24000. ;

//		netsim.arrangeActivityStart(this) ;
		this.state = MobsimAgent.State.ACTIVITY ;
//		netsim.reInsertAgentIntoMobsim(this) ;
		
		
	}
	
	@Override
	public Id chooseNextLinkId() {
		if ( this.getCurrentLinkId().equals( this.getDestinationLinkId() ) ) {
			return null ;
		}
		
		Link currentLink = sc.getNetwork().getLinks().get( this.getCurrentLinkId() ) ;
		Node node = currentLink.getToNode() ;
		
		int nOutLinks = node.getOutLinks().size() ;
		int idx = (int) ( Math.random() * nOutLinks ) ;
		
		Id nextLinkId = null ;
		int ii = 0 ;
		for ( Link outLink : node.getOutLinks().values() ) {
			if ( ii == idx ) {
				nextLinkId = outLink.getId() ;
				break ;
			}
			ii++ ;
		}
		
		
		// lskdjf
		return nextLinkId ;
	}


	@Override
	public void handleEvent(DispatcherTaxiRequestEvent ev) {
		Logger.getLogger("").warn(" getting request for linkId: " + ev.getLinkId() ) ;
		this.destinationLinkId = ev.getLinkId();
		this.expectedPassengerId = ev.getPassengerId() ;
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public String getMode() {
		return "car" ; // !!!!
	}

	@Override
	public Id getCurrentLinkId() {
		return this.currentLinkId ;
	}

	@Override
	public Id getId() {
		return new IdImpl("taxidriver") ;
	}

	@Override
	public Id getPlannedVehicleId() {
		return new IdImpl("taxidriver") ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		this.currentLinkId = newLinkId ;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}
	
	@Override
	public Id getDestinationLinkId() {
		return this.destinationLinkId ;
	}
	
	//@SuppressWarnings("null")
	@Override
	public void endLegAndComputeNextState(double now) {
		
		EventsManager events = this.netsim.getEventsManager() ;
		EventsFactory evFac = (EventsFactory) events.getFactory() ;

		if ( this.expectedPassengerId!=null && this.currentPassenger==null ) {
			// (= no passenger on board, but having a request)
			
			throw new RuntimeException("I don't think this is used anywhere.  Pls complain if this is wrong. kai, dec'11");
//			MobsimAgent passenger = ((QSim) this.netsim).unregisterAdditionalAgentOnLink(this.expectedPassengerId, this.currentLinkId) ;
//			this.expectedPassengerId = null ;
//			this.currentPassenger = passenger ;
//			if ( passenger != null ) {
//				this.destinationLinkId = passenger.getDestinationLinkId() ;
//			}
//
//			events.processEvent( evFac.createPersonEntersVehicleEvent(now, passenger.getId(), this.vehicle.getId(), this.getId() ) ) ;
//
////			this.netsim.arrangeAgentDeparture(this) ; // full taxicab
//			this.state = MobsimAgent.State.LEG ;
////			this.netsim.reInsertAgentIntoMobsim(this);


		} else if ( this.expectedPassengerId==null && this.currentPassenger!=null ) {
			// (= passenger on board, but having no request)
			
			events.processEvent( evFac.createPersonLeavesVehicleEvent(now, this.currentPassenger.getId(), this.vehicle.getId()));
			
			this.currentPassenger.notifyArrivalOnLinkByNonNetworkMode(this.currentLinkId) ;
//			this.currentPassenger.endLegAndAssumeControl(now) ;
			
			throw new RuntimeException("I don't think this is used anywhere.  Pls complain if this is wrong. kai, dec'11");
			
//			
//			this.currentPassenger = null ;
//			
//			this.destinationLinkId = NO_LINK ;
//			
////			this.netsim.arrangeAgentDeparture(this) ; // empty taxicab
//			this.state = MobsimAgent.State.LEG ;
//			this.netsim.reInsertAgentIntoMobsim(this) ;
		
		} else {
			throw new RuntimeException("undefined state") ;
		}
		
	}


	@Override
	public double getActivityEndTime() {
		return this.activityEndTime ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		// this should, in theory, only happen at simulation start.

//		this.netsim.arrangeAgentDeparture(this) ;
		this.state = MobsimAgent.State.LEG ;
//		this.netsim.reInsertAgentIntoMobsim(this);

		
	}
	
    @Override
    public void abort(double now) {
    	this.state = MobsimAgent.State.ABORT ;
    }




	// things that should never happen with the random taxicab agent:
	
	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}




}
