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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author nagel
 *
 */
public class AgentSnapshotInfoFactory {

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

	public AgentSnapshotInfoFactory() {
	}

	// static creators based on x/y

	public static AgentSnapshotInfo createAgentSnapshotInfo(Id agentId, double easting, double northing, double elevation, double azimuth) {
		PositionInfo info = new PositionInfo() ;
		info.setId( agentId ) ;
		info.setEasting( easting ) ;
		info.setNorthing( northing ) ;
		info.setAzimuth( azimuth ) ;
		return info ;
	}

	// static creators based on link
	public static AgentSnapshotInfo createAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane) {
		if (link instanceof LinkImpl){ //as for LinkImpl instances the euklidean distance is already computed we can safe computing time but have a cast instead
			PositionInfo info = new PositionInfo() ;
			info.setId(agentId) ;
			calculateAndSetPosition(info, link.getFromNode().getCoord(), link.getToNode().getCoord(),
					distanceOnLink, link.getLength(), ((LinkImpl)link).getEuklideanDistance(), lane);
			return info;
		}
		return createAgentSnapshotInfo(agentId, link.getFromNode().getCoord(), link.getToNode().getCoord(), distanceOnLink, lane, link.getLength());
	}
	
	/**
	 * Static creator based on Coord
	 * @param lengthOfVector lengths are usually different (usually longer) than the Euklidean distances between the startCoord and endCoord
	 */
	public static AgentSnapshotInfo createAgentSnapshotInfo(Id agentId, Coord startCoord, Coord endCoord, double distanceOnLink, 
			int lane, double lengthOfVector) {
		PositionInfo info = new PositionInfo() ;
		info.setId(agentId) ;
		double euklideanDistance = CoordUtils.calcDistance(startCoord, endCoord);
		calculateAndSetPosition(info, startCoord, endCoord,
				distanceOnLink, lengthOfVector, euklideanDistance, lane) ;
		return info;
	}
	
	

	private static final void calculateAndSetPosition(PositionInfo info, Coord startCoord, Coord endCoord, double distanceOnVector, double lengthOfVector, double euklideanDistance, int lane){
		double dx = -startCoord.getX() + endCoord.getX();
		double dy = -startCoord.getY() + endCoord.getY();
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
		if ( lengthOfVector != 0 ){
			correction = euklideanDistance / lengthOfVector;
		}

		info.setEasting( startCoord.getX() 
				+ (Math.cos(theta) * distanceOnVector * correction)
				+ (Math.sin(theta) * (0.5 * WIDTH_OF_MEDIAN + LANE_WIDTH * lane) * correction) ) ;
		
		info.setNorthing( startCoord.getY() 
				+ Math.sin(theta) * distanceOnVector  * correction 
				- Math.cos(theta) * (0.5 * WIDTH_OF_MEDIAN + LANE_WIDTH * lane) * correction );
		
		info.setAzimuth( theta / TWO_PI * 360. ) ;
	}

}
