/* *********************************************************************** *
 * project: matsim
 * AgentSnapshotInfoFactory.java
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

package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

/**
 * translation of physical position (e.g. odometer distance on link, lane) into visualization position
 *
 * @author nagel
 */
public class AgentSnapshotInfoFactory {

	private static final double TWO_PI = 2.0 * Math.PI;
	private static final double PI_HALF = Math.PI / 2.0;
	private final SnapshotLinkWidthCalculator linkWidthCalculator;

	public AgentSnapshotInfoFactory(SnapshotLinkWidthCalculator widthCalculator) {
		this.linkWidthCalculator = widthCalculator;
	}

	public AgentSnapshotInfo createAgentSnapshotInfo(Id<Person> agentId, double easting, double northing, double elevation, double azimuth) {
		PositionInfo info = new PositionInfo();
		info.setId(agentId);
		info.setEasting(easting);
		info.setNorthing(northing);
		info.setAzimuth(azimuth);
		return info;
	}

	
	/**
	 * Generate snapshot info based on Link. 
	 * 
	 *  Comments:<ul>
	 *  <li>One could argue that this method should not know about Links at all,
	 * but it shortens code at several places, and since Link is a standard interface, I see no reason to not provide this
	 * as a service.
	 * </ul>
	 */
	public AgentSnapshotInfo createAgentSnapshotInfo(Id<Person> agentId, Link link, double distanceOnLink, int lane) {
		PositionInfo info = new PositionInfo() ;
		info.setId(agentId) ;
		info.setLinkId(link.getId());
		double lanePosition = this.linkWidthCalculator.calculateLanePosition(lane);
		calculateAndSetPosition(info, link.getFromNode().getCoord(), link.getToNode().getCoord(), distanceOnLink, link.getLength(), lanePosition );
		return info;
	}
	
	/**
	 *  creator based on Coord
	 * @param curveLength lengths are usually different (usually longer) than the euclidean distances between the startCoord and endCoord
	 */
	public AgentSnapshotInfo createAgentSnapshotInfo(Id<Person> agentId, Id<Vehicle> vehicleId, Id<Link> linkId, Coord startCoord, Coord endCoord, double distanceOnLink,
			int lane, double curveLength) {

		PositionInfo info = new PositionInfo() ;
		info.setId(agentId) ;
		info.setVehicleId(vehicleId);
		info.setLinkId(linkId);
		double lanePosition = this.linkWidthCalculator.calculateLanePosition(lane);
		Gbl.assertNotNull( startCoord );
		Gbl.assertNotNull( endCoord );
		calculateAndSetPosition(info, startCoord, endCoord, distanceOnLink, curveLength, lanePosition) ;
		return info;
	}
	
	
	/**
	 * 
	 * @param lanePosition may be null
	 */
	private static void calculateAndSetPosition(PositionInfo info, Coord startCoord, Coord endCoord, double odometerOnLink,
												double lengthOfCurve, double lanePosition){

		double dx = -startCoord.getX() + endCoord.getX();
		double dy = -startCoord.getY() + endCoord.getY();
		double theta;
		if (dx > 0) {
			theta = Math.atan(dy/dx);
		} else if (dx < 0) {
			theta = Math.PI + Math.atan(dy/dx);
		} else { // i.e. DX==0
			if (dy > 0) {
				theta = PI_HALF;
			} else if ( dy < 0 ) {
				theta = -PI_HALF;
			} else { // i.e. DX==0 && DY==0
				theta = 0.833*Math.PI ; // some default direction towards north north east 
			}
		}
		if (theta < 0.0) theta += TWO_PI;

		double euclideanLength = Math.sqrt( dx*dx + dy*dy ) ;
		// since we already have two atan, two cos and two sin in the method, it seems to make little sense to save on the sqrt. (?) kai, apr'16

		// "correction" is needed because link lengths are usually different (usually longer) than the Euklidean distances.
		// For the visualization, however, the vehicles are distributed in a straight line between the end points.
		// Since the simulation, on the other hand, reports odometer distances, this needs to be corrected.  kai, apr'10
		//
		// The same correction can be used for the orthogonal offsets.  kai, aug'10
		// That did not work so well in some cases.  Since the link width also seems to be plotted without that correction, let's
		// try to also do the "distance from link" without correction.   kai, feb'13
		double correction = 0. ;
		if ( lengthOfCurve != 0 ){
			correction = euclideanLength / lengthOfCurve;
		}
		
		info.setEasting( startCoord.getX() 
				+ (Math.cos(theta) * odometerOnLink * correction)
				+ (Math.sin(theta) * lanePosition ) ) ;
		
		info.setNorthing( startCoord.getY() 
				+ Math.sin(theta) * odometerOnLink  * correction 
				- Math.cos(theta) * lanePosition  );
		
		info.setAzimuth( theta / TWO_PI * 360. ) ;
	}

	public PositionInfoBuilder getAgentSnapshotInfoBuilder() {

		return new PositionInfoBuilder(this.linkWidthCalculator);
	}

	public static class PositionInfoBuilder {

		private final SnapshotLinkWidthCalculator linkWidthCalculator;

		private Id<Person> agentId = null;
		private Id<Link> linkId = null;
		private Id<Vehicle> vehicleId = null;
		private Coord fromCoord;
		private Coord toCoord;
		private double linkLength;
		private int lane;
		private double distanceOnLink;
		private AgentSnapshotInfo.AgentState agentState = null;
		private AgentSnapshotInfo.DrivingState drivingState = null;
		private double colorValue;
		private int user;

		PositionInfoBuilder(SnapshotLinkWidthCalculator linkWidthCalculator) {
			this.linkWidthCalculator = linkWidthCalculator;
		}

