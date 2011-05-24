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
import org.matsim.core.mobsim.framework.PersonDriverAgent;


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
	private final PersonDriverAgent pda;

	public static final double AGENT_WEIGHT = 80;// * 1000;
	public static final double AGENT_DIAMETER = 0.7;

	/**
	 * @param p
	 * @param sim2d
	 */
	public Agent2D(PersonDriverAgent pda) {
		this.pda = pda;
		// TODO think about this
		this.desiredVelocity = 1.34; //0.8 + (MatsimRandom.getRandom().nextDouble() - 0.5) / 8;

	}

	/**
	 * @return
	 */
	public Coordinate getPosition() {
		return this.currentPosition;
	}

	public void setPostion(Coordinate pos) {
		this.currentPosition = pos;
	}

	public Force getForce() {
		return this.force;
	}

	/**
	 * @param newPos
	 */
	public void moveToPostion(Coordinate newPos) {
		// TODO check for choose next link and so on ...
		this.currentPosition.setCoordinate(newPos);
	}

	/**
	 * @return
	 */
	public double getDesiredVelocity() {
		return this.desiredVelocity;
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

	public void notifyMoveOverNode() {
		this.pda.notifyMoveOverNode();
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

	// /**
	// * @return
	// */
	// public double getWeight() {
	// return this.agentWeight;
	// }

}
