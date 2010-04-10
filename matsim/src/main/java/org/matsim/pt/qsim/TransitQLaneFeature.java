package org.matsim.pt.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QLinkImpl;
import org.matsim.ptproject.qsim.QVehicle;
import org.matsim.ptproject.qsim.QVehicleEarliestLinkExitTimeComparator;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;


public class TransitQLaneFeature {
	
	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();
	
	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	private final Queue<QVehicle> transitVehicleStopQueue = new PriorityQueue<QVehicle>(5, VEHICLE_EXIT_COMPARATOR);

	private final QLink queueLane;
	
	public TransitQLaneFeature(QLink queueLane) {
		this.queueLane = queueLane;
	}

	public boolean isFeatureActive() {
		return !this.transitVehicleStopQueue.isEmpty();
	}
	
	public Collection<QVehicle> getFeatureVehicles() {
		return this.transitVehicleStopQueue;
	}
	
	/**
	 * The method name tells when it is called, but not what it does (maybe
	 * because the "Feature" structure is meant to be more general). This method
	 * moves transit vehicles from the stop queue directly to the front of the
	 * "queue" of the QLink. An advantage is that this will observe flow
	 * capacity restrictions. yyyy A disadvantage is that this is a hard-coding
	 * on top of the queue/buffer structure where it is questionable if this
	 * should be used or even visible from the outside. kai, feb'10
	 */
	public void beforeMoveLaneToBuffer(final double now) {
		QVehicle veh;
		// handle transit traffic in stop queue
		List<QVehicle> departingTransitVehicles = null;
		while ((veh = this.transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<QVehicle>();
			}
			departingTransitVehicles.add(this.transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				queueLane.getVehQueue().addFirst(iter.previous());
			}
		}
	}
	
	public boolean handleMoveLaneToBuffer(final double now, QVehicle veh,
			PersonDriverAgent driver) {
		boolean handled = false;
		// handle transit driver if necessary
		if (driver instanceof TransitDriverAgent) {
			TransitDriverAgent transitDriver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = transitDriver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId().equals(queueLane.getLink().getId()))) {
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

	public boolean handleMoveWaitToBuffer(final double now, QVehicle veh) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			// yyyy The way I understand the code, this can only happen at the start of a pt run, when the vehicle
			// with the driver enters the traffic for the first time.  In contrast, pt vehicles at stops
			// are handled via a separate data structure ("transitVehicleStopQueue") --???  kai, nov'09
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = driver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId().equals(queueLane.getLink().getId()))) {
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
	public void positionVehiclesFromTransitStop(final Collection<AgentSnapshotInfo> positions, int cnt2 ) {
		if (this.transitVehicleStopQueue.size() > 0) {
//			lane++; // place them one lane further away
//			double vehPosition = queueLane.getLink().getLength();
			for (QVehicle veh : this.transitVehicleStopQueue) {
////				PositionInfo position = new PositionInfo(OTFDefaultLinkHandler.LINK_SCALE, veh.getDriver().getPerson().getId(), queueLane.getLink(),
////						vehPosition, lane, 0.0, 	AgentSnapshotInfo.AgentState.TRANSIT_DRIVER);
//				AgentSnapshotInfo position = new PositionInfo( veh.getDriver().getPerson().getId(), queueLane.getLink(), cnt2 ) ;
//				position.setAgentState( AgentState.TRANSIT_DRIVER ) ;
//				positions.add(position);
////				vehPosition -= veh.getSizeInEquivalents() * cellSize;
//				// yyyy also add the passengers (so we can track them)? kai, apr'10

				Collection<PersonAgent> peopleInVehicle = getPassengers(veh);
				boolean first = true;
				for (PersonAgent passenger : peopleInVehicle) {
					AgentSnapshotInfo passengerPosition = new PositionInfo( passenger.getPerson().getId(), queueLane.getLink(), cnt2 ); // for the time being, same position as facilities
					if ( passenger.getPerson().getId().toString().startsWith("pt")) {
						passengerPosition.setAgentState(AgentState.TRANSIT_DRIVER);
					} else if (first) {
						passengerPosition.setAgentState(AgentState.PERSON_DRIVING_CAR);
					} else {
						passengerPosition.setAgentState(AgentState.PERSON_OTHER_MODE);
					}
					positions.add(passengerPosition);
					first = false; 
					cnt2++ ; 
				}
			
			}
			
		}
	}

	public Collection<PersonAgent> getPassengers(QVehicle queueVehicle) {
		// yyyy warum macht diese Methode Sinn?  TransitVehicle.getPassengers() gibt doch
		// bereits eine Collection<PersonAgent> zurück.  Dann braucht das Umkopieren hier doch
		// bloss Zeit?  kai, feb'10
			if (queueVehicle instanceof TransitVehicle) {
				Collection<PersonAgent> passengers = new ArrayList<PersonAgent>();
				for (PassengerAgent passenger : ((TransitVehicle) queueVehicle).getPassengers()) {
					passengers.add((PersonAgent) passenger);
				}
				return passengers;
			} else {
				return Collections.emptyList();
		}
	}

}
