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
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

/**
 *
 * @author dgrether
 *
 */
public class TeleportationVisData implements AgentSnapshotInfo {

	//	private static final Logger log = Logger.getLogger(TeleportationVisData.class);

	private static final long serialVersionUID = 4626450928014698099L;
	private double stepsize;
	private double startX;
	private double startY;
	private double normalX;
	private double normalY;
	private double currentX;
	private double currentY;
	private double starttime;
	private Id agentId;
	private int userDefined;
	private int type;
	private double colorval;
	private AgentState state = AgentSnapshotInfo.AgentState.PERSON_OTHER_MODE ;

//	public TeleportationVisData(double now, PersonAgent agent, Link fromLink, Link toLink) {
	public TeleportationVisData(double now, Id personId, Link fromLink, Link toLink, double travelTime ) {
		this.starttime = now;
//		this.agentId = agent.getPerson().getId();
		this.agentId = personId ;
		this.startX = fromLink.getToNode().getCoord().getX();
		this.startY = fromLink.getToNode().getCoord().getY();
//		double traveltime = agent.getCurrentLeg().getTravelTime();
		double endX = toLink.getToNode().getCoord().getX();
		double endY = toLink.getToNode().getCoord().getY();
		double dX = endX - startX;
		double dY = endY - startY;
		double length = Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2));
		this.stepsize = length / travelTime;
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

	@Override
	public double getEasting() {
		return this.currentX;
	}

	@Override
	public double getNorthing() {
		return this.currentY;
	}

	@Override
	public Id getId(){
		return this.agentId;
	}

	//	public double getLength(){
	//		return this.length;
	//	}

	public void calculatePosition(double time) {
		//		log.error("calc pos time: " + time);
		double step = (time - starttime) * this.stepsize;
		this.currentX = this.startX + (step * this.normalX);
		this.currentY = this.startY  + (step * this.normalY);
		//		log.error("currentx: " + this.currentX + " currenty: "+ this.currentY);
	}

	@Override
	public AgentState getAgentState() {
		return this.state ;
	}

	@Override
	public double getAzimuth() {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public double getColorValueBetweenZeroAndOne() {
		return this.colorval ;
	}

	@Override
	public double getElevation() {
		return 0. ;
	}

	@Override
	public int getType() {
		return this.type ;
	}

	@Override
	public int getUserDefined() {
		return this.userDefined ;
	}

	@Override
	public void setAgentState(AgentState state) {
		this.state = state ;
	}

	@Override
	public void setColorValueBetweenZeroAndOne(double tmp) {
		this.colorval = tmp ;
	}

	@Override
	public void setType(int tmp) {
		this.type = tmp ;
	}

	@Override
	public void setUserDefined(int tmp) {
		this.userDefined = tmp ;
	}
}
