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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

/**
 * 
 * @author nagel
 */
class AssignmentEmulatingQLane extends QLaneI {
	private static Logger log = Logger.getLogger(AssignmentEmulatingQLane.class ) ;

	double freespeedTravelTime = Double.NaN;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	VehicleQ<QVehicle> vehQueue;

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	Queue<QVehicle> buffer = new LinkedList<QVehicle>() ;
	/**
	 * null if the link is not signalized
	 */
	final AbstractQLink qLink;
	private final Id id;
	/**
	 * LaneEvents should only be fired if there is more than one QueueLane on a QueueLink
	 * because the LaneEvents are identical with LinkEnter/LeaveEvents otherwise.
	 * Possibly set to "true" in QLane .
	 */
	boolean generatingEvents = false;
	
	// (still) private:
	private VisData visData = new VisDataImpl() ;
	/**
	 * This flag indicates whether the QLane is the first lane on the link or one
	 * of the subsequent lanes.
	 */
	boolean isFirstLane = true ;
	double endsAtMetersFromLinkEnd = 0. ;

	// get properties no longer from qlink, but have them by yourself:
	// NOTE: we need to have qlink since we need access e.g. for vehicle arrival or for public transit
	// On the other hand, the qlink properties (e.g. number of lanes) may not be the ones we need here because they
	// may be divided between parallel lanes.  So we need both.
	double length = Double.NaN ;

	private Map<Id,Double> vehEnterTimeMap = new HashMap<Id,Double>() ;

	private final NetsimEngineContext context;

	private final NetsimInternalInterface netsimEngine;

	private final LinkSpeedCalculator linkSpeedCalculator;


	
	AssignmentEmulatingQLane(AbstractQLink qLinkImpl,  final VehicleQ<QVehicle> vehicleQueue, Id id, 
			NetsimEngineContext context, NetsimInternalInterface netsimEngine2, LinkSpeedCalculator linkSpeedCalculator ) {
		this.id = id ;
		this.qLink = qLinkImpl;
		this.vehQueue = vehicleQueue ;
		this.context = context;
		this.netsimEngine = netsimEngine2;
		this.linkSpeedCalculator = linkSpeedCalculator;

	}
	@Override
	public final void addFromWait(final QVehicle veh) {
		double now = context.getSimTimer().getTimeOfDay() ;
		addToBuffer(veh, now);
	}
	
	private final void addToBuffer(final QVehicle veh, final double now) {
		buffer.add(veh); // we always accept
		qLink.getToNode().activateNode();
	}
	@Override
	public final boolean isAcceptingFromWait() {
		return true ; // we always accept
	}

	@Override
	public boolean doSimStep( ) {
		double now = context.getSimTimer().getTimeOfDay() ;
		this.moveLaneToBuffer(now);
		return true ;
	}

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 * @param now
	 *          The current time.
	 */
	final void moveLaneToBuffer(final double now) {
		QVehicle veh;

		while ((veh = vehQueue.peek()) != null) {
			if ( veh.getEarliestLinkExitTime() > now ) {
				return;
			}

			MobsimDriverAgent driver = veh.getDriver();

			if ( driver instanceof AbstractTransitDriverAgent ) {
				AbstractTransitDriverAgent transitDriver = (AbstractTransitDriverAgent) driver ;
			HandleTransitStopResult handleTransitStop = qLink.getTransitQLink().handleTransitStop(now, veh, transitDriver, qLink.getLink().getId());
			if (handleTransitStop == HandleTransitStopResult.accepted) {
				// vehicle has been accepted into the transit vehicle queue of the link.
				removeVehicleFromQueue(now) ;
				continue;
			} else if (handleTransitStop == HandleTransitStopResult.rehandle) {
				continue; // yy why "continue", and not "break" or "return"?  Seems to me that this
				// is currently only working because qLink.handleTransitStop(...) also increases the
				// earliestLinkExitTime for the present vehicle.  kai, oct'13
			} else if (handleTransitStop == HandleTransitStopResult.continue_driving) {
				// Do nothing, but go on.. 
			} 
			}
			
			// Check if veh has reached destination:
			if ((driver.chooseNextLinkId() == null)) {
				letVehicleArrive(now, veh);
				continue;
			}

			addToBuffer(veh, now);
			removeVehicleFromQueue(now);
		} // end while
	}

	private final QVehicle removeVehicleFromQueue(final double now) {
		QVehicle veh = vehQueue.poll();
		return veh ;
	}

