/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.mobsim;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.mobsim.snapshots.PositionInfo;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Leg;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.vis.netvis.DrawableAgentI;

/**
 * @author david
 *
 * Queue Model Link implementation
 */
public class QueueLink extends Link {

	// ////////////////////////////////////////////////////////////////////
	// Queue Model specific stuff
	// ////////////////////////////////////////////////////////////////////
	/**
	 * parking list includes all vehicle that do not have yet reached their
	 * start time, but will start at this link at some time
	 */
	private final PriorityQueue<Vehicle> parkingList = new PriorityQueue<Vehicle>(30,
			new VehicleDepartureTimeComparator());

	/**
	 * all veh from parking list move to the waiting list as soon as their time
	 * has come. they are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	private final List<Vehicle> waitingList = new LinkedList<Vehicle>();

	// TODO [DS] is ArrayList the right List for this (better use a Queue)

	/**
	 * the list of veh that have not yet reached the end of the link according
	 * to the free travel speed of the link
	 */
	private final List<Vehicle> vehQueue = new ArrayList<Vehicle>();

	/**
	 * buffer is holding all vehicles that are ready to cross the outgoing
	 * intersection
	 */
	private final List<Vehicle> buffer = new LinkedList<Vehicle>();
	private int buffercount = 0;

	protected double freeTravelDuration;

	protected double spaceCap = 10.0;

	private double timeCap = 10.0;

	private int timeCapCeil = 10; // optimization, cache Math.ceil(timeCap)

	protected double buffercap_accumulate = 1.0;

	protected double euklideanDist = 0.0;

	// the status of the link - needed for with-in-day replaning
	public enum LinkStatus { DEFAULT, BLOCKED };
	public LinkStatus linkStatus = LinkStatus.DEFAULT;


	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	protected QueueLink(final NetworkLayer network, final String id, final BasicNodeI from,
			final BasicNodeI to, final String length, final String freespeed, final String capacity,
			final String permlanes, final String origid, final String type) {
		super(network, id, (Node) from, (Node) to, length, freespeed, capacity,
				permlanes, origid, type);

		// yy: I am really not so happy about these indirect constructors with
		// long argument lists. But be it if other people
		// like them. kai, nov06

		/* moved capacity calculation to a method, to be able to call it from outside
		 * e.g. for reducing cap in case of an incident             */
		recalcCapacity(this.capacity, network);
	}

