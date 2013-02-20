/* *********************************************************************** *
 * project: org.matsim.*
 * ORCALineUniversal.java
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

package playground.gregor.sim2d_v4.simulation.physics.orca;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.debugger.VisDebugger;

public class ORCALineUniversal implements ORCALine{

	private double x;
	private double y;
	private double dx;
	private double dy;
	
	@Override
	public boolean solutionSatisfyConstraint(double[] v) {
double leftVal = CGAL.isLeftOfLine(v[0], v[1],this.getPointX(), this.getPointY(), this.getPointX()+this.getDirectionX(),this.getPointY()+this.getDirectionY());
		
		return leftVal > 0;
	}

	@Override
	public double getPointX() {
		return this.x;
	}

	@Override
	public double getPointY() {
		return this.y;
	}

	@Override
	public void setPointX(double x) {
		this.x = x;
	}

	@Override
	public void setPointY(double y) {
		this.y = y;
	}

	@Override
	public double getDirectionX() {
		return this.dx;
	}

	@Override
	public double getDirectionY() {
		return this.dy;
	}

	@Override
	public void debug(VisDebugger visDebugger, int r, int g, int b) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setDirectionX(double x) {
		this.dx = x;
	}

	@Override
	public void setDirectionY(double y) {
		this.dy = y;
	}

}