	private final void letVehicleArrive(final double now, QVehicle veh) {
		qLink.addParkedVehicle(veh);
		netsimEngine.letVehicleArrive(veh);
		qLink.makeVehicleAvailableToNextDriver(veh, now);
		// remove _after_ processing the arrival to keep link active
		removeVehicleFromQueue( now ) ;
	}

	@Override
	public final boolean isActive() {
//		return (this.flowcap_accumulate < 1.0) // still accumulating, thus active 
//		|| (!this.vehQueue.isEmpty()) || (!this.isNotOfferingVehicle()) ;
		return (!this.vehQueue.isEmpty()) || (!this.isNotOfferingVehicle()) ;
		// yyyy ????  Could always return "true"; would be slow but correct.  kai, jul'14
	}

	@Override
	public final boolean isAcceptingFromUpstream() {
		return true ; // we always accept
	}

	@Override
	public final QVehicle getVehicle(final Id vehicleId) {
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

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = new ArrayList<MobsimVehicle>();
		vehicles.addAll(vehQueue);
		vehicles.addAll(buffer);
		return vehicles ;
	}

	@Override
	public final QVehicle popFirstVehicle() {
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh = buffer.poll();
		if (this.generatingEvents) {
			this.context.getEventsManager().processEvent(new LaneLeaveEvent(
					now, veh.getId(), this.qLink.getLink().getId(), this.getId()
			));
		}
		context.getEventsManager().processEvent(new LinkLeaveEvent(
				now, veh.getId(), this.qLink.getLink().getId()
		));
		return veh;
	}

	@Override
	public final boolean isNotOfferingVehicle() {
		return buffer.isEmpty();
	}
	
