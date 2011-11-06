/* *********************************************************************** *
 * project: org.matsim.*
 * Agent2D.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import playground.gregor.sim2d_v2.simulation.floor.forces.Force;
import playground.gregor.sim2d_v2.simulation.floor.forces.deliberative.velocityobstacle.CCWPolygon;


import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Agent2D  {

	private Coordinate currentPosition;
	private final Force force = new Force();
	private final double desiredVelocity;
	private double vx;
	private double vy;
	private final MobsimDriverAgent pda;
	private final Scenario sc;
	private double currentDesiredVelocity;

	public static final double AGENT_WEIGHT = 80;// * 1000;
	public static final double AGENT_DIAMETER = 0.50;

	private CCWPolygon geometry;

	/**
	 * @param p
	 * @param sim2d
	 */
	public Agent2D(MobsimDriverAgent pda, Scenario sc) {

		this.pda = pda;
		this.sc = sc;
		// TODO think about this
		this.desiredVelocity = 1.34; //+(MatsimRandom.getRandom().nextDouble() - 0.5) / 2;
		this.currentDesiredVelocity = this.desiredVelocity;

		initGeometry();

	}

	private void initGeometry() {
		//create agentGeometry
		int numOfParts = 5;
		double agentRadius = AGENT_DIAMETER / 2;

		Coordinate[] c = new Coordinate[numOfParts + 1];
		double angle = Math.PI * 2 / numOfParts;
		for(int i = 0; i <numOfParts; i++){
			c[i] = new Coordinate(agentRadius * Math.cos(angle * i), agentRadius * Math.sin(angle * i));
		}
		c[numOfParts] = c[0];
		this.geometry = new CCWPolygon(c, new Coordinate(0,0));

	}

	public CCWPolygon getGeometry(){
		return this.geometry;
	}

	/**
	 * @return
	 */
	public Coordinate getPosition() {
		return this.currentPosition;
	}

	public void setPostion(Coordinate pos) {
		this.currentPosition = pos;
		this.geometry.translate(pos.x, pos.y);
	}

	public Force getForce() {
		return this.force;
	}

	/**
	 * @param newPos
	 */
	@Deprecated //use translate instead!
	public void moveToPostion(Coordinate newPos) {
		// TODO check for choose next link and so on ...
		this.currentPosition.setCoordinate(newPos);
	}

	public void translate(double dx, double dy) {
		this.currentPosition.x += dx;
		this.currentPosition.y += dy;
		this.geometry.translate(dx, dy);

	}

	/**
	 * @return
	 */
	public double getDesiredVelocity() {
		return this.currentDesiredVelocity;
	}

	public void setCurrentVelocity(double vx, double vy) {
		this.vx = vx;
		this.vy = vy;

	}

	public double getVx() {
		return this.vx;
	}

	public double getVy() {
		return this.vy;
	}

	public double getWeight() {
		return AGENT_WEIGHT;
	}

	public void notifyMoveOverNode(Id newLinkId, double time) {
		this.pda.notifyMoveOverNode(newLinkId);
		double sp = this.sc.getNetwork().getLinks().get(newLinkId).getFreespeed(time);
		this.currentDesiredVelocity = Math.min(this.desiredVelocity, sp);
	}

	public Id getId() {
		return this.pda.getId();
	}

	public Id getCurrentLinkId() {
		return this.pda.getCurrentLinkId();
	}

	public void endLegAndAssumeControl(double time) {
		this.pda.endLegAndAssumeControl(time);
	}

	public Id chooseNextLinkId() {
		return this.pda.chooseNextLinkId();
	}



}
