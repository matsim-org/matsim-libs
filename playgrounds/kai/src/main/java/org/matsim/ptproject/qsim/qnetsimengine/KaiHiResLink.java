/* *********************************************************************** *
 * project: kai
 * KaiHiResLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import java.util.Collection;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class KaiHiResLink extends QLinkInternalI {

	private final KaiHybridEngine hybridEngine;
	private final QNode toQueueNode;
	private final QNetwork qNetwork;
	private final Link link;

	KaiHiResLink(Link link, QNetwork network, QNode toQueueNode, KaiHybridEngine engine) {
		this.link = link;
		this.qNetwork = network;
		this.toQueueNode = toQueueNode;
		this.hybridEngine = engine;
	}

	@Override
	void addFromIntersection(QVehicle veh) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	void addParkedVehicle(MobsimVehicle vehicle) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	void clearVehicles() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	boolean doSimStep(double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	QNode getToNode() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	void letAgentDepartWithVehicle(MobsimDriverAgent agent, QVehicle vehicle, double now) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	void registerAgentWaitingForCar(MobsimDriverAgent agent) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	QVehicle removeParkedVehicle(Id vehicleId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	boolean isNotOfferingVehicle() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	double getLastMovementTimeOfFirstVehicle() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	QVehicle getFirstVehicle() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	boolean hasGreenForToLink(Id toLinkId) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	boolean hasSpace() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	QVehicle popFirstVehicle() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Link getLink() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void recalcTimeVariantAttributes(double time) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

	@Override
	public VisData getVisData() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException() ;
	}

}
