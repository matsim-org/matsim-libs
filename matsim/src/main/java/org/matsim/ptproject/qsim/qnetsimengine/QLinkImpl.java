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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentStuckEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.qsim.TransitQLaneFeature;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.helpers.AgentSnapshotInfoBuilder;
import org.matsim.ptproject.qsim.interfaces.NetsimEngine;
import org.matsim.signalsystems.mobsim.QSignalizedItem;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.VisData;

/**
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 */
public class QLinkImpl extends QLinkInternalI implements SignalizeableItem {

	// static variables (no problem with memory)
	final private static Logger log = Logger.getLogger(QLinkImpl.class);
	private static int spaceCapWarningCount = 0;
	static boolean HOLES = false ; // can be set from elsewhere in package, but not from outside.  kai, nov'10
	private static int congDensWarnCnt = 0;
	private static int congDensWarnCnt2 = 0;

	// instance variables (problem with memory)
	private final Queue<QItem> holes = new LinkedList<QItem>() ;


	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();
	/**
	 * The Link instance containing the data
	 */
	private final Link link;
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private boolean active = false;

	private final Map<Id, QVehicle> parkedVehicles = new LinkedHashMap<Id, QVehicle>(10);

	private final Map<Id, PlanAgent> additionalAgentsOnLink = new LinkedHashMap<Id, PlanAgent>();

	/*package*/ VisData visdata = null ;

	private QSimEngineInternalI qsimEngine = null;

	private double length = Double.NaN;

	private double freespeedTravelTime = Double.NaN;

	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.UNDEFINED_TIME;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final LinkedList<QVehicle> vehQueue = new LinkedList<QVehicle>();

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	/*package*/ final Queue<QVehicle> buffer = new LinkedList<QVehicle>();

	private double storageCapacity;

	private double usedStorageCapacity;

	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep; // previously called timeCap

	/*package*/ double inverseSimulatedFlowCapacityCache; // optimization, cache 1.0 / simulatedFlowCapacity

	private int bufferStorageCapacity; // optimization, cache Math.ceil(simulatedFlowCap)

	private double flowCapFractionCache; // optimization, cache simulatedFlowCap - (int)simulatedFlowCap

	/**
	 * The remaining integer part of the flow capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateBufferCapacity(double)}.
	 */
	private double remainingBufferCap = 0.0;

	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	private double buffercap_accumulate = 1.0;

	private final TransitQLaneFeature transitQueueLaneFeature = new TransitQLaneFeature(this);
	/**
	 * null if the link is not signalized
	 */
	private QSignalizedItem qSignalizedItem = null;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	private boolean thisTimeStepGreen = true;
	private double congestedDensity_veh_m;
	private int nHolesMax;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 */
	protected QLinkImpl(final Link link2, NetsimEngine engine, final QNode toNode) {
		this.link = link2;
		this.toQueueNode = toNode;
		this.length = this.getLink().getLength();
		this.freespeedTravelTime = this.length / this.getLink().getFreespeed();
		this.qsimEngine = (QSimEngineImpl) engine;

		this.calculateCapacities();

		this.visdata = this.new VisDataImpl() ; // instantiating this here so we can cache some things
	}

	@Override
	protected void activateLink() {
		if (!this.active) {
			this.qsimEngine.activateLink(this);
			this.active = true;
		}
	}

	/**
	 * Adds a vehicle to the link (i.e. the "queue"), called by
	 * {@link QNode#moveVehicleOverNode(QVehicle, QueueLane, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	@Override
	final void addFromIntersection(final QVehicle veh) {
		double now = this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.add(veh, now);
		veh.setCurrentLink(this.getLink());
		this.getQSimEngine().getMobsim().getEventsManager().processEvent(
				new LinkEnterEventImpl(now, veh.getDriver().getPerson().getId(),
						this.getLink().getId()));
		if ( HOLES ) {
			holes.poll();
		}
	}

	/**
	 * Adds a vehicle to the lane.
	 *
	 * @param veh
	 * @param now the current time
	 */
	private void add(final QVehicle veh, final double now) {
		// yyyy only called by "add(veh)", i.e. they can be consolidated. kai, jan'10
		this.vehQueue.add(veh);
		this.usedStorageCapacity += veh.getSizeInEquivalents();
		double departureTime;

		/* It's not the original lane, so there is a fractional rest we add to this link's freeSpeedTravelTime */
		departureTime = now + this.freespeedTravelTime + ( veh.getEarliestLinkExitTime() - Math.floor(veh.getEarliestLinkExitTime()) );
		// yyyy freespeedTravelTime may be Inf, in which case the vehicle never leaves, even if the time-variant link
		// is reset to a non-zero speed.  kai, nov'10
		
		/* It's a QueueLane that is directly connected to a QueueNode,
		 * so we have to floor the freeLinkTravelTime in order the get the same
		 * results compared to the old mobSim */
		departureTime = Math.floor(departureTime);
		veh.setLinkEnterTime(now);
		veh.setEarliestLinkExitTime(departureTime);
	}

