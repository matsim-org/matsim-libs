/* *********************************************************************** *
 * project: org.matsim.*
 * Force.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d.simulation;

@Deprecated
public class Force {

	private double x = 0;
	private double y = 0;
	private double oldX = 0;
	private double oldY = 0;

	private final double xCoord;
	private final double yCoord;

	public Force() {
		this.xCoord = Double.NaN;
		this.yCoord = Double.NaN;
	}

	public Force(double fX, double fY, double xCoord, double yCoord) {
		setFx(fX);
		setFy(fY);
		this.xCoord = xCoord;
		this.yCoord = yCoord;
	}

	public double getXCoord() {
		return this.xCoord;
	}

	public double getYCoord() {
		return this.yCoord;
	}

	public void setFx(double x) {
		this.x = x;
	}

	public double getFx() {
		return this.x;
	}

	public void setFy(double y) {
		this.y = y;
	}

	public double getFy() {
		return this.y;
	}

	public void setOldFx(double x) {
		this.oldX = x;
	}

	public double getOldFx() {
		return this.oldX;
	}

	public void setOldFy(double y) {
		this.oldY = y;
	}

	public double getOldFy() {
		return this.oldY;
	}

	// //DEBUG
	// double interactionX = 0;
	// double interactionY = 0;
	// double envX = 0;
	// double envY = 0;
	// double driveX = 0;
	// double driveY = 0;
	// double pathX = 0;
	// double pathY = 0;

}
