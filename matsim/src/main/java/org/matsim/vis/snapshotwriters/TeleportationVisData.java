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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author dgrether
 * 
 *
 */
public class TeleportationVisData implements AgentSnapshotInfo {

	private final double startX;
	private final double startY;
	private double currentX;
	private double currentY;
	private final double starttime;
	private final Id<Person> agentId;
	private int userDefined;
	private double colorval;
	private final AgentState state = AgentSnapshotInfo.AgentState.PERSON_OTHER_MODE;
	private final int intX;
	private final int intY;
	private final double endX;
	private final double endY;
	private final double travelTime;
	private final static int offset = 100;

	public TeleportationVisData(double now, Id<Person> personId, Coord fromCoord, Coord toCoord, double travelTime) {
		this.starttime = now;
		this.travelTime = travelTime;
		this.agentId = personId;
		this.startX = fromCoord.getX();
		this.startY = fromCoord.getY();
		this.endX = toCoord.getX();
		this.endY = toCoord.getY();
		this.currentX = startX;
		this.currentY = startY;

		// the following is there to somewhat shift teleported agents.  So that they are not exactly on top of each other when they have
		// exactly the same dp time and destination ... as may happen e.g. for teleported transit walk and illustrative examples. kai, apr'16
		String idstr = personId.toString();
		int hashCode = idstr.hashCode();
		intX = hashCode%offset;
		hashCode -= intX;
		hashCode /= offset;
		intY = hashCode%offset;
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
	public Id<Person> getId(){
		return this.agentId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		throw new RuntimeException("not yet implemented!");
	}

	@Override
	public Id<Link> getLinkId() {
		throw new RuntimeException("not yet implemented!");
	}

	@Override
	public DrivingState getDrivingState() {
		throw new RuntimeException("not yet implemented!");
	}

	public final void updatePosition(double time) {
		double frac = (time - starttime) / travelTime;
		this.currentX = (1. - frac) * this.startX + frac * this.endX + 0.1 * (intX - offset / 2.);
		this.currentY = (1. - frac) * this.startY + frac * this.endY + 0.1 * (intY - offset / 2.);
	}

	@Override
	public AgentState getAgentState() {
		return this.state;
	}

	@Override
	public double getAzimuth() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getColorValueBetweenZeroAndOne() {
		return this.colorval;
	}

	@Override
	public int getUserDefined() {
		return this.userDefined;
	}
}
