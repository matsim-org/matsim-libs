/* *********************************************************************** *
 * project: org.matsim.*
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
package org.matsim.lanes.data;

import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Class containing some static helpers to calculate the 
 * geospatial order of links in an orthogonal coordinate system.
 * @author aneumann
 * @author dgrether
 *
 */
public class CalculateAngle {
	/**
	 * Calculates the most 'left' outLink for a given inLink (oriented from north to south).
	 * That's the link a driver would refer to when turning left (no u-turn),
	 * even if there is only one link going to the right.
	 * 
	 * @param inLink The inLink given
	 * @return outLink, or null if there is only one outLink back to the inLinks fromNode.
	 */
	public static Link getLeftLane(Link inLink){
		
		TreeMap<Double, Link> result = getOutLinksSortedByAngle(inLink);

		if (result.size() == 0){
			return null;
		}
		return result.get(result.firstKey());
	}
	
	/**
	 * Calculates the orientation of the outLinks for a given inLink
	 * beginning from the right if the inLink goes north to south. 
	 * The most 'left' outLink comes last.
	 * backLink is ignored
	 * 
	 * @param inLink The inLink given
	 * @return Collection of outLinks, or an empty collection, if there is only
	 * one outLink back to the inLinks fromNode.
	 */
	public static TreeMap<Double, Link> getOutLinksSortedByAngle(Link inLink){
		Coord coordInLink = getVector(inLink);
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		
		TreeMap<Double, Link> leftLane = new TreeMap<Double, Link>();
						
		for (Link outLink : inLink.getToNode().getOutLinks().values()) {
			
			if (!(outLink.getToNode().equals(inLink.getFromNode()))){
				
				Coord coordOutLink = getVector(outLink);
				double thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
				
				double thetaDiff = thetaOutLink - thetaInLink;
				
				if (thetaDiff < -Math.PI){
					thetaDiff += 2 * Math.PI;
				} else if (thetaDiff > Math.PI){
					thetaDiff -= 2 * Math.PI;
				}
				
				leftLane.put(Double.valueOf(-thetaDiff), outLink);
				
			}			
		}
		
		return leftLane;
	}	
	
	private static Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();		
		return new CoordImpl(x, y);
	}
	
}
