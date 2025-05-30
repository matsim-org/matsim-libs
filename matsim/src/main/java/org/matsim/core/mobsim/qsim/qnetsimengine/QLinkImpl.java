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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingSearchTimeCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.lanes.VisLaneModelBuilder;
import org.matsim.lanes.VisLinkWLanes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.VisData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 *
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 */
public final class QLinkImpl extends AbstractQLink implements SignalizeableItem {
	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(QLinkImpl.class);

	public static final class Builder {
		private NetsimInternalInterface netsimEngine;
		private final NetsimEngineContext context;
		private LaneFactory laneFactory;
		private LinkSpeedCalculator linkSpeedCalculator;
		private VehicleHandler vehicleHandler;
		private ParkingSearchTimeCalculator parkingSearchTimeCalculator;

		Builder(NetsimEngineContext context, NetsimInternalInterface netsimEngine2) {
			this.context = context;
			this.netsimEngine = netsimEngine2;
		}

		final void setLaneFactory(LaneFactory laneFactory) {
			this.laneFactory = laneFactory;
		}

		public void setLinkSpeedCalculator(LinkSpeedCalculator linkSpeedCalculator) {
			this.linkSpeedCalculator = linkSpeedCalculator;
		}

		public void setVehicleHandler(VehicleHandler vehicleHandler) {
			this.vehicleHandler = vehicleHandler;
		}

		public void setParkingSearchTimeCalculator(ParkingSearchTimeCalculator parkingSearchTimeCalculator) {
			this.parkingSearchTimeCalculator = parkingSearchTimeCalculator;
		}

		QLinkImpl build(Link link, QNodeI toNode) {
			if (laneFactory == null) {
				laneFactory = new QueueWithBuffer.Builder(context);
			}
			Objects.requireNonNull(linkSpeedCalculator);
			Objects.requireNonNull(vehicleHandler);
			return new QLinkImpl(link, toNode, laneFactory, context, netsimEngine, linkSpeedCalculator, vehicleHandler, parkingSearchTimeCalculator);
		}
	}

	public interface LaneFactory {

		/**
		 * If this QLinkImpl is passed an instance of this factory upon construction,
		 * it will call back this factory within the constructor (!) to obtain a road and pass
		 * itself to the creation method.
		 */
		public QLaneI createLane(AbstractQLink qLinkImpl);

	}

	private final VisData visdata;

	private final QLaneI qlane;

	private NetsimEngineContext context;

	private QLinkImpl(final Link link2, final QNodeI toNode, final LaneFactory roadFactory, NetsimEngineContext context,
					  NetsimInternalInterface netsimEngine, LinkSpeedCalculator linkSpeedCalculator, VehicleHandler vehicleHandler, ParkingSearchTimeCalculator parkingSearchTimeCalculator) {
		super(link2, toNode, context, netsimEngine, linkSpeedCalculator, vehicleHandler, parkingSearchTimeCalculator);
		this.context = context;
		// The next line must must by contract stay within the constructor,
		// so that the caller can use references to the created roads to wire them together,
		// if it must.
		this.qlane = roadFactory.createLane(this);
		this.visdata = this.new VisDataImpl(); // instantiating this here and not earlier so we can cache some things
		super.setTransitQLink(new TransitQLink(this.qlane));
	}

	@Override
	public void clearVehicles() {
		super.clearVehicles();
		qlane.clearVehicles();
	}

	@Override
	public boolean doSimStep() {
		double now = context.getSimTimer().getTimeOfDay();
		qlane.initBeforeSimStep();

		if (context.qsimConfig.isInsertingWaitingVehiclesBeforeDrivingVehicles()) {
			this.moveWaitToRoad();
			this.getTransitQLink().handleTransitVehiclesInStopQueue(now);
			qlane.doSimStep();
		} else {
			this.getTransitQLink().handleTransitVehiclesInStopQueue(now);
			qlane.doSimStep();
			this.moveWaitToRoad();
		}
		this.endParkingSearch();

		this.setActive(this.checkForActivity());
		return isActive();
		// yy seems to me that for symmetry there should be something like
		// 			netElementActivationRegistry.registerLinkAsActive(this);
		// and may be a qlink.deactivateLink(...) around it (analogous to qlink.activateLink).
		// That is, do NOT pass the deactivation of the link rather implicitly via returning a false here.
		// kai, mar'16
	}


