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

import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirectionCalculator;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Obstacles;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCALine;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCALineAgent;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCALineEnvironment;
import playground.gregor.sim2d_v4.simulation.physics.orca.ORCASolver;

/**
 * ORCA Agent as proposed by:
 *  van den Berg et al (2009), Reciprocal n-body collision avoidance. In: Inter. Symp. on Robotics Research.
 *  S. Curtis & D. Manocha; "Pedestrian Simulation using Geometric Reasoning in Velocity Space", International Conference on Pedestrian and Evacuation Dynamics 2012; in press
 *  where the "Curtis part" is not yet implemented
 * @author laemmel
 *
 */
public class ORCAVelocityUpdater implements VelocityUpdater {


	private final double tau = 1.f;

	private PhysicalSim2DSection psec;

	private final Neighbors ncalc;
	private final Obstacles obst = new Obstacles();
	private final ORCASolver solver = new ORCASolver();
	//	private VisDebugger debugger;
	private final VisDebugger debugger = null;
	private final double dT;
	private final double maxDelta;
	private final DesiredDirectionCalculator dd;
	private final Sim2DAgent agent;

	public ORCAVelocityUpdater(DesiredDirectionCalculator dd, Neighbors ncalc, Sim2DConfig conf, Sim2DAgent agent) {
		this.ncalc = ncalc;
		this.dT = conf.getTimeStepSize();
		this.maxDelta =.25;// * dT;
		this.dd = dd;
		this.agent = agent;
	}


	@Override
	public void updateVelocity() {

		List<ORCALine> constr = new ArrayList<ORCALine>();
		for (Segment seg : this.agent.getPSec().getObstacles()) {
			ORCALineEnvironment ol = new ORCALineEnvironment(this, seg, this.tau);
			constr.add(ol);

		}

//		if (!(this.psec instanceof DepartureBox)) {
//			LinkInfo li = this.psec.getLinkInfo(getCurrentLinkId());
//			for (Segment seg : this.psec.getOpenings()) {
//				if (!seg.equals(li.finishLine)){
//					ORCALineEnvironment ol = new ORCALineEnvironment(this, seg, this.tau);
//					constr.add(ol);
//				}
//			}
//		}
		for (Tuple<Double, Sim2DAgent> neighbor : this.ncalc.getNeighbors()) {
//			if (this.debugger != null && ( getId().toString().equals("r876"))){//&& neighbor.getSecond().getId().toString().equals("r5")) {
//				ORCALine ol = new ORCALineAgent(this, neighbor, this.tau,this.debugger);
//				constr.add(ol);				
//			} else {
			ORCALine ol = new ORCALineAgent(this, neighbor, this.tau);
			constr.add(ol);
//			}
//			if (this.getId().toString().equals("r996") && this.debugger != null){
//				((ORCALineAgent)ol).debugSetOffset(this.pos[0], this.pos[1]);
//				ol.debug(this.debugger, 0, 0, 0);
//			}
		}

		//		Collections.reverse(constr);

		double[] v = this.agent.getVelocity();


		final double[] dir = this.dd.computeDesiredDirection();
		
		double v0 = this.agent.getV0();
		dir[0] *= v0;
		dir[1] *= v0;
		double dx = dir[0] - v[0];
		double dy = dir[1] - v[1];
		double sqrDelta = (dx*dx+dy*dy);
		if (sqrDelta > this.maxDelta*this.maxDelta){
			double delta = Math.sqrt(sqrDelta);
			dx /= delta;
			dx *= this.maxDelta;
			dy /= delta;
			dy *= this.maxDelta;
			dir[0] = v[0] + dx;
			dir[1] = v[1] + dy;
		}
		
		
		this.solver.run(constr, dir, v0, new double []{0.,0.});
		
		

//		if (this.debugger != null && getId().toString().equals("r876") ){
//			this.debugger.addLine(this.pos[0], this.pos[1], this.pos[0]+this.v[0], this.pos[1]+this.v[1], 255, 0, 0, 255, 0);
//			System.out.println("debug!");
//		}
		
		v[0] = dir[0];
		v[1] = dir[1];

//		if (this.debugger != null &&  getId().toString().equals("r876")){
//			this.debugger.addLine(this.pos[0], this.pos[1], this.pos[0]+this.v[0], this.pos[1]+this.v[1], 0, 255, 0, 255, 0);
//			this.debugger.addAll();
//			System.out.println("debug!");
//		}

	}


	public double[] getPos() {
		return this.agent.getPos();
	}


	public double getRadius() {
		return this.agent.getRadius();
	}


	public double[] getVelocity() {
		return this.agent.getVelocity();
	}

}
