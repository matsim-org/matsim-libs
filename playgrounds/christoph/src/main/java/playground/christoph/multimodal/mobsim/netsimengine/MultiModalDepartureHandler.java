/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalDepartureHandler.java
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

package playground.christoph.multimodal.mobsim.netsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

public class MultiModalDepartureHandler implements DepartureHandler {

	private QSim qSim;
	
	public MultiModalDepartureHandler(QSim qSim) {
		this.qSim = qSim;
	}
	
	@Override
	public boolean handleDeparture(double now, PersonAgent personAgent, Id linkId, Leg leg) {

		if (leg.getMode().equals(TransportMode.walk) || 
			leg.getMode().equals(TransportMode.bike) ||
			leg.getMode().equals(TransportMode.ride) ||
			leg.getMode().equals(TransportMode.pt)) {
			if ( personAgent instanceof PersonDriverAgent ) {
				handleMultiModalDeparture(now, (PersonDriverAgent)personAgent, linkId, leg);
				return true;
			} else {
				throw new UnsupportedOperationException("not supported TransportMode found: " + leg.getMode());
			}
		}
		
		return false;
	}
	
	private void handleMultiModalDeparture(double now, PersonDriverAgent personAgent, Id linkId, Leg leg) {
		
		Route route = leg.getRoute();
		MultiModalQLinkImpl qLink = (MultiModalQLinkImpl) qSim.getQNetwork().getQLink(linkId);
		
		if ((route.getEndLinkId().equals(linkId)) && (personAgent.chooseNextLinkId() == null)) {
			personAgent.legEnds(now);
		} else {
			qLink.addDepartingAgent(personAgent, now);
		}				
	}
}	
