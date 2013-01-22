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

//import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.debugger.VisDebugger;
import playground.gregor.sim2d_v4.simulation.physics.ORCAAgent;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;

@Deprecated
public class ORCALineEnvironment implements ORCALine {

	private float pointX;
	private float pointY;
	private float directionX;
	private float directionY;
	private float dbgX;
	private float dbgY;
	private float cy;
	private float cx;

	public ORCALineEnvironment(ORCAAgent orcaAgent, Segment seg, float tau) {
		construct(orcaAgent,seg,tau);
	}

	private void construct(ORCAAgent orcaAgent, Segment seg, float tau) {
		
		
//		tau = 1f;
		//mm line segment {x0,y0,x1,y1}
		
		final float posX = orcaAgent.getPos()[0];
		final float posY = orcaAgent.getPos()[1];
				
		//here we have two possible orientations of the ORCA line, either it runs parallel to the line segment
		//or it is a tangent on Minkowski sum circle that is closer to the point of origin
		//1. check whether line segment is responsible
		float xpBpA0 = (seg.x0 - posX)/tau;
		float ypBpA0 = (seg.y0 - posY)/tau;
		float xpBpA1 = (seg.x1 - posX)/tau;
		float ypBpA1 = (seg.y1 - posY)/tau;
		
//		GuiDebugger.addVector(0, 0, xpBpA0, ypBpA0);
//		GuiDebugger.addVector(0, 0, xpBpA1, ypBpA1);
		
		float rATau = (orcaAgent.getRadius())/tau;
		
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
	public boolean solutionSatisfyConstraint(float[] v) {
		float leftVal = CGAL.isLeftOfLine(v[0], v[1],this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY());
		
		return leftVal >= 0;
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
	

	@Override
	public void debug(VisDebugger debugger, int r, int g, int b) {
		float x0 = this.pointX - 10*this.directionX;
		float y0 = this.pointY - 10*this.directionY;
		float x1 = this.pointX + 10*this.directionX;
		float y1 = this.pointY + 10*this.directionY;
		float x2 = this.pointX + 10*this.directionX + 2*this.directionY;
		float y2 = this.pointY + 10*this.directionY -2*this.directionX;
		float x3 = this.pointX - 10*this.directionX + 2*this.directionY;
		float y3 = this.pointY - 10*this.directionY -2*this.directionX;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY;
		y2 += this.directionX;
		x3 -= this.directionY;
		y3 += this.directionX;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY/2;
		y2 += this.directionX/2;
		x3 -= this.directionY/2;
		y3 += this.directionX/2;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY/4;
		y2 += this.directionX/4;
		x3 -= this.directionY/4;
		y3 += this.directionX/4;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 32, 0);
		x2 -= this.directionY/8;
		y2 += this.directionX/8;
		x3 -= this.directionY/8;
		y3 += this.directionX/8;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 64, 0);
		x2 -= this.directionY/16;
		y2 += this.directionX/16;
		x3 -= this.directionY/16;
		y3 += this.directionX/16;
		debugger.addPolygon(new float[]{x0,x1,x2,x3}, new float[]{y0,y1,y2,y3}, r, g, b, 128, 0);;
	}
	

}
