/* *********************************************************************** *
 * project: org.matsim.*
 * PositionOnLineStringInfo.java
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

package playground.gregor.snapshots.writers;

import java.util.Map.Entry;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;

import playground.gregor.collections.gnuclasspath.TreeMap;

import com.vividsolutions.jts.geom.LineSegment;

public class GregorsPositionInfo extends PositionInfo {
	
	

		


	//	public enum VehicleState {Driving, Parking};
	private static final double LANE_WIDTH = 0.71;//TODO lane width is no longer static but it is defined in network. The question is,
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
	
	
	
	public GregorsPositionInfo(final Id agentId, final Link link, final double distanceOnLink, final int lane, final double speed,
			final VehicleState vehicleState, final String visualizerData, final LineStringTree lsTree) {
		super(agentId, 0, 0, 0, 0, 0, PositionInfo.VehicleState.Driving,	null);
		this.agentId = agentId;
		this.link = link;
		final TreeMap<Double,LineSegment> lsMap = lsTree.getTreeMap(link.getId().toString());
		
		
		final Entry<Double, LineSegment> e  = lsMap.floorEntry(distanceOnLink);
		
		final double distanceOnSegment = distanceOnLink - e.getKey();
		final LineSegment ls = e.getValue();
		final double dx = -ls.p0.x   + ls.p1.x;
		final double dy = -ls.p0.y   + ls.p1.y;		
		
		this.speed = speed;
		this.distanceOnLink = distanceOnLink;
//		final CoordI fromCoord = link.getFromNode().getCoord();

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
		this.easting  = ls.p0.x + Math.cos(theta) * distanceOnSegment  + Math.sin(theta) * LANE_WIDTH * lane;
		this.northing = ls.p0.y + Math.sin(theta) * distanceOnSegment - Math.cos(theta) * LANE_WIDTH * lane;
		this.elevation = 0.0;
		this.azimuth = theta / (TWO_PI) * 360;
		this.vehicleState = vehicleState;
		this.visualizerData = visualizerData;		

		
	}

//	public GregorsPositionInfo(final Id driverId, final double easting, final double northing, final double elevation, final double azimuth, final double speed, final VehicleState vehicleState, final String visualizerData) {
//		this.agentId = driverId;
//		this.link = null;
//		this.easting = easting;
//		this.northing = northing;
//		this.elevation = elevation;
//		this.azimuth = azimuth;
//		this.speed = speed;
//		this.vehicleState = vehicleState;
//		this.distanceOnLink = 0.0; // is unknown
//		this.visualizerData = visualizerData;
//	}
	

	@Override
	public Id getAgentId() {
		return this.agentId;
	}


	@Override
	public double getEasting() {
		return this.easting;
	}


	@Override
	public double getNorthing() {
		return this.northing;
	}


	@Override
	public double getElevation() {
		return this.elevation;
	}


	@Override
	public double getAzimuth() {
		return this.azimuth;
	}


	@Override
	public double getSpeed() {
		return this.speed;
	}


	@Override
	public VehicleState getVehicleState(){
		return this.vehicleState;
	}


	@Override
	public Link getLink() {
		return this.link;
	}


	@Override
	public double getDistanceOnLink() {
		return this.distanceOnLink;
	}


	@Override
	public String getVisualizerData() {
		return this.visualizerData;
	}

}
