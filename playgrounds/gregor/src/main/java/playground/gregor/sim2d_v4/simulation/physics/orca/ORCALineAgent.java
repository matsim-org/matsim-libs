/* *********************************************************************** *
 * project: org.matsim.*
 * ORCAabTau.java
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

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.simulation.physics.ORCAVelocityUpdater;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;

public class ORCALineAgent implements ORCALine {


	private static final double EPSILON = 0.01f;

	private static final double SQRT_EPSILON = 0.1f;
	
	private double directionX;
	private double directionY;
	private double pointX;
	private double pointY;
	private int sign = 1;



	public ORCALineAgent(ORCAVelocityUpdater orcaAgent, Sim2DAgent neighbor, double tau) {
		construct(orcaAgent,neighbor,tau);
	}

	private void construct(ORCAVelocityUpdater a, Sim2DAgent neighbor, double tau) {



//		final double sqrDist = neighbor.getFirst();
//		final double sqrDist = 
		Sim2DAgent b = neighbor;


		final double[] aPos = a.getPos();
		final double[] bPos = b.getPos();
		final double[] aV = a.getVelocity();
		final double[] bV = b.getVelocity();
		//		Coordinate pA = orcaAgent.getPosition();
		//		Coordinate pB = neighbor.getPosition();

		//+(MatsimRandom.getRandom().nextDouble()-0.5)/10

		// 1. construct VO_{A|B}^\tau
		double xpBpA = bPos[0]-aPos[0];
		double ypBpA = bPos[1]-aPos[1];
		double sqrDist = xpBpA*xpBpA +ypBpA*ypBpA;
		double rAB = a.getRadius()+b.getRadius();

		//				if (this.debug) {
		//					this.debugger.addCircle(xpBpA+this.dbgX, ypBpA+this.dbgY,rAB, 128, 128, 128, 255, 0, false);
		//					this.debugger.addAll();
		//					System.out.println("debug!!");
		//				}

		
		if (sqrDist <= (rAB*rAB+EPSILON)) {//collision! construct ORCA Line so, that the collision is resolved in the next time step  
			double norm = Math.sqrt(sqrDist);
			
			// this is the weight of 0.5 ? then this is the right place to introduce right of way!
			double moveHalfe = (SQRT_EPSILON+rAB-norm);// * (a.getRadius()/rAB);
			
//			if (norm > rAB) {
//				moveHalfe -= EPSILON;
//			} else {
//				moveHalfe += 10*EPSILON;
//			}

			setDirectionX(- ypBpA/norm);
			setDirectionY( xpBpA/norm);
			this.pointX = aV[0] - xpBpA*moveHalfe;
			this.pointY = aV[1] - ypBpA*moveHalfe;

			return;
		}

		final double [] tangents = new double[4]; //left x, left y, right x, right y
		computeVOabTangents(xpBpA,ypBpA,rAB,tangents);

		//		if (this.debug) {
		//			this.debugger.addLine(this.dbgX, this.dbgY, tangents[0]+this.dbgX, tangents[1]+this.dbgY, 128,128,128,255,0);
		//			this.debugger.addLine(this.dbgX, this.dbgY, tangents[2]+this.dbgX, tangents[3]+this.dbgY, 128,128,128,255,0);
		//			this.debugger.addAll();
		//			System.out.println("debug!!");
		//		}

		//DEBUG
		//		GuiDebugger.addVector(new double []{0, 0, tangents[0], tangents[1],128,128,128});
		//		GuiDebugger.addVector(new double []{0, 0, tangents[2], tangents[3],128,128,128});
		//		GuiDebugger.addVector(0, 0, xpBpA,ypBpA);


		double tauX = (xpBpA)/tau;
		double tauY = (ypBpA)/tau;
		double tauR = rAB/tau;

		//		if (this.debug) {
		//			this.debugger.addCircle(tauX+this.dbgX, tauY+this.dbgY, tauR, 255,255,255,128,0, true);
		//			this.debugger.addAll();
		//			System.out.println("debug!!");	
		//		}

		//		GuiDebugger.addCircle(new double []{tauX,tauY,tauR,128,128,128});



		double tauXleft = tangents[0]/tau;
		double tauYleft = tangents[1]/tau;
		double tauXright = tangents[2]/tau;
		double tauYright = tangents[3]/tau;		

		//		if (this.debug) {
		//			double x0 = tauXleft + this.dbgX;
		//			double y0 = tauYleft + this.dbgY;
		//			double x1 = x0 + 10*tauXleft;
		//			double y1 = y0 + 10*tauYleft;
		//			double x3 = tauXright + this.dbgX;
		//			double y3 = tauYright + this.dbgY;
		//			double x2 = x3 + 10*tauXright;
		//			double y2 = y3 + 10*tauYright;
		//			double [] dbgx = {x0,x1,x2,x3};
		//			double [] dbgy = {y0,y1,y2,y3}; 
		//			this.debugger.addPolygon(dbgx, dbgy, 255,255,255,128,0);
		//			this.debugger.addAll();
		//			System.out.println("debug!!");				
		//		}


		//		double vxA = (double) orcaAgent.getVx();
		//		double vyA = (double) orcaAgent.getVy();
		//
		//		double vxB = (double) neighbor.getVx();
		//		double vyB = (double) neighbor.getVy();

		double vxAvxB = aV[0] - bV[0];
		double vyAvyB = aV[1] - bV[1];

		//2. figure out what the responsible component is
		//tau circle is responsible if v_A - v_B is right of line tauLeft ---> tauRight
		if (isLeftOfLine(vxAvxB, vyAvyB, tauXleft, tauYleft, tauXright, tauYright) <= 0) {
			computeVectorUForCircle(tauX, tauY, tauR,vxAvxB,vyAvyB);

		}else {
			//construct centerline of VO cone
			double VOcenterX = tangents[0]+tangents[2];
			double VOcenterY = tangents[1]+tangents[3];

			if (isLeftOfLine(vxAvxB, vyAvyB, 0, 0, VOcenterX, VOcenterY) > 0){//left side
				computeVectorUForTangent(tangents[0],tangents[1],vxAvxB,vyAvyB);
				if (isLeftOfLine(vxAvxB, vyAvyB, 0, 0, tangents[0], tangents[1]) > 0) {
					this.sign  = -1;
				}

			} else {//right side
				computeVectorUForTangent(tangents[2],tangents[3],vxAvxB,vyAvyB);
				if (isLeftOfLine(vxAvxB, vyAvyB, 0, 0, tangents[2], tangents[3]) < 0) {
					this.sign  = -1;
				}
			}
		}


		//		if (this.debug) {
		//			double x0 = this.dbgX + vxAvxB + this.directionX;
		//			double y0 = this.dbgY + vyAvyB + this.directionY;
		//			double x1 = this.dbgX + aV[0];
		//			double y1 = this.dbgY + aV[1];
		//			double x2 = x1 + this.directionX/2;
		//			double y2 = y1 + this.directionY/2;
		//			this.debugger.addLine(x0, y0, x1, y1, 128, 128, 128, 255, 0);
		//			this.debugger.addLine(x1, y1, x2, y2, 128, 128, 255, 255, 0);
		//			this.debugger.addAll();
		//			System.out.println("debug!!");	
		//		}

		calcORCA(aV[0],aV[1]);


	}


	private void calcORCA(double vxA, double vyA) {

		//		if (debug) {
		//			
		//		}

		this.setPointX(vxA + this.getDirectionX()/2f);
		this.setPointY(vyA + this.getDirectionY()/2f);
		double n = norm(this);
//		if (n == 0) {
//			System.out.println("");
//		}
		multiply(this, this.sign/n);
		double tmp = this.getDirectionX();
		this.setDirectionX(this.getDirectionY());
		this.setDirectionY(-tmp);


	}


	private void computeVectorUForTangent(double vx, double vy,double vxAvxB, double vyAvyB) {

		double b = CGAL.vectorCoefOfPerpendicularProjection(vxAvxB, vyAvyB, 0, 0, vx, vy);
		//		double c1 = dot (vxAvxB,vyAvyB,vx,vy);
		//		double c2= dot (vx,vy,vx,vy); //Check!! c2 = sqrt(c2)??
		//		double b = c1 /c2;
		double x = b * vx;
		double y = b * vy;
		if (x == vxAvxB && y == vyAvyB) {
			x = (b+0.01f) * vx;
			y = (b+0.01f) * vy;
		}
		this.setDirectionX(x - vxAvxB);
		this.setDirectionY(y - vyAvyB);

		//		if (this.debug) {
		//			x = getDirectionX();
		//			y = getDirectionY();
		//			this.debugger.addLine(vxAvxB+this.dbgX,vyAvyB+this.dbgY,vxAvxB+x+this.dbgX,vyAvyB+y+this.dbgY,0,0,255,255,0);
		//			this.debugger.addCircle(vxAvxB+this.dbgX,vyAvyB+this.dbgY,.02f,0,0,255,255,0,true);
		//			this.debugger.addAll();
		//			System.out.println("debug!!");	
		//		}

	}



	private void computeVectorUForCircle(double tauX, double tauY, double tauR, double vxAvxB, double vyAvyB) {
		double x = tauX - vxAvxB;
		double y = tauY - vyAvyB;
		//length of vector u
		double norm = Math.sqrt(x*x+y*y);

		
		this.setDirectionX((x / norm) * (norm - tauR));
		this.setDirectionY((y / norm) * (norm - tauR));

		//		if (this.debug) {
		//			x = getDirectionX();
		//			y = getDirectionY();
		//			this.debugger.addLine(vxAvxB+this.dbgX,vyAvyB+this.dbgY,vxAvxB+x+this.dbgX,vyAvyB+y+this.dbgY,0,0,255,255,0);
		//			this.debugger.addCircle(vxAvxB+this.dbgX,vyAvyB+this.dbgY,.02f,0,0,255,255,0,true);
		//			this.debugger.addAll();
		//			System.out.println("debug!!");	
		//		}

		if (norm > tauR) {
			this.sign = -1;
		}
	}

	private void computeVOabTangents(double xpApB, double ypApB, double rAB, double[] tangents) {

		double dx = -xpApB/2;
		double dy = -ypApB/2;

		double d = Math.sqrt(dx*dx+dy*dy); // hypot avoids underflow/overflow  ... but it is to slow

		double rSqr = rAB*rAB;
		double a =  rSqr / (2.0f *d);

		double x2 = xpApB + (dx * a /d);
		double y2 = ypApB + (dy * a /d);

		double aSqr = a*a;
		double h = Math.sqrt(rSqr - aSqr);

		double rx = -dy * (h/d);
		double ry = dx * (h/d);

		double xi = x2 + rx;
		double yi = y2 + ry;
		double xiPrime = x2 - rx;
		double yiPrime = y2 - ry;

		tangents[0] = xiPrime;
		tangents[1] = yiPrime;
		tangents[2] = xi;
		tangents[3] = yi;
	}

	//	/* (non-Javadoc)
	//	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#gisDump()
	//	 */
	//	@Override
	//	public void gisDump() {
	//
	//
	////		LineString left = GisDebugger.geofac.createLineString(new Coordinate[]{new Coordinate(this.getPointX(),this.getPointY()),new Coordinate((double)this.getPointX()+this.getDirectionX(),(double)this.getPointY()+this.getDirectionY()),new Coordinate((double)this.getPointX()+this.getDirectionX()+this.getDirectionY()/10,(double)this.getPointY()+this.getDirectionY()-this.getDirectionX()/10)});
	////		GisDebugger.addGeometry(left, "ORCA_dir");
	//		
	//		Coordinate c0 = new Coordinate(this.pointX - 10*this.directionX,this.pointY - 10*this.directionY);
	//		Coordinate c1 = new Coordinate(this.pointX + 10*this.directionX,this.pointY + 10*this.directionY);
	//		Coordinate c2 = new Coordinate(this.pointX + 10*this.directionX+20*this.directionY,this.pointY + 10*this.directionY-20*this.directionX);
	//		Coordinate c3 = new Coordinate(this.pointX - 10*this.directionX+20*this.directionY,this.pointY - 10*this.directionY-20*this.directionX);
	//		Coordinate [] coords = {c0,c1,c2,c3,c0};
	//		LinearRing lr = GisDebugger.geofac.createLinearRing(coords);
	//		Polygon p = GisDebugger.geofac.createPolygon(lr, null);
	//		GisDebugger.addGeometry(p, "agent");
	//	}

	//	@Override
	//	public boolean solutionSatisfyConstraint(double x, double y) {
	//		return Algorithms.isLeftOfLine(x, y, this.p0x, this.p0y, this.p1x, this.p1y) > 0;
	//	}

	private static double norm(ORCALine line) {
		return Math.sqrt(line.getDirectionX()*line.getDirectionX()+line.getDirectionY()*line.getDirectionY());
	}

	private static void multiply(ORCALineAgent line, double scalar) {
		line.setDirectionX(line.getDirectionX() * scalar);
		line.setDirectionY(line.getDirectionY() * scalar);
	}


	//TODO put public static methods to a more central class like Algorithms.java
	static double dot(double x0, double y0, double x1, double y1) {
		return x0 * x1 + y0  * y1;
	}

	/**
	 * tests whether coordinate x0,y0 is located left of the infinite vector that runs from x1,y1  to x2,y2
	 * @param x0 the x-coordinate to test
	 * @param y0 the y-coordinate to test
	 * @param x1 first x-coordinate of the vector
	 * @param y1 first y-coordinate of the vector
	 * @param x2 second x-coordinate of the vector
	 * @param y2 second y-coordinate of the vector
	 * @return >0 if coordinate is left of the vector
	 * 		  ==0 if coordinate is on the vector
	 * 		   <0 if coordinate is right of the vector
	 */
	static double isLeftOfLine(double x0, double y0, double x1, double y1, double x2, double y2) {
		return (x2 - x1)*(y0 - y1) - (x0 - x1) * (y2 - y1);
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#solutionSatisfyConstraint(double[])
	 */
	@Override
	public boolean solutionSatisfyConstraint(double[] v) {
		return isLeftOfLine(v[0], v[1], this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY()) >= 0;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getPointX()
	 */
	@Override
	public double getPointX() {
		return this.pointX;
	}

	@Override
	public void setPointX(double pointX) {
		this.pointX = pointX;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getPointY()
	 */
	@Override
	public double getPointY() {
		return this.pointY;
	}

	@Override
	public void setPointY(double pointY) {
		this.pointY = pointY;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getDirectionX()
	 */
	@Override
	public double getDirectionX() {
		return this.directionX;
	}

	@Override
	public void setDirectionX(double directionX) {
		this.directionX = directionX;
	}

	/* (non-Javadoc)
	 * @see playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.ORCALineTMP#getDirectionY()
	 */
	@Override
	public double getDirectionY() {
		return this.directionY;
	}

	@Override
	public void setDirectionY(double directionY) {
//		if (directionY == -0 && this.directionX == 0) {
//			System.out.println("got you!!");
//		} else {
//			System.out.println("rm!!");
//		}
		this.directionY = directionY;
		
	}

}
