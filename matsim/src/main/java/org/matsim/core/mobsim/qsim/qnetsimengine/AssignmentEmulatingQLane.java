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
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.AbstractQLink.HandleTransitStopResult;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.lanes.data.v20.Lane;
import org.matsim.lanes.vis.VisLinkWLanes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

/**
 * 
 * @author nagel
 */
class AssignmentEmulatingQLane extends QLaneI {
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(AssignmentEmulatingQLane.class ) ;

	private double freespeedTravelTime = Double.NaN;
	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private VehicleQ<QVehicle> vehQueue;

	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private Queue<QVehicle> buffer = new LinkedList<>() ;

	private final AbstractQLink qLink;

	private final Id<Lane> id;

	private VisData visData = new VisDataImpl() ;

	private double endsAtMetersFromLinkEnd = 0. ;

	// get properties no longer from qlink, but have them by yourself:
	// NOTE: we need to have qlink since we need access e.g. for vehicle arrival or for public transit
	// On the other hand, the qlink properties (e.g. number of lanes) may not be the ones we need here because they
	// may be divided between parallel lanes.  So we need both.
	private final double length ;

	private Map<Id<Vehicle>,Double> vehEnterTimeMap = new HashMap<>() ;

	private final NetsimEngineContext context;

	private final LinkSpeedCalculator linkSpeedCalculator;



	AssignmentEmulatingQLane(AbstractQLink qLinkImpl,  final VehicleQ<QVehicle> vehicleQueue, Id<Lane> id, 
			NetsimEngineContext context, LinkSpeedCalculator linkSpeedCalculator ) {
		this.id = id ;
		this.qLink = qLinkImpl;
		this.vehQueue = vehicleQueue ;
		this.context = context;
		this.linkSpeedCalculator = linkSpeedCalculator;
		this.length = this.qLink.getLink().getLength() ;
	}
	@Override
	public final void addFromWait(final QVehicle veh) {
		double now = context.getSimTimer().getTimeOfDay() ;
		this.vehEnterTimeMap.put( veh.getId(), now ) ;

		addToBuffer(veh);
	}

	private final void addToBuffer(final QVehicle veh) {
		// (called from two places, thus separate method)

		buffer.add(veh); // we always accept
		qLink.getToNode().activateNode();
	}
	@Override
	public final boolean isAcceptingFromWait() {
		return true ; // we always accept
	}

