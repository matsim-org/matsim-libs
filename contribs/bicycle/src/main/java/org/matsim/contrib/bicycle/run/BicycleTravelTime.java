/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
public class BicycleTravelTime implements TravelTime {

	/**
	 * in this class traveltime is calculated depending on the following parameters:
	 * surface, slope/elevation
	 * 
	 * following parameters are supposed to be implemented
	 * cyclewaytype, smoothness? (vs surface), weather/wind?, #crossings (info in nodes)
	 * 
	 * 
	 * following parameters are supposed to be implemented to the disutility
	 * traveltime, distance, surface, smoothness, slope/elevation, #crossings (info in nodes), cyclewaytype, 
	 * size of street aside, weather/wind, parkende autos?
	 * 
	 */
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		
		

		// TODO can we just use bicycle free speed here?
		double travelTime = link.getLength()/link.getFreespeed();
		return travelTime;	
	}
}


/// SLOPE
//double slopeTag = getSlope(way, fromNode, toNode, length, hinweg, matsimID);
//double slopeSpeedFactor = 1; 
//
//if (slopeTag > 0.10) {								//// uphill
//	slopeSpeedFactor= 0.1;
//} else if (slopeTag <=  0.10 && slopeTag >  0.05) {		
//	slopeSpeedFactor= 0.4;		
//} else if (slopeTag <=  0.05 && slopeTag >  0.03) {
//	slopeSpeedFactor= 0.6;	
//} else if (slopeTag <=  0.03 && slopeTag >  0.01) {
//	slopeSpeedFactor= 0.8;
//} else if (slopeTag <=  0.01 && slopeTag > -0.01) { //// flat
//	slopeSpeedFactor= 1;
//} else if (slopeTag <= -0.01 && slopeTag > -0.03) {	//// downhill
//	slopeSpeedFactor= 1.2;
//} else if (slopeTag <= -0.03 && slopeTag > -0.05) {	
//	slopeSpeedFactor= 1.3;
//} else if (slopeTag <= -0.05 && slopeTag > -0.10) {	
//	slopeSpeedFactor= 1.4;
//} else if (slopeTag <= -0.10) {	
//	slopeSpeedFactor= 1.5;
//}








//	switch (surfaceTag){
//	case "paved": 					bike_freespeed_surface=  18; break;
//	case "asphalt": 				bike_freespeed_surface=  18; break;
//	case "cobblestone":				bike_freespeed_surface=   9; break;
//	case "cobblestone (bad)":		bike_freespeed_surface=   8; break;
//	case "cobblestone;flattened":
//	case "cobblestone:flattened": 	bike_freespeed_surface=  10; break;
//	case "sett":					bike_freespeed_surface=  10; break;
//
//	case "concrete": 				bike_freespeed_surface=  18; break;
//	case "concrete:lanes": 			bike_freespeed_surface=  16; break;
//	case "concrete_plates":
//	case "concrete:plates": 		bike_freespeed_surface=  16; break;
//	case "paving_stones": 			bike_freespeed_surface=  12; break;
//	case "paving_stones:35": 
//	case "paving_stones:30": 		bike_freespeed_surface=  12; break;
//
//	case "unpaved": 				bike_freespeed_surface=  14; break;
//	case "compacted": 				bike_freespeed_surface=  16; break;
//	case "dirt": 					bike_freespeed_surface=  10; break;
//	case "earth": 					bike_freespeed_surface=  12; break;
//	case "fine_gravel": 			bike_freespeed_surface=  16; break;
//
//	case "gravel": 					bike_freespeed_surface=  10; break;
//	case "ground": 					bike_freespeed_surface=  12; break;
//	case "wood": 					bike_freespeed_surface=   8; break;
//	case "pebblestone": 			bike_freespeed_surface=  16; break;
//	case "sand": 					bike_freespeed_surface=   8; break; //very different kinds of sand :(
//
//	case "bricks": 					bike_freespeed_surface=  14; break;
//	case "stone": 					bike_freespeed_surface=  14; break;
//	case "grass": 					bike_freespeed_surface=   8; break;
//
//	case "compressed": 				bike_freespeed_surface=  14; break; //guter sandbelag
//	case "asphalt;paving_stones:35":bike_freespeed_surface=  16; break;
//	case "paving_stones:3": 		bike_freespeed_surface=  12; break;
//
//	default: 						bike_freespeed_surface=  14; log.info(surfaceTag + " surface not recognized");
//	}		




///// HIGHWAY
//String highwayTag = way.tags.get(TAG_HIGHWAY);
//if (highwayTag != null) {
//	switch (highwayTag){
//	case "cycleway": 			bike_freespeed_highway= 18; break;
//
//	case "path":
//		if (bicycleTag != null) {	
//			if (bicycleTag.equals("yes") || bicycleTag.equals("designated")) {		
//				bike_freespeed_highway=  15; break;}
//			else 
//				bike_freespeed_highway=  12; break;}
//		else
//		{bike_freespeed_highway=  12; break;}
//	case "footway": 
//		if (bicycleTag != null) {	
//			if (bicycleTag.equals("yes") || bicycleTag.equals("designated")) {		
//				bike_freespeed_highway=  15; break;}
//			else 
//				bike_freespeed_highway=  8; break;}
//		else
//		{bike_freespeed_highway=  8; break;}
//	case "pedestrian":
//		if (bicycleTag != null) {	
//			if (bicycleTag.equals("yes") || bicycleTag.equals("designated")) {		
//				bike_freespeed_highway=  15; break;}
//			else 
//				bike_freespeed_highway=  8; break;}
//		else
//		{bike_freespeed_highway=  8; break;}
//	case "track": 				bike_freespeed_highway= 12; break; 
//	case "service": 			bike_freespeed_highway= 14; break; 
//	case "residential":			bike_freespeed_highway= 18; break;
//	case "minor":				bike_freespeed_highway= 16; break;
//
//	case "unclassified":		bike_freespeed_highway= 16; break;  // if no other highway applies
//	case "road": 				bike_freespeed_highway= 12; break;  // unknown road
//
//	//				case "trunk": 				bike_freespeed_highway= 18; break;  // shouldnt be used by bikes anyways
//	//				case "trunk_link":			bike_freespeed_highway= 18; break; 	// shouldnt be used by bikes anyways
//	case "primary": 			bike_freespeed_highway= 18; break; 
//	case "primary_link":		bike_freespeed_highway= 18; break; 
//	case "secondary":			bike_freespeed_highway= 18; break; 
//	case "secondary_link":		bike_freespeed_highway= 18; break; 
//	case "tertiary": 			bike_freespeed_highway= 18; break;	 
//	case "tertiary_link":		bike_freespeed_highway= 18; break; 
//	case "living_street":		bike_freespeed_highway= 14; break;
//	//	case "steps":				bike_freespeed_highway=  2; break; //should steps be added??
//	default: 					bike_freespeed_highway=  14; log.info(highwayTag + " highwayTag not recognized");
//	}
//}
//else {
//	bike_freespeed_highway= 14;
//	log.info("no highway info");
//}
////		TODO http://wiki.openstreetmap.org/wiki/DE:Key:tracktype		
////		TrackTypeSpeed("grade1", 18); // paved
////      TrackTypeSpeed("grade2", 12); // now unpaved ...
////      TrackTypeSpeed("grade3", 8);
////      TrackTypeSpeed("grade4", 6);
////      TrackTypeSpeed("grade5", 4); // like sand/grass   