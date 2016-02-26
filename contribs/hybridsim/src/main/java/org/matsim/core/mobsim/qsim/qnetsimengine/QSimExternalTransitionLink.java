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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.hybridsim.simulation.ExternalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QSimExternalTransitionLink extends AbstractQLink {

	private final ExternalEngine e;
	private final EventsManager em;
	private final Network net;
	private FakeLane fakeLane;

	QSimExternalTransitionLink(Link link, QNetwork network, ExternalEngine e) {
		super(link, network);
		this.e = e;
		this.em = e.getEventsManager();
		this.net = network.getNetwork();
	}

	@Override
	boolean doSimStep(double now) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	void addFromUpstream(QVehicle veh) {
		Id<Link> nextL = veh.getDriver().chooseNextLinkId();
		Id<Node> leaveId = this.net.getLinks().get(nextL).getToNode().getId();
		this.e.addFromUpstream(this.link.getFromNode().getId(), leaveId, veh);
		double now = this.e.getMobsim().getSimTimer().getTimeOfDay();
		this.em.processEvent(new LinkEnterEvent(now, veh.getId(), this.link
				.getId()));

	}

	@Override
	boolean isNotOfferingVehicle() {
		return true;
	}

	@Override
	boolean isAcceptingFromUpstream() {
		return this.e.hasSpace(this.link.getFromNode().getId());
	}

	@Override
	public Link getLink() {
		return super.link;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
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
	List<QLaneI> getToNodeQueueLanes() {
		List<QLaneI> list = new ArrayList<>() ;
		list.add( fakeLane ) ;
		return list ;

		// Gregor, the popFirstVehicle/getFirstVehicle etc. is now delegated down to QLane.  This probably makes it slightly more 
		// complicated from the perspective here, but makes the QNetsimEngine simpler (no exception for the "multiple lanes" any more).
		// Please ask if you need this and have problems. kai, feb'16
	}
	
	private final class FakeLane extends QLaneI {
		@Override
		void addFromUpstream(QVehicle arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void addFromWait(QVehicle arg0, double arg1) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void addTransitSlightlyUpstreamOfStop(QVehicle arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void changeEffectiveNumberOfLanes(double arg0, double arg1) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void changeUnscaledFlowCapacityPerSecond(double arg0, double arg1) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void clearVehicles() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		boolean doSimStep(double arg0) {
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
		double getSimulatedFlowCapacity() {
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
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
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
		void recalcTimeVariantAttributes(double arg0) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		void updateRemainingFlowCapacity() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}
	}


	
}
