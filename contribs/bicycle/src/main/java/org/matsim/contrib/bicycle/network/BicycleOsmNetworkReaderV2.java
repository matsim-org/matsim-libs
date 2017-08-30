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

package org.matsim.contrib.bicycle.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author dziemke
 */
public class BicycleOsmNetworkReaderV2 extends OsmNetworkReader {

	private final static Logger LOG = Logger.getLogger(BicycleOsmNetworkReaderV2.class);

	private final static String TAG_HIGHWAY = "highway";
//	private final Network network;
	
	// ----- Bicycle-specific
	private final static String TAG_CYCLEWAYTYPE= "cycleway";
	private final static String TAG_SURFACE = "surface";
	private final static String TAG_SMOOTHNESS = "smoothness";
	private final static String TAG_BICYCLE = "bicycle";
	private final static String TAG_ONEWAYBICYCLE = "oneway:bicycle";
	private ObjectAttributes bikeAttributes = new ObjectAttributes();
	private int countCyclewaytype = 0;
	private int countSurface = 0;
	private int countSmoothness = 0;
	boolean firsttimeParseGeoTiff = true;
	List<Long> signalNodes = new ArrayList<Long>();
	List<Long> monitorNodes = new ArrayList<Long>();
	Long currentNodeID = null;
	// -----

	public BicycleOsmNetworkReaderV2(final Network network, final CoordinateTransformation transformation) {
		this(network, transformation, true);
	}

	public BicycleOsmNetworkReaderV2(final Network network, final CoordinateTransformation transformation, final boolean useHighwayDefaults) {
		super(network, transformation, useHighwayDefaults);
		
		if (useHighwayDefaults) {
			LOG.info("Also falling back to bicycle-specific default values.");
			// ----- Bicycle-specific
			//highway=    ( http://wiki.openstreetmap.org/wiki/Key:highway )
			this.setHighwayDefaults(7, "track",			 1,  10.0/3.6, 1.0,  50);	
			this.setHighwayDefaults(7, "cycleway",		 1,  10.0/3.6, 1.0,  50);

			this.setHighwayDefaults(8, "service",		 1,  10.0/3.6, 1.0,  50);
			
			//if bicycle=yes/designated they can ride, otherwise freespeed is 8km/h
			this.setHighwayDefaults(8, "path", 		   	 1,  10.0/3.6, 1.0,  50);
			this.setHighwayDefaults(8, "pedestrian", 	 1,  10.0/3.6, 1.0,  50); 
			this.setHighwayDefaults(8, "footway", 		 1,  10.0/3.6, 1.0,  50); 	
			//	this.setHighwayDefaults(10, "steps", 		 1,   2.0/3.6, 1.0,  50);

			///what cyclewaytypes do exist on osm? - lane, track, shared_busway
			// -----
		}
		
//		this.network = network;
	}
	
	
	
//	private void stats() {
//		// ----- Bicycle-specific
//		LOG.info("BikeObjectAttributs for cyclewaytype created: " + countCyclewaytype + " which is " + ((float)countCyclewaytype/this.network.getLinks().size())*100 + "%");
//		LOG.info("BikeObjectAttributs for surface created:      " + countSurface      + " which is " + ((float)countSurface/this.network.getLinks().size())*100 + "%");
//		LOG.info("BikeObjectAttributs for smoothness created:   " + countSmoothness   + " which is " + ((float)countSmoothness/this.network.getLinks().size())*100 + "%");
//		// -----
//	}
	
	
	@Override
	protected void setAdditionalLinkAttributes(Link l, OsmWay way, boolean forwardDirection) {
		Set<String> modes = new HashSet<String>();
		
		String bicycleTag = way.tags.get(TAG_BICYCLE);
		OsmHighwayDefaults defaults = this.highwayDefaults.get(way.tags.get(TAG_HIGHWAY));
		if (  !((defaults.hierarchy == 1) || (defaults.hierarchy == 2)) && ( //autobahnen raus und schnellstarassen
				(defaults.hierarchy != 8) || 
				(defaults.hierarchy == 8) && (bicycleTag!= null && (bicycleTag.equals("yes") || bicycleTag.equals("designated")))
				)) { 
			
			modes.add("bicycle");
		}
		
		if ( (forwardDirection && !isOnewayReverse(way)) || (!forwardDirection && !isOneway(way)) ) {
			if (defaults.hierarchy != 6 && defaults.hierarchy != 7 && defaults.hierarchy != 8) {
				modes.add(TransportMode.car);
			}
		}
		l.setAllowedModes(modes);
		
		
		bikeLinkAtts(way, l.getLength(), true, l.getId().toString());
	}


