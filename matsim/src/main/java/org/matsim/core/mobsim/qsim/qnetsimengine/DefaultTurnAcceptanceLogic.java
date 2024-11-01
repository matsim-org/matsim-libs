
/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTurnAcceptanceLogic.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * this class checks whether turning is physically possible, i.e. if the next link exists and if it is connected to the current link
 *
 * @author kainagel
 *
 */
public final class DefaultTurnAcceptanceLogic implements TurnAcceptanceLogic {
	private static final Logger log = LogManager.getLogger( DefaultTurnAcceptanceLogic.class) ;

	/** We need qNetwork to get the next QLink, because the link lookup may lead to a NullPointer otherwise */
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork, double now){
		if (nextLinkId == null) {
			log.error( "Agent has no or wrong route! agentId=" + veh.getDriver().getId()
					+ " currentLink=" + currentLink.getId().toString()
					+ ". The agent is removed from the simulation.");
			return AcceptTurn.ABORT;
		}
		QLinkI nextQLink = qNetwork.getNetsimLinks().get(nextLinkId);

		if (nextQLink == null){
			log.warn("The link id " + nextLinkId + " is not available in the simulation network, but vehicle " + veh.getId() +
					" plans to travel on that link from link " + veh.getCurrentLink().getId());
			return AcceptTurn.ABORT ;
		}
		if (currentLink.getToNode() != nextQLink.getLink().getFromNode()) {
			log.warn("Cannot move vehicle " + veh.getId() + " from link " + currentLink.getId() + " to link " + nextQLink.getLink().getId());
			return AcceptTurn.ABORT ;
		}
//		if ( !nextQLink.getLink().getAllowedModes().contains( veh.getDriver().getMode() ) ) {
//			final String message = "The link with id " + nextLinkId + " does not allow the current mode, which is " + veh.getDriver().getMode();
//			throw new RuntimeException( message ) ;
////			log.warn(message );
////			return acceptTurn.ABORT ;
//			// yyyy is rather nonsensical to get the mode from the driver, not from the vehicle.  However, this seems to be
//			// how it currently works: network links are defined for modes, not for vehicle types.  kai, may'16
//		}
		// currently does not work, see MATSIM-533

		/* note: it cannot happen, that currentLane does not lead to nextLink (e.g. due to turn restrictions) because this is checked before:
		 * a vehicle only enters a lane when that lane leads to the next link. see QLinkLanesImpl.moveBufferToNextLane() and .chooseNextLane()
		 * tthunig, oct'17 */

		return AcceptTurn.GO ;
	}

}
