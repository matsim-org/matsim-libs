/* *********************************************************************** *
 * project: org.matsim.*
 * ORCALineTMP.java
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



public interface ORCALine {

//	public abstract void gisDump();

	public abstract boolean solutionSatisfyConstraint(double[] v);

	public abstract double getPointX();

	public abstract double getPointY();
	
	public abstract void setPointX(double x);
	
	public abstract void setPointY(double y);

	public abstract double getDirectionX();

	public abstract double getDirectionY();

	public abstract void setDirectionX(double x);
	
	public abstract void setDirectionY(double y);
	


}