	@Override
	public boolean doSimStep( ) {
		this.moveLaneToBuffer();
		return true ;
	}

	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 */
	private final void moveLaneToBuffer() {
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh;

		while ( (veh = vehQueue.peek()) != null ) {
			if ( veh.getEarliestLinkExitTime() > now ) {
				return;
			}

			MobsimDriverAgent driver = veh.getDriver();

			if ( driver instanceof AbstractTransitDriverAgent ) {
				AbstractTransitDriverAgent transitDriver = (AbstractTransitDriverAgent) driver ;
				HandleTransitStopResult handleTransitStop = qLink.getTransitQLink().handleTransitStop(now, veh, transitDriver, qLink.getLink().getId());
				if (handleTransitStop == HandleTransitStopResult.accepted) {
					// vehicle has been accepted into the transit vehicle queue of the link.
					removeVehicleFromQueue() ;
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
			if ( driver.isWantingToArriveOnCurrentLink() ){
				letVehicleArrive(veh);
				continue;
			} else {
				addToBuffer(veh);
				removeVehicleFromQueue();
			}
		} // end while
	}

	private final QVehicle removeVehicleFromQueue() {
		QVehicle veh = vehQueue.poll();
		return veh ;
	}

	private final void letVehicleArrive(QVehicle veh) {
		qLink.addParkedVehicle(veh);
		qLink.letVehicleArrive(veh);
		qLink.makeVehicleAvailableToNextDriver(veh);
		
		// remove _after_ processing the arrival to keep link active:
		removeVehicleFromQueue( ) ;
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
	public final QVehicle getVehicle(final Id<Vehicle> vehicleId) {
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
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		vehicles.addAll(vehQueue);
		vehicles.addAll(buffer);
		return vehicles ;
	}

	@Override
	public final QVehicle popFirstVehicle() {
		double now = context.getSimTimer().getTimeOfDay() ;

		QVehicle veh = buffer.poll();
		if (this.context.qsimConfig.isUseLanes()) {
			this.context.getEventsManager().processEvent(new LaneLeaveEvent( now, veh.getId(), this.qLink.getLink().getId(), this.getId() ));
		}
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
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		vehQueue.clear();

		for (QVehicle veh : buffer) {
			context.getEventsManager().processEvent( new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			context.getEventsManager().processEvent( new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
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
		
		double density_per_km = 1000. * ( vehQueue.size() + buffer.size() ) / this.qLink.getLink().getLength() / this.qLink.getLink().getNumberOfLanes() ;

		final double maxVel = linkSpeedCalculator.getMaximumVelocity(veh, this.qLink.getLink(), now);
		double freeTravelTime = this.length / maxVel;
		
		final double MAX_FLOW_DENSITY_PER_KM = 15 ;
		final double JAM_DENSITY_PER_KM = 133 ;
		final double JAM_SPEED_KM_H = 5 ;
		
		double earliestExitTime = now + freeTravelTime ;
		
		if ( density_per_km > MAX_FLOW_DENSITY_PER_KM ) {
			double fraction = (density_per_km - MAX_FLOW_DENSITY_PER_KM )/( JAM_DENSITY_PER_KM - MAX_FLOW_DENSITY_PER_KM) ;
			earliestExitTime += fraction * fraction * this.qLink.getLink().getLength() / (JAM_SPEED_KM_H / 3.6) ;  
		}
		
		// yyyyyy the above somehow should be anchored at flow, not density.  ???  kai, jul'16

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

		if (this.context.qsimConfig.isUseLanes()) {
			if (  this.qLink.getAcceptingQLane() != this.qLink.getOfferingQLanes().get(0) ) {
				context.getEventsManager() .processEvent(new LaneEnterEvent(now, veh.getId(), this.qLink.getLink().getId(), this.getId()));
			}
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
	@Override public final void addTransitSlightlyUpstreamOfStop( final QVehicle veh) {
		this.vehQueue.addFirst(veh) ;
	}

	@Override public final void changeUnscaledFlowCapacityPerSecond( final double val ) {
		// irrelevant so we ignore it
	}

	@Override public final void changeEffectiveNumberOfLanes( final double val ) {
		// this variable will not do anything so we will ignore it.
	}

	@Override public Id<Lane> getId() {
		// need this so we can generate lane events although we do not need them here. kai, sep'13
		return this.id ;
	}

	class VisDataImpl implements QLaneI.VisData {


		private VisLinkWLanes otfLink;

		@Override
		public final Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions, double now ) {
			double numberOfVehiclesDriving = buffer.size() + vehQueue.size();
			if (numberOfVehiclesDriving > 0) {
				double spacing = length / numberOfVehiclesDriving ;

				double lastDistanceFromFromNode = Double.NaN;
				for (QVehicle veh : buffer){
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, context.snapshotInfoBuilder, now,
							lastDistanceFromFromNode, qLink.getLink(), spacing, veh);
				}
				for (QVehicle veh : vehQueue) {
					lastDistanceFromFromNode = createAndAddVehiclePositionAndReturnDistance(positions, context.snapshotInfoBuilder, now,
							lastDistanceFromFromNode, qLink.getLink(), spacing, veh);
				}
			}

			return positions ;
		}

		double createAndAddVehiclePositionAndReturnDistance(final Collection<AgentSnapshotInfo> positions,
				AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder, double now, double lastDistanceFromFromNode, Link link,
				double spacing, QVehicle veh)
		{
			double remainingTravelTime = veh.getEarliestLinkExitTime() - now ;
			lastDistanceFromFromNode = snapshotInfoBuilder.calculateOdometerDistanceFromFromNode(length, spacing, 
					lastDistanceFromFromNode, now, freespeedTravelTime, remainingTravelTime);
			Integer lane = VisUtils.guessLane(veh, NetworkUtils.getNumberOfLanesAsInt(Time.UNDEFINED_TIME, link));
			final Double vehEnterTime = vehEnterTimeMap.get(veh.getId());
			Gbl.assertNotNull(vehEnterTime);
			double speedValue = freespeedTravelTime / ( veh.getEarliestLinkExitTime() - vehEnterTime ) ;
			if ( speedValue>1. ) { speedValue=1. ; }
			if (this.otfLink != null){
				snapshotInfoBuilder.positionAgentGivenDistanceFromFNode(positions, this.otfLink.getLinkStartCoord(), this.otfLink.getLinkEndCoord(), 
						length, veh, lastDistanceFromFromNode, lane, speedValue);
			} else {
				snapshotInfoBuilder.positionAgentGivenDistanceFromFNode(positions, link.getFromNode().getCoord(), link.getToNode().getCoord(), 
						length, veh , lastDistanceFromFromNode, lane, speedValue);
			}
			return lastDistanceFromFromNode;
		}

		public void setOtfLink(VisLinkWLanes otfLink) {
			this.otfLink = otfLink;
		}
	}

	@Override
	double getSimulatedFlowCapacityPerTimeStep() {
		throw new RuntimeException("not implemented") ;
	}
	@Override
	double getStorageCapacity() {
		return Double.POSITIVE_INFINITY ;
	}
	@Override boolean hasGreenForToLink(Id<Link> toLinkId) {
		return true ;
	}
	@Override void changeSpeedMetersPerSecond( double val ) {
		throw new RuntimeException("not implemented") ;
	}
	@Override double getLoadIndicator() {
		throw new RuntimeException("not implemented") ;
	}

}