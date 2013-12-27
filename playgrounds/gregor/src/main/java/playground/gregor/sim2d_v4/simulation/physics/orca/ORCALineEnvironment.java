/* *********************************************************************** *
 * project: org.matsim.*
 * ORCALineEnvironment.java
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

package playground.gregor.sim2d_v4.simulation.physics.orca;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.simulation.physics.ORCAVelocityUpdater;

public class ORCALineEnvironment implements ORCALine {
	
	private static final Logger log = Logger.getLogger(ORCALineEnvironment.class);

	private double pointX;
	private double pointY;
	private double directionX;
	private double directionY;
	
	private static int penetrationCnt = 0;

	public ORCALineEnvironment(ORCAVelocityUpdater orcaAgent, LineSegment seg, double tau) {
		construct(orcaAgent,seg,tau);
	}

	private void construct(ORCAVelocityUpdater orcaAgent, LineSegment seg, double tau) {
		
		
//		tau = 1f;
		//mm line segment {x0,y0,x1,y1}
		
		final double posX = orcaAgent.getPos()[0];
		final double posY = orcaAgent.getPos()[1];
		

//		if (dist < 0){
//			System.out.println(dist + " " );
//			
//		}
		
		//here we have two possible orientations of the ORCA line, either it runs parallel to the line segment
		//or it is a tangent on Minkowski sum circle that is closer to the point of origin
		//1. check whether line segment is responsible
		double xpBpA0 = (seg.x0 - posX)/tau;
		double ypBpA0 = (seg.y0 - posY)/tau;
		double xpBpA1 = (seg.x1 - posX)/tau;
		double ypBpA1 = (seg.y1 - posY)/tau;
		
//		GuiDebugger.addVector(0, 0, xpBpA0, ypBpA0);
//		GuiDebugger.addVector(0, 0, xpBpA1, ypBpA1);
		double r = orcaAgent.getRadius();
		double rATau = (r)/tau;
		
		double dxpBpA = xpBpA1-xpBpA0;
		double dypBpA = ypBpA1-ypBpA0;
		
		double c1 = ORCALineAgent.dot(-xpBpA0, -ypBpA0,dxpBpA,dypBpA);
		double c2 = ORCALineAgent.dot(dxpBpA,dypBpA,dxpBpA,dypBpA);
		double b  = c1/c2;
		

		if (b > 0 && b < 1) {// line segment
			
			double tmpX = (xpBpA0 + b*dxpBpA);
			double tmpY = (ypBpA0 + b*dypBpA);
			double norm = Math.sqrt(tmpX*tmpX+tmpY*tmpY);//distance to origin

			double xn  = tmpX/norm;
			double yn  = tmpY/norm;
			
			if (norm < rATau) {
				double move = rATau - norm;
//				double coeff = move/norm;
				this.pointX = xn*move;//(double) (a.getVx() - tmpX*coeff);
				this.pointY = yn*move;//(double) (a.getVy() - tmpY*coeff);
			} else {
				this.pointX = xn * (norm-rATau);
				this.pointY = yn * (norm-rATau);
			}
			
			this.directionX = -yn;
			this.directionY = xn;
			double dist = CGAL.signDistPointLine(posX, posY, seg.x0, seg.y0, seg.dx, seg.dy)-r; 
			if (dist < 0) {
				this.pointX += -(dist-0.1) * this.directionY;
				this.pointY += +(dist-0.1) * this.directionX;
				
				if (penetrationCnt < 10) {
					log.warn("Agent: " + orcaAgent + " is penetrating a wall. This is a bug! As a workaround ORCALine has been moved inward! Needs to be fixed! [GL August '13]");
					penetrationCnt++;
					if (penetrationCnt == 10) {
						log.warn(Gbl.FUTURE_SUPPRESSED);
					}
				}
			}
		} else {//circle
			//1. determine which side
			double sqrDist0 = xpBpA0 * xpBpA0  + ypBpA0 * ypBpA0;
			double sqrDist1 = xpBpA1 * xpBpA1  + ypBpA1 * ypBpA1;
			double cx;
			double cy;
			if (sqrDist0 < sqrDist1) {
				cx = xpBpA0;
				cy = ypBpA0;
			} else {
				cx = xpBpA1;
				cy = ypBpA1;
			}
			double norm = Math.sqrt(cx*cx+cy*cy);
			double xn = cx/norm;
			double yn = cy/norm;

			this.pointX = xn * (norm-rATau);
			this.pointY = yn * (norm-rATau);
			this.directionX = -yn;
			this.directionY = xn;
			
		}
		

				
	}


	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#solutionSatisfyConstraint(double[])
	 */
	@Override
	public boolean solutionSatisfyConstraint(double[] v) {
		double leftVal = CGAL.isLeftOfLine(v[0], v[1],this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY());
		
		return leftVal >= 0;
	}

	@Override
	public double getPointX() {
		return this.pointX;
	}

	@Override
	public double getPointY() {
		return this.pointY;
	}

	@Override
	public double getDirectionX() {
		return this.directionX;
	}

	@Override
	public double getDirectionY() {
		return this.directionY;
	}

	@Override
	public void setPointX(double x) {
		throw new RuntimeException("not supported");
		
	}

	@Override
	public void setPointY(double y) {
		throw new RuntimeException("not supported");
	}
	

	@Override
	public void setDirectionX(double x) {
		this.directionX = x;
	}

	@Override
	public void setDirectionY(double y) {
		this.directionY = y;
	}
	

}
