/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.gregor.rtcadyts.io;

public class SensorDataVehicle {
	private double time;
	private double x;
	private double y;
	private double angle;
	private double v;

	public SensorDataVehicle(double time, double x, double y,double v, double angle){
		this.time = time;
		this.x = x;
		this.y = y;
		this.angle = angle;
		this.v = v;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.time);
		buf.append(' ');
		buf.append(x);
		buf.append(' ');
		buf.append(y);
		buf.append(' ');
		buf.append(v);
		buf.append(' ');
		buf.append(angle);
		return buf.toString();
	}

	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}

	public double getAngle() {
		return this.angle;
	}

	public double getSpeed() {
		return this.v;
	}

	public double getTime() {
		return this.time;
	}
}
