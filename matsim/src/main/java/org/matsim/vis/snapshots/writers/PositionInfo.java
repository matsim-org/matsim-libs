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

package org.matsim.vis.snapshots.writers;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

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
	
	private static final long serialVersionUID = -8168936182264521667L;

	private Id agentId;
	private double easting;
	private double northing;
	private double elevation;
	private double azimuth;
	private double colorValue;
	private AgentState agentState;
	private Id linkId;
	private int type = 0;
	private int user = 0;
	
	/* package-private */ PositionInfo() { }
	
//	/**
//	 * This is here so that the data class can be initialized in an incomplete state.  
//	 */
//	/* package-private */ PositionInfo( final Id agentId ) {
//		this.agentId = agentId ;
//	}
	

//	/* package-private */ PositionInfo(final Id driverId, final double easting, final double northing, final double elevation, final double azimuth ) {
//		this.agentId = driverId;
//		this.linkId = null;
//		this.easting = easting;
//		this.northing = northing;
//		this.elevation = elevation;
//		this.azimuth = azimuth;
//	}


//	/**
//	 * Creates a new PositionInfo with the specified position and speed.
//	 *
//	 * @param driverId
//	 * @param easting
//	 * @param northing
//	 * @param elevation
//	 * @param azimuth
//	 * @param speed
//	 * @param agentState The state of the vehicle (Parking, Driving)
//	 */
//	protected PositionInfo(final Id driverId, final double easting, final double northing, final double elevation, final double azimuth, final double speed, final AgentState agentState) {
//		this( driverId, easting, northing, elevation, azimuth ) ;
//		this.speed = speed;
//		this.agentState = agentState;
//	}
	
	public final Id getId() {
		return this.agentId;
	}
	public final void setId( Id tmp ) {
		this.agentId = tmp ;
	}

	public final double getEasting() {
		return this.easting;
	}
	public final void setEasting( double tmp ) {
		this.easting = tmp ;
	}

	public final double getNorthing() {
		return this.northing;
	}
	public final void setNorthing( double tmp ) {
		this.northing = tmp ;
	}

	public final double getElevation() {
		return this.elevation;
	}
	public final void setElevation( double tmp ) {
		this.elevation = tmp ;
	}

	public final double getAzimuth() {
		return this.azimuth;
	}
	public final void setAzimuth( double tmp ) {
		this.azimuth = tmp ;
	}

	public final double getColorValueBetweenZeroAndOne() {
		return this.colorValue;
	}
	public final void setColorValueBetweenZeroAndOne( double tmp ) {
		this.colorValue = tmp ;
	}

	public final AgentState getAgentState(){
		return this.agentState;
	}
	public final void setAgentState( AgentState state ) {
		this.agentState = state ;
	}

	public final Id getLinkId() {
		return this.linkId;
	}
	public final void setLinkId( Id tmp ) {
		this.linkId = tmp ;
	}

	@Deprecated // yyyy I don't know what this is.  kai, jan'10
	public int getType() {
		return this.type;
	}
	
	@Deprecated // yyyy I don't know what this is.  kai, jan'10
	public void setType( int tmp ) {
		this.type = tmp ;
	}

	public int getUserDefined() {
		return this.user;
	}
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
