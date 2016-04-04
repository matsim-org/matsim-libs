/* *********************************************************************** *
 * project: org.matsim.*
 * PositionInfo.java
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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * A helper class to store information about agents (id, position, speed), mainly used to create
 * {@link SnapshotWriter snapshots}.  It also provides a way to convert graph coordinates (linkId, offset) into
 * Euclidean coordinates.  Also does some additional coordinate shifting (e.g. to the "right") to improve visualization.
 * In contrast to earlier versions of this comment, it does _not_ define a physical position of particles in the queue model;
 * that functionality needs to be provided elsewhere.
 *
 * @author mrieser, knagel
 */
public class PositionInfo implements AgentSnapshotInfo {

	private Id<Person> agentId = null;
	private double easting = Double.NaN;
	private double northing = Double.NaN;
	private double azimuth = Double.NaN;
	private double colorValue = 0;
	private AgentState agentState = null;
	private Id<Link> linkId = null;
	private int user = 0;

	/* package-private */ PositionInfo() { }

	@Override
	public final Id<Person> getId() {
		return this.agentId;
	}
	public final void setId( Id<Person> tmp ) {
		this.agentId = tmp ;
	}

	@Override
	public final double getEasting() {
		return this.easting;
	}
	public final void setEasting( double tmp ) {
		this.easting = tmp ;
	}

	@Override
	public final double getNorthing() {
		return this.northing;
	}
	public final void setNorthing( double tmp ) {
		this.northing = tmp ;
	}

	@Override
	public final double getAzimuth() {
		return this.azimuth;
	}
	public final void setAzimuth( double tmp ) {
		this.azimuth = tmp ;
	}

	@Override
	public final double getColorValueBetweenZeroAndOne() {
		return this.colorValue;
	}
	@Override
	public final void setColorValueBetweenZeroAndOne( double tmp ) {
		this.colorValue = tmp ;
	}

	@Override
	public final AgentState getAgentState(){
		return this.agentState;
	}
	@Override
	public final void setAgentState( AgentState state ) {
		this.agentState = state ;
	}

	public final Id<Link> getLinkId() {
		return this.linkId;
	}
	public final void setLinkId( Id<Link> tmp ) {
		this.linkId = tmp ;
	}

	@Override
	public int getUserDefined() {
		return this.user;
	}
	@Override
	public void setUserDefined( int tmp ) {
		this.user = tmp ;
	}

	@Override
	public String toString() {
		return "PositionInfo; agentId: " + this.agentId.toString()
		+ " easting: " + this.easting
		+ " northing: " + this.northing ;
	}

}
