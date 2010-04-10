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

import java.awt.geom.Point2D;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vis.otfvis.handler.OTFDefaultLinkHandler;
import org.matsim.vis.vecmathutils.VectorUtils;

/**
 * This provides a position for the visualizer. Since the QueueSimulation has no physics these coordinates
 * are meant for the visualizer and not necessary meaningful in engineering terms.
 * A helper class to store information about agents (id, position, speed), mainly used to create
 * {@link SnapshotWriter snapshots}.
 *
 * @author mrieser, knagel
 */
public class PositionInfo implements AgentSnapshotInfo {
	
	// yyyy presumably, this should become a factory:
	//   PositionInfoFactory factory = new PositionInfoFactory() ;
 	//   factory.setXYZ( ... ) ;
	//   ...
	//   AgentSnapshotInfo info = factory.createAgentSnapshotInfo() ;
	// ??? kai, apr'10
	
	private static final double LANE_WIDTH = 3.75;
	//TODO lane width is no longer static but it is defined in network. The question is,
	//how to get this information here? One possibility is to use Gbl ... but I suppose not everyone
	//would be comfortable with this idea? Is there a solution to do this in a strict OO manner
	//(without a static Gbl)? [GL] - april 08
	//
	// yyyy Could put this as a subclass into "Network".  That's probably where it belongs: convert "position on graph" to
	// "x/y-positions". Kai

	/**
	 * Distance (in m) between the two innermost opposing lanes. Setting this to a larger number (e.g. 30) lets you see the direction
	 * in which cars are going.  (Setting this to a negative number probably gives you "driving on the left", but lanes will be wrong
	 * (could be fixed), and, more importantly, the simulation still assumes "driving on the right" when coordinates are connected
	 * to links.
	 *
	 * TODO should be configurable
	 */
	private static final double WIDTH_OF_MEDIAN = 30. ;

	private static final double PI_HALF = Math.PI / 2.0;
	
	private static final double TWO_PI = 2.0 * Math.PI;

	final private Id agentId;

	private double easting;
	
	private double northing;
	
	private double elevation;
	
	private double azimuth;
	
	private double speed;

	private AgentState agentState;
	
	final private Id linkId;

	private int type = 0;

	private int user = 0;

	public PositionInfo( final Id agentId, final Link link ) {
		this( agentId, link, 0 ) ;
	}
	
	/**
	 * Uses PositionInfo to generate a position for facilities. This looks like an abuse of PositionInfo only because it was made
	 * quite vehicle-oriented over the last years.
	 * 
	 * @param cnt used to calculate offset from link to place items side-by-side rather than on top of each other. Use 0 if you
	 *            don't know.
	 */
	public PositionInfo( final Id agentId, final Link link, int cnt ) {
		this( agentId, link, 0.9*link.getLength(), 10+2*cnt, 0., AgentState.PERSON_AT_ACTIVITY) ;
	}

	/**
	 * Use when you have something moving on a link.
	 */
	public PositionInfo(final Id agentId, final Link link, final double distanceOnLink, final int lane ) {
		this( agentId, link, distanceOnLink, lane, 0 ) ;
	}

	/**
	 * Use when you have something moving on a link.
	 * 
	 * @param cnt is used to "count" persons in a vehicle to possibly offset them to the side.  Not supported without code modification. kai, apr'10
	 */
	public PositionInfo(final Id agentId, final Link link, final double distanceOnLink, final int lane, final int cnt ) {
		this.agentId = agentId;
		this.linkId = link.getId();
		this.calculatePosition(link, OTFDefaultLinkHandler.LINK_SCALE, distanceOnLink, lane + 2*cnt );
	}

	public PositionInfo(final Id driverId, final double easting, final double northing, final double elevation, final double azimuth ) {
		this.agentId = driverId;
		this.linkId = null;
		this.easting = easting;
		this.northing = northing;
		this.elevation = elevation;
		this.azimuth = azimuth;
	}

	/**
	 * Creates a new PositionInfo based on the agent's position between to nodes.
	 *
	 * @param agentId The id of the agent.
	 * @param link The link the vehicle is currently driving or parking on.
	 * @param distanceOnLink The distance of the agent from the fromNode of the link (measured on the link's real length, not its Euclidean length)
	 * @param lane The number of the lane the agent is on.
	 * 		Lanes are counted from the middle of a bi-directional link, beginning with 1.
	 * @param speed The speed the agent is traveling with.
	 * @param vehicleState The state of the vehicle (Parking,Driving)
	 */
	@Deprecated // in my view, use shorter constructors.  kai, jan'10
	public PositionInfo(final Id agentId, final Link link, final double distanceOnLink, final int lane, final double speed, final AgentState vehicleState ) {
		this.agentId = agentId;
		this.linkId = link.getId();
		this.calculatePosition(link, OTFDefaultLinkHandler.LINK_SCALE, distanceOnLink, lane);
		this.speed = speed;
		this.agentState = vehicleState;
	}
	
