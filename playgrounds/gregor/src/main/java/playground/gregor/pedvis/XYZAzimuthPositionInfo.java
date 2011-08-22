/* *********************************************************************** *
 * project: org.matsim.*
 * XYZAzimuthPositionInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.pedvis;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;

import playground.gregor.sim2d_v2.helper.GEO;

import com.vividsolutions.jts.geom.Coordinate;

public class XYZAzimuthPositionInfo implements AgentSnapshotInfo {


	private final Coordinate c;
	private final double time;
	private final Id id;
	private final double vx;
	private final double vy;

	public XYZAzimuthPositionInfo(Id id,Coordinate c, double vx, double vy, double time) {
		this.c = c;
		this.vx = vx;
		this.vy = vy;
		this.time = time;
		this.id = id;
	}



	@Override
	public AgentState getAgentState() {
		return AgentState.PERSON_OTHER_MODE;
	}

	@Override
	public double getAzimuth() {
		return GEO.getAzimuth(this.vx, this.vy);
	}

	@Override
	public double getColorValueBetweenZeroAndOne() {
		//		int id = Integer.parseInt(this.id.toString());
		int id = this.id.hashCode();
		MatsimRandom.reset(id);
		return MatsimRandom.getRandom().nextDouble();
	}

	@Override
	public double getEasting() {
		return this.c.x;
	}

	@Override
	public double getNorthing() {
		return this.c.y;
	}

	@Override
	public int getType() {
		//		int id = Integer.parseInt(this.id.toString());
		int id = this.id.hashCode();
		return id <= 2519 ? 1 : id <= 2639 ? 30 : 254;
	}

	@Override
	public int getUserDefined() {
		//		int id = Integer.parseInt(this.id.toString());
		int id = this.id.hashCode();
		return id <= 2519 ? 1 : id <= 2639 ? 30 : 254;
	}

	@Override
	public void setAgentState(AgentState state) {
		throw new RuntimeException("not yet implemented");

	}

	@Override
	public void setColorValueBetweenZeroAndOne(double tmp) {
		throw new RuntimeException("not yet implemented");

	}

	@Override
	public void setType(int tmp) {
		throw new RuntimeException("not yet implemented");

	}

	@Override
	public void setUserDefined(int tmp) {
		throw new RuntimeException("not yet implemented");

	}

	@Override
	public Id getId() {
		return this.id;
	}

}
