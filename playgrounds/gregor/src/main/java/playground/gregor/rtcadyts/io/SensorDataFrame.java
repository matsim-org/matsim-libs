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

import java.util.ArrayList;
import java.util.List;

public class SensorDataFrame {
	
	private double time;
	
	private final List<SensorDataVehicle> vehicles = new ArrayList<>();

	public SensorDataFrame(double time) {
		this.time = time;
	}
	
	public void addVehicle(double time, double x, double y, double v, double angle){
		if (time != this.time) {
			throw new RuntimeException("Vehicle's time step differs from frame time step");
		}
		SensorDataVehicle veh = new SensorDataVehicle(time, x, y, v, angle);
		this.vehicles.add(veh);
	}
 
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (SensorDataVehicle veh : this.vehicles){
			buf.append(veh);
			buf.append('\n');
		}
		return buf.toString();
	}

	public List<SensorDataVehicle> getVehicles() {
		return vehicles;
	}

	public double getTime() {
		return this.time;
	}
}
