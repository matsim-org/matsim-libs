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
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
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
 *  van den Berg et al (2009), Reciprocal n-body collision avoidance. In: Inter. Symp. on Robotics Research.
 *  S. Curtis & D. Manocha; "Pedestrian Simulation using Geometric Reasoning in Velocity Space", International Conference on Pedestrian and Evacuation Dynamics 2012; in press
 *  where the "Curtis part" is not yet implemented
 * @author laemmel
 *
 */
public class ORCAAgent implements Sim2DAgent, DelegableSim2DAgent {


	private final double r = MatsimRandom.getRandom().nextDouble()*.1 + .25; //radius
	private final double tau = 1.f;
	private double v0 = 1.f; //desired velocity

	private PhysicalSim2DSection psec;
	private final double[] pos = {0,0};
	private final double[] v = {0,0};
	private final QVehicle veh;
	private final MobsimDriverAgent driver;

	private final Neighbors ncalc;
	private final Obstacles obst = new Obstacles();
	private DesiredDirection dd = new DesiredDirection(this);
	private final ORCASolver solver = new ORCASolver();
	//	private VisDebugger debugger;
	private final VisDebugger debugger = null;
	private final double dT;
	private final double maxDelta;

	public ORCAAgent(QVehicle veh, double spawnX, double spawnY, Sim2DConfig config) {
		this.pos[0] = spawnX;
		this.pos[1] = spawnY;
		this.veh = veh;
		this.driver = veh.getDriver();
		this.ncalc = new Neighbors(this, config);
		this.ncalc.setRangeAndMaxNrOfNeighbors(5, 5);
		this.dT = config.getTimeStepSize();
		this.maxDelta =.25;// * dT;
	}

	@Override
	public QVehicle getQVehicle() {
		return this.veh;
	}

	@Override
	public void updateVelocity() {

		List<ORCALine> constr = new ArrayList<ORCALine>();
		for (Segment seg : this.psec.getObstacles()) {
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




		final double[] dir = this.dd.computeDesiredDirection();
		dir[0] *= this.v0;
		dir[1] *= this.v0;
		double dx = dir[0] - this.v[0];
		double dy = dir[1] - this.v[1];
		double sqrDelta = (dx*dx+dy*dy);
		if (sqrDelta > this.maxDelta*this.maxDelta){
			double delta = Math.sqrt(sqrDelta);
			dx /= delta;
			dx *= this.maxDelta;
			dy /= delta;
			dy *= this.maxDelta;
			dir[0] = this.v[0] + dx;
			dir[1] = this.v[1] + dy;
		}
		
		
		this.solver.run(constr, dir, this.v0, new double []{0.,0.});
		
		

//		if (this.debugger != null && getId().toString().equals("r876") ){
//			this.debugger.addLine(this.pos[0], this.pos[1], this.pos[0]+this.v[0], this.pos[1]+this.v[1], 255, 0, 0, 255, 0);
//			System.out.println("debug!");
//		}
		
		this.v[0] = dir[0];
		this.v[1] = dir[1];

//		if (this.debugger != null &&  getId().toString().equals("r876")){
//			this.debugger.addLine(this.pos[0], this.pos[1], this.pos[0]+this.v[0], this.pos[1]+this.v[1], 0, 255, 0, 255, 0);
//			this.debugger.addAll();
//			System.out.println("debug!");
//		}

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
	public double getRadius() {
		return this.r;
	}

	@Override
	public void move(double dx, double dy) {
		this.pos[0] += dx;
		this.pos[1] += dy;
	}

	@Override
	public double[] getVelocity() {
		return this.v;
	}

	@Override
	public Id getCurrentLinkId() {
		return this.driver.getCurrentLinkId();
	}

	@Override
	public double[] getPos() {
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
		if (getId().toString().contains("g")) {
			visDebugger.addCircle((float)this.getPos()[0], (float)this.getPos()[1], (float)this.r, 0, 192, 64, 128,0,true);
		} else if (getId().toString().contains("r")) {
			visDebugger.addCircle((float)this.getPos()[0], (float)this.getPos()[1], (float)this.r, 192, 0, 64, 128,0,true);
		} else {
			int nr = this.hashCode()%3*255;
			int r,g,b;
			if (nr > 2*255) {
				r= nr-2*255;
				g =0;
				b=64;
			} else if (nr > 255) {
				r=0;
				g=nr-255;
				b=64;
			} else {
				r=64;
				g=0;
				b=nr;
			}
			visDebugger.addCircle((float)this.getPos()[0], (float)this.getPos()[1], (float)this.r, r, g, b, 222,0,true);
		}
		visDebugger.addText((float)this.getPos()[0], (float)this.getPos()[1], this.driver.getId()+"", 50);
//		this.debugger = visDebugger;
	}

	@Override
	public double getXLocation() {
		return this.pos[0];
	}

	@Override
	public double getYLocation() {
		return this.pos[1];
	}

	@Override
	public void setDesiredDirectionCalculator(DesiredDirection dd) {
		this.dd = dd;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Sim2DAgent) {
			return getId().equals(((Sim2DAgent) obj).getId());
		}
		return false;
	}

	@Override
	public void setDesiredSpeed(double v) {
		this.v0 = v;
		
	}
}
