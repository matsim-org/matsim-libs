/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehlerScenario2Commodities
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
package playground.dgrether.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dgrether
 * 
 */
public class DgMatsimPopulation2Zones {

	private static final Logger log = Logger.getLogger(DgMatsimPopulation2Zones.class);

	private List<DgZone> cells = null;
	
	private GeometryFactory geoFac = new GeometryFactory();

	private List<Feature> featureCollection;

	private FeatureType featureType;

	//FIXME remove shape file writer
	public List<DgZone> convert2Zones(Scenario scenario, List<DgZone> cells, Envelope networkBoundingBox) {
		this.cells = cells;
		this.initShapeFileWriter();
		this.convertPopulation2OD(scenario, networkBoundingBox);
		this.writeShape();
		return cells;
	}
	
	private void writeShape() {
		ShapeFileWriter.writeGeometries(featureCollection, DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/network_small/od_pairs.shp");
	}

	private void initShapeFileWriter(){
		CoordinateReferenceSystem crs = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		this.featureCollection = new ArrayList<Feature>();
		AttributeType [] attribs = new AttributeType[1];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString", LineString.class, true, null, null, crs);
		try {
			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "od_pair");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	
	private void convertPopulation2OD(Scenario scenario, Envelope networkBoundingBox) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity startAct = null;
			Activity targetAct = null;
			Leg leg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (startAct == null) {
						startAct = (Activity) pe;
					}
					else if (targetAct == null) {
						targetAct = (Activity) pe;
						processLeg(scenario.getNetwork(), startAct, leg, targetAct, networkBoundingBox);
						startAct = targetAct;
						targetAct = null;
					}
				}
				else if (pe instanceof Leg) {
					leg = (Leg) pe;
				}
			}
		}
	}

	private void addFromToRelationshipToShape(Coordinate startCoordinate, Coordinate endCoordinate){
		Coordinate[] coordinates = {startCoordinate, endCoordinate};
		LineString lineString = this.geoFac.createLineString(coordinates);
		Object[] atts = {lineString};
		try {
			Feature feature = this.featureType.create(atts);
			this.featureCollection.add(feature);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	
	private void addFromToRelationshipToGrid(Coordinate startCoordinate, Coordinate endCoordinate){
		this.addFromToRelationshipToShape(startCoordinate, endCoordinate);
		DgZone startCell = this.searchGridCell(startCoordinate);
		DgZone endCell = this.searchGridCell(endCoordinate);
		log.info("  created od pair from cell " + startCell.getId() + " to " + endCell.getId());
		startCell.addToRelationship(endCell);
	}

	private DgZone searchGridCell(Coordinate coordinate){
		Point p = this.geoFac.createPoint(coordinate);
		for (DgZone cell : this.cells){
			if (cell.getPolygon().covers(p)){
				log.info("  found cell " + cell.getId() + " for Coordinate: " + coordinate);
				return cell;
			}
		}
		log.warn("No cell found for Coordinate: " + coordinate);
		return null;
	}

	private void processLeg(Network network, Activity startAct, Leg leg, Activity targetAct,
			Envelope networkBoundingBox) {
		Coordinate startCoordinate = MGC.coord2Coordinate(startAct.getCoord());
		Coordinate endCoordinate = MGC.coord2Coordinate(targetAct.getCoord());
		log.info("Processing leg from: " + startCoordinate + " to " + endCoordinate);
		if (networkBoundingBox.contains(startCoordinate)
				&& networkBoundingBox.contains(endCoordinate)) {
			log.info("  coordinates in grid...");
			this.addFromToRelationshipToGrid(startCoordinate, endCoordinate);
		}
		else {
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			List<Coordinate> coordinateSequence = this.createCoordinateSequenceFromRoute(network, route);
			boolean isRouteInGrid = false;
			while (! coordinateSequence.isEmpty()){
				Tuple<Coordinate, Coordinate> nextFromTo = this.getNextFromToOfRoute(network, coordinateSequence, networkBoundingBox);
				if (nextFromTo != null){
					this.addFromToRelationshipToGrid(nextFromTo.getFirst(), nextFromTo.getSecond());
					isRouteInGrid = true;
				}
			}
			if (! isRouteInGrid){
				log.info("  Route is not in area of interest");
			}
		}
	}

	private List<Coordinate> createCoordinateSequenceFromRoute(Network network, NetworkRoute route){
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		List<Coordinate> coordinates = new ArrayList<Coordinate>();
		for (Id linkId : linkIds){
			Link currentLink = network.getLinks().get(linkId);
			Coordinate currentCoordinate = MGC.coord2Coordinate(currentLink.getCoord());
			coordinates.add(currentCoordinate);
		}
		return coordinates;
	}
	
	private Tuple<Coordinate, Coordinate> getNextFromToOfRoute(Network network, List<Coordinate> coordinateSequence,
			Envelope networkBoundingBox) {
		Coordinate routeStartCoordinate = null;
		Coordinate routeEndCoordinate = null;
		Coordinate currentCoordinate = null;
		//search next start coordinate within grid on route
		while (! coordinateSequence.isEmpty()){
			currentCoordinate = coordinateSequence.remove(0);
			if (networkBoundingBox.contains(currentCoordinate)){
				routeStartCoordinate = currentCoordinate;
				break;
			}
		}
		//search last link that lies in grid
		while (! coordinateSequence.isEmpty()){
			currentCoordinate = coordinateSequence.remove(0);
			if (networkBoundingBox.contains(currentCoordinate)){
				routeEndCoordinate = currentCoordinate;
			}
			else {
				break;
			}
		}
		if (routeStartCoordinate != null && routeEndCoordinate != null){
			return new Tuple<Coordinate, Coordinate>(routeStartCoordinate, routeEndCoordinate);
		}
		return null;
	}
	
}
