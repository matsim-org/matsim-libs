/* *********************************************************************** *
 * project: org.matsim.*
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

import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl.Hole;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.mobsim.DefaultSignalizeableItem;

class QueueWithBuffer {
	/**
	 * The remaining integer part of the flow capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateBufferCapacity(double)}.
	 */
	double remainingflowCap;
	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	double flowcap_accumulate;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	boolean thisTimeStepGreen;
	double inverseFlowCapacityPerTimeStep;
	double flowCapacityPerTimeStepFractionalPart;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	double flowCapacityPerTimeStep;
	int bufferStorageCapacity;
	double usedBufferStorageCapacity;
	Queue<Hole> holes;
	double freespeedTravelTime;
	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	double bufferLastMovedTime;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	VehicleQ<QVehicle> vehQueue;
	/**
	 * This needs to be a ConcurrentHashMap because it can be accessed concurrently from
	 * two different threads via addFromIntersection(...) and popFirstVehicle().
	 */
	Map<QVehicle, Double> linkEnterTimeMap;
	double storageCapacity;
	double usedStorageCapacity;
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	Queue<QVehicle> buffer;
	/**
	 * null if the link is not signalized
	 */
	DefaultSignalizeableItem qSignalizedItem;
	double congestedDensity_veh_m;
	int nHolesMax;
	private final QLinkImpl qLinkImpl;
	private final QNetwork network ;
	static int congDensWarnCnt2 = 0;
	static int congDensWarnCnt = 0;
	static int spaceCapWarningCount = 0;
	static boolean HOLES = false ; // can be set from elsewhere in package, but not from outside.  kai, nov'10

	QueueWithBuffer(double remainingflowCap, double flowcap_accumulate,
			boolean thisTimeStepGreen, double usedBufferStorageCapacity,
			Queue<Hole> holes, double freespeedTravelTime,
			double bufferLastMovedTime, Map<QVehicle, Double> linkEnterTimeMap,
			Queue<QVehicle> buffer, DefaultSignalizeableItem qSignalizedItem, QLinkImpl qLinkImpl) {
		this.remainingflowCap = remainingflowCap;
		this.flowcap_accumulate = flowcap_accumulate;
		this.thisTimeStepGreen = thisTimeStepGreen;
		this.usedBufferStorageCapacity = usedBufferStorageCapacity;
		this.holes = holes;
		this.freespeedTravelTime = freespeedTravelTime;
		this.bufferLastMovedTime = bufferLastMovedTime;
		this.linkEnterTimeMap = linkEnterTimeMap;
		this.buffer = buffer;
		this.qSignalizedItem = qSignalizedItem;
		this.qLinkImpl = qLinkImpl;
		this.network = qLinkImpl.network ;
	}

	void addToBuffer(final QVehicle veh, final double now) {
		// We are trying to modify this so it also works for vehicles different from size one.  The idea is that vehicles
		// _larger_ than size one can move as soon as at least one unit of flow or storage capacity is available.  
		// kai/mz/amit, mar'12
	
		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12
	
		if (remainingflowCap >= 1.0) {
			remainingflowCap -= qLinkImpl.road.effectiveVehicleFlowConsumptionInPCU(veh); 
		}
		else if (flowcap_accumulate >= 1.0) {
			flowcap_accumulate -= qLinkImpl.road.effectiveVehicleFlowConsumptionInPCU(veh);
		}
		else {
			throw new IllegalStateException("Buffer of link " + qLinkImpl.getLink().getId() + " has no space left!");
		}
		buffer.add(veh);
		usedBufferStorageCapacity = usedBufferStorageCapacity + veh.getSizeInEquivalents();
		if (buffer.size() == 1) {
			bufferLastMovedTime = now;
			// (if there is one vehicle in the buffer now, there were zero vehicles in the buffer before.  in consequence,
			// need to reset the lastMovedTime.  If, in contrast, there was already a vehicle in the buffer before, we can
			// use the lastMovedTime that was (somehow) computed for that vehicle.)
		}
		qLinkImpl.getToNode().activateNode();
	}

	boolean hasBufferSpaceLeft() {
		return usedBufferStorageCapacity < bufferStorageCapacity;
	}

	final boolean hasFlowCapacityLeftAndBufferSpace() {
		return (
				hasBufferSpaceLeft() 
				&& 
				((remainingflowCap >= 1.0) || (flowcap_accumulate >= 1.0))
				);
	}

	final void updateRemainingFlowCapacity() {
		remainingflowCap = flowCapacityPerTimeStep;
		//				if (this.thisTimeStepGreen && this.flowcap_accumulate < 1.0 && this.hasBufferSpaceLeft()) {
		if (thisTimeStepGreen && flowcap_accumulate < 1.0 && qLinkImpl.isNotOfferingVehicle() ) {
			flowcap_accumulate += flowCapacityPerTimeStepFractionalPart;
		}
	}

	void calculateCapacities() {
		this.calculateFlowCapacity(Time.UNDEFINED_TIME);
		this.calculateStorageCapacity(Time.UNDEFINED_TIME);
		flowcap_accumulate = (flowCapacityPerTimeStepFractionalPart == 0.0 ? 0.0 : 1.0);
	}

	void calculateFlowCapacity(final double time) {
		flowCapacityPerTimeStep = ((LinkImpl)qLinkImpl.getLink()).getFlowCapacity(time);
		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		flowCapacityPerTimeStep = flowCapacityPerTimeStep
				* network.simEngine.getMobsim().getSimTimer().getSimTimestepSize()
				* network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getFlowCapFactor();
		inverseFlowCapacityPerTimeStep = 1.0 / flowCapacityPerTimeStep;
		flowCapacityPerTimeStepFractionalPart = flowCapacityPerTimeStep - (int) flowCapacityPerTimeStep;
	}

	void calculateStorageCapacity(final double time) {
		double storageCapFactor = network.simEngine.getMobsim().getScenario().getConfig().getQSimConfigGroup().getStorageCapFactor();
		bufferStorageCapacity = (int) Math.ceil(flowCapacityPerTimeStep);
	
		double numberOfLanes = qLinkImpl.getLink().getNumberOfLanes(time);
		// first guess at storageCapacity:
		storageCapacity = (qLinkImpl.length * numberOfLanes)
				/ ((NetworkImpl) network.simEngine.getMobsim().getScenario().getNetwork()).getEffectiveCellSize() * storageCapFactor;
	
		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		storageCapacity = Math.max(storageCapacity, bufferStorageCapacity);
	
		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap = TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = freespeedTravelTime * flowCapacityPerTimeStep;
		// yy note: freespeedTravelTime may be Inf.  In this case, storageCapacity will also be set to Inf.  This can still be
		// interpreted, but it means that the link will act as an infinite sink.  kai, nov'10
	
		if (storageCapacity < tempStorageCapacity) {
			if (QueueWithBuffer.spaceCapWarningCount <= 10) {
				QLinkImpl.log.warn("Link " + qLinkImpl.getLink().getId() + " too small: enlarge storage capacity from: " + storageCapacity
						+ " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (QueueWithBuffer.spaceCapWarningCount == 10) {
					QLinkImpl.log.warn("Additional warnings of this type are suppressed.");
				}
				QueueWithBuffer.spaceCapWarningCount++;
			}
			storageCapacity = tempStorageCapacity;
		}
	
		if ( QueueWithBuffer.HOLES ) {
			// yyyy number of initial holes (= max number of vehicles on link given bottleneck spillback) is, in fact, dicated
			// by the bottleneck flow capacity, together with the fundamental diagram. :-(  kai, ???'10
			//
			// Alternative would be to have link entry capacity constraint.  This, however, does not work so well with the
			// current "parallel" logic, where capacity constraints are modeled only on the link.  kai, nov'10
			double bnFlowCap_s = ((LinkImpl)qLinkImpl.link).getFlowCapacity() ;
	
			// ( c * n_cells - cap * L ) / (L * c) = (n_cells/L - cap/c) ;
			congestedDensity_veh_m = storageCapacity/qLinkImpl.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
	
			if ( congestedDensity_veh_m > 10. ) {
				if ( QueueWithBuffer.congDensWarnCnt2 < 1 ) {
					QueueWithBuffer.congDensWarnCnt2++ ;
					QLinkImpl.log.warn("congestedDensity_veh_m very large: " + congestedDensity_veh_m
							+ "; does this make sense?  Setting to 10 veh/m (which is still a lot but who knows). "
							+ "Definitely can't have it at Inf." ) ;
				}
			}
	
			// congestedDensity is in veh/m.  If this is less than something reasonable (e.g. 1veh/50m) or even negative,
			// then this means that the link has not enough storageCapacity (essentially not enough lanes) to transport the given
			// flow capacity.  Will increase the storageCapacity accordingly:
			if ( congestedDensity_veh_m < 1./50 ) {
				if ( QueueWithBuffer.congDensWarnCnt < 1 ) {
					QueueWithBuffer.congDensWarnCnt++ ;
					QLinkImpl.log.warn( "link not ``wide'' enough to process flow capacity with holes.  increasing storage capacity ...") ;
					QLinkImpl.log.warn( Gbl.ONLYONCE ) ;
				}
				storageCapacity = (1./50 + bnFlowCap_s*3600./(15.*1000)) * qLinkImpl.link.getLength() ;
				congestedDensity_veh_m = storageCapacity/qLinkImpl.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
			}
	
			nHolesMax = (int) Math.ceil( congestedDensity_veh_m * qLinkImpl.link.getLength() ) ;
			QLinkImpl.log.warn(
					" nHoles: " + nHolesMax
					+ " storCap: " + storageCapacity
					+ " len: " + qLinkImpl.link.getLength()
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

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 * @param now
	 *          The current time.
	 */
	void moveLaneToBuffer(final double now) {
		QVehicle veh;
	
		qLinkImpl.moveTransitToQueue(now);
	
		// handle regular traffic
		while ((veh = vehQueue.peek()) != null) {
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}
			MobsimDriverAgent driver = veh.getDriver();
	
			boolean handled = qLinkImpl.handleTransitStop(now, veh, driver);
	
			if (!handled) {
				// Check if veh has reached destination:
				if ((qLinkImpl.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
					qLinkImpl.addParkedVehicle(veh);
					network.simEngine.letVehicleArrive(veh);
					qLinkImpl.makeVehicleAvailableToNextDriver(veh, now);
					// remove _after_ processing the arrival to keep link active
					vehQueue.poll();
					usedStorageCapacity -= veh.getSizeInEquivalents();
					if ( QueueWithBuffer.HOLES ) {
						Hole hole = new Hole() ;
						hole.setEarliestLinkExitTime( now + qLinkImpl.link.getLength()*3600./15./1000. ) ;
						holes.add( hole ) ;
					}
					continue;
				}
	
				/* is there still room left in the buffer, or is it overcrowded from the
				 * last time steps? */
				if (!hasFlowCapacityLeftAndBufferSpace()) {
					return;
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
	
						network.simEngine.letVehicleArrive(veh);
						qLinkImpl.addParkedVehicle(veh);
						qLinkImpl.makeVehicleAvailableToNextDriver(veh, now);
						// remove _after_ processing the arrival to keep link active
						vehQueue.poll();
						usedStorageCapacity -= veh.getSizeInEquivalents();
						if ( QueueWithBuffer.HOLES ) {
							Hole hole = new Hole() ;
							hole.setEarliestLinkExitTime( now + qLinkImpl.link.getLength()*3600./15./1000. ) ;
							holes.add( hole ) ;
						}
						continue;
					}
				}
				addToBuffer(veh, now);
				vehQueue.poll();
				usedStorageCapacity -= veh.getSizeInEquivalents();
				if ( QueueWithBuffer.HOLES ) {
					Hole hole = new Hole() ;
					double offset = qLinkImpl.link.getLength()*3600./15./1000. ;
					hole.setEarliestLinkExitTime( now + 0.9*offset + 0.2*MatsimRandom.getRandom().nextDouble()*offset ) ;
					holes.add( hole ) ;
				}
			}
		} // end while
	}

	double effectiveVehicleFlowConsumptionInPCU( QVehicle veh ) {
		//		return Math.min(1.0, veh.getSizeInEquivalents() ) ;
		return veh.getSizeInEquivalents();
	}

	int vehInQueueCount() {
		// called by one test case
		return vehQueue.size();
	}

}