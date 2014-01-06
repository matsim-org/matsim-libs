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
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.DesiredDirectionCalculator;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.Neighbors;
import playground.gregor.sim2d_v4.simulation.physics.algorithms.SpaceDependentSpeed;
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


	private final Neighbors ncalc;
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


	private final SpaceDependentSpeed ps;
	
	public ORCAVelocityUpdater(SpaceDependentSpeed ps, DesiredDirectionCalculator dd, Neighbors ncalc, Sim2DConfig conf, Sim2DAgent agent) {
		this.ncalc = ncalc;
		this.dT = conf.getTimeStepSize();
		this.maxDelta =5 * this.dT;
		this.dd = dd;
		this.agent = agent;
		this.ps = ps;
	}


	@Override
	public void updateVelocity() {
		


		List<ORCALine> constr = new ArrayList<ORCALine>();
		for (LineSegment seg : this.agent.getPSec().getObstacleSegments()) {
			ORCALineEnvironment ol = new ORCALineEnvironment(this, seg, this.tau);
			constr.add(ol);
			
			
//			if (this.agent.getId().equals(new IdImpl("c11045"))) {
//				System.out.println();
//			}
//			if (this.agent.getId().toString().equals("b7975")) {
//				
//				double x0 = ol.getPointX()+this.agent.getPos()[0];
//				double y0 = ol.getPointY()+this.agent.getPos()[1];
//				double x1 = x0 + 5*ol.getDirectionX();
//				double y1 = y0 + 5*ol.getDirectionY();
//				x0 -=  5*ol.getDirectionX();
//				y0 -= 5*ol.getDirectionY();
//				Segment s = new Segment();
//				s.x0 = x0;
//				s.x1 = x1;
//				s.y0 = y0;
//				s.y1 = y1;
//				this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new LineEvent(time, s, false,255,0,0,255,0));
////				System.out.println("stop");
//			}

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
		final double[] dir = this.dd.computeDesiredDirection();
//		double orthoX = this.agent.getPos()[0]-dir[1];
//		double orthoY = this.agent.getPos()[1]+dir[0];

		List<Sim2DAgent> neighbors = this.ncalc.getNeighbors();
		for (Sim2DAgent neighbor : neighbors) {
//			if (this.debugger != null && ( getId().toString().equals("r876"))){//&& neighbor.getSecond().getId().toString().equals("r5")) {
//				ORCALine ol = new ORCALineAgent(this, neighbor, this.tau,this.debugger);
//				constr.add(ol);				
//			} else {
			
//			double lft = CGAL.isLeftOfLine(neighbor.getSecond().getPos()[0], neighbor.getSecond().getPos()[1], this.agent.getPos()[0], this.agent.getPos()[1], orthoX, orthoY);
//			if (lft > 0) {
//				continue;
//			}
			
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

		
		

		
		double v0 = this.agent.getV0();
		double freeSpeed = v0;
//		this.ps.computeSpaceDependentSpeed(this.agent, neighbors);
		
//		if (voronoiApproach) {
//			double x = this.agent.getPos()[0];
//			double y = this.agent.getPos()[1];
//			VoronoiDensity vd = new VoronoiDensity(0.01, x-10,y-10,x+10,y+10);
//			double [] xx = new double [neighbors.size()+1];
//			double [] yy = new double [neighbors.size()+1];
//			int idx = 0;
//			for (Tuple<Double, Sim2DAgent> n : neighbors){
//				Sim2DAgent nA = n.getSecond();
//				xx[idx] = nA.getPos()[0];
//				yy[idx] = nA.getPos()[1];
//				idx++;
//			}
//			xx[idx] = x;
//			yy[idx] = y;
//			List<VoronoiCell> cc = vd.computeVoronoiDensity(xx, yy);
//			
//
//			VoronoiCell cell = cc.get(idx);
////			for (GraphEdge e : cell.edges) {
////				Segment s = new Segment();
////				s.x0 = e.x1;
////				s.x1 = e.x2;
////				s.y0 = e.y1;
////				s.y1 = e.y2;
////				this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new LineEvent(time, s, false));
////			}
//			
//			if (cell.area < 10 && cell.area > 0) {
//				double rho = 1/cell.area;
//				freeSpeed = 1.34 * (1 - Math.exp(-1.913*(1/rho-1/5.4)));
////				System.out.println(freeSpeed);
//			}
//			
//		} else {
//			double perceivedSpace = computePerceivedSpace(dir,neighbors)+0.18;
//			double tmp = (perceivedSpace*this.alpha)/(this.agent.getHeight()/this.normHeightDenom *(1+this.beta));
//			freeSpeed = Math.max(tmp*tmp,0.15);
//		}
//		if (freeSpeed > v0) {
//			freeSpeed = v0;
//		}
//		System.out.println(v0);
//		double area = this.ps.computePersonalSpace(this.agent,neighbors);
//		if (area < 10 && area > 0.0001) {
//			double rho = Math.min(4, 1/area);
//			
//			freeSpeed = 1.34 * (1 - Math.exp(-1.913*(1/rho-1/5.4)));
//		}
		
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
		
		
		this.solver.run(constr, dir, v0, new double []{v[0],v[1]});
		
		

//		if (this.debugger != null && getId().toString().equals("r876") ){
//			this.debugger.addLine(this.pos[0], this.pos[1], this.pos[0]+this.v[0], this.pos[1]+this.v[1], 255, 0, 0, 255, 0);
//			System.out.println("debug!");
//		}
		
		v[0] = dir[0];
		v[1] = dir[1];
		
//		if (this.agent.getId().toString().equals("b7975")) {
//			
//			double x0 = this.agent.getPos()[0];
//			double y0 = this.agent.getPos()[1];
//			double x1 = x0 + v[0];
//			double y1 = y0 + v[1];
//			Segment s = new Segment();
//			s.x0 = x0;
//			s.x1 = x1;
//			s.y0 = y0;
//			s.y1 = y1;
//			this.agent.getPSec().getPhysicalEnvironment().getEventsManager().processEvent(new LineEvent(time, s, false,0,0,255,255,0));
////			System.out.println("stop");
//		}

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
//		double orthoX = this.agent.getPos()[0]-dir[1];
//		double orthoY = this.agent.getPos()[1]+dir[0];
	
		for (Tuple<Double, Sim2DAgent> n : neighbors) {
			
			final Sim2DAgent nA = n.getSecond();
//			if (this.agent.getId().toString().equals("b884") && nA.getId().toString().equals("b939")) {
//				System.out.println("got you!");
//			}
//			double lft = CGAL.isLeftOfLine(nA.getPos()[0], nA.getPos()[1], this.agent.getPos()[0], this.agent.getPos()[1], orthoX, orthoY);
//			if (lft > 0) {
//				continue;
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
			double directionalPenalty = .15*delta*(1-dotProd);
//			double directionalPenalty = delta*(1-dotProd);
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
	
	@Override
	public String toString() {
		return this.agent.toString();
	}

}
