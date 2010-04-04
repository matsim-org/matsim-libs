/* *********************************************************************** *
 * project: org.matsim.*
 * TeleportationVisData
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.data.teleportation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.DriverAgent;

/**
 * 
 * @author dgrether
 *
 */
public class TeleportationVisData {

	//	private static final Logger log = Logger.getLogger(TeleportationVisData.class);

	private double stepsize;
	private double startX;
	private double startY;
	private double normalX;
	private double normalY;
	private double currentX;
	private double currentY;
	private double starttime;
	private Id id;

	private double length;

	public TeleportationVisData(double now, DriverAgent agent, Link fromLink, Link toLink) {
		this.starttime = now;
		this.id = agent.getPerson().getId();
		this.startX = fromLink.getToNode().getCoord().getX();
		this.startY = fromLink.getToNode().getCoord().getY();
		double traveltime = agent.getCurrentLeg().getTravelTime();
		double endX = toLink.getToNode().getCoord().getX();
		double endY = toLink.getToNode().getCoord().getY();
		double dX = endX - startX;
		double dY = endY - startY;
		length = Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2));
		this.stepsize = length / traveltime;
		this.normalX = dX / length;
		this.normalY = dY / length;
		this.currentX = startX;
		this.currentY = startY;
		//		log.error("offset north: "  + OTFServerQuad.offsetNorth);
		//		log.error("startX " + startX);
		//		log.error("startY " + startY);
		//		log.error("endX " + endX);
		//		log.error("endY " + endY);
		//		log.error("length " + length);
		//		log.error("stepsize " + stepsize);
		//		log.error("currentX " + currentX);
		//		log.error("currentY " + currentY);
	}

	public double getX() {
		return this.currentX;
	}

	public double getY() {
		return this.currentY;
	}

	public Id getId(){
		return this.id;
	}

	public double getLength(){
		return this.length;
	}

	public void calculatePosition(double time) {
		//		log.error("calc pos time: " + time);
		double step = (time - starttime) * this.stepsize;
		this.currentX = this.startX + (step * this.normalX);
		this.currentY = this.startY  + (step * this.normalY);
		//		log.error("currentx: " + this.currentX + " currenty: "+ this.currentY);
	}
}
