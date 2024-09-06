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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author dziemke
 * @deprecated This class is replaced by OsmBicycleReader in the osm-contrib. Use {@link OsmBicycleReader} instead
 */
@Deprecated
public final class BicycleOsmNetworkReaderV2 extends OsmNetworkReader {
	private final static Logger LOG = LogManager.getLogger(BicycleOsmNetworkReaderV2.class);

	private ElevationDataParser elevationDataParser;

//	private final static String TAG_HIGHWAY = "highway";
	private final static String TAG_BICYCLE = "bicycle";
	private final static String TAG_ONEWAYBICYCLE = "oneway:bicycle";
	
	private int countSmoothness = 0;
	private int countSurfaceDirect = 0;
	private int countSurfaceInferred = 0;
	private int countCyclewayType = 0;
	private int countBicycle = 0;

	private final String bicycleAsTransportModeName;
	private int warnCount = 0;

	// OSM Reader is using hierarchy until 8, why following hierarchies are not starting with 9? Amit Feb'18
	private static final int BICYCLE_TRACK_HIERARCHY = 9;
	private static final int FOOTPATH_TRACK_HIERARCHY = 10;
	private static final int STEPS_TRACK_HIERARCHY = 11;
	
	public static void main(String[] args) throws Exception {
		String inputCRS = "EPSG:4326"; // WGS84
		String outputCRS = "EPSG:31468"; // DHDN Gauss-Krüger Zone 4
//		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/input/osm/2017-08-29_mitte.osm";
		String inputOSM = "../../../shared-svn/studies/countries/de/berlin-bike/input/osm/berlin-latest.osm";
		String tiffFile = "../../../shared-svn/studies/countries/de/berlin-bike/input/eu-dem/BerlinEUDEM.tif"; // Berlin EU-DEM
//		String outputXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/2017-08-29_mitte.xml";
		String outputXML = "../../../shared-svn/studies/countries/de/berlin-bike/input/network/berlin-latest.xml";

		// adding bicycle way tags by default: Amit Feb'18
//		List<String> bicycleWayTags = Arrays.asList(
//				new String[]{BicycleUtils.CYCLEWAY, BicycleUtils.SURFACE, BicycleUtils.SMOOTHNESS, TAG_BICYCLE, TAG_ONEWAYBICYCLE});

		Network network = NetworkUtils.createNetwork();
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		
		ElevationDataParser elevationDataParser = new ElevationDataParser(tiffFile, outputCRS);

		BicycleOsmNetworkReaderV2 bicycleNetworkReader = new BicycleOsmNetworkReaderV2(network, ct, elevationDataParser);
		bicycleNetworkReader.setKeepPaths(true);
//		bicycleNetworkReader.addWayTags(bicycleWayTags);
		
		bicycleNetworkReader.parse(inputOSM);
		
		bicycleNetworkReader.stats(network);

		new NetworkCleaner().run(network);
		new NetworkWriter(network).write(outputXML);
	}

	public BicycleOsmNetworkReaderV2(final Network network, final CoordinateTransformation transformation, ElevationDataParser elevationDataParser,
									 final boolean useHighwayDefaults) {
		this(network, transformation, elevationDataParser, useHighwayDefaults, "bicycle",0.2);
	}

	public BicycleOsmNetworkReaderV2(final Network network, final CoordinateTransformation transformation, ElevationDataParser elevationDataParser) {
		this(network, transformation, elevationDataParser, true, "bicycle",0.2);
	}

	public BicycleOsmNetworkReaderV2(final Network network, final CoordinateTransformation transformation, ElevationDataParser elevationDataParser,
									 final boolean useHighwayDefaults, String bicycleAsTransportModeName, double bicyclePCU) {
		this(network, transformation, elevationDataParser, useHighwayDefaults, bicycleAsTransportModeName, bicyclePCU, false);
	}

