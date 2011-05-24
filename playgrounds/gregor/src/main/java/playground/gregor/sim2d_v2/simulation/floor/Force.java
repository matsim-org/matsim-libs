/* *********************************************************************** *
 * project: org.matsim.*
 * Force.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.simulation.floor;


/**
 * @author laemmel
 * 
 */
public class Force {
	private double fx = 0;
	private double fy = 0;

	private double vx = 0;
	private double vy = 0;


	public double getXComponent() {
		return this.fx;
	}

	public double getYComponent() {
		return this.fy;
	}

	/* package */void setXComponent(double fx) {
		this.fx = fx;
	}

	/* package */void setYComponent(double fy) {
		this.fy = fy;
	}

	/* package */void incrementX(double incrfx) {
		this.fx += incrfx;
	}

	/* package */void incrementY(double incrfy) {
		this.fy += incrfy;
	}



	public double getVx() {
		return this.vx;
	}

	public double getVy() {
		return this.vy;
	}

	public void setVx(double vx) {
		this.vx = vx;
	}

	public void setVy(double vy) {
		this.vy = vy;
	}


	public void update(double weight, double deltaT) {
		this.vx += (deltaT*this.fx)/weight;
		this.vy += (deltaT*this.fy)/weight;
		reset();
	}

	public void reset() {
		this.fx = 0;
		this.fy = 0;
	}

}
