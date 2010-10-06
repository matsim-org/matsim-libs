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

package org.matsim.vis.snapshots.writers;

import java.awt.geom.Point2D;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;
import org.matsim.vis.vecmathutils.VectorUtils;

/**
 * @author nagel
 *
 */
public class AgentSnapshotInfoFactory {

	private Scenario sc = null ;
	private static final double TWO_PI = 2.0 * Math.PI;
	private static final double PI_HALF = Math.PI / 2.0;
	/**
	 * Distance (in m) between the two innermost opposing lanes. Setting this to a larger number (e.g. 30) lets you see the
	 * direction in which cars are going. (Setting this to a negative number probably gives you "driving on the left", but lanes
	 * will be wrong (could be fixed), and, more importantly, the simulation still assumes "driving on the right" when locations
	 * (with coordinates) are connected to links.) TODO should be configurable
	 */
	private static final double WIDTH_OF_MEDIAN = 30. ; // default

	private static double LANE_WIDTH = 3.75;
	public static void setLaneWidth( double dd ) {
		LANE_WIDTH = dd ;
	}

	//TODO lane width is no longer static but it is defined in network. The question is,
	//how to get this information here? One possibility is to use Gbl ... but I suppose not everyone
	//would be comfortable with this idea? Is there a solution to do this in a strict OO manner
	//(without a static Gbl)? [GL] - april 08
	//
	// The design is now in a way that this could, in principle, be fixed.  kai, aug'10

	/**
	 * default factory that is here so that the static methods can use the non-static "calculatePosition".  kai, aug'10
	 */
	private static final AgentSnapshotInfoFactory defaultFactory = new AgentSnapshotInfoFactory( null ) ;

	public AgentSnapshotInfoFactory( Scenario sc ) {
		this.sc = sc ;
	}

	// static creators based on x/y

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, double easting, double northing, double elevation, double azimuth) {
		PositionInfo info = new PositionInfo() ;
		info.setId( agentId ) ;
		info.setEasting( easting ) ;
		info.setNorthing( northing ) ;
		info.setAzimuth( azimuth ) ;
		return info ;
	}

	@Deprecated // in my view, use creational method with shorter argument list (set colorValue, agentState separately).  kai, aug'10
	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, double easting, double northing, double elevation, double azimuth,
			double colorValue, AgentState agentState)
	{
		PositionInfo info = new PositionInfo() ;
		info.setId( agentId ) ;
		info.setEasting( easting ) ;
		info.setNorthing( northing ) ;
		info.setAzimuth( azimuth ) ;
		info.setColorValueBetweenZeroAndOne(colorValue) ;
		info.setAgentState( agentState ) ;
		return info ;
	}

	// static creators based on link

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link) {
		return staticCreateAgentSnapshotInfo( agentId, link, 0 ) ;
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, int cnt) {
		return staticCreateAgentSnapshotInfo( agentId, link, 0.9*link.getLength(), cnt ) ;
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane) {
		return staticCreateAgentSnapshotInfo( agentId, link, distanceOnLink, lane, 0 ) ;
//		return new PositionInfo(agentId, link, distanceOnLink, lane);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane, int cnt) {
		return staticCreateAgentSnapshotInfo( 1.0, agentId, link, distanceOnLink, lane+2*cnt, 0., null ) ;
//		return new PositionInfo(agentId, link, distanceOnLink, lane, cnt);
	}

	@Deprecated // in my view, use creational method with shorter argument list (set colorValue, agentState separately).  kai, aug'10
	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane, double colorValue,
			AgentState agentState)
	{
		return staticCreateAgentSnapshotInfo( 1.0, agentId, link, distanceOnLink, lane, colorValue, agentState ) ;
//		return new PositionInfo(agentId, link, distanceOnLink, lane, speed, agentState);
	}

	@Deprecated // in my view, use creational method with shorter argument list (set colorValue, agentState separately).  kai, aug'10
	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(double linkScale, Id agentId, Link link, double distanceOnLink, int lane,
			double colorValue, AgentState agentState)
	{
		PositionInfo info = new PositionInfo() ;
		info.setId( agentId ) ;
		defaultFactory.calculatePosition( info, link, linkScale, distanceOnLink, lane ) ;
		info.setColorValueBetweenZeroAndOne( colorValue ) ;
		info.setAgentState( agentState ) ;
		return info ;
//		return new PositionInfo(linkScale, agentId, link, distanceOnLink, lane, speed, agentState);
	}

	protected final void calculatePosition(PositionInfo info, Link link, double linkScale, double distanceOnLink, int lane){
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
		// Since the simulation, on the other hand, reports odometer distances, this needs to be corrected.  kai, apr'10
		// The same correction can be used for the orthogonal offsets.  kai, aug'10
		double correction = 0. ;
		if ( link.getLength() != 0 ){
			if (link instanceof LinkImpl) {
				correction = ((LinkImpl)link).getEuklideanDistance() / link.getLength();
			} else  {
				correction = CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord()) / link.getLength();
			}
		}

		// "link scale" is not from me.  Presumably, it "pulls back" the drawing of the vehicles from the nodes on
		// both ends.  kai, apr'10
		if (linkScale != 1.0) {
			Tuple<Point2D.Double, Point2D.Double> scaledLinkTuple = VectorUtils.scaleVector(linkStart, linkEnd, linkScale);
			linkStart = scaledLinkTuple.getFirst();
		}

		info.setEasting( linkStart.getX() + Math.cos(theta) * (distanceOnLink * linkScale) * correction
		                + Math.sin(theta) * (0.5*WIDTH_OF_MEDIAN + LANE_WIDTH*lane)*correction ) ;
		info.setNorthing( linkStart.getY() + Math.sin(theta) * (distanceOnLink * linkScale) * correction
		                - Math.cos(theta) * (0.5*WIDTH_OF_MEDIAN + LANE_WIDTH*lane)*correction );
		info.setAzimuth( theta / TWO_PI * 360. ) ;
	}

}
