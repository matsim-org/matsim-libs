/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAAgent.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirection;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Obstacles;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCALine;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCALineAgent;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCALineEnvironment;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCASolver;

/**
 * ORCA Agent as proposed by:
 *  S. Curtis & D. Manocha; "Pedestrian Simulation using Geometric Reasoning in Velocity Space", International Conference on Pedestrian and Evacuation Dynamics 2012; in press
 * @author laemmel
 *
 */
public class ORCAAgent implements Sim2DAgent {
	
	
	private final float r = (float) (MatsimRandom.getRandom().nextDouble()*.1 + .25); //radius
	private final float tau = 2f;
	private final float v0 = 1.34f; //desired velocity

	private PhysicalSim2DSection psec;
	private final float[] pos = {0,0};
	private final float[] v = {0,0};
	private final QVehicle veh;
	private final MobsimDriverAgent driver;
	
	private final Neighbors ncalc = new Neighbors();
	private final Obstacles obst = new Obstacles();
	private final DesiredDirection dd = new DesiredDirection();
	private final ORCASolver solver = new ORCASolver();
//	private VisDebugger debugger;
	private VisDebugger debugger;
	
	public ORCAAgent(QVehicle veh, float spawnX, float spawnY) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.ncalc.setRangeAndMaxNrOfNeighbors(5, 8);
	}

	@Override
	public QVehicle getQVehicle() {
		return this.veh;
	}

	@Override
	public void updateVelocity() {
		
		List<ORCALine> constr = new ArrayList<ORCALine>();
		
		for (Tuple<Float, Sim2DAgent> neighbor : this.ncalc.computeNeighbors(this)) {
			ORCALine ol = new ORCALineAgent(this, neighbor, this.tau);
			constr.add(ol);
		}
		
		for (Segment seg : this.psec.getObstacles()) {
			ORCALineEnvironment ol = new ORCALineEnvironment(this, seg, this.tau);
			constr.add(ol);

		}
		

		final float[] dir = this.dd.computeDesiredDirection(this);
		dir[0] *= this.v0;
		dir[1] *= this.v0;

		if (this.debugger != null) {
			this.solver.addDebugger(this.debugger);
		}
		this.solver.run(constr, dir, this.v0*2);
		
		this.v[0] = dir[0];
		this.v[1] = dir[1];

	}

	@Override
	public void setPSec(PhysicalSim2DSection physicalSim2DSection) {
		this.psec = physicalSim2DSection;

	}

	@Override
	public PhysicalSim2DSection getPSec() {
		return this.psec;
	}
	
	@Override
	public float getRadius() {
		return this.r;
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
		visDebugger.addCircle(this.getPos()[0], this.getPos()[1], 2*this.r, 0, 192, 64, 128,0,true);
		this.debugger = visDebugger;
	}

}
