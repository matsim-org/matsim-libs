/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQLink
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author knagel
 * @author mrieser
 * @author dgrether
 * 
 */
final class TransitQLink {
	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	private final Queue<QVehicle> transitVehicleStopQueue = new PriorityQueue<>(5, VEHICLE_EXIT_COMPARATOR);
	private final QLaneI road;

	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();


	TransitQLink(QLaneI road){
		this.road = road;
	}
	
	Queue<QVehicle> getTransitVehicleStopQueue(){
		return this.transitVehicleStopQueue;
	}
	
	final boolean addTransitToStopQueue(final double now, final QVehicle veh, final Id<Link> linkId) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			while (true) {
				TransitStopFacility stop = driver.getNextTransitStop();
				if ((stop != null) && (stop.getLinkId().equals(linkId))) {
					double delay = driver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						// yy removing this condition makes at least one test fail.  I still think we could discuss doing this. kai, jun'13

						veh.setEarliestLinkExitTime(now + delay);
						// add it to the stop queue: vehicle that is not yet on the road will never block
						transitVehicleStopQueue.add(veh);
						return true;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}
	
	
	/**
	 * This method moves transit vehicles from the stop queue directly to the front of the
	 * "queue" of the QLink. An advantage is that this will observe flow
	 * capacity restrictions. 
	 */
	void handleTransitVehiclesInStopQueue(final double now) {
		QVehicle veh;
		// handle transit traffic in stop queue
		List<QVehicle> departingTransitVehicles = null;
		while ((veh = transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<>();
			}
			departingTransitVehicles.add(transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				this.road.addTransitSlightlyUpstreamOfStop(iter.previous()) ;
			}
		}
	}
	
	 HandleTransitStopResult handleTransitStop(final double now, final QVehicle veh, 
			final TransitDriverAgent transitDriver, Id<Link> linkId) {
		TransitStopFacility stop = transitDriver.getNextTransitStop();
		if ((stop != null) && (stop.getLinkId().equals(linkId))) {
			double delay = transitDriver.handleTransitStop(stop, now);
			if (delay > 0.0) {
				veh.setEarliestLinkExitTime(now + delay);
				// (if the vehicle is not removed from the queue in the following lines, then this will effectively block the lane
				if (!stop.getIsBlockingLane()) {
					transitVehicleStopQueue.add(veh);
					// transit vehicle which is removed to the transit stop space
					return HandleTransitStopResult.accepted;
				} else {
					// transit vehicle which blocks its lane by getting its exit time increased
					return HandleTransitStopResult.rehandle;
				}
			} else {
				// transit vehicle which instantaneously delivered passangers
				return HandleTransitStopResult.rehandle;
			}
		} else {
			// transit vehicle which either arrives or continues driving
			return HandleTransitStopResult.continue_driving;
		}
	}

    /**
     * @author dstrippgen
     *
     * Comparator object, to sort the Vehicle objects in QueueLink.parkingList
     * according to their departure time
     */
    static class QVehicleEarliestLinkExitTimeComparator implements Comparator<QVehicle>,
            Serializable, MatsimComparator {

        private static final long serialVersionUID = 1L;

        @Override
        public int compare(final QVehicle veh1, final QVehicle veh2) {
            if (veh1.getEarliestLinkExitTime() > veh2.getEarliestLinkExitTime()) {
                return 1;
            }
            if (veh1.getEarliestLinkExitTime() < veh2.getEarliestLinkExitTime()) {
                return -1;
            }

            // Both depart at the same time -> let the one with the larger id be first
            return veh2.getId().compareTo(veh1.getId());
        }
    }
}
