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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.signalsystems.mobsim.DefaultSignalizeableItem;
import org.matsim.signalsystems.model.SignalGroupState;

/**
 * Separating out the "lane" functionality from the "link" functionality also for QLinkImpl.  Ultimate goal is to unite this class here
 * with QLane.
 * <p/>
 * Design thoughts:<ul>
 * <li> It seems a bit doubtful why something this data structure needs to know something like "hasGreenForToLink(Id)".
 * The alternative, I guess, would be to have this in the surrounding QLink(Lanes)Impl.  Since the info is different for each lane,
 * after thinking about it it makes some sense to attach this directly to the lanes.  kai, jun'13
 * </ul>
 * 
 * @author nagel
 */
class QueueWithBuffer {

	/**
	 * The remaining integer part of the flow capacity available in one time step to move vehicles into the
	 * buffer. This value is updated each time step by a call to
	 * {@link #updateBufferCapacity(double)}.
	 */
	private double remainingflowCap = 0.0 ;
	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	private double flowcap_accumulate = 1.0 ;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	boolean thisTimeStepGreen = true ;
	double inverseFlowCapacityPerTimeStep;
	private double flowCapacityPerTimeStepFractionalPart;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	private double flowCapacityPerTimeStep;
	int bufferStorageCapacity;
	double usedBufferStorageCapacity = 0.0 ;
	Queue<QueueWithBuffer.Hole> holes = new LinkedList<QueueWithBuffer.Hole>();
	double freespeedTravelTime = Double.NaN;
	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	double bufferLastMovedTime = Time.UNDEFINED_TIME ;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	VehicleQ<QVehicle> vehQueue;
	/**
	 * This needs to be a ConcurrentHashMap because it can be accessed concurrently from
	 * two different threads via addFromIntersection(...) and popFirstVehicle().
	 * <p/>
	 * Design thoughts: <ul>
	 * <li> yyyy The _only_ place where this is needed is visualization.  I am, however, convinced that we can also get this from
	 * earliest link exit time. kai, jun'13
	 */
	Map<QVehicle, Double> linkEnterTimeMap = new ConcurrentHashMap<QVehicle, Double>() ;
	private double storageCapacity;
	double usedStorageCapacity;
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	Queue<QVehicle> buffer = new LinkedList<QVehicle>() ;
	/**
	 * null if the link is not signalized
	 */
	DefaultSignalizeableItem qSignalizedItem = null ;
	private double congestedDensity_veh_m;
	private int nHolesMax;
	private final QLinkImpl qLinkImpl;
	private final QNetwork network ;
	private static int congDensWarnCnt2 = 0;
	private static int congDensWarnCnt = 0;
	private static int spaceCapWarningCount = 0;
	static boolean HOLES = false ; // can be set from elsewhere in package, but not from outside.  kai, nov'10

	QueueWithBuffer(QLinkImpl qLinkImpl,  final VehicleQ<QVehicle> vehicleQueue ) {
		this.qLinkImpl = qLinkImpl;
		this.network = qLinkImpl.network ;
		this.vehQueue = vehicleQueue ;

		freespeedTravelTime = qLinkImpl.length / qLinkImpl.getLink().getFreespeed();
		if (Double.isNaN(freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}
		calculateCapacities();

		if ( QueueWithBuffer.HOLES ) {
			for ( int ii=0 ; ii<this.getStorageCapacity(); ii++ ) {
				Hole hole = new Hole() ;	
				hole.setEarliestLinkExitTime( Double.NEGATIVE_INFINITY ) ;
				this.holes.add(hole) ;
			}
			// yyyyyy this does, once more, not work with variable vehicle sizes.  kai, may'13
		}
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
				QueueWithBuffer.Hole hole = new QueueWithBuffer.Hole() ;
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

		while ((veh = vehQueue.peek()) != null) {
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}
			MobsimDriverAgent driver = veh.getDriver();

			if ( qLinkImpl.handleTransitStop(now, veh, driver) ) {
				continue ;
			}

			// Check if veh has reached destination:
			if ((qLinkImpl.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
				letVehicleArrive(now, veh);
				continue;
			}

			/* is there still room left in the buffer? */
			if (!hasFlowCapacityLeftAndBufferSpace() ) {
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

					letVehicleArrive(now,veh) ;
					continue;
				}
			}
			addToBuffer(veh, now);
			removeVehicleFromQueue(now, veh);
		} // end while
	}


	void removeVehicleFromQueue(final double now, QVehicle veh) {
		vehQueue.poll();
		usedStorageCapacity -= veh.getSizeInEquivalents();
		if ( QueueWithBuffer.HOLES ) {
			QueueWithBuffer.Hole hole = new QueueWithBuffer.Hole() ;
			double offset = qLinkImpl.link.getLength()*3600./15./1000. ;
			hole.setEarliestLinkExitTime( now + 0.9*offset + 0.2*MatsimRandom.getRandom().nextDouble()*offset ) ;
			holes.add( hole ) ;
		}
	}

