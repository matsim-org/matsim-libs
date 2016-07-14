/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.rtcadyts.frames2counts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.gregor.rtcadyts.io.SensorDataVehicle;

public class LinkInfo {
	private double angle;
	private List<SensorDataVehicle> vehs = new ArrayList<>();
	private Link link;
	private double q;
	
	public LinkInfo(Link tentativeLink) {
		double dx = tentativeLink.getToNode().getCoord().getX()-tentativeLink.getFromNode().getCoord().getX();
		double dy = tentativeLink.getToNode().getCoord().getX()-tentativeLink.getFromNode().getCoord().getX();
		this.angle = getAngle(dx,dy);
		this.link = tentativeLink;
	}
	
	public void addVeh(SensorDataVehicle veh) {
		this.vehs.add(veh);
	}

	
	private static double getAngle(double x, double y) {
		double angle = Math.atan2(y, x)*180/Math.PI-90.;
		if (angle < 0) {
			angle += 360;
		} else if (angle == 0) {
			return angle;
		}
		return 360-angle;
	}
	
	public Link getLink() {
		return this.link;
	}

	public double getAngle() {
		return this.angle;
	}
	public List<SensorDataVehicle> getVeh() {
		return this.vehs;
	}

	public void setFlow(double q) {
		this.q = q;
		
	}
	
	public double getFlow() {
		return this.q;
	}
}