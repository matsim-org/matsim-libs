/* *********************************************************************** *
 * project: org.matsim.*
 * CALink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class CALink extends QLinkI {

	private final Link l;
	private final int dir;

	CALink(Link l, int dir) {
		this.l = l;
		this.dir = dir;
	}
	@Override
	QLaneI getAcceptingQLane() {
		throw new RuntimeException("not implemented") ;
	}
	
	@Override
	public Link getLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void recalcTimeVariantAttributes() {
		throw new RuntimeException("Not yet implemented!");

	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	public VisData getVisData() {
		throw new RuntimeException("Not yet implemented!");
	}

	@Override
	QNode getToNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void addParkedVehicle(MobsimVehicle vehicle) {
		// TODO Auto-generated method stub

	}

	@Override
	QVehicle removeParkedVehicle(Id vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	QVehicle getParkedVehicle(Id vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		// TODO Auto-generated method stub

	}

	@Override
	MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void letVehicleDepart(QVehicle vehicle, double now) {
		// TODO Auto-generated method stub

	}

	@Override
	boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id vehicleId,
			double now) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) {
		// TODO Auto-generated method stub

	}

	@Override
	void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		// TODO Auto-generated method stub

	}

	@Override
	MobsimAgent unregisterDriverAgentWaitingForPassengers(Id agentId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) {
		// TODO Auto-generated method stub

	}

	@Override
	MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent,
			Id vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Set<MobsimAgent> getAgentsWaitingForCar(Id vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	boolean doSimStep() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	void clearVehicles() {
		// TODO Auto-generated method stub

	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	boolean isNotOfferingVehicle() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	List<QLaneI> getOfferingQLanes() {
		return null ;
	}

}
