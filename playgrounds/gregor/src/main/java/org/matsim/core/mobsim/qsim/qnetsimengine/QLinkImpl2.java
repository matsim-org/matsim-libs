package org.matsim.core.mobsim.qsim.qnetsimengine;
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



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.vis.VisLaneModelBuilder;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.signalsystems.mobsim.DefaultSignalizeableItem;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 */
public class QLinkImpl2 extends AbstractQLink implements SignalizeableItem {

	// static variables (no problem with memory)
	final private static Logger log = Logger.getLogger(QLinkImpl2.class);
	private static int spaceCapWarningCount = 0;
	static boolean HOLES = false ; // can be set from elsewhere in package, but not from outside.  kai, nov'10
	private static int congDensWarnCnt = 0;
	private static int congDensWarnCnt2 = 0;

	// instance variables (problem with memory)
	private final Queue<QItem> holes = new LinkedList<QItem>() ;

	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private boolean active = false;

	/*package*/ VisData visdata = null ;

	private double length = Double.NaN;

	private double freespeedTravelTime = Double.NaN;

	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.UNDEFINED_TIME;

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final VehicleQ<QVehicle> vehQueue;

	private final Map<QVehicle, Double> linkEnterTimeMap = new HashMap<QVehicle, Double>();

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
	private double remainingflowCap = 0.0;

	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	private double flowcap_accumulate = 1.0;

	/**
	 * null if the link is not signalized
	 */
	private DefaultSignalizeableItem qSignalizedItem = null;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	private boolean thisTimeStepGreen = true;
	private double congestedDensity_veh_m;
	private int nHolesMax;

	
	private final boolean includingArrivalsInLinkCapacities = true; //TODO make this configurabel [gl dec 2012]
	
	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param queueNetwork
	 * @param toNode
	 */
	public QLinkImpl2(final Link link2, QNetwork network, final QNode toNode) {
		this(link2, network, toNode, new FIFOVehicleQ());
	}

	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public QLinkImpl2(final Link link2, QNetwork network, final QNode toNode, final VehicleQ<QVehicle> vehicleQueue) {
		super(link2, network) ;
		this.toQueueNode = toNode;
		this.vehQueue = vehicleQueue;
		this.length = this.getLink().getLength();
		this.freespeedTravelTime = this.length / this.getLink().getFreespeed();
		if (Double.isNaN(this.freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}
		this.calculateCapacities();
		this.visdata = this.new VisDataImpl() ; // instantiating this here so we can cache some things
	}

	/* 
	 * yyyyyy There are two "active" functionalities (see isActive()).  It probably still works, but it does not look like
	 * it is intended this way.  kai, nov'11
	 */
	@Override
	void activateLink() {
		if (!this.active) {
			this.netElementActivator.activateLink(this);
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
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.linkEnterTimeMap.put(veh, now);
		this.usedStorageCapacity += veh.getSizeInEquivalents();
		double vehicleTravelTime = this.length / veh.getMaximumVelocity();
		double earliestExitTime = now + Math.max(this.freespeedTravelTime, vehicleTravelTime);
		earliestExitTime = Math.floor(earliestExitTime);
		veh.setEarliestLinkExitTime(earliestExitTime);
		veh.setCurrentLink(this.getLink());
		this.vehQueue.add(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getId(),
						this.getLink().getId(), veh.getId()));
		if ( HOLES ) {
			this.holes.poll();
		}
	}

	@Override
	void clearVehicles() {
		super.clearVehicles();

		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : this.vehQueue) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.vehQueue.clear();
		this.linkEnterTimeMap.clear();

		for (QVehicle veh : this.buffer) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.buffer.clear();
	}


	@Override
	boolean doSimStep(double now) {
		updateBufferCapacity();

		// move vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.
		moveLaneToBuffer(now);
		// move vehicles from waitingQueue into buffer if possible
		moveWaitToBuffer(now);

		this.active = this.isActive();
		return this.active;
	}


