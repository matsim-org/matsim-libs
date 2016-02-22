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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author nagel
 *
 */
/**
 * @author nagel
 *
 */
public class AgentSnapshotInfoFactory {

	private static final double TWO_PI = 2.0 * Math.PI;
	private static final double PI_HALF = Math.PI / 2.0;
	private SnapshotLinkWidthCalculator linkWidthCalculator;


	public AgentSnapshotInfoFactory(SnapshotLinkWidthCalculator widthCalculator) {
		this.linkWidthCalculator = widthCalculator;
	}

	// creators based on x/y

	@SuppressWarnings("static-method")
	public AgentSnapshotInfo createAgentSnapshotInfo(Id<Person> agentId, double easting, double northing, double elevation, double azimuth) {
		PositionInfo info = new PositionInfo() ;
		info.setId( agentId ) ;
		info.setEasting( easting ) ;
		info.setNorthing( northing ) ;
		info.setAzimuth( azimuth ) ;
		return info ;
	}

	
	/**
	 * Generate snapshot info based on Link. 
	 * 
	 *  Comments:<ul>
	 *  <li>One could argue that this method should not know about Links at all,
	 * but it shortens code at several places, and since Link is a standard interface, I see no reason to not provide this
	 * as a service.
	 * </ul>
	 * @param agentId
	 * @param link
	 * @param distanceOnLink
	 * @param lane
	 * @return
	 */
	public AgentSnapshotInfo createAgentSnapshotInfo(Id<Person> agentId, Link link, double distanceOnLink, int lane) {
		PositionInfo info = new PositionInfo() ;
		info.setId(agentId) ;
		double euklidean;
		if (link instanceof LinkImpl){ //as for LinkImpl instances the Euklidean distance is already computed we can save computing time
			euklidean = ((LinkImpl)link).getEuklideanDistance();
		}
		else {
			euklidean = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
		}
		calculateAndSetPosition(info, link.getFromNode().getCoord(), link.getToNode().getCoord(),
				distanceOnLink, link.getLength(), euklidean, lane);
		return info;
	}
	
	/**
	 *  creator based on Coord
	 * @param curveLength lengths are usually different (usually longer) than the euclidean distances between the startCoord and endCoord
	 */
	public AgentSnapshotInfo createAgentSnapshotInfo(Id<Person> agentId, Coord startCoord, Coord endCoord, double distanceOnLink, 
			Integer lane, double curveLength, double euclideanLength) {
		PositionInfo info = new PositionInfo() ;
		info.setId(agentId) ;
		calculateAndSetPosition(info, startCoord, endCoord,
				distanceOnLink, curveLength, euclideanLength, lane) ;
		return info;
	}
	
	
	/**
	 * 
	 * @param lane may be null
	 */
	private final void calculateAndSetPosition(PositionInfo info, Coord startCoord, Coord endCoord, double distanceOnVector, 
			double lengthOfCurve, double euclideanLength, Integer lane){
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
			} else if ( dy < 0 ) {
				theta = -PI_HALF;
			} else { // i.e. DX==0 && DY==0
				theta = 0.833*Math.PI ; // some default direction towards north north east 
			}
		}
		if (theta < 0.0) theta += TWO_PI;

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
		double lanePosition = 0;
		if (lane != null){
			lanePosition = this.linkWidthCalculator.calculateLanePosition(lane);
		}

		info.setEasting( startCoord.getX() 
				+ (Math.cos(theta) * distanceOnVector * correction)
				+ (Math.sin(theta) * lanePosition ) ) ;
		
		info.setNorthing( startCoord.getY() 
				+ Math.sin(theta) * distanceOnVector  * correction 
				- Math.cos(theta) * lanePosition  );
		
		info.setAzimuth( theta / TWO_PI * 360. ) ;
	}

}