	private void letVehicleArrive(final double now, QVehicle veh) {
		qLinkImpl.addParkedVehicle(veh);
		network.simEngine.letVehicleArrive(veh);
		qLinkImpl.makeVehicleAvailableToNextDriver(veh, now);
		// remove _after_ processing the arrival to keep link active
		removeVehicleFromQueue( now, veh ) ;
	}

	double effectiveVehicleFlowConsumptionInPCU( QVehicle veh ) {
		//		return Math.min(1.0, veh.getSizeInEquivalents() ) ;
		return veh.getSizeInEquivalents();
	}

	int vehInQueueCount() {
		// called by one test case
		return vehQueue.size();
	}

	boolean isActive() {
		return (this.flowcap_accumulate < 1.0) // still accumulating, thus active 
				|| (!this.vehQueue.isEmpty()) ;
	}

	void setSignalStateAllTurningMoves(SignalGroupState state) {
		qSignalizedItem.setSignalStateAllTurningMoves(state);

		thisTimeStepGreen  = qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation)
	}

	double getSimulatedFlowCapacity() {
		return this.flowCapacityPerTimeStep;
	}

	boolean isAcceptingFromUpstream() {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;

		boolean storageOk = usedStorageCapacity < storageCapacity ;
		if ( !QueueWithBuffer.HOLES ) {
			return storageOk ;
		}
		// continue only if HOLES
		if ( !storageOk ) {
			return false ;
		}
		// at this point, storage is ok, so start checking holes:
		QItem hole = holes.peek();
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

	void recalcTimeVariantAttributes(double now) {
		freespeedTravelTime = qLinkImpl.length / qLinkImpl.getLink().getFreespeed(now);
		calculateFlowCapacity(now);
		calculateStorageCapacity(now);
	}

	public QVehicle getVehicle(Id vehicleId) {
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.buffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return null;
	}

	void getAllVehicles(Collection<MobsimVehicle> vehicles) {
		vehicles.addAll(vehQueue);
		vehicles.addAll(buffer);
	}

	QVehicle popFirstVehicle() {
		double now = qLinkImpl.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = buffer.poll();
		usedBufferStorageCapacity = usedBufferStorageCapacity - veh.getSizeInEquivalents();
		bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		linkEnterTimeMap.remove(veh);

		//		Assert.assertTrue( veh != null ) ;
		//		Assert.assertTrue( veh.getDriver() != null ) ;
		//		Assert.assertTrue( this.getLink() != null ) ;

		qLinkImpl.network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getDriver().getId(), qLinkImpl.getLink().getId(), veh.getId()));
		return veh;
	}

	void setSignalStateForTurningMove(SignalGroupState state, Id toLinkId) {
		if (!qLinkImpl.getToNode().getNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " + qLinkImpl.getLink().getId());
		}
		qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		thisTimeStepGreen = qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	boolean hasGreenForToLink(Id toLinkId) {
		if (qSignalizedItem != null){
			return qSignalizedItem.isLinkGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	double getStorageCapacity() {
		return storageCapacity;
	}

	boolean isNotOfferingVehicle() {
		return buffer.isEmpty();
	}

	void clearVehicles() {
		double now = qLinkImpl.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		for (QVehicle veh : vehQueue) {
			qLinkImpl.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			qLinkImpl.network.simEngine.getMobsim().getAgentCounter().incLost();
			qLinkImpl.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		vehQueue.clear();
		linkEnterTimeMap.clear();

		for (QVehicle veh : buffer) {
			qLinkImpl.network.simEngine.getMobsim().getEventsManager().processEvent(
					new AgentStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			qLinkImpl.network.simEngine.getMobsim().getAgentCounter().incLost();
			qLinkImpl.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		buffer.clear();
		usedBufferStorageCapacity = 0;
	}

	void addFromUpstream(final QVehicle veh) {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		qLinkImpl.activateLink();
		linkEnterTimeMap.put(veh, now);
		usedStorageCapacity += veh.getSizeInEquivalents();
		double vehicleTravelTime = qLinkImpl.length / veh.getMaximumVelocity();
		double earliestExitTime = now + Math.max(freespeedTravelTime, vehicleTravelTime);
		earliestExitTime = Math.floor(earliestExitTime);
		veh.setEarliestLinkExitTime(earliestExitTime);
		veh.setCurrentLink(qLinkImpl.getLink());
		vehQueue.add(veh);
		qLinkImpl.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getDriver().getId(),
						qLinkImpl.getLink().getId(), veh.getId()));
		if ( QueueWithBuffer.HOLES ) {
			holes.poll();
		}
	}

	static class Hole extends QItem {
		private double earliestLinkEndTime ;

		@Override
		public double getEarliestLinkExitTime() {
			return earliestLinkEndTime;
		}

		@Override
		public void setEarliestLinkExitTime(double earliestLinkEndTime) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}
	}


}