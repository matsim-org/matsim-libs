/* *********************************************************************** *
 * project: org.matsim.*
 * RoadType.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.population.analysis.vkt;

/**
 * Road types as parsed from OpenStreetMap highway tags.
 * 
 * @author jwjoubert
 */
public enum RoadType {
	FREEWAY, 
	ARTERIAL, 
	STREET, 
	OTHER;
	
	public static RoadType getRoadType(String type){
		if(type.startsWith("motorway")){
			return RoadType.FREEWAY;
		} else if(type.startsWith("trunk")){
			return RoadType.FREEWAY;
		} else if(type.startsWith("primary")){
			return RoadType.ARTERIAL;
		} else if(type.startsWith("secondary")){
			return RoadType.ARTERIAL;
		} else if(type.startsWith("tertiary")){
			return RoadType.ARTERIAL;
		} else if(type.startsWith("residential")){
			return RoadType.STREET;
		} else if(type.startsWith("living")){
			return RoadType.STREET;
		}
		return RoadType.OTHER;
	}

}

