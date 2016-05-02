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
import org.matsim.lanes.data.v20.Lane;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class QSimExternalTransitionLink extends AbstractQLink {

	private final ExternalEngine e;
	private final EventsManager em;
	private FakeLane fakeLane = new FakeLane();
	private final NetsimEngineContext context ;
	private final QNode toQNode ;

	QSimExternalTransitionLink(Link link, ExternalEngine e, NetsimEngineContext context, NetsimInternalInterface netsimEngine, QNode toQNode) {
		super(link, toQNode, context, netsimEngine);
		this.e = e;
		this.em = e.getEventsManager();
		this.context = context ;
		this.toQNode = toQNode ;
	}

	@Override
	boolean doSimStep() {
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
	boolean isNotOfferingVehicle() {
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
	QNode getToNode() {
		throw new RuntimeException("not yet implemented");
	}

	@Override
	List<QLaneI> getOfferingQLanes() {
		List<QLaneI> list = new ArrayList<>() ;
		list.add( fakeLane ) ;
		return list ;

		// Gregor, the popFirstVehicle/getFirstVehicle etc. is now delegated down to QLane.  This probably makes it slightly more 
		// complicated from the perspective here, but makes the QNetsimEngine simpler (no exception for the "multiple lanes" any more).
		// Please ask if you need this and have problems. kai, feb'16
	}
	
	@Override
	QLaneI getAcceptingQLane() {
		return this.fakeLane ;
	}
	
	private final class FakeLane extends QLaneI {
		@Override
		void addFromUpstream(QVehicle veh) {
			double now = context.getSimTimer().getTimeOfDay() ;
			
			Id<Link> nextL = veh.getDriver().chooseNextLinkId();
			Id<Node> leaveId = toQNode.getNode().getId() ;
//			e.addFromUpstream( getLink().getFromNode().getId(), leaveId, veh);
			e.addFromUpstream( veh);
			em.processEvent(new LinkEnterEvent(now, veh.getId(), getLink().getId()));
		}
		
		@Override
		double getLoadIndicator() {
			return 0. ;
		}
		@Override
		void changeSpeedMetersPerSecond( double spd ) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void addFromWait(QVehicle arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void addTransitSlightlyUpstreamOfStop(QVehicle arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void changeEffectiveNumberOfLanes(double arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void changeUnscaledFlowCapacityPerSecond(double arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void clearVehicles() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean doSimStep() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		Collection<MobsimVehicle> getAllVehicles() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		QVehicle getFirstVehicle() {
			// something like
//			QSimExternalTransitionLink.this.e.getFirstFehicle() ;
			
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		double getLastMovementTimeOfFirstVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
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
		QVehicle getVehicle(Id<Vehicle> arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		VisData getVisData() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean hasGreenForToLink(Id<Link> arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean isAcceptingFromUpstream() {
			return e.hasSpace(getLink().getFromNode().getId());
		}

		@Override
		boolean isAcceptingFromWait() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean isActive() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean isNotOfferingVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		QVehicle popFirstVehicle() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Id<Lane> getId() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

	}


	
}