		public BicycleOsmNetworkReaderV2(final Network network, final CoordinateTransformation transformation, ElevationDataParser elevationDataParser,
			final boolean useHighwayDefaults, String bicycleAsTransportModeName, double bicyclePCU, boolean useVspAdjustments) {
		// If "useHighwayDefaults" is set to true, super sets defaults for all "roads" ("motorway" to "residential", except "service")
		// and all "link roads" and "living_street" (part of "special road types"). Hierachies 1 to 6 are used.
		super(network, transformation, useHighwayDefaults, useVspAdjustments);
			List<String> bicycleWayTags = Arrays.asList( BicycleUtils.CYCLEWAY, BicycleUtils.SURFACE, BicycleUtils.SMOOTHNESS, TAG_BICYCLE, TAG_ONEWAYBICYCLE );
			super.addWayTags( bicycleWayTags );
		
//		double bicyclePCU = 0.2; // Use the same in your run
			this.bicycleAsTransportModeName = bicycleAsTransportModeName;
		
		if (useHighwayDefaults) {
			LOG.info("Also falling back to bicycle-specific default values.");
			// All highway types whose defaults are no set via the super class, see above.
			// Cf. http://wiki.openstreetmap.org/wiki/Key:highway
			// hierarchy , highwayType, lanesPerDirection, freespeed, freespeedFactor, laneCapacity_vehPerHour
			
			// Capacity of a track for bicycles analogous to that of a primary road for cars, dz, aug'18
			// this.setHighwayDefaults(7, "track",	1, 10.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(BICYCLE_TRACK_HIERARCHY, "track", 1, 30.0/3.6, 1.0, 1500 * bicyclePCU);
			
			//this.setHighwayDefaults(7, "cycleway", 1, 10.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(BICYCLE_TRACK_HIERARCHY, "cycleway", 1, 30.0/3.6, 1.0, 1500 * bicyclePCU);
			
			// this.setHighwayDefaults(8, "service", 1, 10.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(BICYCLE_TRACK_HIERARCHY, "service", 1, 10.0/3.6, 1.0, 1000 * bicyclePCU);
			
			// Capacity of a footway/pedestrian/path zone for bicycles analogous to that of a residential road for cars, dz, aug'18
			// this.setHighwayDefaults(8, "footway", 1, 10.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(FOOTPATH_TRACK_HIERARCHY, "footway", 1, 10.0/3.6, 1.0, 600 * bicyclePCU); // Simon's values
			// this.setHighwayDefaults(8, "pedestrian", 1, 10.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(FOOTPATH_TRACK_HIERARCHY, "pedestrian", 1, 10.0/3.6, 1.0, 600 * bicyclePCU); // Simon's values
			// this.setHighwayDefaults(8, "path", 1, 10.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(FOOTPATH_TRACK_HIERARCHY, "path", 1, 20.0/3.6, 1.0, 600 * bicyclePCU);
			
			// "bridleway" is exclusively for equestrians; if others are allowed to use it, it should not be classified as bridleway
			
			// this.setHighwayDefaults(10, "steps", 1, 2.0/3.6, 1.0, 50); // Simon's values
			this.setHighwayDefaults(STEPS_TRACK_HIERARCHY, "steps", 1, 1.0/3.6, 1.0, 50);
		} else {
			LOG.error("The way this is written assumes that highway defaults are used! This is, however, not done here."
					+ "Reconsider this. It will likely lead to awkward resuklts otherwise.");
		}

		if (elevationDataParser==null){
			LOG.warn("Elevation parser is not provided, thus, elevation can not be computed.");
		} else {
			this.elevationDataParser = elevationDataParser;
		}
	}
	
	private void stats(Network network) {
		LOG.info("Smoothness created: " + countSmoothness + " which is " + ((float) countSmoothness / network.getLinks().size()) * 100 + "%.");
		LOG.info("Surface (direct) created: " + countSurfaceDirect  + " which is " + ((float) countSurfaceDirect / network.getLinks().size()) * 100 + "%.");
		LOG.info("Surface (inferred) created: " + countSurfaceInferred  + " which is " + ((float) countSurfaceInferred / network.getLinks().size()) * 100 + "%.");
//		LOG.info("Highway type created: " + countHighwayType + " which is " + ((float) countHighwayType / network.getLinks().size()) * 100 + "%.");
		LOG.info("Cycleway type created: " + countCyclewayType + " which is " + ((float) countCyclewayType / network.getLinks().size()) * 100 + "%.");
		LOG.info("Bicyle created: " + countBicycle + " which is " + ((float) countBicycle / network.getLinks().size()) * 100 + "%.");
	}
	
	@Override
	protected void setOrModifyNodeAttributes(Node n, OsmNode node) {
		if (this.elevationDataParser==null) return;
		Coord coord = n.getCoord();
		double elevation = elevationDataParser.getElevation(n.getCoord());
		Coord elevationCoord = CoordUtils.createCoord(coord.getX(), coord.getY(), elevation);
		n.setCoord(elevationCoord);
	}
	
