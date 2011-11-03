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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.qnetsimengine.QVehicle;

/**
 * @author nagel
 *
 */
public class TaxicabAgent implements MobsimDriverAgent, DispatcherTaxiRequestEventHandler {
	
	Netsim netsim ;
	Scenario sc ;
	private Id currentLinkId;
	private QVehicle vehicle;
	private Id destinationLinkId;
	private Id expectedPassengerId;
	private Id currentPassengerId;

	public TaxicabAgent(Netsim simulation) {
		netsim = simulation ;
		sc = netsim.getScenario();
		
		// I want to get some random link id in order to initialize the position:
		for ( Link link : sc.getNetwork().getLinks().values() ) {
			currentLinkId = link.getId() ;
		}
		
	}
	
	// figure out how this is inserted in matsim.  Look at "createAdditionaAgents"
	
	@Override
	public Id chooseNextLinkId() {
		// where are we?
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
		return new IdImpl("defaultVehicleType") ;
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
		// this should happen when the taxi "arrives" in order to pick up a passenger!
		
		// the car is "parked" before this method is called
		
		MobsimAgent passenger = 
			this.netsim.getNetsimNetwork().getNetsimLink(this.currentLinkId).unregisterAdditionalAgentOnLink(this.expectedPassengerId) ;
		this.expectedPassengerId = null ;
		this.currentPassengerId = passenger.getId();
		if ( passenger != null ) {
			this.destinationLinkId = passenger.getDestinationLinkId() ;
		}

		netsim.arrangeAgentDeparture(this) ;
		
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

	@Override
	public double getActivityEndTime() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void endActivityAndAssumeControl(double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}





}
