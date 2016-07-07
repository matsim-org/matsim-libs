/* *********************************************************************** *
 * project: org.matsim.*
 * Utils3D.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.jjoubert.coord3D;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * A number of utilities to deal with 3D networks.
 * 
 * @author jwjoubert
 */
public class Utils3D {
	private final static Logger LOG = Logger.getLogger(Utils3D.class);
	
	public static double calculateAngle(Link link){
		if(!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate grade if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double angle = Math.asin((c2.getZ()-c1.getZ())/length);
		
		return angle;
	}

	public static double calculateGrade(Link link){
		if(!link.getFromNode().getCoord().hasZ() || !link.getToNode().getCoord().hasZ()){
			LOG.error("From node: " + link.getFromNode().getCoord().toString());
			LOG.error("To node: " + link.getToNode().getCoord().toString());
			throw new IllegalArgumentException("Cannot calculate grade if both nodes on the link do not have elevation (z) set.");
		}
		
		Coord c1 = link.getFromNode().getCoord();
		Coord c2 = link.getToNode().getCoord();
		
		double length = link.getLength();
		double grade = ((c2.getZ()-c1.getZ())/length) / length;
		
		return grade;
	}
}
