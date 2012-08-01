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

package playground.gregor.sim2d_v3.simulation.floor.forces.deliberative;

//import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;

public class ORCALineEnvironment implements ORCALine {

	private float pointX;
	private float pointY;
	private float directionX;
	private float directionY;

	public ORCALineEnvironment(Agent2D agent, float[] mm, float tau) {
		construct(agent,mm,tau);
	}

	private void construct(Agent2D a, float[] mm, float tau) {
		
		
		tau = 1.f;
		//mm line segment {x0,y0,x1,y1}
		
		float posX = (float) a.getPosition().x;
		float posY = (float) a.getPosition().y;
				
		//here we have two possible orientations of the ORCA line, either it runs parallel to the line segment
		//or it is a tangent on Minkowski sum circle that is closer to the point of origin
		//1. check whether line segment is responsible
		float xpBpA0 = (mm[0] - posX)/tau;
		float ypBpA0 = (mm[1] - posY)/tau;
		float xpBpA1 = (mm[2] - posX)/tau;
		float ypBpA1 = (mm[3] - posY)/tau;
		
//		GuiDebugger.addVector(0, 0, xpBpA0, ypBpA0);
//		GuiDebugger.addVector(0, 0, xpBpA1, ypBpA1);
		
		float rATau = (float) (a.getPhysicalAgentRepresentation().getAgentDiameter()/2)/tau;
		
		float dxpBpA = xpBpA1-xpBpA0;
		float dypBpA = ypBpA1-ypBpA0;
		
		float c1 = ORCALineAgent.dot(-xpBpA0, -ypBpA0,dxpBpA,dypBpA);
		float c2 = ORCALineAgent.dot(dxpBpA,dypBpA,dxpBpA,dypBpA);
		float b  = c1/c2;
		

		if (b > 0 && b < 1) {// line segment
			
			float tmpX = (xpBpA0 + b*dxpBpA);
			float tmpY = (ypBpA0 + b*dypBpA);
			float norm = (float) Math.sqrt(tmpX*tmpX+tmpY*tmpY);//distance to origin

			float xn  = tmpX/norm;
			float yn  = tmpY/norm;
			
			if (norm < rATau) {
				float move = rATau - norm;
//				float coeff = move/norm;
				this.pointX = xn*move;//(float) (a.getVx() - tmpX*coeff);
				this.pointY = yn*move;//(float) (a.getVy() - tmpY*coeff);
			} else {
				this.pointX = xn * (norm-rATau);
				this.pointY = yn * (norm-rATau);
			}
			
			this.directionX = -yn;
			this.directionY = xn;
		} else {//circle
			//1. determine which side
			float sqrDist0 = xpBpA0 * xpBpA0  + ypBpA0 * ypBpA0;
			float sqrDist1 = xpBpA1 * xpBpA1  + ypBpA1 * ypBpA1;
			float cx;
			float cy;
			if (sqrDist0 < sqrDist1) {
				cx = xpBpA0;
				cy = ypBpA0;
			} else {
				cx = xpBpA1;
				cy = ypBpA1;
			}
			float norm = (float)Math.sqrt(cx*cx+cy*cy);
			float xn = cx/norm;
			float yn = cy/norm;

			this.pointX = xn * (norm-rATau);
			this.pointY = yn * (norm-rATau);
			this.directionX = -yn;
			this.directionY = xn;
			
		}
				
	}

//	@Override
//	public void gisDump() {
//		
//		//TODO create polygons!!
////		LineString ls = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.pointX,this.pointY),new Coordinate(this.pointX+this.directionX,this.pointY+this.directionY),new Coordinate(this.pointX+this.directionX-this.directionY/10,this.pointY+this.directionY+this.directionX/10)});
////		GisDebugger.addGeometry(ls,""+this.hashCode());
////		LineString ls2 = GisDebugger.geofac.createLineString(this.lsc);
////		GisDebugger.addGeometry(ls2,""+this.hashCode());
//		
//		Coordinate c0 = new Coordinate(this.pointX - 10*this.directionX,this.pointY - 10*this.directionY);
//		Coordinate c1 = new Coordinate(this.pointX + 10*this.directionX,this.pointY + 10*this.directionY);
//		Coordinate c2 = new Coordinate(this.pointX + 10*this.directionX+20*this.directionY,this.pointY + 10*this.directionY-20*this.directionX);
//		Coordinate c3 = new Coordinate(this.pointX - 10*this.directionX+20*this.directionY,this.pointY - 10*this.directionY-20*this.directionX);
//		Coordinate [] coords = {c0,c1,c2,c3,c0};
//		LinearRing lr = GisDebugger.geofac.createLinearRing(coords);
//		Polygon p = GisDebugger.geofac.createPolygon(lr, null);
//		GisDebugger.addGeometry(p, "env");
//	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#solutionSatisfyConstraint(float[])
	 */
	@Override
	public boolean solutionSatisfyConstraint(float[] ret) {
		return ORCALineAgent.isLeftOfLine(ret[0], ret[1], this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY()) >= 0;
	}
	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#solutionSatisfiesConstraint(float, float)
	 */
	@Override
	public boolean solutionSatisfiesConstraint(float x, float y) {
		return ORCALineAgent.isLeftOfLine(x, y, this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY()) >= 0;
	}

	@Override
	public float getPointX() {
		return this.pointX;
	}

	@Override
	public float getPointY() {
		return this.pointY;
	}

	@Override
	public float getDirectionX() {
		return this.directionX;
	}

	@Override
	public float getDirectionY() {
		return this.directionY;
	}

	@Override
	public void setPointX(float x) {
		throw new RuntimeException("not supported");
		
	}

	@Override
	public void setPointY(float y) {
		throw new RuntimeException("not supported");
	}
	
	

}