	/**
	 * Creates a new PositionInfo based on the agent's position between to nodes
	 * and scales the position by the given scale parameter.
	 * @param agentId The id of the agent.
	 * @param link The link the vehicle is currently driving or parking on.
	 * @param distanceOnLink The distance of the agent from the fromNode of the link (measured on the link's real length, not its Euklidean length)
	 * @param lane The number of the lane the agent is on.
	 * 		Lanes are counted from the middle of a bi-directional link, beginning with 1.
	 * @param speed The speed the agent is traveling with.
	 * @param agentState The state of the vehicle (Parking,Driving)
	 */
	@Deprecated // (1) try to use shorter constructors.  (2) I don't think it makes sense to hand linkScale as a method into positionInfo; it should come frome somewhere else. kai, apr'10
	public PositionInfo(double linkScale, final Id agentId, final Link link, final double distanceOnLink, final int lane, final double speed, final AgentState agentState) {
		this( linkScale, agentId, link, distanceOnLink, lane ) ;
		this.speed = speed;
		this.agentState = agentState;
	}

	@Deprecated //  I don't think it makes sense to hand linkScale as a method into positionInfo; it should come from somewhere else. kai, apr'10
	public PositionInfo(double linkScale, final Id agentId, final Link link, final double distanceOnLink, final int lane ) {
		this.agentId = agentId;
		this.linkId = link.getId();
		this.calculatePosition(link, linkScale, distanceOnLink, lane);
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
	 */
	@Deprecated // please try to use the shorter constructors.  kai, apr'10
	public PositionInfo(final Id driverId, final double easting, final double northing, final double elevation, final double azimuth, final double speed, final AgentState vehicleState) {
		this( driverId, easting, northing, elevation, azimuth ) ;
		this.speed = speed;
		this.agentState = vehicleState;
	}
	
	private void calculatePosition(Link link, double linkScale, double distanceOnLink, int lane){
		Point2D.Double linkStart = new Point2D.Double(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Point2D.Double linkEnd = new Point2D.Double(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		double dx = -linkStart.getX() + linkEnd.getX();
		double dy = -linkStart.getY() + linkEnd.getY();
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
		
		// "correction" is needed because link lengths are usually different (usually longer) than the Euklidean distances.
		// For the visualization, however, the vehicles are distributed in a straight line between the end points.
		// Since the simulation, on the other hand, reports odometer distances, this needs to be corrected.
		// (And this is, in my view, the right place to correct this.)  kai, apr'10
		double correction = 0. ;
		if ( link.getLength() != 0 ){
			correction = ((LinkImpl)link).getEuklideanDistance() / link.getLength();
		}
		
		// "link scale" is not from me.  Presumably, it "pulls back" the drawing of the vehicles from the nodes on
		// both ends.  kai, apr'10
		if (linkScale != 1.0) {
			Tuple<Point2D.Double, Point2D.Double> scaledLinkTuple = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
			linkStart = scaledLinkTuple.getFirst();
		}
		
		this.easting  = linkStart.getX() + Math.cos(theta) * (distanceOnLink * linkScale) * correction
		                + Math.sin(theta) * (0.5*WIDTH_OF_MEDIAN + LANE_WIDTH * lane ) ;
		this.northing = linkStart.getY() + Math.sin(theta) * (distanceOnLink * linkScale) * correction
		                - Math.cos(theta) * (0.5*WIDTH_OF_MEDIAN + LANE_WIDTH * lane ) ;
		this.elevation = 0.0;
		this.azimuth = theta / (TWO_PI) * 360;
	}

	public Id getId() {
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

	public double getColorValueBetweenZeroAndOne() {
		return this.speed;
	}
	public void setColorValueBetweenZeroAndOne( double spd ) {
		this.speed = spd ;
	}

	public AgentState getAgentState(){
		return this.agentState;
	}
	public void setAgentState( AgentState state ) {
		this.agentState = state ;
	}

	public Id getLinkId() {
		return this.linkId;
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

}
