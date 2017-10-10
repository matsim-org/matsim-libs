/* *********************************************************************** *
 * project: org.matsim.*
 * SignalEngine
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultTurnAcceptanceLogic;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLaneI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.TurnAcceptanceLogic;

/**
 * @author tthunig
 */
final class SignalTurnAcceptanceLogic implements TurnAcceptanceLogic {

	TurnAcceptanceLogic delegate = new DefaultTurnAcceptanceLogic();
	
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QLinkI nextQLink, QVehicle veh) {
		AcceptTurn defaultTurn = delegate.isAcceptingTurn(currentLink, currentLane, nextLinkId, nextQLink, veh);
		if ( defaultTurn.equals(AcceptTurn.ABORT) ) {
			return defaultTurn;
		}
		// else: check whether signals show green
		if ( ! currentLane.hasGreenForToLink(nextLinkId) ) {
			//there is no longer a stuck check for red links. This means that
			//in case of an infinite red time the simulation will not stop automatically because
			//vehicles waiting in front of the red signal will never reach their destination. dg, mar'14
			return AcceptTurn.WAIT;
		}
		return AcceptTurn.GO;
	}

}