	public void recalcCapacity(final double capacity, final NetworkLayer network) {
		super.setCapacity(capacity);
		/* network.capperiod is in hours, we need it per sim-tick and multiplied
		 * with flowCapFactor                */
		double flowCapFactor = Gbl.getConfig().simulation().getFlowCapFactor();

		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();

		/* factor to scale down capacity from file to capacity per sim time
		 * step, assuming flowCapFactor=1:    */
		double simTickCapFactor = SimulationTimer.getSimTickTime()
				/ network.getCapacityPeriod();

		/* multiplying capacity from file by simTickCapFactor **and**
		 * flowCapFactor:                     */
		this.timeCap = this.capacity * simTickCapFactor * flowCapFactor;

		// also computing the ceiling of the capacity:
		this.timeCapCeil = (int) Math.ceil(this.timeCap);

		// first guess at storageCapacity:
		this.spaceCap = (this.length * this.permlanes) / NetworkLayer.CELL_LENGTH
				* storageCapFactor;

		/* storage capacity needs to be at least enough to handle the
		 * cap_per_time_step:                  */
		this.spaceCap = Math.max(this.spaceCap, this.timeCapCeil);

		this.euklideanDist = ((Node)this.from).getCoord().calcDistance(((Node)this.to).getCoord());

		this.freeTravelDuration = getLength() / getFreespeed();

		/* If speed on link is relatively slow, then we need MORE cells than the above spaceCap to handle the flowCap. Example:
		 * Assume freeSpeedTravelTime (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap TWO times the flowCap to
		 * handle the flowCap.
		 */
		if (this.spaceCap < this.freeTravelDuration * this.timeCap) {
			if ( spaceCapWarningCount <=10 ) {
				System.err.println(" link " + this.id + " too small: enlarge spaceCap");
				if ( spaceCapWarningCount == 10 ) {
					System.err.println(" Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++ ;
			}
			this.spaceCap = this.freeTravelDuration * this.timeCap;
		}
	}
	static int spaceCapWarningCount = 0 ;

	// ////////////////////////////////////////////////////////////////////
	// Is called after link has been read completely
	// ////////////////////////////////////////////////////////////////////
	public void finishInit() {
	  this.buffercap_accumulate = 1.0;
	}

	private void processVehicleArrival(final double now, final Vehicle veh ) {
		QueueSimulation.getEvents().processEvent(
				new EventAgentArrival(now, veh.getDriverID(), veh.getCurrentLegNumber(), getId().toString(),
						veh.getDriver(), veh.getCurrentLeg(), this));
		// Need to inform the veh that it now reached its destination.
		veh.reachActivity();
	}

	/**
	 * Moves those vehicles, whose departure time has come, from the parking
	 * list to the wait list, from where they can later enter the link.
	 *
	 * @param now the current time
	 */
	private void moveParkToWait(final double now) {
		while (this.parkingList.peek() != null) {
			Vehicle veh = this.parkingList.peek();
			if (veh.getDepartureTime_s() > now) {
				break;
			}

			// Need to inform the veh that it now leaves its activity.
			veh.leaveActivity();

			// Generate departure event
			QueueSimulation.getEvents().processEvent(
				new EventAgentDeparture(now, veh.getDriverID(), veh.getCurrentLegNumber(),
						getId().toString(), veh.getDriver(), veh.getCurrentLeg(), this));

			/* A.) we have an unknown leg mode (aka != "car")
			 * in this cases do teleport veh to next Activity location
			 * B.) we have no route (aka "next activity on same link") ->
			 * no waitingList
			 * C.) route known AND mode == "car" -> regular case, put veh in
			 * waitingList
			 */
			Leg actLeg = veh.getCurrentLeg();

			boolean hasRoute = actLeg.getRoute() != null && actLeg.getRoute().getRoute().size() != 0;
			boolean unknownMode = !actLeg.getMode().equals("car");

			if (unknownMode) {
				QueueSimulation.handleUnknownLegMode(veh);
			} else {
				if (hasRoute) {
					this.waitingList.add(veh);
				} else {
				// this is the case where (hopefully) the next act happens at the same location as this act
				processVehicleArrival(now, veh);
				}
			}

			/* Remove vehicle from parkingList
			 * Do that as the last step to guarantee that the link is ACTIVE all the time
			 * because veh.reinit() calls addParking which might come to the false conclusion,
			 * that this link needs to be activated, as parkingqueue is empty */

			this.parkingList.poll();
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// move as many waiting cars to link as is possible < cap
	// ////////////////////////////////////////////////////////////////////
	private void moveWaitToBuffer(final double now) {
		if (!this.waitingList.isEmpty()) {
			for (ListIterator<Vehicle> i = this.waitingList.listIterator(); i.hasNext();) {
				if (!hasBufferSpace())
					break;

				Vehicle veh = i.next();
				this.buffer.add(veh);
				this.buffercount++;
				veh.setLastMovedTime(now);

				QueueSimulation.getEvents().processEvent(
						new EventAgentWait2Link(now, veh.getDriverID(), veh
								.getCurrentLegNumber(), getId().toString(), veh
								.getDriver(), veh.getCurrentLeg(), this));

				i.remove();
			}
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// move veh from link to buffer, according to bufer caps and dep time of veh
	// ////////////////////////////////////////////////////////////////////
	synchronized private void moveLinkToBuffer(final double now) {
		// move items if possible
		double max_buffercap = this.timeCap;

		for (ListIterator<Vehicle> i = this.vehQueue.listIterator(); i.hasNext();) {
			Vehicle veh = i.next();
			if ( veh.getDepartureTime_s() > now ) {
				break ;
			}

			// Check if veh has reached destination:
			if (veh.getDestinationLink().getId() == this.getId()) {
				processVehicleArrival(now, veh);

				// remove _after_ processing the arrival to keep link active
				i.remove();
				continue;
			}

			// is there still room left in the buffer, or is he overcrowded
			// from the last time steps?
			if ( !hasBufferSpace() ) {
				break;
			}

			if (max_buffercap >= 1.) {
				max_buffercap--;

				this.buffer.add(veh);
				this.buffercount++;
				veh.setLastMovedTime(now);
				i.remove();

				continue ;

			// using the following line instead should, I think, be an easy way to make the mobsim
			// stochastic. not tested. kai
//			} else if ( Gbl.random.nextDouble() < this.buffercap_accumulate ) {

			} else if (this.buffercap_accumulate >= 1.0) {
				this.buffercap_accumulate--;

				this.buffer.add(veh);
				this.buffercount++;
				veh.setLastMovedTime(now);
				i.remove();
				break;
			} else {
				break;
			}
		}

		/* if there is an fractional piece of buffercapacity left over
		 * leave it for the next iteration so it might accumulate to a whole agent.
		 */
		if (this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.timeCap - (int) this.timeCap;
		}
	}

	protected boolean isActive() {
		/* Leave link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		return (this.buffercap_accumulate < 1.0)
				|| ((this.parkingList.size() + this.vehQueue.size() + this.waitingList.size() ) != 0);
	}

	public void activateLink() {
		if (!isActive()) {
			((QueueNetworkLayer) this.layer).addActiveLink(this);
		}
//		QueueNode toNode = (QueueNode) this.getToNode() ;
//		toNode.activateNode() ;
	}

	// ////////////////////////////////////////////////////////////////////
	// called from framework, do everything related to link movement here
	// ////////////////////////////////////////////////////////////////////
	public boolean moveLink(final double now) {

		// move vehicles from parking into waitqueue if applicable
		moveParkToWait(now);
		// move vehicles from link to buffer
		moveLinkToBuffer(now);
		// move vehicles from waitqueue into buffer if possible
		moveWaitToBuffer(now);

		return isActive();
	}

	// ////////////////////////////////////////////////////////////////////
	// called from framework, do everything related to link movement here
	// ////////////////////////////////////////////////////////////////////
	public boolean moveLinkWaitFirst(final double now) {
		// move vehicles from parking into waitqueue if applicable
		moveParkToWait(now);
		// move vehicles from waitqueue into buffer if possible
		moveWaitToBuffer(now);
		// move vehicles from link to buffer
		moveLinkToBuffer(now);

		return isActive();
	}

	public void addParking(final Vehicle veh) {
		activateLink();
		this.parkingList.add(veh);
	}

	/**
	 * Adds a vehicle to the link, called by
	 * {@link QueueNode#moveVehicleOverNode(Vehicle, double)}.
	 *
	 * @param veh the vehicle
	 */
	synchronized public void add(final Vehicle veh) {
		double now = SimulationTimer.getTime();

		activateLink();
		veh.setCurrentLink(this);
		this.vehQueue.add(veh);
		veh.setDepartureTime_s((int) (now + this.freeTravelDuration));
		QueueSimulation.getEvents().processEvent(
				new EventLinkEnter(now, veh.getDriverID(), veh.getCurrentLegNumber(),
						this.getId().toString(), veh.getDriver(), this));
	}

	// ////////////////////////////////////////////////////////////////////
	// getter / setter
	// ////////////////////////////////////////////////////////////////////

	/**
	 * @return Returns the freeTravelDuration.
	 */
	public double getFreeTravelDuration() {
		return this.freeTravelDuration;
	}

	/**
	 * @param freeTravelDuration
	 *            The freeTravelDuration to set.
	 */
	public void setFreeTravelDuration(final double freeTravelDuration) {
		this.freeTravelDuration = freeTravelDuration;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.List#get(int)
	 */
	public Vehicle popFirstFromBuffer() {
		double now = SimulationTimer.getTime();
		Vehicle veh = this.buffer.get(0);
		this.buffer.remove(0);
		this.buffercount--;
		if (this.buffercount != 0) {
			Vehicle v2 = this.buffer.get(0);
			v2.setLastMovedTime(now);
		}

		QueueSimulation.getEvents().processEvent(
				new EventLinkLeave(now, veh.getDriverID(), veh.getCurrentLegNumber(),
						this.getId().toString(), veh.getDriver(), this));

		return veh;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.List#get(int)
	 */
	public Vehicle getFirstFromBuffer() {
		return this.buffer.get(0);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.List#isEmpty()
	 */
	public boolean bufferIsEmpty() {
		return this.buffercount == 0;
	}

	/**
	 * @return <code>true</code> if there are less vehicles in buffer + vehQueue (= the whole link),
	 * than there is space for vehicles.
	 */
	synchronized public boolean hasSpace() {
		if (this.vehQueue.size() < getSpaceCap())
			return true;
		return false;
	}

	synchronized protected int vehOnLinkCount() {
		return this.vehQueue.size();
	}

	/**
	 * @return <code>true</code> if there are less vehicles in buffer than the flowCapacity's ceil
	 */
	private boolean hasBufferSpace() {
		if (this.buffercount < this.timeCapCeil)
			return true;
		return false;
	}

	// ////////////////////////////////////////////////////////////////////
	// Value for coloring the link in viz
	// actual veh count / maxcap
	// ////////////////////////////////////////////////////////////////////
	public double getDisplayableSpaceCapValue() {
		return (this.buffercount + this.vehQueue.size()) / this.spaceCap;
	}

	/**
	 * Returns a measure for how many vehicles on the link have a travel time
	 * higher than freespeedTraveltime on a scale from 0 to 2.
	 * When more then half of the possible vehicles are delayed, the value 1
	 * will be returned, which depicts the worst case on a (traditional) scale
	 * from 0 to 1.
	 *
	 * @return A measure for the number of vehicles being delayed on this link.
	 */
	public double getDisplayableTimeCapValue() {
		int count = this.buffercount;
		double now = SimulationTimer.getTime();
		for (ListIterator<Vehicle> i = this.vehQueue.listIterator(); i.hasNext();) {
			Vehicle veh = i.next();
			// Check if veh has reached destination
			if (veh.getDepartureTime_s() <= now) {
				count++;
			}
		}
		return count * 2.0 / this.spaceCap;
	}

	/**
	 * @return Returns the maxCap.
	 */
	public double getSpaceCap() {
		return this.spaceCap;
	}

	/**
	 * @return Returns a collection of all vehicles (driving, parking, in buffer, ...) on the link.
	 */
	public Collection<Vehicle> getAllVehicles(){

		Collection<Vehicle> vehicles = new ArrayList<Vehicle>();

		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.parkingList);
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);

		return vehicles;
	}

	synchronized public Collection<PositionInfo> getVehiclePositions(final Collection<PositionInfo> positions) {
		double now = SimulationTimer.getTime();
		int cnt = 0;
		double queueLen = 0.0; // the length of the queue jammed vehicles build at the end of the link
		double storageCapFactor = Gbl.getConfig().simulation().getStorageCapFactor();
		double vehLen = Math.min(	// the length of a vehicle in visualization
				this.euklideanDist / this.spaceCap, // all vehicles must have place on the link
				NetworkLayer.CELL_LENGTH / storageCapFactor); // a vehicle should not be larger than it's actual size

		// put all cars in the buffer one after the other
		for (Vehicle veh : this.buffer) {
			cnt++;
			// distance from fnode:
			double distanceFromFromNode = this.euklideanDist - cnt * vehLen;

			// lane:
			int lane = 1 + (veh.getID() % getLanes());

			// speed:
			double speed = getFreespeed();
			int cmp = (int) (veh.getDepartureTime_s() + 1.0 / this.timeCap + 2.0);

			if (now > cmp) {
				speed = 0.0;
			}
			veh.setSpeed(speed);

			PositionInfo position = new PositionInfo(veh.getDriver().getId(),
					this, distanceFromFromNode + NetworkLayer.CELL_LENGTH,
					lane, speed, PositionInfo.VehicleState.Driving);
			positions.add(position);
		}
		queueLen += this.buffer.size() * vehLen;

		/* place other driving cars according the following rule:
		 * - calculate the time how long the vehicle is on the link already
		 * - calculate the position where the vehicle should be if it could drive with freespeed
		 * - if the position is already within the congestion queue, add it to the queue with slow speed
		 * - if the position is not within the queue, just place the car with free speed at that place
		 */
		double lastDistance = Integer.MAX_VALUE;
		for (Vehicle veh : this.vehQueue) {
			double speed = getFreespeed();
			double travelTime = now - (veh.getDepartureTime_s() - this.freeTravelDuration);
			double distanceOnLink = (this.freeTravelDuration == 0.0 ? 0.0 : ((travelTime / this.freeTravelDuration) * this.euklideanDist));
			if (distanceOnLink > this.euklideanDist - queueLen) { // vehicle is already in queue
				queueLen += vehLen;
				distanceOnLink = this.euklideanDist - queueLen;
				speed = 0.0;
			}
			if (distanceOnLink >= lastDistance) {
				/* we have a queue, so it should not be possible that one vehicles overtakes another.
				 * additionally, if two vehicles entered at the same time, they would be drawn on top of each other.
				 * we don't allow this, so in this case we put one after the other. Theoretically, this could lead to
				 * vehicles placed at negative distance when a lot of vehicles all enter at the same time on an empty
				 * link. not sure what to do about this yet... just setting them to 0 currently.
				 */
				distanceOnLink = lastDistance - vehLen;
				if (distanceOnLink < 0) distanceOnLink = 0.0;
			}
			veh.setSpeed(speed);
			int lane = 1 + (veh.getID() % getLanes());
			PositionInfo position = new PositionInfo(veh.getDriver().getId(),
					this, distanceOnLink + NetworkLayer.CELL_LENGTH,
					lane, speed, PositionInfo.VehicleState.Driving);
			positions.add(position);
			lastDistance = distanceOnLink;
		}

		/* Put the vehicles from the waiting list in positions.
		 * Their actual position doesn't matter, so they are just placed
		 * to the coordinates of the from node */
		int lane = getLanes() + 1; // place them next to the link
		for (Vehicle veh : this.waitingList) {
			PositionInfo position = new PositionInfo(veh.getDriver().getId(),
					this, NetworkLayer.CELL_LENGTH, lane, 0.0, PositionInfo.VehicleState.Parking);
			positions.add(position);
		}

		/* put the vehicles from the parking list in positions
		 * their actual position doesn't matter, so they are just placed
		 * to the coordinates of the from node */
		lane = getLanes() + 2; // place them next to the link
		for (Vehicle veh : this.parkingList) {
			PositionInfo position = new PositionInfo(veh.getDriver().getId(),
					this, NetworkLayer.CELL_LENGTH, lane, 0.0, PositionInfo.VehicleState.Parking);
			positions.add(position);
		}

		return positions;
	}

	public void clearVehicles() {
		double now = SimulationTimer.getTime();

		for (Vehicle veh : this.parkingList) {
			QueueSimulation.getEvents().processEvent(
					new EventAgentStuck(now, veh.getDriverID(), veh
							.getCurrentLegNumber(), getId().toString(), veh.getDriver(),
							veh.getCurrentLeg(), veh.getCurrentLink()));
		}
		Simulation.decLiving(this.parkingList.size());
		Simulation.incLost(this.parkingList.size());
		this.parkingList.clear();


		for (Vehicle veh : this.waitingList) {
			QueueSimulation.getEvents().processEvent(
					new EventAgentStuck(now, veh.getDriverID(), veh
							.getCurrentLegNumber(), getId().toString(), veh.getDriver(),
							veh.getCurrentLeg(), veh.getCurrentLink()));
		}
		Simulation.decLiving(this.waitingList.size());
		Simulation.incLost(this.waitingList.size());
		this.waitingList.clear();

		for (Vehicle veh : this.vehQueue) {
			QueueSimulation.getEvents().processEvent(
					new EventAgentStuck(now, veh.getDriverID(), veh
							.getCurrentLegNumber(), getId().toString(), veh.getDriver(),
							veh.getCurrentLeg(), veh.getCurrentLink()));
		}
		Simulation.decLiving(this.vehQueue.size());
		Simulation.incLost(this.vehQueue.size());
		this.vehQueue.clear();

		for (Vehicle veh : this.buffer) {
			QueueSimulation.getEvents().processEvent(
					new EventAgentStuck(now, veh.getDriverID(), veh
							.getCurrentLegNumber(), getId().toString(), veh.getDriver(),
							veh.getCurrentLeg(), veh.getCurrentLink()));
		}
		Simulation.decLiving(this.buffer.size());
		Simulation.incLost(this.buffer.size());
		this.buffer.clear();
		this.buffercount = 0;
	}

	// //////////////////////////////////////////////////////////
	// For NetStateWriter
	// /////////////////////////////////////////////////////////

	static public class AgentOnLink implements DrawableAgentI {
		public double posInLink_m;
		public int lane = 1;

		public double getPosInLink_m() {
			return this.posInLink_m;
		}

		public int getLane() {
			return this.lane;
		}
	}

	/* If writeTimeCap then we use LinkSetRendererTRANSIMS to create a
	 * transims-like output.
	 * Vehicles that are to long on the link are drawn red other ones green
	 * also vehicles in buffer are drawn yellow (unlike TRANSIMS!)
	 */
	public Collection<AgentOnLink> getDrawableCollection(final boolean writeTimeCap) {


		Collection<PositionInfo> positions = new ArrayList<PositionInfo>();
		getVehiclePositions(positions);

		List<AgentOnLink> vehs = new ArrayList<AgentOnLink>();
		for (PositionInfo pos : positions) {
			AgentOnLink veh = new AgentOnLink();
			veh.posInLink_m = pos.getDistanceOnLink();
			vehs.add(veh);
		}

		// OLD vis method deprecated DS 06/07
//		double d = this.getLength()	/ (double) (this.vehQueue.size() + this.buffercount);
//		int i = 0;
//		for (Vehicle veh : this.vehQueue) {
//			AgentOnLink veh2 = new AgentOnLink();
//			veh2.posInLink_m = i * d;
//			if (writeTimeCap)
//				veh2.lane = veh.getSpeed() == 0. ? 2 : 1;
//			vehs.add(veh2);
//			i++;
//		}
//
//		for (Vehicle veh : this.buffer) {
//			AgentOnLink veh2 = new AgentOnLink();
//			veh2.posInLink_m = i * d;
//			if (writeTimeCap)
//				veh2.lane = veh.getSpeed() == 0. ? 2 : 1;
//			vehs.add(veh2);
//			i++;
//		}
		return vehs;
	}

	public double getMaxFlow_veh_s() {
		return this.timeCap;
	}

	public static class IdComparator implements Comparator<QueueLink>, Serializable {
		private static final long serialVersionUID = 1L;

		public int compare(final QueueLink o1, final QueueLink o2) {
			return o1.getId().toString().compareTo(o2.getId().toString());
		}

	}

	// search for vehicleId..
	public Vehicle getVehicle(final IdI id) {
		for (Vehicle veh : this.vehQueue) {
			if (veh.getDriver().getId().equals(id)) return veh;
		}
		for (Vehicle veh : this.buffer) {
			if (veh.getDriver().getId().equals(id)) return veh;
		}
		for (Vehicle veh : this.parkingList) {
			if (veh.getDriver().getId().equals(id)) return veh;
		}
		return null;
	}

}
