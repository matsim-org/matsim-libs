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

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2d_v4.cgal.CGAL;
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
	private final double dT;
	private final double maxDelta;
	private final DesiredDirectionCalculator dd;
	private final Sim2DAgent agent;
	
	private final double vmaxSqrt = StrictMath.sqrt(2); //what is vmax? [gl April '13]
	
	//Curtis constants
	private final double alpha = 1.57 + MatsimRandom.getRandom().nextGaussian()*.15;
	private final double normHeightDenom = 1.72;
	private final double beta = .9 + MatsimRandom.getRandom().nextGaussian()*.2;

	public ORCAVelocityUpdater(DesiredDirectionCalculator dd, Neighbors ncalc, Sim2DConfig conf, Sim2DAgent agent) {
		this.ncalc = ncalc;
		this.dT = conf.getTimeStepSize();
		this.maxDelta =5 * this.dT;
		this.dd = dd;
		this.agent = agent;
	}


	@Override
	public void updateVelocity(double time) {

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
		List<Tuple<Double, Sim2DAgent>> neighbors = this.ncalc.getNeighbors(time);
		for (Tuple<Double, Sim2DAgent> neighbor : neighbors) {
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
		
		double perceivedSpace = computePerceivedSpace(dir,neighbors)+.19;
		double tmp = (perceivedSpace*this.alpha)/(this.agent.getHeight()/this.normHeightDenom *(1+this.beta));
		double freeSpeed = Math.max(.1, tmp*tmp);
//		
		if (freeSpeed > v0) {
			freeSpeed = v0;
		}
//		
		double vS = freeSpeed;
//		double vS = v0;
//		vS = v0;
		
		dir[0] *= vS;
		dir[1] *= vS;
//		double dx = dir[0] - v[0];
//		double dy = dir[1] - v[1];
//		double sqrDelta = (dx*dx+dy*dy);
//		if (sqrDelta > this.maxDelta*this.maxDelta){
//			double delta = Math.sqrt(sqrDelta);
//			dx /= delta;
//			dx *= this.maxDelta;
//			dy /= delta;
//			dy *= this.maxDelta;
//			dir[0] = v[0] + dx;
//			dir[1] = v[1] + dy;
//		}
		
		
		this.solver.run(constr, dir, vS, new double []{v[0],v[1]});
		
		

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


	private double computePerceivedSpace(double[] dir,
			List<Tuple<Double, Sim2DAgent>> neighbors) {
		double s = Double.POSITIVE_INFINITY;
//		if (this.agent.getId().toString().equals("b939")) {
//			System.out.println("got you!");
//		}
		
		for (Tuple<Double, Sim2DAgent> n : neighbors) {
			
			final Sim2DAgent nA = n.getSecond();
//			if (this.agent.getId().toString().equals("b884") && nA.getId().toString().equals("b939")) {
//				System.out.println("got you!");
//			}
			
			
			double dx = (nA.getPos()[0] - this.agent.getPos()[0]);
			double dy = (nA.getPos()[1] - this.agent.getPos()[1]);
			
			final double dist = StrictMath.sqrt(dx*dx+dy*dy);
			double effectiveDist = dist;
			
			double normHeight = this.agent.getHeight()/this.normHeightDenom;
			dx /= dist;
			dy /= dist;

			//directional penalty 
			double delta = (1 + this.beta)*normHeight*this.vmaxSqrt/(2*this.alpha);
			
			double dotProd = CGAL.dot(dir[0], dir[1], dx, dy);
//			double directionalPenalty = .15*delta*(1-dotProd);
			double directionalPenalty = delta*(1-dotProd);
			effectiveDist += directionalPenalty;
			
			//orientation penalty
			double sqrSpeedNA = nA.getVelocity()[0]*nA.getVelocity()[0]+nA.getVelocity()[1]*nA.getVelocity()[1];
			double sqrtSpeedNA = StrictMath.sqrt(StrictMath.sqrt(sqrSpeedNA));
			double tmp = (normHeight*sqrtSpeedNA*(1+this.beta)*StrictMath.abs(CGAL.dot(this.agent.getVelocity()[0], this.agent.getVelocity()[1], -dx, -dy)))/(2*this.alpha);
			double orientationPenalty = StrictMath.max(nA.getRadius(), tmp); //TODO check whether r_j denotes really the radius [gl April '13]
			effectiveDist -= orientationPenalty;
			
			if (effectiveDist < s) {
				s = effectiveDist;
			}
		}
		return s;
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
