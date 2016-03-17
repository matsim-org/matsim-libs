/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DQTransitionLink.java
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vis.snapshotwriters.VisData;

public class CAQTransitionLink extends QLinkI{
	
	private final QLinkI ql;

	CAQTransitionLink(QLinkI qLinkImpl) {
		this.ql = qLinkImpl;
	}

	@Override
	QLaneI getAcceptingQLane() {
		return this.ql.getAcceptingQLane() ;
	}
	
	@Override
	public Link getLink() {
		return this.ql.getLink();
	}

	@Override
	public void recalcTimeVariantAttributes() {
		this.ql.recalcTimeVariantAttributes();
		
	}

	@Override
	public Collection<MobsimVehicle> getAllVehicles() {
		return this.ql.getAllVehicles();
	}

	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles() {
		return this.ql.getAllNonParkedVehicles();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return this.ql.getCustomAttributes();
	}

	@Override
	public VisData getVisData() {
		return this.ql.getVisData();
	}

	@Override
	boolean doSimStep() {
		return this.ql.doSimStep();
	}

	@Override
	QNode getToNode() {
		return this.ql.getToNode();
	}

	@Override
	void addParkedVehicle(MobsimVehicle vehicle) {
		this.ql.addParkedVehicle(vehicle);		
	}

	@Override
	QVehicle removeParkedVehicle(Id vehicleId) {
		return this.ql.removeParkedVehicle(vehicleId);
	}

	@Override
	QVehicle getParkedVehicle(Id vehicleId) {
		return this.ql.getParkedVehicle(vehicleId);
	}

	@Override
	void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		this.ql.registerAdditionalAgentOnLink(planAgent);
	}

	@Override
	MobsimAgent unregisterAdditionalAgentOnLink(Id mobsimAgentId) {
		return this.ql.unregisterAdditionalAgentOnLink(mobsimAgentId);
	}

	@Override
	Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		return this.ql.getAdditionalAgentsOnLink();
	}

	@Override
	void clearVehicles() {
		this.ql.clearVehicles();
	}

	@Override
	void letVehicleDepart(QVehicle vehicle, double now) {
		this.ql.letVehicleDepart(vehicle, now);
	}

	@Override
	boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id vehicleId,
			double now) {
		return this.ql.insertPassengerIntoVehicle(passenger, vehicleId, now);
	}

	@Override
	QVehicle getVehicle(Id vehicleId) {
		return this.ql.getVehicle(vehicleId);
	}

	@Override
	void registerDriverAgentWaitingForCar(MobsimDriverAgent agent) {
		this.ql.registerDriverAgentWaitingForCar(agent);
	}

	@Override
	void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		this.ql.registerDriverAgentWaitingForPassengers(agent);
	}

	@Override
	MobsimAgent unregisterDriverAgentWaitingForPassengers(Id agentId) {
		return this.ql.unregisterDriverAgentWaitingForPassengers(agentId);
	}

	@Override
	void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id vehicleId) {
		this.ql.registerPassengerAgentWaitingForCar(agent, vehicleId);
	}

	@Override
	MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent,
			Id vehicleId) {
		return this.ql.unregisterPassengerAgentWaitingForCar(agent, vehicleId);
	}

	@Override
	Set<MobsimAgent> getAgentsWaitingForCar(Id vehicleId) {
		return this.ql.getAgentsWaitingForCar(vehicleId);
	}

	@Override
	boolean isNotOfferingVehicle() {
		return this.ql.isNotOfferingVehicle();
	}

	@Override
	List<QLaneI> getOfferingQLanes() {
		return this.ql.getOfferingQLanes() ;
	}

}