	@Override
	protected boolean reverseDirectionExists(OsmWay way) {
		return !isOneway(way) || oppositeDirectionAllowedForBicycles(way);
	}
	
	
	@Override
	protected boolean forwardDirectionExists(OsmWay way) {
		return !isOnewayReverse(way) || oppositeDirectionAllowedForBicycles(way);
	}
	

	private boolean oppositeDirectionAllowedForBicycles(OsmWay way) {
		boolean reverseDirectionAllowedForBicycles = false;
		String onewayTagBicycle = way.tags.get(TAG_ONEWAYBICYCLE);
		if (onewayTagBicycle != null) {
			if ("no".equals(onewayTagBicycle)) { // This means cyclists are allowed to travel in the opposite direction
				reverseDirectionAllowedForBicycles = true;
			} 
		}
		String cyclewayType = way.tags.get(TAG_CYCLEWAYTYPE);
		if (cyclewayType != null) {
			if ("opposite".equals(cyclewayType) || "opposite_track".equals(cyclewayType) || "opposite_lane".equals(cyclewayType)) {
				reverseDirectionAllowedForBicycles = true;
			}
		}
		return reverseDirectionAllowedForBicycles;
	}
	
	
	

	
//	// good example for setting parameters for bike-routing:
//	// https://github.com/graphhopper/graphhopper/blob/master/core/src/main/java/com/graphhopper/routing/util/BikeCommonFlagEncoder.java
//	private double getBikeFreespeed(final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length, boolean hinweg, long matsimID) {
//
//		double bike_freespeed_highway = 0;
//		double bike_freespeed_surface = 0;
//
//		String bicycleTag = way.tags.get(TAG_BICYCLE);
//
//
//
//		/// HIGHWAY
//		String highwayTag = way.tags.get(TAG_HIGHWAY);
//		if (highwayTag != null) {
//			switch (highwayTag){
//			case "cycleway": 			bike_freespeed_highway= 18; break;
//
//			case "path":
//				if (bicycleTag != null) {	
//					if (bicycleTag.equals("yes") || bicycleTag.equals("designated")) {		
//						bike_freespeed_highway=  15; break;}
//					else 
//						bike_freespeed_highway=  12; break;}
//				else
//				{bike_freespeed_highway=  12; break;}
//			case "footway": 
//				if (bicycleTag != null) {	
//					if (bicycleTag.equals("yes") || bicycleTag.equals("designated")) {		
//						bike_freespeed_highway=  15; break;}
//					else 
//						bike_freespeed_highway=  8; break;}
//				else
//				{bike_freespeed_highway=  8; break;}
//			case "pedestrian":
//				if (bicycleTag != null) {	
//					if (bicycleTag.equals("yes") || bicycleTag.equals("designated")) {		
//						bike_freespeed_highway=  15; break;}
//					else 
//						bike_freespeed_highway=  8; break;}
//				else
//				{bike_freespeed_highway=  8; break;}
//			case "track": 				bike_freespeed_highway= 12; break; 
//			case "service": 			bike_freespeed_highway= 14; break; 
//			case "residential":			bike_freespeed_highway= 18; break;
//			case "minor":				bike_freespeed_highway= 16; break;
//
//			case "unclassified":		bike_freespeed_highway= 16; break;  // if no other highway applies
//			case "road": 				bike_freespeed_highway= 12; break;  // unknown road
//
//			//				case "trunk": 				bike_freespeed_highway= 18; break;  // shouldnt be used by bikes anyways
//			//				case "trunk_link":			bike_freespeed_highway= 18; break; 	// shouldnt be used by bikes anyways
//			case "primary": 			bike_freespeed_highway= 18; break; 
//			case "primary_link":		bike_freespeed_highway= 18; break; 
//			case "secondary":			bike_freespeed_highway= 18; break; 
//			case "secondary_link":		bike_freespeed_highway= 18; break; 
//			case "tertiary": 			bike_freespeed_highway= 18; break;	 
//			case "tertiary_link":		bike_freespeed_highway= 18; break; 
//			case "living_street":		bike_freespeed_highway= 14; break;
//			//	case "steps":				bike_freespeed_highway=  2; break; //should steps be added??
//			default: 					bike_freespeed_highway=  14; log.info(highwayTag + " highwayTag not recognized");
//			}
//		}
//		else {
//			bike_freespeed_highway= 14;
//			log.info("no highway info");
//		}
//		//		TODO http://wiki.openstreetmap.org/wiki/DE:Key:tracktype		
//		//		TrackTypeSpeed("grade1", 18); // paved
//		//      TrackTypeSpeed("grade2", 12); // now unpaved ...
//		//      TrackTypeSpeed("grade3", 8);
//		//      TrackTypeSpeed("grade4", 6);
//		//      TrackTypeSpeed("grade5", 4); // like sand/grass   
//
//
//		// 	TODO may be useful to combine with smoothness-tag
//		/// SURFACE
//		String surfaceTag = way.tags.get(TAG_SURFACE);
//		if (surfaceTag != null) {
//			switch (surfaceTag){
//			case "paved": 					bike_freespeed_surface=  18; break;
//			case "asphalt": 				bike_freespeed_surface=  18; break;
//			case "cobblestone":				bike_freespeed_surface=   9; break;
//			case "cobblestone (bad)":		bike_freespeed_surface=   8; break;
//			case "cobblestone;flattened":
//			case "cobblestone:flattened": 	bike_freespeed_surface=  10; break;
//			case "sett":					bike_freespeed_surface=  10; break;
//
//			case "concrete": 				bike_freespeed_surface=  18; break;
//			case "concrete:lanes": 			bike_freespeed_surface=  16; break;
//			case "concrete_plates":
//			case "concrete:plates": 		bike_freespeed_surface=  16; break;
//			case "paving_stones": 			bike_freespeed_surface=  12; break;
//			case "paving_stones:35": 
//			case "paving_stones:30": 		bike_freespeed_surface=  12; break;
//
//			case "unpaved": 				bike_freespeed_surface=  14; break;
//			case "compacted": 				bike_freespeed_surface=  16; break;
//			case "dirt": 					bike_freespeed_surface=  10; break;
//			case "earth": 					bike_freespeed_surface=  12; break;
//			case "fine_gravel": 			bike_freespeed_surface=  16; break;
//
//			case "gravel": 					bike_freespeed_surface=  10; break;
//			case "ground": 					bike_freespeed_surface=  12; break;
//			case "wood": 					bike_freespeed_surface=   8; break;
//			case "pebblestone": 			bike_freespeed_surface=  16; break;
//			case "sand": 					bike_freespeed_surface=   8; break; //very different kinds of sand :(
//
//			case "bricks": 					bike_freespeed_surface=  14; break;
//			case "stone": 					bike_freespeed_surface=  14; break;
//			case "grass": 					bike_freespeed_surface=   8; break;
//
//			case "compressed": 				bike_freespeed_surface=  14; break; //guter sandbelag
//			case "asphalt;paving_stones:35":bike_freespeed_surface=  16; break;
//			case "paving_stones:3": 		bike_freespeed_surface=  12; break;
//
//			default: 						bike_freespeed_surface=  14; log.info(surfaceTag + " surface not recognized");
//			}		
//		} else {
//			if (highwayTag != null) {
//				if (highwayTag.equals("primary") || highwayTag.equals("primary_link") ||highwayTag.equals("secondary") || highwayTag.equals("secondary_link")) {	
//					bike_freespeed_surface= 18;
//				} else {
//					bike_freespeed_surface = 14;
//					//log.info("no surface info");
//				}
//			}
//		}
//
//		//Minimum of surface_speed and highwaytype_speed
//		double bike_freespeedMin = Math.min(bike_freespeed_surface, bike_freespeed_highway);
//
//
//		/// SLOPE
//		double slopeTag = getSlope(way, fromNode, toNode, length, hinweg, matsimID);
//		double slopeSpeedFactor = 1; 
//
//		if (slopeTag > 0.10) {								//// uphill
//			slopeSpeedFactor= 0.1;
//		} else if (slopeTag <=  0.10 && slopeTag >  0.05) {		
//			slopeSpeedFactor= 0.4;		
//		} else if (slopeTag <=  0.05 && slopeTag >  0.03) {
//			slopeSpeedFactor= 0.6;	
//		} else if (slopeTag <=  0.03 && slopeTag >  0.01) {
//			slopeSpeedFactor= 0.8;
//		} else if (slopeTag <=  0.01 && slopeTag > -0.01) { //// flat
//			slopeSpeedFactor= 1;
//		} else if (slopeTag <= -0.01 && slopeTag > -0.03) {	//// downhill
//			slopeSpeedFactor= 1.2;
//		} else if (slopeTag <= -0.03 && slopeTag > -0.05) {	
//			slopeSpeedFactor= 1.3;
//		} else if (slopeTag <= -0.05 && slopeTag > -0.10) {	
//			slopeSpeedFactor= 1.4;
//		} else if (slopeTag <= -0.10) {	
//			slopeSpeedFactor= 1.5;
//		}
//
//		//bike_freespeed incl. slope und signal
//		double bike_freespeed= bike_freespeedMin*slopeSpeedFactor; //*signalSpeedReductionFactor;
//
//		//not slower than 4km/h
//		bike_freespeed = Math.max(bike_freespeed, 4.0);
//		return bike_freespeed/3.6;
//	}
//
//	
//	private double getSlope(final OsmWay way, final OsmNode fromNode, final OsmNode toNode, final double length, boolean hinweg, long matsimID) {
//
//		String matsimId = Long.toString(matsimID); 		// MAsim Link ID
//		double realSlope= 0;
//
//		ElevationDataParser tiffObject = new ElevationDataParser();
//		try {
//			double heightFrom = tiffObject.parseGeoTiff(fromNode.coord.getX(), fromNode.coord.getY(), firsttimeParseGeoTiff);
//			firsttimeParseGeoTiff = false;
//			double heightTo = tiffObject.parseGeoTiff(toNode.coord.getX(), toNode.coord.getY(), firsttimeParseGeoTiff);
//			double eleDiff = heightTo - heightFrom;
//			double slope = eleDiff/length;
//
//			//for better visualisation
//			double avgHeight= (heightFrom+heightTo)/2;
//			bikeAttributes.putAttribute(matsimId, "avgHeight", avgHeight);
//			//
//
//			if (hinweg){
//				realSlope = slope;
//				bikeAttributes.putAttribute(matsimId, "eleDiff", eleDiff);
//				bikeAttributes.putAttribute(matsimId, "slope", slope);
//			} else {
//				realSlope = -1*slope;
//				bikeAttributes.putAttribute(matsimId, "eleDiff", -1*eleDiff);
//				bikeAttributes.putAttribute(matsimId, "slope", realSlope);
//			}
//
//		} catch (Exception e) {
//			e.printStackTrace();}
//
//		return realSlope;
//	}

	
	// schreiben der Bike-Attribute: wichtig fuer Disutility und Visualisierung
	private void bikeLinkAtts(final OsmWay way, final double length, boolean hinweg, String matsimId) {

		// cyclewaytype
		String cyclewaytypeTag = way.tags.get(TAG_CYCLEWAYTYPE);
		if (cyclewaytypeTag != null) {
			bikeAttributes.putAttribute(matsimId, "cyclewaytype", cyclewaytypeTag);
			countCyclewaytype++;
		};

		//highwaytype
		String highwayTag = way.tags.get(TAG_HIGHWAY);
		if (highwayTag != null) {
			bikeAttributes.putAttribute(matsimId, "highway", highwayTag);
			//countHighway++;
		};

		//surfacetype
		String surfaceTag = way.tags.get(TAG_SURFACE);
		if (surfaceTag != null) {
			bikeAttributes.putAttribute(matsimId, "surface", surfaceTag);
			countSurface++;
		};
		//osm defaeult for prim and sec highways is asphalt
		if ((surfaceTag != null) && (highwayTag.equals("primary") || highwayTag.equals("primary_link") || highwayTag.equals("secondary") || highwayTag.equals("secondary_link"))){
			bikeAttributes.putAttribute(matsimId, "surface", "asphalt");
			countSurface++;
		};

		//smoothness
		String smoothnessTag = way.tags.get(TAG_SMOOTHNESS);
		if (smoothnessTag != null) {
			bikeAttributes.putAttribute(matsimId, "smoothness", smoothnessTag);
			countSmoothness++;
		};

		//bicycleTag
		String bicycleTag = way.tags.get(TAG_BICYCLE);
		if (bicycleTag != null) {
			bikeAttributes.putAttribute(matsimId, "bicycleTag", bicycleTag);
			//countHighway++;
		};
	}

	
	public ObjectAttributes getBikeAttributes() {
		return this.bikeAttributes;
	}
}