		public PositionInfoBuilder setPersonId(Id<Person> personId) {
			this.agentId = personId;
			return this;
		}

		public PositionInfoBuilder setLinkId(Id<Link> linkId) {
			this.linkId = linkId;
			return this;
		}

		public PositionInfoBuilder setVehicleId(Id<Vehicle> vehicleId) {
			this.vehicleId = vehicleId;
			return this;
		}

		public PositionInfoBuilder setAgentState(AgentSnapshotInfo.AgentState agentState) {
			this.agentState = agentState;
			return this;
		}

		public PositionInfoBuilder setDrivingState(AgentSnapshotInfo.DrivingState drivingState) {
			this.drivingState = drivingState;
			return this;
		}

		public PositionInfoBuilder setFromCoord(Coord fromCoord) {
			this.fromCoord = fromCoord;
			return this;
		}

		public PositionInfoBuilder setToCoord(Coord toCoord) {
			this.toCoord = toCoord;
			return this;
		}

		public PositionInfoBuilder setLinkLength(double linkLength) {
			this.linkLength = linkLength;
			return this;
		}

		public PositionInfoBuilder setLane(int lane) {
			this.lane = lane;
			return this;
		}

		public PositionInfoBuilder setDistanceOnLink(double distanceOnLink) {
			this.distanceOnLink = distanceOnLink;
			return this;
		}

		public PositionInfoBuilder setColorValue(double colorValue) {
			this.colorValue = colorValue;
			return this;
		}

		public PositionInfoBuilder setUser(int user) {
			this.user = user;
			return this;
		}

		public AgentSnapshotInfo build() {

			var theta = calculateTheta(this.fromCoord, this.toCoord);
			var euclideanLength = CoordUtils.calcEuclideanDistance(this.fromCoord, this.toCoord);
			var correction = calculateCorrection(euclideanLength, this.linkLength);
			var lanePosition = linkWidthCalculator.calculateLanePosition(lane);
			var easting = fromCoord.getX()
					+ (Math.cos(theta) * distanceOnLink * correction)
					+ (Math.sin(theta) * lane);
			var northing = fromCoord.getY()
					+ Math.sin(theta) * distanceOnLink  * correction
					- Math.cos(theta) * lanePosition;
			var azimuth = theta / TWO_PI * 360.;

			return new PositionInfo(
					this.agentId, this.linkId, this.vehicleId,
					easting, northing, azimuth,
					colorValue, agentState, drivingState, user);
		}

		private double calculateTheta(Coord startCoord, Coord endCoord) {

			double dx = -startCoord.getX() + endCoord.getX();
			double dy = -startCoord.getY() + endCoord.getY();
			double theta;
			if (dx > 0) {
				theta = Math.atan(dy/dx);
			} else if (dx < 0) {
				theta = Math.PI + Math.atan(dy/dx);
			} else { // i.e. DX==0
				if (dy > 0) {
					theta = PI_HALF;
				} else if ( dy < 0 ) {
					theta = -PI_HALF;
				} else { // i.e. DX==0 && DY==0
					theta = 0.833*Math.PI ; // some default direction towards north north east
				}
			}
			if (theta < 0.0) theta += TWO_PI;

			return theta;
		}

		private double calculateCorrection(double euclideanLength, double curvedLength) {
			return curvedLength != 0 ? euclideanLength / curvedLength : 0;
		}
	}

	/**
	 * A helper class to store information about agents (id, position, speed), mainly used to create
	 * {@link SnapshotWriter snapshots}.  It also provides a way to convert graph coordinates (linkId, offset) into
	 * Euclidean coordinates.  Also does some additional coordinate shifting (e.g. to the "right") to improve visualization.
	 * In contrast to earlier versions of this comment, it does _not_ define a physical position of particles in the queue model;
	 * that functionality needs to be provided elsewhere.
	 *
	 * @author mrieser, knagel
	 */
	 private static class PositionInfo implements AgentSnapshotInfo {

		private Id<Person> agentId = null;
		private Id<Link> linkId = null;
		private Id<Vehicle> vehicleId = null;
		private double easting = Double.NaN;
		private double northing = Double.NaN;
		private double azimuth = Double.NaN;
		private double colorValue = 0;
		private AgentState agentState = null;
		private DrivingState drivingState = null;
		private int user = 0;

		private PositionInfo(Id<Person> agentId, Id<Link> linkId, Id<Vehicle> vehicleId, double easting, double northing, double azimuth, double colorValue, AgentState agentState, DrivingState drivingState, int user) {
			this.agentId = agentId;
			this.linkId = linkId;
			this.vehicleId = vehicleId;
			this.easting = easting;
			this.northing = northing;
			this.azimuth = azimuth;
			this.colorValue = colorValue;
			this.agentState = agentState;
			this.drivingState = drivingState;
			this.user = user;
		}

		/* package-private */ PositionInfo() { }



		@Override
		public final Id<Person> getId() {
			return this.agentId;
		}
		public final void setId( Id<Person> tmp ) {
			this.agentId = tmp ;
		}

		@Override
		public final Id<Link> getLinkId() {
			return this.linkId;
		}
		public final void setLinkId( Id<Link> tmp ) {
			this.linkId = tmp ;
		}

		@Override
		public final Id<Vehicle> getVehicleId() {return vehicleId;}
		public final void setVehicleId(Id<Vehicle> vehicleId) {this.vehicleId = vehicleId;}

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

		@Override
		public final DrivingState getDrivingState() { return this.drivingState; 		}
		public final void setDrivingState(DrivingState drivingState) { this.drivingState = drivingState; }

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
}
