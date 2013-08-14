/* *********************************************************************** *
 * project: org.matsim.*
 * BiPedQueueWithBuffer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.mobsim.SignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * A bidirectional pedestrian queue implementation. Most parts of code are c 'n p from QueueWithBuffer
 * @author laemmel
 *
 */
public class BiPedQueueWithBuffer extends AbstractQLane implements
SignalizeableItem, QLaneI {






	private static final Logger log = Logger.getLogger(BiPedQueueWithBuffer.class);
	private static final boolean HOLES = true;
	private static int spaceCapWarningCount;
	private final QLinkInternalI link;
	private final QNetwork network;
	private final VehicleQ<QVehicle> vehQueue;

	
	//TODO flow capacity for backward queue [GL August '13]
	private final Queue<BiPedQueueWithBuffer.Hole> holes = new LinkedList<BiPedQueueWithBuffer.Hole>();
	private double usedBufferStorageCapacity;
	private int bufferStorageCapacity;
	private double remainingflowCap;
	private double flowcap_accumulate;


	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep;
	private double flowCapacityPerTimeStepFractionalPart;


	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<QVehicle> buffer = new LinkedList<QVehicle>() ;
	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.UNDEFINED_TIME ;
	private double storageCapacity;
	private double usedStorageCapacity;
	private double freespeedTravelTime;
	private int nHolesMax;



	BiPedQueueWithBuffer(QNetwork network, QLinkInternalI link, final VehicleQ<QVehicle> vehicleQueue){



		//		this.network = link.ne
		this.link = link;
		this.network = network;
		this.vehQueue = vehicleQueue;



		this.freespeedTravelTime = link.getLink().getLength() / link.getLink().getFreespeed();
		calculateCapacities();

		//TODO
		//initial num of holes
	}



	private void calculateCapacities() {
		this.calculateFlowCapacity(Time.UNDEFINED_TIME);
		this.calculateStorageCapacity(Time.UNDEFINED_TIME);
		this.flowcap_accumulate = (this.flowCapacityPerTimeStepFractionalPart == 0.0 ? 0.0 : 1.0);
	}

	private void calculateStorageCapacity(final double time) {
		double storageCapFactor = this.network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getStorageCapFactor();
		this.bufferStorageCapacity = (int) Math.ceil(this.flowCapacityPerTimeStep);

		double numberOfLanes = this.link.getLink().getNumberOfLanes(time);
		// first guess at storageCapacity:
		this.storageCapacity = (this.link.getLink().getLength() * numberOfLanes)
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
			if (BiPedQueueWithBuffer.spaceCapWarningCount <= 10) {
				log.warn("Link " + this.link.getLink().getId() + " too small: enlarge storage capacity from: " + this.storageCapacity
						+ " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (BiPedQueueWithBuffer.spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				BiPedQueueWithBuffer.spaceCapWarningCount++;
			}
			this.storageCapacity = tempStorageCapacity;
		}

		
		//don't know what we make out of this. For now we set nr. of initial holes to storage_cap/2 [GL August '13]
//			// yyyy number of initial holes (= max number of vehicles on link given bottleneck spillback) is, in fact, dicated
//			// by the bottleneck flow capacity, together with the fundamental diagram. :-(  kai, ???'10
//			//
//			// Alternative would be to have link entry capacity constraint.  This, however, does not work so well with the
//			// current "parallel" logic, where capacity constraints are modeled only on the link.  kai, nov'10
//			double bnFlowCap_s = ((LinkImpl)this.qLinkImpl.link).getFlowCapacity() ;
//
//			// ( c * n_cells - cap * L ) / (L * c) = (n_cells/L - cap/c) ;
//			this.congestedDensity_veh_m = this.storageCapacity/this.qLinkImpl.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
//
//			if ( this.congestedDensity_veh_m > 10. ) {
//				if ( QueueWithBuffer.congDensWarnCnt2 < 1 ) {
//					QueueWithBuffer.congDensWarnCnt2++ ;
//					QLinkImpl.log.warn("congestedDensity_veh_m very large: " + this.congestedDensity_veh_m
//							+ "; does this make sense?  Setting to 10 veh/m (which is still a lot but who knows). "
//							+ "Definitely can't have it at Inf." ) ;
//				}
//			}
//
//			// congestedDensity is in veh/m.  If this is less than something reasonable (e.g. 1veh/50m) or even negative,
//			// then this means that the link has not enough storageCapacity (essentially not enough lanes) to transport the given
//			// flow capacity.  Will increase the storageCapacity accordingly:
//			if ( this.congestedDensity_veh_m < 1./50 ) {
//				if ( QueueWithBuffer.congDensWarnCnt < 1 ) {
//					QueueWithBuffer.congDensWarnCnt++ ;
//					QLinkImpl.log.warn( "link not ``wide'' enough to process flow capacity with holes.  increasing storage capacity ...") ;
//					QLinkImpl.log.warn( Gbl.ONLYONCE ) ;
//				}
//				this.storageCapacity = (1./50 + bnFlowCap_s*3600./(15.*1000)) * this.qLinkImpl.link.getLength() ;
//				this.congestedDensity_veh_m = this.storageCapacity/this.qLinkImpl.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
//			}

			this.nHolesMax = (int) Math.ceil( this.getStorageCapacity()) ;
//			QLinkImpl.log.warn(
//					" nHoles: " + this.nHolesMax
//					+ " storCap: " + this.storageCapacity
//					+ " len: " + this.qLinkImpl.link.getLength()
//					+ " bnFlowCap: " + bnFlowCap_s
//					+ " congDens: " + this.congestedDensity_veh_m
//					) ;
			for ( int ii=0 ; ii<this.nHolesMax ; ii++ ) {
				Hole hole = new Hole() ;
				hole.setEarliestLinkExitTime( 0. ) ;
				this.holes.add( hole ) ;
			}
			//			System.exit(-1);
	}



	private void calculateFlowCapacity(final double time) {
		this.flowCapacityPerTimeStep = ((LinkImpl)this.link.getLink()).getFlowCapacity(time);
		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		this.flowCapacityPerTimeStep = this.flowCapacityPerTimeStep
				* this.network.simEngine.getMobsim().getSimTimer().getSimTimestepSize()
				* this.network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getFlowCapFactor();
		this.flowCapacityPerTimeStepFractionalPart = this.flowCapacityPerTimeStep - (int) this.flowCapacityPerTimeStep;
	}
	


	@Override
	public boolean doSimStep(final double now ) {
		this.moveLaneToBuffer(now);
		return true ;
	}

	private void moveLaneToBuffer(double now) {
		QVehicle veh;

		while ((veh = this.vehQueue.peek()) != null) {


			if (veh.getEarliestLinkExitTime() > now){
				return;
			}
			//TODO
			//update earliest exit time here and check > now again XOR use BiPedQ!!! 


			MobsimDriverAgent driver = veh.getDriver();

			//			if ( qLinkImpl.handleTransitStop(now, veh, driver) ) {
			//				continue ;
			//			}

			// Check if veh has reached destination:
			if ((this.link.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
				// (1) we want to be able to have vehicles drive in circles. Thus, as long as they still have a next link,
				// they continue, even if here is their ultimate destination.
				// (2) On the other hand, we do not want to rely ONLY on the route plan since technically an agent does not
				// need to know the next link here already (this might be more picky than necessary; presumably,
				// it started out as the first condition, and then the second was added to allow driving in circles.)
				// kai, jun'13
				letVehicleArrive(now, veh);
				continue;
			}

			/* is there still room left in the buffer? */
			if (!hasFlowCapacityLeftAndBufferSpace() ) {
				return;
			}

			if (driver instanceof TransitDriverAgent) {
				throw new RuntimeException("for now TransitDriverAgent is not allowed here!");
			}
			addFromWait(veh, now);
			removeVehicleFromQueue(now);
		} // end while

	}


	//adds a vehicle to the buffer
	@Override
	public void addFromWait(final QVehicle veh, final double now) {
		// We are trying to modify this so it also works for vehicles different from size one.  The idea is that vehicles
		// _larger_ than size one can move as soon as at least one unit of flow or storage capacity is available.  
		// kai/mz/amit, mar'12

		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12

		if (this.remainingflowCap >= 1.0) {
			this.remainingflowCap--; //flow consumption of pedestrians is 1 [GL August '13]// -= qLinkImpl.road.effectiveVehicleFlowConsumptionInPCU(veh); 
		}
		else if (this.flowcap_accumulate >= 1.0) {
			this.flowcap_accumulate --; //dito //= qLinkImpl.road.effectiveVehicleFlowConsumptionInPCU(veh);
		}
		else {
			throw new IllegalStateException("Buffer of link " + this.link.getLink().getId() + " has no space left!");
		}
		this.buffer.add(veh);
		this.usedBufferStorageCapacity++; //all pedestrians have same size [GL August '13]// = this.usedBufferStorageCapacity + veh.getSizeInEquivalents();
		if (this.buffer.size() == 1) {
			this.bufferLastMovedTime = now;
			// (if there is one vehicle in the buffer now, there were zero vehicles in the buffer before.  in consequence,
			// need to reset the lastMovedTime.  If, in contrast, there was already a vehicle in the buffer before, we can
			// use the lastMovedTime that was (somehow) computed for that vehicle.)
		}
		this.link.getToNode().activateNode();
	}

	private boolean hasFlowCapacityLeftAndBufferSpace() {
		return (
				hasBufferSpaceLeft() 
				&& 
				((this.remainingflowCap >= 1.0) || (this.flowcap_accumulate >= 1.0))
				);
	}


	private boolean hasBufferSpaceLeft() {
		return this.usedBufferStorageCapacity < this.bufferStorageCapacity;
	}


	@Override
	public void updateRemainingFlowCapacity() {
		this.remainingflowCap = this.flowCapacityPerTimeStep;
		if (this.flowcap_accumulate < 1.0 && this.link.isNotOfferingVehicle() ) {
			this.flowcap_accumulate += this.flowCapacityPerTimeStepFractionalPart;
		}

	}



	@Override
	public double getSimulatedFlowCapacity() {
		return this.flowCapacityPerTimeStep;
	}



	@Override
	public double getStorageCapacity() {
		return this.storageCapacity;
	}

	@Override
	public QVehicle removeVehicleFromQueue(double now) {
		QVehicle veh = this.vehQueue.poll();
		this.usedStorageCapacity --; //peds = veh.getSizeInEquivalents();

		
		//At this point BiPedQueueWithBuffer differs from QueueWithBuffer
		Hole hole = new Hole() ;
		double L = .26;//((NetworkImpl)this.network.getNetwork()).getEffectiveCellSize();//.26m
		double z = 2; //2s
		double v = L/z;
		
		double offset = this.link.getLink().getLength()/v;
		hole.setEarliestLinkExitTime( now + offset);
		this.holes.add( hole ) ;
		return veh;
	}




	@Override
	public boolean isAcceptingFromUpstream() {
		//TODO check hole available!!
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
		
		
		
		//TODO add interface time to earliest link exit time for hole
		
		if ( hole.getEarliestLinkExitTime() > now ) {
			//			log.warn( " !hasSpace since all hole arrival times lie in future ") ;
			return false ;
		}
		return true ;
	}


	@Override
	public void addFromUpstream(final QVehicle veh) {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
	
		((BiPedQLinkImpl)this.link).activateLink();
		//		linkEnterTimeMap.put(veh, now);
		this.usedStorageCapacity += veh.getSizeInEquivalents();
		double vehicleTravelTime = this.link.getLink().getLength() / veh.getMaximumVelocity();//TODO debug her!!!
//		log.info(veh.getMaximumVelocity());

		double linkTravelTime = Math.max(this.freespeedTravelTime, vehicleTravelTime);
		double earliestExitTime = now + linkTravelTime;
		earliestExitTime = Math.floor(earliestExitTime);
		veh.setEarliestLinkExitTime(earliestExitTime);
		veh.setCurrentLink(this.link.getLink());
		this.vehQueue.add(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getId(),
						this.link.getLink().getId(), veh.getId()));
		if ( HOLES ) {
			this.holes.poll();
		}
	}

	@Override
	public boolean isNotOfferingVehicle() {
		return this.buffer.isEmpty();
	}
	
	public int getBufferSize() {
		return this.buffer.size();
	}

	@Override
	public QVehicle popFirstVehicle() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = this.buffer.poll();
		this.usedBufferStorageCapacity = this.usedBufferStorageCapacity - veh.getSizeInEquivalents();
		this.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		//		linkEnterTimeMap.remove(veh);

		//		Assert.assertTrue( veh != null ) ;
		//		Assert.assertTrue( veh.getDriver() != null ) ;
		//		Assert.assertTrue( this.getLink() != null ) ;

		this.network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getDriver().getId(), this.link.getLink().getId(), veh.getId()));
		return veh;
	}

	@Override
	public QVehicle getFirstVehicle() {
		return this.buffer.peek() ;
	}

	@Override
	public double getLastMovementTimeOfFirstVehicle() {
		return this.bufferLastMovedTime;
	}



	
	@Override
	public void clearVehicles() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : this.vehQueue) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.vehQueue.clear();
		//		linkEnterTimeMap.clear();

		for (QVehicle veh : this.buffer) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.buffer.clear();
		this.usedBufferStorageCapacity = 0;

	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = new ArrayList<MobsimVehicle>();
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);
		return vehicles ;
	}

	@Override
	public boolean isAcceptingFromWait() {
		return this.hasFlowCapacityLeftAndBufferSpace() ;
	}
	
	@Override
	public void recalcTimeVariantAttributes( final double now) {
		this.freespeedTravelTime = this.link.getLink().getLength() / this.link.getLink().getFreespeed(now);
		calculateFlowCapacity(now);
		calculateStorageCapacity(now);
	}
	

	
	private void letVehicleArrive(final double now, QVehicle veh) {
		this.link.addParkedVehicle(veh);
		this.network.simEngine.letVehicleArrive(veh);
//		this.link.makeVehicleAvailableToNextDriver(veh, now);
		// remove _after_ processing the arrival to keep link active
		removeVehicleFromQueue( now ) ;
	}
	
	//methods below will be implemnted at a later time [GL August '13]





	@Override
	public boolean isActive() {
		//for now we return true here [GL August '13]
		return true;
	}

	//methods below are not implemted for now (maybe later) [GL August '13]


	@Override
	public VisData getVisData() {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void addTransitSlightlyUpstreamOfStop(QVehicle veh) {
		throw new UnsupportedOperationException() ;

	}


	@Override
	public void setSignalized(boolean isSignalized) {
		throw new UnsupportedOperationException() ;

	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		throw new UnsupportedOperationException() ;

	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		throw new UnsupportedOperationException() ;

	}


	@Override
	public boolean hasGreenForToLink(Id toLinkId) {
		return true;
	}

	@Override
	public QVehicle getVehicle(Id vehicleId) {
		throw new UnsupportedOperationException() ;
	}


	public class Hole extends QItem {
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

}
