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
package playground.gregor.sim2d_v2.simulation;

import org.matsim.api.core.v01.population.Person;
import org.matsim.ptproject.qsim.helpers.DefaultPersonDriverAgent;

import playground.gregor.sim2d_v2.simulation.floor.Force;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class Agent2D extends DefaultPersonDriverAgent {

	private Coordinate currentPosition;
	private Force force;

	/**
	 * @param p
	 * @param sim2d
	 */
	public Agent2D(Person p, Sim2D sim2d) {
		super(p, sim2d);
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
	public void moveTotPostion(Coordinate newPos) {
		// TODO check for choose next link and so on ...
		throw new RuntimeException("not yet implemented!!");
	}

}