	@Override
	protected void setOrModifyLinkAttributes(Link l, OsmWay way, boolean forwardDirection) {
		Set<String> modes = new HashSet<String>();
		
		String highwayType = way.tags.get(TAG_HIGHWAY);
		OsmHighwayDefaults defaults = this.highwayDefaults.get(way.tags.get(TAG_HIGHWAY));
		
		// Allow bicycles on all infrastructures except motorways (hierarchy 1) and trunk roads (hierarchy 2)
//		if (defaults.hierarchy == 3 || defaults.hierarchy == 4 || defaults.hierarchy == 5 || defaults.hierarchy == 6
//			|| defaults.hierarchy == 7 || defaults.hierarchy == 8 || defaults.hierarchy == 9) {
		if (! (defaults.hierarchy == 1 || defaults.hierarchy == 2) ) {
			modes.add(this.bicycleAsTransportModeName); // TODO add TransportMode "bicycle"
		}

		// Allow cars if the direction under consideration is open to cars and if we are not on an infrastructure mainly intended
		// for bicyclists (hierarchy 7) or pedestrians (hierarchy 8)
		/* use of following condition (direction/oneway) will set different allowed modes on links which are continuous (continuous hierarchy)
		 * this mean, a continuous routes will not be available and router may throw an exception. Amit Feb'18
		  */
//		if ( (forwardDirection && !isOnewayReverse(way)) || (!forwardDirection && !isOneway(way)) ) {
		// above condition is commented but one can discuss it again. AA, DZ. Feb'18
			if (defaults.hierarchy != BICYCLE_TRACK_HIERARCHY
					&& defaults.hierarchy != FOOTPATH_TRACK_HIERARCHY
					&& defaults.hierarchy != STEPS_TRACK_HIERARCHY) {
				modes.add(TransportMode.car);
			}
//		}
		l.setAllowedModes(modes);

		if (  this.elevationDataParser!=null ){
			// Gradient
//		double gradient = (l.getToNode().getCoord().getZ() - l.getFromNode().getCoord().getZ()) / l.getLength();
//		l.getAttributes().putAttribute(BicycleUtils.GRADIENT, gradient);

			// Elevation
			double averageElevation = (l.getToNode().getCoord().getZ() + l.getFromNode().getCoord().getZ()) / 2;
			l.getAttributes().putAttribute(BicycleUtils.AVERAGE_ELEVATION, averageElevation);
		}

		// Smoothness
		String smoothness = way.tags.get(BicycleUtils.SMOOTHNESS);
		if (smoothness != null) {
			BicycleUtils.setSmoothness( l, smoothness );
			this.countSmoothness++;
		}

		// Surface
		String surface = way.tags.get(BicycleUtils.SURFACE);
		if (surface != null) {
			l.getAttributes().putAttribute(BicycleUtils.SURFACE, surface);
			this.countSurfaceDirect++;
		} else {
			int maxWarnCount = 5;
			if (highwayType != null) {
				// it used to be '&&' instead of '||' which will always be false. Most likely, it must be '||'. Amit Feb'18
				if (defaults.hierarchy == 3 || defaults.hierarchy == 4) { // 3 = primary, 4 = secondary
					l.getAttributes().putAttribute(BicycleUtils.SURFACE, "asphalt");
					this.countSurfaceInferred++;
				} else {
					if (warnCount <= maxWarnCount ){
						LOG.warn("Link did not get a surface.");
						LOG.warn(Gbl.FUTURE_SUPPRESSED);
						warnCount++;
					}
				}
			} else {
				if (warnCount <= maxWarnCount ){
					LOG.warn("Link did not get a surface.");
					LOG.warn(Gbl.FUTURE_SUPPRESSED);
					warnCount++;
				}
			}
		}

		// Cycleway
		String cyclewayType = way.tags.get(BicycleUtils.CYCLEWAY);
		if (cyclewayType != null) {
			l.getAttributes().putAttribute(BicycleUtils.CYCLEWAY, cyclewayType);
			this.countCyclewayType++;
		}

		// Bicycle restrictions
		String bicycle = way.tags.get(TAG_BICYCLE);
		if (bicycle != null) {
			l.getAttributes().putAttribute(this.bicycleAsTransportModeName, bicycle);
			countBicycle++;
		}
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
		String cyclewayType = way.tags.get(BicycleUtils.CYCLEWAY);
		if (cyclewayType != null) {
			if ("opposite".equals(cyclewayType) || "opposite_track".equals(cyclewayType) || "opposite_lane".equals(cyclewayType)) {
				reverseDirectionAllowedForBicycles = true;
			}
		}
		return reverseDirectionAllowedForBicycles;
	}
}
