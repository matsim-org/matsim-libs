/* *********************************************************************** *
 * project: org.matsim.*
 * PTTravelTimeKTIEvacuationInitializer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.trafficmonitoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.matrices.Matrix;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.balmermi.world.Layer;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.router.SwissHaltestelle;
import playground.meisterk.kti.router.SwissHaltestellen;

/*
 * Extended PTTravelTimeKTIFactory that adapted the travel time matrix
 * for evacuation scenarios by adding an evacuation zone.
 */
public class PTTravelTimeKTIEvacuationFactory extends PTTravelTimeKTIFactory {

	static final Logger log = Logger.getLogger(PTTravelTimeKTIEvacuationFactory.class);
	
	public PTTravelTimeKTIEvacuationFactory(Scenario scenario, TravelTime travelTime) {
		super(scenario, travelTime);
	}

	public void prepareMatrixForEvacuation(CoordAnalyzer coordAnalyzer) {
		this.prepareMatrixForEvacuation(coordAnalyzer, null, null);
	}
	
	public void prepareMatrixForEvacuation(CoordAnalyzer coordAnalyzer, String stopsFile, String travelTimesFile) {
		PlansCalcRouteKtiInfo pcrKTIi = this.getPlansCalcRouteKtiInfo();
		/*
		 * Identify affected stops and zones. A zone is affected if one of its
		 * stops is affected.
		 */
		SwissHaltestellen swissHaltestellen = pcrKTIi.getHaltestellen();
		Layer municipalities = pcrKTIi.getLocalWorld().getLayer("municipality");
		Set<Id> affectedMunicipalities = new LinkedHashSet<Id>();
		for (SwissHaltestelle swissHaltestelle : swissHaltestellen.getHaltestellenMap().values()) {
			Coord coord = swissHaltestelle.getCoord();
			boolean isAffected = coordAnalyzer.isCoordAffected(coord);
			
			if (isAffected) {
				for (BasicLocation location : municipalities.getNearestLocations(coord)) {
					affectedMunicipalities.add(location.getId());
				}
			}
		}
		
		/*
		 * Also check the municipalities themselves. Some of them have no stops assigned and
		 * therefore would not be identified.
		 */
		for (BasicLocation location : municipalities.getLocations().values()) {
			Coord coord = location.getCoord();
			boolean isAffected = coordAnalyzer.isCoordAffected(coord);
			if (isAffected) affectedMunicipalities.add(location.getId());
		}
		log.info("total municipalities: " + municipalities.getLocations().size());
		log.info("affected municipalities: " + affectedMunicipalities.size());
		
		/*
		 * Create exit/rescue entry in the municipalities and haltestellen data structures
		 */
		log.info("creating additional entries in the PT travel time matrix...");
		Id rescueFacilityId = scenario.createId("rescueFacility");
		Id rescueLinkId = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(rescueFacilityId).getLinkId();
		Link rescueLink = scenario.getNetwork().getLinks().get(rescueLinkId);
		municipalities.getLocations().put(rescueLinkId, rescueLink);
		swissHaltestellen.addHaltestelle(scenario.createId("rescueHaltestelle"), 
				EvacuationConfig.getRescueCoord().getX(), EvacuationConfig.getRescueCoord().getY());
		
		/*
		 * Create entries in the travel time matrix
		 * 
		 * If the municipality is affected, calculate the minimal travel time
		 * to the next non-affected municipality. Otherwise use a travel time of 1.0.
		 */
		Matrix ptMatrix = pcrKTIi.getPtTravelTimes();
		for (Id fromId : municipalities.getLocations().keySet()) {
			if (affectedMunicipalities.contains(fromId)) {
				
				double minTT = Double.MAX_VALUE;
				for (Id id : municipalities.getLocations().keySet()) {
					if (affectedMunicipalities.contains(id)) continue;
					else if (id.equals(rescueLinkId)) continue;
					else if (id.equals(fromId)) continue;
					double tt = ptMatrix.getEntry(fromId, id).getValue();
					if (tt < minTT) minTT = tt;
				}
				ptMatrix.createEntry(fromId, rescueLinkId, minTT);
			} else {
				ptMatrix.createEntry(fromId, rescueLinkId, 1.0);
			}
		}		
		log.info("done.");

		try {
			log.info("writing evacuation pt times to shp file...");
			
			GeometryFactory geofac = new GeometryFactory();
			Collection<Feature> fts;
			
			CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 4326");
			AttributeType po = DefaultAttributeTypeFactory.newAttributeType("Point", Point.class, true, null, null, targetCRS);
			AttributeType zone = AttributeTypeFactory.newAttributeType("zone", String.class);
			AttributeType stop = AttributeTypeFactory.newAttributeType("stop", String.class);
			AttributeType time = AttributeTypeFactory.newAttributeType("time", Double.class);
			AttributeType affected = AttributeTypeFactory.newAttributeType("affected", Boolean.class);
			FeatureType ftPoint;

			// create and write affected stops
			if (stopsFile != null) {
				fts = new ArrayList<Feature>();
				ftPoint = FeatureTypeBuilder.newFeatureType(new AttributeType[] {po, stop, zone, affected}, "Point");
				for (SwissHaltestelle swissHaltestelle : swissHaltestellen.getHaltestellenMap().values()) {
					Coord coord = swissHaltestelle.getCoord();
					boolean isAffected = coordAnalyzer.isCoordAffected(coord);
					final List<? extends BasicLocation> froms = municipalities.getNearestLocations(coord);
					BasicLocation fromMunicipality = froms.get(0);
					
					fts.add(ftPoint.create(new Object[] {geofac.createPoint(new Coordinate(coord.getX(), coord.getY())), 
							swissHaltestelle.getId().toString(), fromMunicipality.getId().toString(), isAffected}));
				}
				ShapeFileWriter.writeGeometries(fts, stopsFile);				
			}
				
			// create and write evacuation travel times
			if (travelTimesFile != null) {
				fts = new ArrayList<Feature>();
				ftPoint = FeatureTypeBuilder.newFeatureType(new AttributeType[] {po, zone, time, affected}, "Point");
				for (Id fromId : municipalities.getLocations().keySet()) {
					double tt = ptMatrix.getEntry(fromId, rescueLinkId).getValue();
					boolean isAffected = affectedMunicipalities.contains(fromId);
					Coord coord = municipalities.getLocation(fromId).getCoord();
					fts.add(ftPoint.create(new Object[] {geofac.createPoint(new Coordinate(coord.getX(), coord.getY())), fromId.toString(), tt, isAffected}));
				}				
				ShapeFileWriter.writeGeometries(fts, travelTimesFile);
			}
			
			log.info("done.");
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}

	}
}
