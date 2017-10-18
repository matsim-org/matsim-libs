/* *********************************************************************** *
 * project: org.matsim.*
 * ConflictingLeftTurnAcceptanceLogic
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

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;

/**
 * @author tthunig
 */
final class UnprotectedLeftTurnAcceptanceLogic implements TurnAcceptanceLogic {

	private final TurnAcceptanceLogic delegate = new SignalTurnAcceptanceLogic();
	
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork) {
		AcceptTurn defaultTurn = delegate.isAcceptingTurn(currentLink, currentLane, nextLinkId, veh, qNetwork);
		if ( !defaultTurn.equals(AcceptTurn.GO) ) {
			return defaultTurn;
		}
		/* so far, this reduction of capacity (by waiting) only is active for signalized intersections. it would be possible to extend this
		to non signalized intersections. but one would have to think about how to increase/set flow capacities of all links. tthunig, oct'17 */
		if (currentLane instanceof SignalizeableItem) {
			// signal shows green. check whether there is prioritized crossing traffic
			
			/* TODO how to get prioritized, conflicting lanes. Look into Nils Code how it is stored. */
			List<QLaneI> conflictingLanes = new LinkedList<>();
			for (QLaneI conflictingLane : conflictingLanes) {

				if ( conflictingLane instanceof SignalizeableItem &&
						((SignalizeableItem) conflictingLane).hasGreenForAllToLinks() &&
						!conflictingLane.isNotOfferingVehicle() ) {
					/* so far, this logic uses every second gap between approaching vehicles. one could alternatively check the earliest arrival 
					 * time of the next vehicle and the free speed of the link/lane */
					return AcceptTurn.WAIT;
				}
			}
			throw new UnsupportedOperationException("not yet implemented.");
		}
		return AcceptTurn.GO;
	}

}