	@Override
	protected void clearVehicles() {
		this.parkedVehicles.clear();
		double now = this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : this.waitingList) {
			this.getQSimEngine().getMobsim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), veh.getCurrentLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
		}
		this.getQSimEngine().getMobsim().getAgentCounter().decLiving(this.waitingList.size());
		this.getQSimEngine().getMobsim().getAgentCounter().incLost(this.waitingList.size());
		this.waitingList.clear();

		for (QVehicle veh : this.vehQueue) {
			this.getQSimEngine().getMobsim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), veh.getCurrentLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
		}
		this.getQSimEngine().getMobsim().getAgentCounter().decLiving(this.vehQueue.size());
		this.getQSimEngine().getMobsim().getAgentCounter().incLost(this.vehQueue.size());
		this.vehQueue.clear();

		for (QVehicle veh : this.buffer) {
			this.getQSimEngine().getMobsim().getEventsManager().processEvent(
					new AgentStuckEventImpl(now, veh.getDriver().getPerson().getId(), veh.getCurrentLink().getId(), veh.getDriver().getCurrentLeg().getMode()));
		}
		this.getQSimEngine().getMobsim().getAgentCounter().decLiving(this.buffer.size());
		this.getQSimEngine().getMobsim().getAgentCounter().incLost(this.buffer.size());
		this.buffer.clear();
	}

	@Override
	public void addParkedVehicle(QVehicle vehicle) {
		this.parkedVehicles.put(vehicle.getId(), vehicle);
		vehicle.setCurrentLink(this.link);
	}

	/*package*/ QVehicle getParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	@Override
	final QVehicle removeParkedVehicle(Id vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

//	@Override
//	public void reinsertBus( QVehicle vehicle ) {
//		this.vehQueue.addFirst(vehicle);
//	}

	@Override
	public void addDepartingVehicle(QVehicle vehicle) {
		this.waitingList.add(vehicle);
		vehicle.setCurrentLink(this.getLink());
		this.activateLink();
	}

	@Override
	protected boolean moveLink(double now) {
		// yyyy needs to be final
		boolean ret = false;
		ret = this.moveLane(now);
		this.active = ret;
		return ret;
	}

	/** called from framework, do everything related to link movement here
	 *
	 * @param now current time step
	 * @return
	 */
	protected boolean moveLane(final double now) {
		updateBufferCapacity();

		// move vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.
		moveLaneToBuffer(now);
		// move vehicles from waitingQueue into buffer if possible
		moveWaitToBuffer(now);
		return this.isActive();
	}

	private void updateBufferCapacity() {
		this.remainingBufferCap = this.flowCapacityPerTimeStep;
		if (this.thisTimeStepGreen && this.buffercap_accumulate < 1.0) {
			this.buffercap_accumulate += this.flowCapFractionCache;
		}
	}

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 *
	 * @param now
	 *          The current time.
	 */
	protected void moveLaneToBuffer(final double now) {
		QVehicle veh;

		this.transitQueueLaneFeature.beforeMoveLaneToBuffer(now);

		// handle regular traffic
		while ((veh = this.vehQueue.peek()) != null) {
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}
			PersonDriverAgent driver = veh.getDriver();

			boolean handled = this.transitQueueLaneFeature.handleMoveLaneToBuffer(now, veh, driver);

			if (!handled) {
				// Check if veh has reached destination:
				if ((this.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
					driver.endLegAndAssumeControl(now);
					this.addParkedVehicle(veh);
					// remove _after_ processing the arrival to keep link active
					this.vehQueue.poll();
					this.usedStorageCapacity -= veh.getSizeInEquivalents();
					if ( HOLES ) {
						Hole hole = new Hole() ;
						hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
						holes.add( hole ) ;
					}
					continue;
				}

				/* is there still room left in the buffer, or is it overcrowded from the
				 * last time steps? */
				if (!hasBufferSpace()) {
					return;
				}

				addToBuffer(veh, now);
				this.vehQueue.poll();
				this.usedStorageCapacity -= veh.getSizeInEquivalents();
				if ( HOLES ) {
					Hole hole = new Hole() ;
					double offset = this.link.getLength()*3600./15./1000. ;
					hole.setEarliestLinkExitTime( now + 0.9*offset + 0.2*MatsimRandom.getRandom().nextDouble()*offset ) ;
					holes.add( hole ) ;
				}
			}
		} // end while
	}

	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToBuffer(final double now) {
		while (hasBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.getQSimEngine().getMobsim().getEventsManager().processEvent(
					new AgentWait2LinkEventImpl(now, veh.getDriver().getPerson().getId(), this.getLink().getId()));
			boolean handled = this.transitQueueLaneFeature.handleMoveWaitToBuffer(now, veh);

			if (!handled) {
				addToBuffer(veh, now);
			}
		}
	}

	@Override
	final boolean bufferIsEmpty() {
		return this.buffer.isEmpty();
	}

	@Override
	final boolean hasSpace() {
		boolean storageOk = this.usedStorageCapacity < getStorageCapacity();
		if ( !HOLES || !storageOk ) {
			return storageOk ;
		}
		// at this point, storage is ok!
		QItem hole = holes.peek();
		if ( hole==null ) { // no holes available at all; in theory, this should not happen since covered by !storageOk
			//			log.warn( " !hasSpace since no holes available ") ;
			return false ;
		}
		if ( hole.getEarliestLinkExitTime() > this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay() ) {
			//			log.warn( " !hasSpace since all hole arrival times lie in future ") ;
			return false ;
		}
		return true ;
	}


	@Override
	public void recalcTimeVariantAttributes(double now) {
		this.freespeedTravelTime = this.length / this.getLink().getFreespeed(now);
		calculateFlowCapacity(now);
		calculateStorageCapacity(now);
	}

	void calculateCapacities() {
		calculateFlowCapacity(Time.UNDEFINED_TIME);
		calculateStorageCapacity(Time.UNDEFINED_TIME);
		this.buffercap_accumulate = (this.flowCapFractionCache == 0.0 ? 0.0 : 1.0);
	}

	private void calculateFlowCapacity(final double time) {
		this.flowCapacityPerTimeStep = ((LinkImpl)this.getLink()).getFlowCapacity(time);
		// we need the flow capcity per sim-tick and multiplied with flowCapFactor
		this.flowCapacityPerTimeStep = this.flowCapacityPerTimeStep
			* this.getQSimEngine().getMobsim().getSimTimer().getSimTimestepSize()
			* this.getQSimEngine().getMobsim().getScenario().getConfig().getQSimConfigGroup().getFlowCapFactor();
		this.inverseSimulatedFlowCapacityCache = 1.0 / this.flowCapacityPerTimeStep;
		this.flowCapFractionCache = this.flowCapacityPerTimeStep - (int) this.flowCapacityPerTimeStep;
	}

	private void calculateStorageCapacity(final double time) {
		double storageCapFactor = this.getQSimEngine().getMobsim().getScenario().getConfig().getQSimConfigGroup().getStorageCapFactor();
		this.bufferStorageCapacity = (int) Math.ceil(this.flowCapacityPerTimeStep);

		double numberOfLanes = this.getLink().getNumberOfLanes(time);
		// first guess at storageCapacity:
		this.storageCapacity = (this.length * numberOfLanes)
		/ ((NetworkImpl) this.qsimEngine.getMobsim().getScenario().getNetwork()).getEffectiveCellSize() * storageCapFactor;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap = TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = this.freespeedTravelTime * this.flowCapacityPerTimeStep;
		// yy note: freespeedTravelTime may be Inf.  In this case, storageCapacity will also be set to Inf.  This can still be
		// interpreted, but it means that the link will act as an infinite sink.  kai, nov'10
		
		if (this.storageCapacity < tempStorageCapacity) {
			if (spaceCapWarningCount <= 10) {
				log.warn("Link " + this.getLink().getId() + " too small: enlarge storage capcity from: " + this.storageCapacity 
						+ " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++;
			}
			this.storageCapacity = tempStorageCapacity;
		}

		if ( HOLES ) {
			// yyyy number of initial holes (= max number of vehicles on link given bottleneck spillback) is, in fact, dicated
			// by the bottleneck flow capacity, together with the fundamental diagram. :-(  kai, ???'10
			//
			// Alternative would be to have link entry capacity constraint.  This, however, does not work so well with the 
			// current "parallel" logic, where capacity constraints are modeled only on the link.  kai, nov'10
			double bnFlowCap_s = ((LinkImpl)this.link).getFlowCapacity() ;

			// ( c * n_cells - cap * L ) / (L * c) = (n_cells/L - cap/c) ;
			congestedDensity_veh_m = this.storageCapacity/this.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;

			if ( congestedDensity_veh_m > 10. ) {
				if ( congDensWarnCnt2 < 1 ) {
					congDensWarnCnt2++ ;
					log.warn("congestedDensity_veh_m very large: " + congestedDensity_veh_m 
							+ "; does this make sense?  Setting to 10 veh/m (which is still a lot but who knows). "
							+ "Definitely can't have it at Inf." ) ;
				}
			}
			
			// congestedDensity is in veh/m.  If this is less than something reasonable (e.g. 1veh/50m) or even negative,
			// then this means that the link has not enough storageCapacity (essentially not enough lanes) to transport the given
			// flow capacity.  Will increase the storageCapacity accordingly:
			if ( congestedDensity_veh_m < 1./50 ) {
				if ( congDensWarnCnt < 1 ) {
					congDensWarnCnt++ ;
					log.warn( "link not ``wide'' enough to process flow capacity with holes.  increasing storage capacity ...") ;
					log.warn( Gbl.ONLYONCE ) ;
				}
				this.storageCapacity = (1./50 + bnFlowCap_s*3600./(15.*1000)) * this.link.getLength() ;
				congestedDensity_veh_m = this.storageCapacity/this.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
			}

			nHolesMax = (int) Math.ceil( congestedDensity_veh_m * this.link.getLength() ) ;
			log.warn(
					" nHoles: " + nHolesMax
					+ " storCap: " + this.storageCapacity
					+ " len: " + this.link.getLength()
					+ " bnFlowCap: " + bnFlowCap_s
					+ " congDens: " + congestedDensity_veh_m
			) ;
			for ( int ii=0 ; ii<nHolesMax ; ii++ ) {
				Hole hole = new Hole() ;
				hole.setEarliestLinkExitTime( 0. ) ;
				holes.add( hole ) ;
			}
			//			System.exit(-1);
		}
	}


	@Override
	public QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = getParkedVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.buffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return null;
	}

	@Override
	public Collection<QVehicle> getAllVehicles() {

		Collection<QVehicle> vehicles = this.getAllNonParkedVehicles();
		vehicles.addAll(this.parkedVehicles.values());
		//	    new ArrayList<QueueVehicle>(this.parkedVehicles.values());
		//	  vehicles.addAll(transitQueueLaneFeature.getFeatureVehicles());
		//    vehicles.addAll(this.waitingList);
		//    vehicles.addAll(this.vehQueue);
		//    vehicles.addAll(this.buffer);
		return vehicles;
	}

	@Override
	public Collection<QVehicle> getAllNonParkedVehicles(){
		Collection<QVehicle> vehicles = new ArrayList<QVehicle>();
		vehicles.addAll(this.transitQueueLaneFeature.getFeatureVehicles());
		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);
		return vehicles;
	}

	/**
	 * @return Returns the maximum number of vehicles that can be placed on the
	 *         link at a time.
	 */
	/*package*/ double getStorageCapacity() {
		return this.storageCapacity;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	@Override
	public double getSpaceCap() {
		return this.storageCapacity;
	}

	@Override
	protected QSimEngineInternalI getQSimEngine(){
		return this.qsimEngine;
	}

	@Override
	public QSim getMobsim() {
		return this.qsimEngine.getMobsim();
	}

	@Override
	void setQSimEngine(NetsimEngine qsimEngine) { // yyyy this methods feels a bit non-sensical.  kai, oct'10
		this.qsimEngine = (QSimEngineInternalI) qsimEngine;
	}

	/**
	 * One should think about the need for this method
	 * because it is only called by one testcase
	 * </p>
	 * If it is only called by the test case, can protect it by making it package-private and putting the test in the same
	 * package.  kai, aug'10
	 * @return
	 */
	int vehOnLinkCount() {
		return this.vehQueue.size();
	}


	@Override
	public Link getLink() {
		return this.link;
	}

	@Override
	public QNode getToQueueNode() {
		return this.toQueueNode;
	}

	/**
	 * This method returns the normalized capacity of the link, i.e. the capacity
	 * of vehicles per second. It is considering the capacity reduction factors
	 * set in the config and the simulation's tick time.
	 *
	 * @return the flow capacity of this link per second, scaled by the config
	 *         values and in relation to the SimulationTimer's simticktime.
	 */
	double getSimulatedFlowCapacity() {
		return this.flowCapacityPerTimeStep;
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	private boolean isActive() {
		/*
		 * Leave Lane active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.buffercap_accumulate < 1.0) || (!this.vehQueue.isEmpty()) || (!this.waitingList.isEmpty() || this.transitQueueLaneFeature.isFeatureActive());
		return active;
	}

	@Override
	public LinkedList<QVehicle> getVehQueue() {
		return this.vehQueue;
	}

	/**
	 * @return <code>true</code> if there are less vehicles in buffer than the flowCapacity's ceil
	 */
	private boolean hasBufferSpace() {
		return ((this.buffer.size() < this.bufferStorageCapacity) && ((this.remainingBufferCap >= 1.0)
				|| (this.buffercap_accumulate >= 1.0)));
	}

	private void addToBuffer(final QVehicle veh, final double now) {
		if (this.remainingBufferCap >= 1.0) {
			this.remainingBufferCap--;
		}
		else if (this.buffercap_accumulate >= 1.0) {
			this.buffercap_accumulate--;
		}
		else {
			throw new IllegalStateException("Buffer of link " + this.getLink().getId() + " has no space left!");
		}
		this.buffer.add(veh);
		if (this.buffer.size() == 1) {
			this.bufferLastMovedTime = now;
		}
		this.getToQueueNode().activateNode();
	}

	@Override
	QVehicle popFirstFromBuffer() {
		double now = this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = this.buffer.poll();
		this.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		this.getQSimEngine().getMobsim().getEventsManager().processEvent(new LinkLeaveEventImpl(now, veh.getDriver().getPerson().getId(), this.getLink().getId()));
		return veh;
	}

	@Override
	QVehicle getFirstFromBuffer() {
		return this.buffer.peek();
	}

	@Override
	public void registerAgentOnLink(PlanAgent planAgent) {
		this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
	}

	@Override
	public void unregisterAgentOnLink(PlanAgent planAgent) {
		this.additionalAgentsOnLink.remove(planAgent.getId());
	}

	@Override
	double getBufferLastMovedTime() {
		return this.bufferLastMovedTime;
	}

	@Override
	public boolean hasGreenForToLink(Id toLinkId){
		if (this.qSignalizedItem != null){
			return this.qSignalizedItem.isLinkGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.qSignalizedItem.setSignalStateAllTurningMoves(state);
		this.thisTimeStepGreen  = this.qSignalizedItem.isLinkGreen();
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		if (!this.getToQueueNode().getNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " + this.getLink().getId());
		}
		this.qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);
		this.thisTimeStepGreen = this.qSignalizedItem.isLinkGreen();
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		this.qSignalizedItem  = new QSignalizedItem(this.getLink().getToNode().getOutLinks().keySet());
	}


	/**
	 * Inner class to encapsulate visualization methods
	 *
	 * @author dgrether
	 */
	class VisDataImpl implements VisData {

		private VisDataImpl() {
		}

		@Override
		public Collection<AgentSnapshotInfo> getVehiclePositions( final Collection<AgentSnapshotInfo> positions) {
			double time = QLinkImpl.this.getQSimEngine().getMobsim().getSimTimer().getTimeOfDay() ;

			AgentSnapshotInfoBuilder snapshotInfoBuilder = QLinkImpl.this.getQSimEngine().getAgentSnapshotInfoBuilder();

			snapshotInfoBuilder.addVehiclePositions(positions, time, QLinkImpl.this.link, QLinkImpl.this.buffer,
					QLinkImpl.this.vehQueue, QLinkImpl.this.holes, QLinkImpl.this.inverseSimulatedFlowCapacityCache,
					QLinkImpl.this.storageCapacity, QLinkImpl.this.bufferStorageCapacity, QLinkImpl.this.getLink().getLength(), 
					QLinkImpl.this.transitQueueLaneFeature, QLinkImpl.this.nHolesMax );

			int cnt2 = 0 ; // a counter according to which non-moving items can be "spread out" in the visualization
			// treat vehicles from transit stops
			QLinkImpl.this.transitQueueLaneFeature.positionVehiclesFromTransitStop(positions, cnt2 );

			// treat vehicles from waiting list:
			snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkImpl.this.link, cnt2,
					QLinkImpl.this.waitingList, QLinkImpl.this.transitQueueLaneFeature);

			snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkImpl.this.link,
					QLinkImpl.this.additionalAgentsOnLink.values(), cnt2);

			// return:
			return positions;
		}
	}

	static class Hole implements QItem {
		private double earliestLinkEndTime ;

		public double getEarliestLinkExitTime() {
			return earliestLinkEndTime;
		}

		public void setEarliestLinkExitTime(double earliestLinkEndTime) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}
	}

}