	/**
	 * Move as many waiting cars to the link as it is possible
	 */
	private void moveWaitToRoad() {
		while (!getWaitingList().isEmpty()) {
			if (!qlane.isAcceptingFromWait(this.getWaitingList().peek())) {
				return;
			}
			QVehicle veh = this.getWaitingList().poll();

			double now = context.getSimTimer().getTimeOfDay();
			context.getEventsManager().processEvent(
				new VehicleEntersTrafficEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId(), veh.getDriver().getMode(), 1.0));

			if (this.getTransitQLink().addTransitToStopQueue(now, veh, this.getLink().getId())) {
				continue;
			}

			if (veh.getDriver().isWantingToArriveOnCurrentLink()) {
				// If the driver wants to stop (again) on this link, give them a special treatment.
				// addFromWait doesn't work here, because after that, they cannot stop anymore.
				qlane.addTransitSlightlyUpstreamOfStop(veh);
				continue;
			}

			qlane.addFromWait(veh);
		}
	}

	private void endParkingSearch() {
		// if the vehicle is allowed to leave the parking search, remove it from the queue
		while (!getParkingSearchQueue().isEmpty() && getParkingSearchQueue().peek().getEarliestLinkExitTime() <= context.getSimTimer().getTimeOfDay()) {
			QVehicle veh = this.getParkingSearchQueue().poll();
			arriveAndPark(veh, true);
		}
	}

	@Override
	public boolean isNotOfferingVehicle() {
		return qlane.isNotOfferingVehicle();
	}

	@Override
	public void recalcTimeVariantAttributes() {
		double now = context.getSimTimer().getTimeOfDay();
		qlane.changeUnscaledFlowCapacityPerSecond(this.getLink().getFlowCapacityPerSec(now));
		qlane.changeEffectiveNumberOfLanes(this.getLink().getNumberOfLanes(now));

		//		qlane.changeSpeedMetersPerSecond( getLink().getFreespeed(now) ) ; flowCap & nLanes are "push", freeSpeed is
		//"pull".  This is not completely honest w.r.t. freeSpeed consequences to the fdiag, but also wasn't correctly
		//thought through/implemented when I found it.  kai, feb'18

		qlane.recalcTimeVariantAttributes();
	}

	@Override
	public QVehicle getVehicle(Id<Vehicle> vehicleId) {
		QVehicle ret = super.getVehicle(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.getWaitingList()) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return this.qlane.getVehicle(vehicleId);
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		Collection<MobsimVehicle> vehicles = new ArrayList<>();
		vehicles.addAll(this.getTransitQLink().getTransitVehicleStopQueue());
		vehicles.addAll(this.getWaitingList());
		vehicles.addAll(qlane.getAllVehicles());
		return vehicles;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		return this.qlane.getStorageCapacity();
	}

	/**
	 * This method returns the normalized capacity of the link, i.e. the capacity
	 * of vehicles per second. It is considering the capacity reduction factors
	 * set in the config and the simulation's tick time.
	 *
	 * @return the flow capacity of this link per second, scaled by the config
	 * values and in relation to the SimulationTimer's simticktime.
	 */
	double getSimulatedFlowCapacityPerTimeStep() {
		return this.qlane.getSimulatedFlowCapacityPerTimeStep();
	}

	@Override
	public VisData getVisData() {
		return this.visdata;
	}

	private boolean checkForActivity() {
		/*
		 * Leave Link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		return qlane.isActive() || !this.getWaitingList().isEmpty() || !this.getTransitQLink().getTransitVehicleStopQueue().isEmpty() || !this.getParkingSearchQueue().isEmpty();
	}

	@Override
	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		((SignalizeableItem) qlane).setSignalStateAllTurningMoves(state);
	}

	@Override
	public void setSignalStateForTurningMove(SignalGroupState state, Id<Link> toLinkId) {
		((SignalizeableItem) qlane).setSignalStateForTurningMove(state, toLinkId);
	}

	@Override
	public void setSignalized(boolean isSignalized) {
		((SignalizeableItem) qlane).setSignalized(isSignalized);
	}

	@Override
	public boolean hasGreenForToLink(Id<Link> toLinkId) {
		return ((SignalizeableItem) qlane).hasGreenForToLink(toLinkId);
	}

	@Override
	public boolean hasGreenForAllToLinks() {
		return ((SignalizeableItem) qlane).hasGreenForAllToLinks();
	}

	/**
	 * Inner class to encapsulate visualization methods
	 *
	 * @author dgrether
	 */
	class VisDataImpl implements VisData {

		private VisLaneModelBuilder visModelBuilder = null;
		private VisLinkWLanes visLink = null;

		private VisDataImpl() {
			double nodeOffset = context.qsimConfig.getNodeOffset();
			if (nodeOffset != 0.0) {
				nodeOffset = nodeOffset + 2.0; // +2.0: eventually we need a bit space for the signal
				visModelBuilder = new VisLaneModelBuilder();
				CoordinateTransformation transformation = new IdentityTransformation();
				visLink = visModelBuilder.createVisLinkLanes(transformation, QLinkImpl.this, nodeOffset, null);
				visModelBuilder.recalculatePositions(visLink, context.linkWidthCalculator);
			}
		}

		@Override
		public Collection<AgentSnapshotInfo> addAgentSnapshotInfo(Collection<AgentSnapshotInfo> positions) {
//			AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = qnetwork.simEngine.getAgentSnapshotInfoBuilder();

			QLaneI.VisData roadVisData = getAcceptingQLane().getVisData();
			if (visLink != null) {
				((QueueWithBuffer.VisDataImpl) roadVisData).setVisInfo(visLink.getLinkStartCoord(), visLink.getLinkEndCoord());
				// yyyy not so great but an elegant solution needs more thinking about visualizer structure. kai, jun'13
			}

			double now = context.getSimTimer().getTimeOfDay();
			positions = roadVisData.addAgentSnapshotInfo(positions, now);

			int cnt2 = 10; // a counter according to which non-moving items can be "spread out" in the visualization
			// initialize a bit away from the lane

			// treat vehicles from transit stops
			cnt2 = context.snapshotInfoBuilder.positionVehiclesFromTransitStop(positions, getLink(), getTransitQLink().getTransitVehicleStopQueue(), cnt2);

			// treat vehicles from waiting list:
			cnt2 = context.snapshotInfoBuilder.positionVehiclesFromWaitingList(positions, QLinkImpl.this.getLink(), cnt2,
				QLinkImpl.this.getWaitingList());

			cnt2 = context.snapshotInfoBuilder.positionAgentsInActivities(positions, QLinkImpl.this.getLink(),
				QLinkImpl.this.getAdditionalAgentsOnLink(), cnt2);

			return positions;
		}

	}

	@Override
	public List<QLaneI> getOfferingQLanes() {
		List<QLaneI> list = new ArrayList<>();
		list.add(this.qlane);
		return list;
	}

	@Override
	public QLaneI getAcceptingQLane() {
		return qlane;
	}

}
