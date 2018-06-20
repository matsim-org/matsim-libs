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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.Direction;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;

/**
 * This class extends the SignalTurnAcceptanceLogic by also checking for oncoming traffic:
 * If a turn is physically possible and the corresponding signal shows green, additionally, 
 * a vehicle is only accepted for the turn, if no prioritized oncoming traffic is crossing the intersection at the same time.
 * Otherwise, the vehicle has to wait (AcceptTurn.WAIT is returned).
 * 
 * @author tthunig
 */
final class UnprotectedLeftTurnAcceptanceLogic implements TurnAcceptanceLogic {

	private final TurnAcceptanceLogic delegate = new SignalTurnAcceptanceLogic();
	private final ConflictData conflicts;
	
	public UnprotectedLeftTurnAcceptanceLogic(ConflictData conflicts) {
		this.conflicts = conflicts;
	}
	
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork) {
		AcceptTurn defaultTurn = delegate.isAcceptingTurn(currentLink, currentLane, nextLinkId, veh, qNetwork);
		if ( !defaultTurn.equals(AcceptTurn.GO) ) {
			return defaultTurn;
		}
		/* so far, this reduction of capacity (by waiting) only is active for signalized intersections. it would be possible to extend this
		to non signalized intersections. but one would have to think about how to increase/set flow capacities of all links. tthunig, oct'17 */
		if (currentLane instanceof SignalizeableItem) {
			/* signal shows green. check whether there is prioritized crossing traffic, i.e. traffic at directions with right of way.
			 * note: conflicting directions don't have to be checked here. They are already checked in ConflictingDirectionsLogic and are not allowed to show green together. 
			 */
			IntersectionDirections intersectionConflicts = conflicts.getConflictsPerNode().get(currentLink.getToNode().getId());
			Direction thisDirection = intersectionConflicts.getDirection(currentLink.getId(), nextLinkId);
			
			for (Id<Direction> rightOfWayDirId : thisDirection.getDirectionsWithRightOfWay()) {
				Direction rightOfWayDir = intersectionConflicts.getDirections().get(rightOfWayDirId);
				QLinkI rightOfWayLink = qNetwork.getNetsimLink(rightOfWayDir.getFromLink());
				
				for (QLaneI rightOfWayLane : rightOfWayLink.getOfferingQLanes()) {
					if (rightOfWayLane instanceof SignalizeableItem
							&& ((SignalizeableItem) rightOfWayLane).hasGreenForAllToLinks()
							&& !rightOfWayLane.isNotOfferingVehicle()) {
						/* vehicles have to wait if at least one lane with right of way is 'offering vehicles' and shows green.
						 * note: so far, this logic uses every second gap between approaching vehicles. one
						 * could alternatively check the earliest arrival time of the next vehicle and
						 * the free speed of the link/lane.
						 */
						return AcceptTurn.WAIT;
					}
				}
			}
		}
		return AcceptTurn.GO;
	}

}
