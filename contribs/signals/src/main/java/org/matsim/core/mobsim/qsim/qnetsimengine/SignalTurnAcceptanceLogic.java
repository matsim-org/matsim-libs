/* *********************************************************************** *
 * project: org.matsim.*
 * SignalTurnAcceptanceLogic
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
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;

/**
 * This class extends the DefaultTurnAcceptanceLogic by also checking for signal states: 
 * It only accepts a turn, if the corresponding signal shows green. If not, it returns AcceptTurn.WAIT.
 * 
 * @author tthunig
 */
final class SignalTurnAcceptanceLogic implements TurnAcceptanceLogic {

	private final TurnAcceptanceLogic delegate = new DefaultTurnAcceptanceLogic();
	
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork) {
		AcceptTurn defaultTurn = delegate.isAcceptingTurn(currentLink, currentLane, nextLinkId, veh, qNetwork);
		if ( defaultTurn.equals(AcceptTurn.ABORT) ) {
			return defaultTurn;
		}
		// else: check whether there are signals at the lane/link and whether they show green/red
		if ( (currentLane instanceof SignalizeableItem) && 
				(! ((SignalizeableItem)currentLane).hasGreenForToLink(nextLinkId)) ) {
			/* because turn acceptance is checked before stuck time, an infinite red time
			 * does not lead to stuck event of waiting vehicles. dg, mar'14 */
			return AcceptTurn.WAIT;
		}
		return AcceptTurn.GO;
	}

}
