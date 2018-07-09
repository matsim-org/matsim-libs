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
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

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
	private final Lanes lanes;
	
	public UnprotectedLeftTurnAcceptanceLogic(ConflictData conflicts, Lanes lanes) {
		this.conflicts = conflicts;
		this.lanes = lanes;
	}
	
	@Override
	public AcceptTurn isAcceptingTurn(Link currentLink, QLaneI currentLane, Id<Link> nextLinkId, QVehicle veh, QNetwork qNetwork, double now) {
		AcceptTurn defaultTurn = delegate.isAcceptingTurn(currentLink, currentLane, nextLinkId, veh, qNetwork, now);
		if ( !defaultTurn.equals(AcceptTurn.GO) ) {
			// the signal shows red or the turn is not allowed because of the network topology
			return defaultTurn;
		}
		/* so far, this reduction of capacity (by waiting) only is active for signalized intersections. it would be possible to extend this
		to non signalized intersections. but one would have to think about how to increase/set flow capacities of all links. tthunig, oct'17 */
		if (conflicts.getConflictsPerNode().containsKey(currentLink.getToNode().getId())) {
			/* it is an intersection where conflicting data exists. check whether there is prioritized crossing traffic, 
			 * i.e. traffic at directions with right of way.
			 * note: conflicting directions don't have to be checked here. They are already checked in ConflictingDirectionsLogic 
			 * and are not allowed to show green together. 
			 */
			IntersectionDirections intersectionConflicts = conflicts.getConflictsPerNode().get(currentLink.getToNode().getId());
			Direction thisDirection = intersectionConflicts.getDirection(currentLink.getId(), nextLinkId);
			
			for (Id<Direction> rightOfWayDirId : thisDirection.getDirectionsWithRightOfWay()) {
				Direction rightOfWayDir = intersectionConflicts.getDirections().get(rightOfWayDirId);
				QLinkI rightOfWayLink = qNetwork.getNetsimLink(rightOfWayDir.getFromLink());
				
				for (QLaneI rightOfWayQLane : rightOfWayLink.getOfferingQLanes()) {
					// this gives us all lanes on the link from which the direction starts -> if multiple lanes exist, check whether the lane leads into the correct direction
					if (lanes != null && lanes.getLanesToLinkAssignments().containsKey(rightOfWayDir.getFromLink())) {
						// lanes for this link exist
						Lane rightOfWayLane = lanes.getLanesToLinkAssignments().get(rightOfWayDir.getFromLink()).getLanes().get(rightOfWayQLane.getId());
						if (!rightOfWayLane.getToLinkIds().contains(rightOfWayDir.getToLink())) {
							// this lane does not lead to the correct link -> skip it
							continue;
						}
					} // no lanes exist or the lane leads to the correct link

					/*
					 * Get the first vehicle on the link by taking the first vehicle of the
					 * collection returned by 'getAllVehicles'. This only works, because
					 * getAllVehicles keeps the order of the vehicles on the link. It would be
					 * easier if one could access vehQueue.peek() in QueueWithBuffer, e.g. with a
					 * method getFirstVehicleInQueue() directly.
					 */
					QVehicle firstVehOnLink = rightOfWayQLane.getAllVehicles()!=null && rightOfWayQLane.getAllVehicles().iterator().hasNext()? 
							(QVehicle) rightOfWayQLane.getAllVehicles().iterator().next() 
							: null;
					/*
					 * Just checking for the first vehicle in the buffer (not the whole link) does
					 * not work properly. Probably, checking for conflicts and moving vehicles over
					 * nodes happens in parallel, such that the sequence of how links are processed
					 * causes situations where vehicles that must yield can always go, although the
					 * conflicting queue contains vehicles, because the buffer has already emptied
					 * for this time step. 
					 * 
					 * theresa, jun'18
					 */
					if (rightOfWayQLane instanceof SignalizeableItem
							&& ((SignalizeableItem) rightOfWayQLane).hasGreenForAllToLinks()
//							&& !rightOfWayQLane.isNotOfferingVehicle() // this only considers the buffer of the link (see argument above)
//							&& rightOfWayQLane.getFirstVehicle() != null && rightOfWayQLane.getFirstVehicle().getEarliestLinkExitTime() <= now // this only considers the buffer of the link (see argument above)
							&& firstVehOnLink != null && firstVehOnLink.getEarliestLinkExitTime() <= now
							) {
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
		// no conflicting traffic is approaching or no conflict data is stored for this intersection
		return AcceptTurn.GO;
	}

}
