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

package org.matsim.utils.vis.snapshots.writers;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.utils.geometry.CoordI;

/**
 * A helper class to store information about agents (id, position, speed), mainly used to create
 * {@link SnapshotWriterI snapshots}.
 *
 * @author mrieser
 */
public class PositionInfo {
	public enum VehicleState {Driving, Parking};

	private static final double LANE_WIDTH = 3.75;//TODO lane width is no longer static but it is defined in network. The question is,
																						//how to get this information here? One possibility is to use Gbl ... but I suppose not everyone
																						//would be comfortable with this idea? Is there a solution to do this in a strict OO manner 
																						//(without a static Gbl)? [GL] - april 08 
	private static final double PI_HALF = Math.PI / 2.0;
	private static final double TWO_PI = 2.0 * Math.PI;

	final private Id agentId;

	final private double easting;
	final private double northing;
	final private double elevation;
	final private double azimuth;
	final private double distanceOnLink;
	final private String visualizerData;

	final private double speed;

	final private VehicleState vehicleState;
	final private Link link;



	// the constructor does all the work:
	/**
	 * Creates a new PositionInfo based on the agent's position between to nodes.
	 *
	 * @param agentId The id of the agent.
	 * @param link The link the vehicle is currently driving or parking on.
	 * @param distanceOnLink The distance of the agent from the fromNode of the link (measured on the link's real length, not its euklidean length)
	 * @param lane The number of the lane the agent is on.
	 * 		Lanes are counted from the middle of a bi-directional link, beginning with 1.
	 * @param speed The speed the agent is traveling with.
	 * @param vehicleState The state of the vehicle (Parking,Driving)
	 * @param visualizerData additional data (null allowed) that may be used by some visualizers
	 */
	public PositionInfo(final Id agentId, final Link link, final double distanceOnLink, final int lane, final double speed, final VehicleState vehicleState, final String visualizerData) {
		this.agentId = agentId;
		this.link = link;
		this.speed = speed;
		this.distanceOnLink = distanceOnLink;
		CoordI fromCoord = link.getFromNode().getCoord();
		double dx = -fromCoord.getX() + link.getToNode().getCoord().getX();
		double dy = -fromCoord.getY() + link.getToNode().getCoord().getY();
		double theta = 0.0;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else {
				theta = -PI_HALF;
			}
		}
		if (theta < 0.0) theta += TWO_PI;
		double correction = link.getEuklideanDistance() / link.getLength();
		this.easting  = fromCoord.getX() + Math.cos(theta) * distanceOnLink * correction + Math.sin(theta) * LANE_WIDTH * lane;
		this.northing = fromCoord.getY() + Math.sin(theta) * distanceOnLink * correction - Math.cos(theta) * LANE_WIDTH * lane;
		this.elevation = 0.0;
		this.azimuth = theta / (TWO_PI) * 360;
		this.vehicleState = vehicleState;
		this.visualizerData = visualizerData;
	}

	/**
	 * Creates a new PositionInfo with the specified position and speed.
	 *
	 * @param driverId
	 * @param easting
	 * @param northing
	 * @param elevation
	 * @param azimuth
	 * @param speed
	 * @param vehicleState The state of the vehicle (Parking, Driving)
	 * @param visualizerData additional data (null allowed) that may be used by some visualizers
	 */
	public PositionInfo(final Id driverId, final double easting, final double northing, final double elevation, final double azimuth, final double speed, final VehicleState vehicleState, final String visualizerData) {
		this.agentId = driverId;
		this.link = null;
		this.easting = easting;
		this.northing = northing;
		this.elevation = elevation;
		this.azimuth = azimuth;
		this.speed = speed;
		this.vehicleState = vehicleState;
		this.distanceOnLink = 0.0; // is unknown
		this.visualizerData = visualizerData;
	}

	public Id getAgentId() {
		return this.agentId;
	}

	public double getEasting() {
		return this.easting;
	}

	public double getNorthing() {
		return this.northing;
	}

	public double getElevation() {
		return this.elevation;
	}

	public double getAzimuth() {
		return this.azimuth;
	}

	public double getSpeed() {
		return this.speed;
	}

	public VehicleState getVehicleState(){
		return this.vehicleState;
	}

	public Link getLink() {
		return this.link;
	}

	public double getDistanceOnLink() {
		return this.distanceOnLink;
	}

	public String getVisualizerData() {
		return this.visualizerData;
	}

}
