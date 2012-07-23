/* *********************************************************************** *
 * project: org.matsim.*
 * Constraint.java
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

public interface Constraint {
	
	public boolean solutionSatisfyConstraint(double x, double y);
	
	public double getP0x();

	public void setP0x(double val);
	
	public double getP0y();

	public void setP0y(double val);
	
	public double getP1x();

	public void setP1x(double val);
	
	public double getP1y();

	public void setP1y(double val);
}
