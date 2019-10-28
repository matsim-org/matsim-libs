/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.hybridsim.simulation.ExternalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class QSimExternalTransitionLink extends AbstractQLink {

	private final ExternalEngine e;
	private final EventsManager em;
	private FakeLane fakeLane = new FakeLane();
	private final NetsimEngineContext context ;
	private final QNodeI toQNode ;

	QSimExternalTransitionLink(Link link, ExternalEngine e, NetsimEngineContext context, NetsimInternalInterface netsimEngine,
							   QNodeI toQNode, LinkSpeedCalculator linkSpeedCalculator, VehicleHandler vehicleHandler) {
		super(link, toQNode, context, netsimEngine, linkSpeedCalculator, vehicleHandler);
		this.e = e;
		this.em = e.getEventsManager();
		this.context = context ;
		this.toQNode = toQNode ;
	}

	@Override
	public boolean doSimStep() {
		return false;
	}

//	@Override
//	void addFromUpstream(QVehicle veh) {
//
//		this.e.addFromUpstream(veh);
//		veh.getDriver().chooseNextLinkId();
//		double now = this.e.getMobsim().getSimTimer().getTimeOfDay();
//		this.em.processEvent(new LinkEnterEvent(now, veh.getId(), this.link
//				.getId()));
//
//	}
	// now in QLaneI, see below. kai, mar'16

	@Override
	public boolean isNotOfferingVehicle() {
		return true;
	}

	@Override
	public void recalcTimeVariantAttributes() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public VisData getVisData() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public QNodeI getToNode() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public List<QLaneI> getOfferingQLanes() {
		List<QLaneI> list = new ArrayList<>() ;
		list.add( fakeLane ) ;
		return list ;

		// Gregor, the popFirstVehicle/getFirstVehicle etc. is now delegated down to QLane.  This probably makes it slightly more 
		// complicated from the perspective here, but makes the QNetsimEngine simpler (no exception for the "multiple lanes" any more).
		// Please ask if you need this and have problems. kai, feb'16
	}
	
	@Override
	public QLaneI getAcceptingQLane() {
		return this.fakeLane ;
	}
	
	private final class FakeLane implements QLaneI {
		@Override
		public void addFromUpstream(QVehicle veh) {
			double now = context.getSimTimer().getTimeOfDay() ;
			
			Id<Link> nextL = veh.getDriver().chooseNextLinkId();
			Id<Node> leaveId = toQNode.getNode().getId() ;
//			e.addFromUpstream( getLink().getFromNode().getId(), leaveId, veh);
			e.addFromUpstream( veh);
			em.processEvent(new LinkEnterEvent(now, veh.getId(), getLink().getId()));
		}
		
		@Override
		public double getLoadIndicator() {
			return 0. ;
		}
//		@Override
//		public void changeSpeedMetersPerSecond( double spd ) {
//			throw new RuntimeException("not implemented") ;
//		}

		@Override
		public void addFromWait(QVehicle arg0) {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public void addTransitSlightlyUpstreamOfStop(QVehicle arg0) {
			throw new RuntimeException("not implemented") ;
		}
		
		@Override
		public void changeUnscaledFlowCapacityPerSecond(double val) {
			throw new RuntimeException("not implemented");
		}
		
		@Override
		public void changeEffectiveNumberOfLanes(double val) {
			throw new RuntimeException("not implemented");
		}

//		@Override
//		public void changeEffectiveNumberOfLanes(double arg0) {
//			throw new RuntimeException("not implemented") ;
//		}
//
//		@Override
//		public void changeUnscaledFlowCapacityPerSecond(double arg0) {
//			throw new RuntimeException("not implemented") ;
//		}

		@Override
		public void clearVehicles() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public boolean doSimStep() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Collection<MobsimVehicle> getAllVehicles() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public QVehicle getFirstVehicle() {
			// something like
//			QSimExternalTransitionLink.this.e.getFirstFehicle() ;
			
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public double getLastMovementTimeOfFirstVehicle() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public double getSimulatedFlowCapacityPerTimeStep() {
			throw new RuntimeException("not implemented") ;
		}
		
		@Override
		public void recalcTimeVariantAttributes() {
			throw new RuntimeException("not implemented");
		}
		
		@Override
		public double getStorageCapacity() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public QVehicle getVehicle(Id<Vehicle> arg0) {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public VisData getVisData() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public boolean isAcceptingFromUpstream() {
			return e.hasSpace(getLink().getFromNode().getId());
		}

		@Override
		public boolean isAcceptingFromWait(QVehicle veh) {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public boolean isActive() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public boolean isNotOfferingVehicle() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public QVehicle popFirstVehicle() {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Id<Lane> getId() {
			throw new RuntimeException("not implemented") ;
		}
		
		@Override
		public void initBeforeSimStep() {
		}
	}


	
}