	@Override
	public final void clearVehicles() {
		double now = context.getSimTimer().getTimeOfDay() ;

		for (QVehicle veh : vehQueue) {
			context.getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		vehQueue.clear();

		for (QVehicle veh : buffer) {
			context.getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		buffer.clear();
	}

	@Override
	public final void addFromUpstream(final QVehicle veh) {
		double now = context.getSimTimer().getTimeOfDay() ;

		
		qLink.activateLink();
		this.vehEnterTimeMap.put( veh.getId(), now ) ;
		
		double linkTravelTime = this.length / linkSpeedCalculator.getMaximumVelocity(veh, this.qLink.getLink(), now);
		// yyyyyy needs to be replaced by density-dependent travel time. kai, jul'14
		System.exit(-1) ;
		
		double earliestExitTime = now + linkTravelTime;

		earliestExitTime +=  veh.getEarliestLinkExitTime() - Math.floor(veh.getEarliestLinkExitTime());
		// (yy this is what makes it pass the tests but I don't see why this is correct. kai, jun'13)
		// (I now think that this is some fractional leftover from an earlier lane. kai, sep'13)
		// (I also think it is never triggered for regular lanes since there the numbers are integerized (see below). kai, sep'13)
		// yyyy this may have changed in the main code; check before continuing here. kai, jul'14

		if ( this.endsAtMetersFromLinkEnd == 0.0 ) {
//			/* It's a QLane that is directly connected to a QNode,
//			 * so we have to floor the freeLinkTravelTime in order the get the same
//			 * results compared to the old mobSim */
			earliestExitTime = Math.floor(earliestExitTime);
			// yyyy I have no idea why this is in here.  Supposedly pulls the link travel times to "second"
			// values, but I don't see why this has to be, and worse, it is wrong when the time step is
			// not one second.  And obviously dangerous if someone tries sub-second time steps.
			// kai, sep'13
		}
		// keeping the above so we have identical speeds in emtpy networks (although I don't like it) 	
		
		veh.setEarliestLinkExitTime(earliestExitTime);
		veh.setCurrentLink(qLink.getLink());
		vehQueue.add(veh);

		if (this.generatingEvents) {
			context.getEventsManager()
			.processEvent(new LaneEnterEvent(now, veh.getId(), this.qLink.getLink().getId(), this.getId()));
		}
	}

	@Override
	public final VisData getVisData() {
		return this.visData  ;
	}

	@Override
	public final QVehicle getFirstVehicle() {
		return this.buffer.peek() ;
	}

	@Override
	public final double getLastMovementTimeOfFirstVehicle() {
		return Double.NEGATIVE_INFINITY ;
	}
	
	/**
	 * Needs to be added _upstream_ of the regular stop location so that a possible second stop on the link can also be served.
	 */
	@Override
	public final void addTransitSlightlyUpstreamOfStop( final QVehicle veh) {
		this.vehQueue.addFirst(veh) ;
	}

	@Override
	public final void changeUnscaledFlowCapacityPerSecond( final double val ) {
		// irrelevant so we ignore it
	}
	
	@Override
	public final void changeEffectiveNumberOfLanes( final double val ) {
		// this variable will not do anything so we will ignore it.
	}
	
	@Override
	public
	Id getId() {
		// need this so we can generate lane events although we do not need them here. kai, sep'13
		// yyyy would probably be better to have this as a final variable set during construction. kai, sep'13
		return this.qLink.getLink().getId() ;
	}

	class VisDataImpl implements QLaneI.VisData {


		private VisLinkWLanes otfLink;

		@Override
		public final Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions, double now ) {
			double numberOfVehiclesDriving = AssignmentEmulatingQLane.this.buffer.size() + AssignmentEmulatingQLane.this.vehQueue.size();
			if (numberOfVehiclesDriving > 0) {
				Link link = AssignmentEmulatingQLane.this.qLink.getLink();
//				double spacing = snapshotInfoBuilder.calculateVehicleSpacing(AssignmentEmulatingQLane.this.length, numberOfVehiclesDriving,
//						AssignmentEmulatingQLane.this.getStorageCapacity(), AssignmentEmulatingQLane.this.bufferStorageCapacity);
				double spacing = AssignmentEmulatingQLane.this.length / numberOfVehiclesDriving ;
				double freespeedTraveltime = AssignmentEmulatingQLane.this.length / link.getFreespeed(now);

				double lastDistanceFromFromNode = Double.NaN;
				for (QVehicle veh : AssignmentEmulatingQLane.this.buffer){
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, context.snapshotInfoBuilder, now,
							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
				}
				for (QVehicle veh : AssignmentEmulatingQLane.this.vehQueue) {
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, context.snapshotInfoBuilder, now,
							lastDistanceFromFromNode, link, spacing, freespeedTraveltime, veh);
				}
			}
			
			return positions ;
		}

		 double createAndAddVehiclePositionAndReturnDistance(final Collection<AgentSnapshotInfo> positions,
				AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder, double now, double lastDistanceFromFromNode, Link link,
				double spacing, double freespeedTraveltime, QVehicle veh)
		{
			double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			lastDistanceFromFromNode = snapshotInfoBuilder.calculateOdometerDistanceFromFromNode(AssignmentEmulatingQLane.this.length, spacing, 
					lastDistanceFromFromNode, now, freespeedTraveltime, remainingTravelTime);
			Integer lane = VisUtils.guessLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
//			double speedValue = snapshotInfoBuilder.calcSpeedValueBetweenZeroAndOne(veh, 
//					AssignmentEmulatingQLane.this.inverseFlowCapacityPerTimeStep, now, link.getFreespeed());
			double speedValue = AssignmentEmulatingQLane.this.freespeedTravelTime 
					/ ( veh.getEarliestLinkExitTime() - AssignmentEmulatingQLane.this.vehEnterTimeMap.get(veh.getId()) ) ;
			if ( speedValue>1. ) { speedValue=1. ; }
			if (this.otfLink != null){
				snapshotInfoBuilder.positionAgentGivenDistanceFromFNode(positions, this.otfLink.getLinkStartCoord(), this.otfLink.getLinkEndCoord(), 
						AssignmentEmulatingQLane.this.length, veh, lastDistanceFromFromNode, 
						lane, speedValue);
			}
			else {
				snapshotInfoBuilder.positionAgentGivenDistanceFromFNode(positions, link.getFromNode().getCoord(), link.getToNode().getCoord(), 
						AssignmentEmulatingQLane.this.length, veh , lastDistanceFromFromNode, lane, speedValue);
			}
			return lastDistanceFromFromNode;
		}

		public void setOtfLink(VisLinkWLanes otfLink) {
			this.otfLink = otfLink;
		}
	}

	@Override
	double getSimulatedFlowCapacityPerTimeStep() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	@Override
	double getStorageCapacity() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}
	@Override
	boolean hasGreenForToLink(Id toLinkId) {
		return true ;
	}
	@Override
	void changeSpeedMetersPerSecond( double val ) {
		throw new RuntimeException("not implemented") ;
	}
	@Override
	double getLoadIndicator() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}