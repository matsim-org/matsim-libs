package org.matsim.pt.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.PersonAgentI;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueVehicle;
import org.matsim.ptproject.qsim.QueueVehicleEarliestLinkExitTimeComparator;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;


public class TransitQueueLaneFeature {
	
	private static final Comparator<QueueVehicle> VEHICLE_EXIT_COMPARATOR = new QueueVehicleEarliestLinkExitTimeComparator();
	
	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	private final Queue<QueueVehicle> transitVehicleStopQueue = new PriorityQueue<QueueVehicle>(5, VEHICLE_EXIT_COMPARATOR);

	private final QueueLink queueLane;
	
	public TransitQueueLaneFeature(QueueLink queueLane) {
		this.queueLane = queueLane;
	}

	public boolean isFeatureActive() {
		return !this.transitVehicleStopQueue.isEmpty();
	}
	
	public Collection<QueueVehicle> getFeatureVehicles() {
		return this.transitVehicleStopQueue;
	}
	
	public void beforeMoveLaneToBuffer(final double now) {
		QueueVehicle veh;
		// handle transit traffic in stop queue
		List<QueueVehicle> departingTransitVehicles = null;
		while ((veh = this.transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<QueueVehicle>();
			}
			departingTransitVehicles.add(this.transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QueueVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				queueLane.getVehQueue().addFirst(iter.previous());
			}
		}
	}
	
	public boolean handleMoveLaneToBuffer(final double now, QueueVehicle veh,
			DriverAgent driver) {
		boolean handled = false;
		// handle transit driver if necessary
		if (driver instanceof TransitDriverAgent) {
			TransitDriverAgent transitDriver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = transitDriver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId() == queueLane.getLink().getId())) {
				double delay = transitDriver.handleTransitStop(stop, now);
				if (delay > 0.0) {
					veh.setEarliestLinkExitTime(now + delay);
					if (!stop.getIsBlockingLane()) {
						queueLane.getVehQueue().poll(); // remove the bus from the queue
						this.transitVehicleStopQueue.add(veh); // and add it to the stop queue
					}
				}
				/* start over: either this veh is still first in line,
				 * but has another stop on this link, or on another link, then it is moved on
				 */
				handled = true;
			}
		}
		return handled;
	}

	public boolean handleMoveWaitToBuffer(final double now, QueueVehicle veh) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			// yyyy The way I understand the code, this can only happen at the start of a pt run, when the vehicle
			// with the driver enters the traffic for the first time.  In contrast, pt vehicles at stops
			// are handled via a separate data structure ("transitVehicleStopQueue") --???  kai, nov'09
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = driver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId() == queueLane.getLink().getId())) {
				double delay = driver.handleTransitStop(stop, now);
				if (delay > 0.0) {
					veh.setEarliestLinkExitTime(now + delay);
					// add it to the stop queue, can do this as the waitQueue is also non-blocking anyway
					this.transitVehicleStopQueue.add(veh);
					return true;
				}
			}
		}
		return false;
	}
	

	/**
	 * Put the transit vehicles from the transit stop list in positions.
	 */
	public void positionVehiclesFromTransitStop(
			final Collection<PositionInfo> positions, double cellSize,
			int lane) {
		if (this.transitVehicleStopQueue.size() > 0) {
			lane++; // place them one lane further away
			double vehPosition = queueLane.getLink().getLength();
			for (QueueVehicle veh : this.transitVehicleStopQueue) {
				PositionInfo position = new PositionInfo(OTFDefaultLinkHandler.LINK_SCALE, veh.getDriver().getPerson().getId(), queueLane.getLink(),
						vehPosition, lane, 0.0, 	AgentSnapshotInfo.AgentState.AGENT_MOVING, null);
				positions.add(position);
				vehPosition -= veh.getSizeInEquivalents() * cellSize;
			}
		}
	}

	public Collection<PersonAgentI> getPassengers(
			QueueVehicle queueVehicle) {
			if (queueVehicle instanceof TransitVehicle) {
				Collection<PersonAgentI> passengers = new ArrayList<PersonAgentI>();
				for (PassengerAgent passenger : ((TransitVehicle) queueVehicle).getPassengers()) {
					passengers.add((PersonAgentI) passenger);
				}
				return passengers;
			} else {
				return Collections.emptyList();
		}
	}

}