	private void updateBufferCapacity() {
		this.remainingflowCap = this.flowCapacityPerTimeStep;
		if (this.thisTimeStepGreen && this.flowcap_accumulate < 1.0) {
			this.flowcap_accumulate += this.flowCapFractionCache;
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
	private void moveLaneToBuffer(final double now) {
		QVehicle veh;

		this.moveTransitToQueue(now);

		// handle regular traffic
		while ((veh = this.vehQueue.peek()) != null) {
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}
			MobsimDriverAgent driver = veh.getDriver();

			boolean handled = this.handleTransitStop(now, veh, driver);

			if (!handled) {
				
				if (this.includingArrivalsInLinkCapacities) {
					/* is there still room left in the buffer, or is it overcrowded from the
					 * last time steps? */
					if (!hasFlowCapacityLeftAndBufferSpace()) {
						return;
					}	
				}
				
				
				
				// Check if veh has reached destination:
				if ((this.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
					this.addParkedVehicle(veh);
					this.network.simEngine.letVehicleArrive(veh);
					this.makeVehicleAvailableToNextDriver(veh, now);
					// remove _after_ processing the arrival to keep link active
					this.vehQueue.poll();
					this.usedStorageCapacity -= veh.getSizeInEquivalents();
					if ( HOLES ) {
						Hole hole = new Hole() ;
						hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
						this.holes.add( hole ) ;
					}
					updateAccumaltions(veh);
					continue;
				}

				if (!this.includingArrivalsInLinkCapacities) {
					/* is there still room left in the buffer, or is it overcrowded from the
					 * last time steps? */
					if (!hasFlowCapacityLeftAndBufferSpace()) {
						return;
					}	
				}

				if (driver instanceof TransitDriverAgent) {
					TransitDriverAgent trDriver = (TransitDriverAgent) driver;
					Id nextLinkId = trDriver.chooseNextLinkId();
					if (nextLinkId == null || nextLinkId.equals(trDriver.getCurrentLinkId())) {
						// special case: transit drivers can specify the next link being the current link
						// this can happen when a transit-lines route leads over exactly one link
						// normally, vehicles would not even drive on that link, but transit vehicles must
						// "drive" on that link in order to handle the stops on that link
						// so allow them to return some non-null link id in chooseNextLink() in order to be
						// placed on the link, and here we'll remove them again if needed...
						// ugly hack, but I didn't find a nicer solution sadly... mrieser, 5mar2011
						
						// Beispiel: Kanzler-Ubahn in Berlin.  Im Visum-Netz mit nur 1 Kante, mit Haltestelle am Anfang und
						// am Ende der Kante.  Zweite Haltestelle wird nur bedient, wenn das Fahrzeug im matsim-Sinne zum 
						// zweiten Mal auf die Kante gesetzt wird (oder so ähnlich, aber wir brauchen "nextLink==currentLink").
						// kai & marcel, mar'12
						
						this.network.simEngine.letVehicleArrive(veh);
						this.addParkedVehicle(veh);
						makeVehicleAvailableToNextDriver(veh, now);
						// remove _after_ processing the arrival to keep link active
						this.vehQueue.poll();
						this.usedStorageCapacity -= veh.getSizeInEquivalents();
						if ( HOLES ) {
							Hole hole = new Hole() ;
							hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
							this.holes.add( hole ) ;
						}
						continue;
					}
				}
				addToBuffer(veh, now);
				this.vehQueue.poll();
				this.usedStorageCapacity -= veh.getSizeInEquivalents();
				if ( HOLES ) {
					Hole hole = new Hole() ;
					double offset = this.link.getLength()*3600./15./1000. ;
					hole.setEarliestLinkExitTime( now + 0.9*offset + 0.2*MatsimRandom.getRandom().nextDouble()*offset ) ;
					this.holes.add( hole ) ;
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
		while (hasFlowCapacityLeftAndBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentWait2LinkEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId()));
			boolean handled = this.addTransitToBuffer(now, veh);

			if (!handled) {


				if (veh.getDriver() instanceof TransitDriverAgent) {
					TransitDriverAgent trDriver = (TransitDriverAgent) veh.getDriver();
					Id nextLinkId = trDriver.chooseNextLinkId();
					if (nextLinkId == null || nextLinkId.equals(trDriver.getCurrentLinkId())) {
						// special case: transit drivers can specify the next link being the current link
						// this can happen when a transit-lines route leads over exactly one link
						// normally, vehicles would not even drive on that link, but transit vehicles must
						// "drive" on that link in order to handle the stops on that link
						// so allow them to return some non-null link id in chooseNextLink() in order to be
						// placed on the link, and here we'll remove them again if needed...
						// ugly hack, but I didn't find a nicer solution sadly... mrieser, 5mar2011
						trDriver.endLegAndComputeNextState(now);
						this.addParkedVehicle(veh);
						this.network.simEngine.internalInterface.arrangeNextAgentState(trDriver) ;
						this.makeVehicleAvailableToNextDriver(veh, now);
						// remove _after_ processing the arrival to keep link active
						this.vehQueue.poll();
						this.usedStorageCapacity -= veh.getSizeInEquivalents();
						if ( HOLES ) {
							Hole hole = new Hole() ;
							hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
							this.holes.add( hole ) ;
						}
						continue;
					}
				}

				addToBuffer(veh, now);
				//				this.linkEnterTimeMap.put(veh, now);
				// (yyyyyy really??  kai, jan'11)
			}
		}
	}

	/**
	 * This method
	 * moves transit vehicles from the stop queue directly to the front of the
	 * "queue" of the QLink. An advantage is that this will observe flow
	 * capacity restrictions. 
	 */
	private void moveTransitToQueue(final double now) {
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
				this.vehQueue.addFirst(iter.previous());
			}
		}
	}


	private boolean handleTransitStop(final double now, final QVehicle veh,
			final MobsimDriverAgent driver) {
		boolean handled = false;
		// handle transit driver if necessary
		if (driver instanceof TransitDriverAgent) {
			TransitDriverAgent transitDriver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = transitDriver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
				double delay = transitDriver.handleTransitStop(stop, now);
				if (delay > 0.0) {

					veh.setEarliestLinkExitTime(now + delay);
					// (if the vehicle is not removed from the queue in the following lines, then this will effectively block the lane

					if (!stop.getIsBlockingLane()) {
						this.vehQueue.poll(); // remove the bus from the queue
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

	@Override
	boolean isNotOfferingVehicle() {
		return this.buffer.isEmpty();
	}

	@Override
	boolean hasSpace() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;

		boolean storageOk = this.usedStorageCapacity < this.storageCapacity ;
		if ( !HOLES ) {
			return storageOk ;
		}
		// continue only if HOLES
		if ( !storageOk ) {
			return false ;
		}
		// at this point, storage is ok, so start checking holes:
		QItem hole = this.holes.peek();
		if ( hole==null ) { // no holes available at all; in theory, this should not happen since covered by !storageOk
			//			log.warn( " !hasSpace since no holes available ") ;
			return false ;
		}
		if ( hole.getEarliestLinkExitTime() > now ) {
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

	private void calculateCapacities() {
		calculateFlowCapacity(Time.UNDEFINED_TIME);
		calculateStorageCapacity(Time.UNDEFINED_TIME);
		this.flowcap_accumulate = (this.flowCapFractionCache == 0.0 ? 0.0 : 1.0);
	}

	private void calculateFlowCapacity(final double time) {
		this.flowCapacityPerTimeStep = ((LinkImpl)this.getLink()).getFlowCapacity(time);
		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		this.flowCapacityPerTimeStep = this.flowCapacityPerTimeStep
				* this.network.simEngine.getMobsim().getSimTimer().getSimTimestepSize()
				* this.network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getFlowCapFactor();
		this.inverseSimulatedFlowCapacityCache = 1.0 / this.flowCapacityPerTimeStep;
		this.flowCapFractionCache = this.flowCapacityPerTimeStep - (int) this.flowCapacityPerTimeStep;
	}

	private void calculateStorageCapacity(final double time) {
		double storageCapFactor = this.network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getStorageCapFactor();
		this.bufferStorageCapacity = (int) Math.ceil(this.flowCapacityPerTimeStep);

		double numberOfLanes = this.getLink().getNumberOfLanes(time);
		// first guess at storageCapacity:
		this.storageCapacity = (this.length * numberOfLanes)
				/ ((NetworkImpl) this.network.simEngine.getMobsim().getScenario().getNetwork()).getEffectiveCellSize() * storageCapFactor;

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
				log.warn("Link " + this.getLink().getId() + " too small: enlarge storage capacity from: " + this.storageCapacity
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
			this.congestedDensity_veh_m = this.storageCapacity/this.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;

			if ( this.congestedDensity_veh_m > 10. ) {
				if ( congDensWarnCnt2 < 1 ) {
					congDensWarnCnt2++ ;
					log.warn("congestedDensity_veh_m very large: " + this.congestedDensity_veh_m
							+ "; does this make sense?  Setting to 10 veh/m (which is still a lot but who knows). "
							+ "Definitely can't have it at Inf." ) ;
				}
			}

			// congestedDensity is in veh/m.  If this is less than something reasonable (e.g. 1veh/50m) or even negative,
			// then this means that the link has not enough storageCapacity (essentially not enough lanes) to transport the given
			// flow capacity.  Will increase the storageCapacity accordingly:
			if ( this.congestedDensity_veh_m < 1./50 ) {
				if ( congDensWarnCnt < 1 ) {
					congDensWarnCnt++ ;
					log.warn( "link not ``wide'' enough to process flow capacity with holes.  increasing storage capacity ...") ;
					log.warn( Gbl.ONLYONCE ) ;
				}
				this.storageCapacity = (1./50 + bnFlowCap_s*3600./(15.*1000)) * this.link.getLength() ;
				this.congestedDensity_veh_m = this.storageCapacity/this.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
			}

			this.nHolesMax = (int) Math.ceil( this.congestedDensity_veh_m * this.link.getLength() ) ;
			log.warn(
					" nHoles: " + this.nHolesMax
					+ " storCap: " + this.storageCapacity
					+ " len: " + this.link.getLength()
					+ " bnFlowCap: " + bnFlowCap_s
					+ " congDens: " + this.congestedDensity_veh_m
					) ;
			for ( int ii=0 ; ii<this.nHolesMax ; ii++ ) {
				Hole hole = new Hole() ;
				hole.setEarliestLinkExitTime( 0. ) ;
				this.holes.add( hole ) ;
			}
			//			System.exit(-1);
		}
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		QVehicle ret = super.getVehicle(vehicleId);
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
	public Collection<MobsimVehicle> getAllNonParkedVehicles(){
		Collection<MobsimVehicle> vehicles = new ArrayList<MobsimVehicle>();
		vehicles.addAll(this.transitVehicleStopQueue);
		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);
		return vehicles;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		return this.storageCapacity;
	}

	int vehOnLinkCount() {
		// called by one test case
		return this.vehQueue.size();
	}


	@Override
	public Link getLink() {
		return this.link;
	}

	@Override
	public QNode getToNode() {
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
		 * Leave Link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.flowcap_accumulate < 1.0) || (!this.vehQueue.isEmpty()) 
				|| (!this.waitingList.isEmpty() || (!this.transitVehicleStopQueue.isEmpty()));
		return active;
	}

	private boolean hasFlowCapacityLeftAndBufferSpace() {
		return (
				(this.buffer.size() < this.bufferStorageCapacity) 
				&& 
				((this.remainingflowCap >= 1.0) || (this.flowcap_accumulate >= 1.0))
				);
	}
	
	private double effectiveVehicleFlowConsumptionInPCU( QVehicle veh ) {
//		return Math.min(1.0, veh.getSizeInEquivalents() ) ;
		return veh.getSizeInEquivalents();
	}

	private void addToBuffer(final QVehicle veh, final double now) {
		updateAccumaltions(veh);
		this.buffer.add(veh);
		if (this.buffer.size() == 1) {
			this.bufferLastMovedTime = now;
			// (if there is one vehicle in the buffer now, there were zero vehicles in the buffer before.  in consequence,
			// need to reset the lastMovedTime.  If, in contrast, there was already a vehicle in the buffer before, we can
			// use the lastMovedTime that was (somehow) computed for that vehicle.)
		}
		this.getToNode().activateNode();
	}

	private void updateAccumaltions(QVehicle veh) {
		// We are trying to modify this so it also works for vehicles different from size one.  The idea is that vehicles
		// _larger_ than size one can move as soon as at least one unit of flow or storage capacity is available.  
		// kai/mz/amit, mar'12
		
		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12
		
		if (this.remainingflowCap >= 1.0) {
			this.remainingflowCap -= this.effectiveVehicleFlowConsumptionInPCU(veh); 
		}
		else if (this.flowcap_accumulate >= 1.0) {
			this.flowcap_accumulate -= this.effectiveVehicleFlowConsumptionInPCU(veh);
		}
		else {
			throw new IllegalStateException("Buffer of link " + this.getLink().getId() + " has no space left!");
		}
	}

	@Override
	QVehicle popFirstVehicle() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = this.buffer.poll();
		this.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		this.linkEnterTimeMap.remove(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId()));
		return veh;
	}

	@Override
	QVehicle getFirstVehicle() {
		return this.buffer.peek();
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
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
		// (this is only for capacity accumulation)
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		if (!this.getToNode().getNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " + this.getLink().getId());
		}
		this.qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		this.thisTimeStepGreen = this.qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		this.qSignalizedItem  = new DefaultSignalizeableItem(this.getLink().getToNode().getOutLinks().keySet());
	}


	/**
	 * Inner class to encapsulate visualization methods
	 *
	 * @author dgrether
	 */
	class VisDataImpl implements VisData {

		private VisLaneModelBuilder laneModelBuilder = null;
		private VisLinkWLanes otfLink = null;

		private VisDataImpl() {
			double nodeOffset = QLinkImpl2.this.network.simEngine.getMobsim().getScenario().getConfig().otfVis().getNodeOffset(); 
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset +2.0; // +2.0: eventually we need a bit space for the signal
				this.laneModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				this.otfLink = this.laneModelBuilder.createOTFLinkWLanes(transformation, QLinkImpl2.this, nodeOffset, null);
				SnapshotLinkWidthCalculator linkWidthCalculator = QLinkImpl2.this.network.getLinkWidthCalculator();
				this.laneModelBuilder.recalculatePositions(this.otfLink, linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> getVehiclePositions( final Collection<AgentSnapshotInfo> positions) {
			AgentSnapshotInfoBuilder snapshotInfoBuilder = QLinkImpl2.this.network.simEngine.getAgentSnapshotInfoBuilder();

			double numberOfVehiclesDriving = QLinkImpl2.this.buffer.size() + QLinkImpl2.this.vehQueue.size();
			if (numberOfVehiclesDriving > 0) {
				double now = QLinkImpl2.this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
				Link link = QLinkImpl2.this.getLink();
				double spacing = snapshotInfoBuilder.calculateVehicleSpacing(link.getLength(), numberOfVehiclesDriving,
						QLinkImpl2.this.storageCapacity, QLinkImpl2.this.bufferStorageCapacity); 
				double freespeedTraveltime = link.getLength() / link.getFreespeed(now);

				double lastDistanceFromFromNode = Double.NaN;
				for (QVehicle veh : QLinkImpl2.this.buffer){
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, snapshotInfoBuilder, now,
							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
				}
				for (QVehicle veh : QLinkImpl2.this.vehQueue) {
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, snapshotInfoBuilder, now,
							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
				}
			}


			int cnt2 = 10 ; // a counter according to which non-moving items can be "spread out" in the visualization
			// initialize a bit away from the lane

			// treat vehicles from transit stops
			snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, QLinkImpl2.this.link, QLinkImpl2.this.transitVehicleStopQueue, cnt2 );

			// treat vehicles from waiting list:
			snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkImpl2.this.link, cnt2,
					QLinkImpl2.this.waitingList);

			snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkImpl2.this.link,
					QLinkImpl2.this.getAdditionalAgentsOnLink(), cnt2);

			// return:
			return positions;
		}

		private double createAndAddVehiclePositionAndReturnDistance(final Collection<AgentSnapshotInfo> positions,
				AgentSnapshotInfoBuilder snapshotInfoBuilder, double now, double lastDistanceFromFromNode, Link link,
				double spacing, double freespeedTraveltime, QVehicle veh)
		{
			double travelTime = Double.POSITIVE_INFINITY ;
			if ( QLinkImpl2.this.linkEnterTimeMap.get(veh) != null ) {
				// (otherwise the vehicle has never entered from an intersection)
				travelTime = now - QLinkImpl2.this.linkEnterTimeMap.get(veh);
			}
			lastDistanceFromFromNode = snapshotInfoBuilder.calculateDistanceOnVectorFromFromNode(link.getLength(), spacing, 
					lastDistanceFromFromNode, now, freespeedTraveltime, travelTime);
			Integer lane = snapshotInfoBuilder.guessLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			double speedValue = snapshotInfoBuilder.calcSpeedValueBetweenZeroAndOne(veh, 
					QLinkImpl2.this.getInverseSimulatedFlowCapacity(), now, link.getFreespeed());
			//					log.error("speed: " + speedValue + " distance: " + lastDistanceFromFromNode + " lane " + lane);
			if (this.otfLink != null){
				snapshotInfoBuilder.createAndAddVehiclePosition(positions, this.otfLink.getLinkStartCoord(), this.otfLink.getLinkEndCoord(), 
						QLinkImpl2.this.length, this.otfLink.getEuklideanDistance(), veh, 
						lastDistanceFromFromNode, lane, speedValue);
			}
			else {
				snapshotInfoBuilder.createAndAddVehiclePosition(positions, link.getFromNode().getCoord(), link.getToNode().getCoord(), 
						link.getLength(), ((LinkImpl)link).getEuklideanDistance() , veh, lastDistanceFromFromNode, lane, speedValue);
			}
			return lastDistanceFromFromNode;
		}
	}

	static class Hole extends QItem {
		private double earliestLinkEndTime ;

		@Override
		public double getEarliestLinkExitTime() {
			return this.earliestLinkEndTime;
		}

		@Override
		public void setEarliestLinkExitTime(double earliestLinkEndTime) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}
	}

	double getInverseSimulatedFlowCapacity() {
		return this.inverseSimulatedFlowCapacityCache ;
	}

}
