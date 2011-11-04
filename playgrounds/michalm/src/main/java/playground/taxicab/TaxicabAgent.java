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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

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
	private QVehicle vehicle;
	private Id destinationLinkId = NO_LINK ; 
	private Id expectedPassengerId;
	private MobsimAgent currentPassenger ;
	private double activityEndTime;

	private TaxicabAgent(Netsim simulation) {
		netsim = simulation ;
		sc = netsim.getScenario();
		
		// I want to get some random link id in order to initialize the position:
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			currentLinkId = link.getId() ;
		}
		
		this.activityEndTime = 24000. ;
		netsim.arrangeActivityStart(this) ;
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
	public QVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public void notifyMoveOverNode(Id newLinkId) {
		this.currentLinkId = newLinkId ;
	}

	@Override
	public void setVehicle(QVehicle veh) {
		this.vehicle = veh ;
	}
	
	@Override
	public Id getDestinationLinkId() {
		return this.destinationLinkId ;
	}
	
	@SuppressWarnings("null")
	@Override
	public void endLegAndAssumeControl(double now) {
		
		EventsManager events = this.netsim.getEventsManager() ;
		EventsFactoryImpl evFac = (EventsFactoryImpl) events.getFactory() ;

		if ( this.expectedPassengerId!=null && this.currentPassenger==null ) {
			// (= no passenger on board, but having a request)
			
			MobsimAgent passenger = 
				this.netsim.getNetsimNetwork().getNetsimLink(this.currentLinkId).unregisterAdditionalAgentOnLink(this.expectedPassengerId) ;
			this.expectedPassengerId = null ;
			this.currentPassenger = passenger ;
			if ( passenger != null ) {
				this.destinationLinkId = passenger.getDestinationLinkId() ;
			}

			events.processEvent( evFac.createPersonEntersVehicleEvent(now, passenger.getId(), this.vehicle.getId(), this.getId() ) ) ;

			this.netsim.arrangeAgentDeparture(this) ; // full taxicab

		} else if ( this.expectedPassengerId==null && this.currentPassenger!=null ) {
			// (= passenger on board, but having no request)
			
			events.processEvent( evFac.createPersonLeavesVehicleEvent(now, this.currentPassenger.getId(), this.vehicle.getId(), 
					this.getId() ) ) ;
			
			this.currentPassenger.notifyTeleportToLink(this.currentLinkId) ;
			this.currentPassenger.endLegAndAssumeControl(now) ;
			this.currentPassenger = null ;
			
			this.destinationLinkId = NO_LINK ;
			
			this.netsim.arrangeAgentDeparture(this) ; // empty taxicab
		
		} else {
			throw new RuntimeException("undefined state") ;
		}
		
	}


	@Override
	public double getActivityEndTime() {
		return this.activityEndTime ;
	}

	@Override
	public void endActivityAndAssumeControl(double now) {
		// this should, in theory, only happen at simulation start.

		this.netsim.arrangeAgentDeparture(this) ;
	}


	// things that should never happen with the random taxicab agent:
	
	@Override
	public void notifyTeleportToLink(Id linkId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}




}
