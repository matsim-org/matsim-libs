/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleAgent.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.LinkInfo;

public class SimpleAgent implements Sim2DAgent {
	
	private final float v0 = 1.f;
	
	private final float [] pos = {0,0};
	
	private final float [] v = {0,0};
	
	private final QVehicle veh;
	private final MobsimDriverAgent driver;
	private PhysicalSim2DSection currentPSec;

	private final float r = .5f;

	public SimpleAgent(QVehicle veh, float spawnX, float spawnY) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
	}

	@Override
	public QVehicle getQVehicle() {
		return this.veh;
	}

//	@Override
//	public void calcNeighbors(PhysicalSim2DSection physicalSim2DSection) {
//		//nothing to be done here
//	}
//
//	@Override
//	public void setObstacles(Segment[] obstacles) {
//		//nothing to be done here
//	}

	@Override
	public void updateVelocity() {
		Id id = this.driver.getCurrentLinkId();
		LinkInfo li = this.currentPSec.getLinkInfo(id);
		this.v[0] = li.dx * this.v0;
		this.v[1] = li.dy * this.v0;
	}

	@Override
	public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
		this.currentPSec = physicalSim2DSection;
		
	}

	@Override
	public void move(float dx, float dy) {
		this.pos[0] += dx;
		this.pos[1] += dy;
	}

	@Override
	public float[] getVelocity() {
		return this.v;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.driver.getCurrentLinkId();
	}

	@Override
	public float[] getPos() {
		return this.pos;
	}

	@Override
	public Id chooseNextLinkId() {
		Id id = this.driver.chooseNextLinkId();
		return id;
	}

	@Override
	public Id getId() {
		return this.driver.getId();
	}

	@Override
	public void notifyMoveOverNode(Id nextLinkId) {
		this.driver.notifyMoveOverNode(nextLinkId);
	}

	@Override
	public void debug(VisDebugger visDebugger) {
		visDebugger.addCircle(this.getPos()[0], this.getPos()[1], .5f, 192, 0, 64, 128);
		
	}

	@Override
	public PhysicalSim2DSection getPSec() {
		return this.currentPSec;
	}

}
