/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents.runExample;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accidents.AccidentsConfigGroup;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.GeoFileReader;

/**
* @author mmayobre, ikaddoura
*/

public class AccidentsNetworkModification {
	private static final Logger log = LogManager.getLogger(AccidentsNetworkModification.class);

	private final Scenario scenario;

	public AccidentsNetworkModification(Scenario scenario) {
		this.scenario = scenario;
	}

	public Network setLinkAttributsBasedOnOSMFile(String landuseOsmFile, String osmCRS, String[] tunnelLinkIDs, String[] planfreeLinkIDs) throws MalformedURLException, IOException {

		AccidentsConfigGroup accidentsCfg = (AccidentsConfigGroup) scenario.getConfig().getModules().get(AccidentsConfigGroup.GROUP_NAME);

		Map<String, SimpleFeature> landUseFeaturesBB = new HashMap<>();
		Map<String, String> landUseDataBB = new HashMap<>();


		log.info("Initializing all link-specific information...");

		if (landuseOsmFile == null) {
			log.warn("Landuse shape file is null. Using default values...");
		} else {
			SimpleFeatureSource ftsLandUseBB;
			if (!landuseOsmFile.startsWith("http")) {
				ftsLandUseBB = GeoFileReader.readDataFile(landuseOsmFile);
			} else {
				ftsLandUseBB = FileDataStoreFinder.getDataStore(new URL(landuseOsmFile)).getFeatureSource();
			}
			try (SimpleFeatureIterator itLandUseBB = ftsLandUseBB.getFeatures().features()) {
				while (itLandUseBB.hasNext()) {
					SimpleFeature ftLandUseBB = itLandUseBB.next();
					String osmId = ftLandUseBB.getAttribute("osm_id").toString();
					String fclassName = ftLandUseBB.getAttribute("fclass").toString();
					landUseFeaturesBB.put(osmId, ftLandUseBB);
					landUseDataBB.put(osmId, fclassName);
				}
				itLandUseBB.close();
				DataStore ds = (DataStore) ftsLandUseBB.getDataStore();
				ds.dispose();
				log.info("Reading shp file for built-up/nonbuilt-up area & AreaType... Done.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		int linkCounter = 0;
		for (Link link : this.scenario.getNetwork().getLinks().values()) {

			if (linkCounter % 100 == 0) {
				log.info("Link #" + linkCounter + "  (" + (int) ((double) linkCounter / this.scenario.getNetwork().getLinks().size() * 100) + "%)");
			}
			linkCounter++;

			link.getAttributes().putAttribute(accidentsCfg.getAccidentsComputationMethodAttributeName(), AccidentsConfigGroup.AccidentsComputationMethod.BVWP.toString());

			ArrayList<Integer> bvwpRoadType = new ArrayList<>();

			// 'plangleich', 'planfrei' or tunnel?
			bvwpRoadType.add(0, 1);

			for(int j=0; j < planfreeLinkIDs.length; j++){
			    if(planfreeLinkIDs[j].equals(String.valueOf(link.getId()))){
			    	bvwpRoadType.set(0, 0); // Change to Plan free
			    	log.info(link.getId() + " Changed to Plan free!");
			    	break;
			    }
			}

			for(int i=0; i < tunnelLinkIDs.length; i++){
				if(tunnelLinkIDs[i].equals(String.valueOf(link.getId()))){
					bvwpRoadType.set(0, 2); // Change to Tunnel
					log.info(link.getId() + " Changed to Tunnel!");
					break;
				}
			}

			// builtup or not builtup area?
			String osmLandUseFeatureBBId = getOSMLandUseFeatureBBId(link, landUseFeaturesBB, osmCRS);

			if (osmLandUseFeatureBBId == null) {
				log.warn("No area type found for link " + link.getId() + ". Using default value: not built-up area.");
				if (link.getFreespeed() > 16.) {
					bvwpRoadType.add(1, 0);
				} else {
					bvwpRoadType.add(1, 2);
				}

			} else {
				String landUseTypeBB = landUseDataBB.get(osmLandUseFeatureBBId);
				if (landUseTypeBB.matches("commercial|industrial|recreation_ground|residential|retail")) { //built-up area
					if (link.getFreespeed() > 16.) {
						bvwpRoadType.add(1, 1);
					} else {
						bvwpRoadType.add(1, 3);
					}
				} else {
					if (link.getFreespeed() > 16.) {
						bvwpRoadType.add(1, 0);
					} else {
						bvwpRoadType.add(1, 2);
					}
				}
			}

			int numberOfLanesBVWP;
			if (link.getNumberOfLanes() > 4){
				numberOfLanesBVWP = 4;
			} else {
				numberOfLanesBVWP = (int) link.getNumberOfLanes();
			}
			bvwpRoadType.add(2, numberOfLanesBVWP);

			link.getAttributes().putAttribute( AccidentsConfigGroup.BVWP_ROAD_TYPE_ATTRIBUTE_NAME, bvwpRoadType.get(0) + "," + bvwpRoadType.get(1) + "," + bvwpRoadType.get(2));
		}
		log.info("Initializing all link-specific information... Done.");
		return scenario.getNetwork();
	}

	private String getOSMLandUseFeatureBBId(Link link, Map<String, SimpleFeature> landUseFeaturesBB, String osmCRS) {

		if (landUseFeaturesBB == null || landUseFeaturesBB.isEmpty()) return null;

		CoordinateTransformation ctScenarioCRS2osmCRS = TransformationFactory.getCoordinateTransformation(this.scenario.getConfig().global().getCoordinateSystem(), osmCRS);

		Coord linkCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getCoord()); // this Method gives the middle point of the link back
		Point pMiddle = MGC.xy2Point(linkCoordinateTransformedToOSMCRS.getX(), linkCoordinateTransformedToOSMCRS.getY());

		Coord linkStartCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getFromNode().getCoord());
		Point pStart = MGC.xy2Point(linkStartCoordinateTransformedToOSMCRS.getX(), linkStartCoordinateTransformedToOSMCRS.getY());

		Coord linkEndCoordinateTransformedToOSMCRS = ctScenarioCRS2osmCRS.transform(link.getToNode().getCoord());
		Point pEnd = MGC.xy2Point(linkEndCoordinateTransformedToOSMCRS.getX(), linkEndCoordinateTransformedToOSMCRS.getY());

		String osmLandUseFeatureBBId = null;

		for (SimpleFeature feature : landUseFeaturesBB.values()) {
			if (((Geometry) feature.getDefaultGeometry()).contains(pMiddle)) {
				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
			}
		}

		for (SimpleFeature feature : landUseFeaturesBB.values()) {
			if (((Geometry) feature.getDefaultGeometry()).contains(pStart)) {
				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
			}
		}

		for (SimpleFeature feature : landUseFeaturesBB.values()) {
			if (((Geometry) feature.getDefaultGeometry()).contains(pEnd)) {
				return osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
			}
		}

		// look around the link

		GeometryFactory geoFac = new GeometryFactory();
		CoordinateTransformation cTosmCRSToGK4 = TransformationFactory.getCoordinateTransformation(osmCRS, "EPSG:31468");

		double distance = 10.0;

		while (osmLandUseFeatureBBId == null && distance <= 500) {
			Coord coordGK4 = cTosmCRSToGK4.transform(MGC.coordinate2Coord(pMiddle.getCoordinate()));
			Point pGK4 = geoFac.createPoint(MGC.coord2Coordinate(coordGK4));

			Point pRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY()));
			Point pRight = transformPointFromGK4ToOSMCRS(pRightGK4, osmCRS);

			Point pDownGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() - distance));
			Point pDown = transformPointFromGK4ToOSMCRS(pDownGK4, osmCRS);

			Point pLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY()));
			Point pLeft = transformPointFromGK4ToOSMCRS(pLeftGK4, osmCRS);

			Point pUpGK4 = geoFac.createPoint(new Coordinate(pGK4.getX(), pGK4.getY() + distance));
			Point pUp = transformPointFromGK4ToOSMCRS(pUpGK4, osmCRS);

			Point pUpRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() + distance));
			Point pUpRight = transformPointFromGK4ToOSMCRS(pUpRightGK4, osmCRS);

			Point pDownRightGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() + distance, pGK4.getY() - distance));
			Point pDownRight = transformPointFromGK4ToOSMCRS(pDownRightGK4, osmCRS);

			Point pDownLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() - distance));
			Point pDownLeft = transformPointFromGK4ToOSMCRS(pDownLeftGK4, osmCRS);

			Point pUpLeftGK4 = geoFac.createPoint(new Coordinate(pGK4.getX() - distance, pGK4.getY() + distance));
			Point pUpLeft = transformPointFromGK4ToOSMCRS(pUpLeftGK4, osmCRS);

			for (SimpleFeature feature : landUseFeaturesBB.values()) {

				if (((Geometry) feature.getDefaultGeometry()).contains(pRight)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDown)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pLeft)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUp)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUpRight)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDownRight)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pDownLeft)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				} else if (((Geometry) feature.getDefaultGeometry()).contains(pUpLeft)) {
					osmLandUseFeatureBBId = feature.getAttribute("osm_id").toString();
					return osmLandUseFeatureBBId;
				}
			}

			distance += 10.0;
		}

		log.warn("No area type found. Returning null...");
		return null;
	}

	private Point transformPointFromGK4ToOSMCRS(Point pointGK4, String osmCRS) {
		CoordinateTransformation ctGK4toOSMCRS = TransformationFactory.getCoordinateTransformation("EPSG:31468", osmCRS);

		Coord coordGK4 = MGC.coordinate2Coord(pointGK4.getCoordinate());
		Point pointOSMCRS = new GeometryFactory().createPoint(MGC.coord2Coordinate(ctGK4toOSMCRS.transform(coordGK4)));
		return pointOSMCRS;
	}

}

