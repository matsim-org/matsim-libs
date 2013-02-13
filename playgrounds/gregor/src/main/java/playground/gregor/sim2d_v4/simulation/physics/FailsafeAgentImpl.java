/* *********************************************************************** *
 * project: org.matsim.*
 * FailsafeAgentImpl.java
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

package playground.gregor.sim2d_v4.simulation.physics;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirection;

public class FailsafeAgentImpl implements Sim2DAgent {

	private final Stack<Id> linkIds = new Stack<Id>();
	
	private final Sim2DAgent delegate;
	public FailsafeAgentImpl(DelegableSim2DAgent agent) {
		agent.setDesiredDirectionCalculator(new DesiredDirection(this));
		this.delegate = agent;
	}

	
	@Override
	public float getXLocation() {
		return this.delegate.getXLocation();
	}

	@Override
	public float getYLocation() {
		return this.delegate.getYLocation();
	}

	@Override
	public QVehicle getQVehicle() {
		return this.delegate.getQVehicle();
	}

	@Override
	public void updateVelocity() {
		this.delegate.updateVelocity();
	}

	@Override
	public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
		this.delegate.setPSec(physicalSim2DSection);
	}

	@Override
	public PhysicalSim2DSection getPSec() {
		return this.delegate.getPSec();
	}

	@Override
	public void move(float dx, float dy) {
		this.delegate.move(dx, dy);
	}

	@Override
	public float[] getVelocity() {
		return this.delegate.getVelocity();
	}


	@Override
	public float[] getPos() {
		return this.delegate.getPos();
	}

	@Override
	public Id getId() {
		return this.delegate.getId();
	}


	@Override
	public void debug(VisDebugger visDebugger) {
		this.delegate.debug(visDebugger);
	}

	@Override
	public float getRadius() {
		return this.delegate.getRadius();
	}

	@Override
	public Id getCurrentLinkId() {
		if (this.linkIds.empty()) {
			return this.delegate.getCurrentLinkId();
		} 
		return this.linkIds.peek();
	}

	public void pushCurrentLinkId(Id id) {
		this.linkIds.push(id);
	}
	
	@Override
	public Id chooseNextLinkId() {
		if (!this.linkIds.empty()){
			this.linkIds.pop();
			if (this.linkIds.empty()) {
				return this.delegate.getCurrentLinkId();
			}
		}
		if (this.linkIds.empty()) {
			return this.delegate.chooseNextLinkId();
		}
		return this.linkIds.peek();
	}
	
	@Override
	public void notifyMoveOverNode(Id nextLinkId) {
		if (nextLinkId != this.delegate.getCurrentLinkId() && this.linkIds.empty()) {
			this.delegate.notifyMoveOverNode(nextLinkId);
		}
		
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sim2DAgent){
			return getId().equals(((Sim2DAgent) obj).getId());
		}
		return false;
	}
}
