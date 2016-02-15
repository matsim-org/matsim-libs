/* *********************************************************************** *
 * project: org.matsim.*
 * AffectedAreaPenaltyCalculator.java
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

package playground.christoph.evacuation.router.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineSegment;

public class AffectedAreaPenaltyCalculator {

	private static final Logger log = Logger.getLogger(AffectedAreaPenaltyCalculator.class);
	
	public static void main(String[] args) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../matsim/mysimulations/census2000V2/input_10pct/network_ivtch.xml.gz");
		Network network = scenario.getNetwork();
		
		new EvacuationConfigReader().readFile("../../matsim/mysimulations/census2000V2/config_evacuation.xml");
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		Geometry affectedArea = util.mergeGeometries(features);
		
		AffectedAreaPenaltyCalculator penaltyCalculator = new AffectedAreaPenaltyCalculator(network, affectedArea, 5000, 1.20);
		penaltyCalculator.calculatePenaltyFactors();
	}
	
	private final Geometry bufferedAffectedArea;
	private final CoordAnalyzer coordAnalyzer;
	private final Set<Link> affectedLinks;
	private final Map<Id, Double> distanceFactors;	// 0..1 (0 .. link at outer boundary, 1 .. link at center point)
	private final double timePenaltyFactor;
	
	/**
	 * @param network MATSim network
	 * @param affectedArea geometry from a shp file
	 * @param buffer buffer distance added to the affected area in m
	 * @param timePenaltyFactor factors that defines the penalty increase per hour (p(t+1) = p(t)*timeFactor)
	 */
	public AffectedAreaPenaltyCalculator(Network network, Geometry affectedArea, double buffer, double timePenaltyFactor) {
		this.timePenaltyFactor = timePenaltyFactor;
		
		// TODO: create buffer around affected area and hand this buffer to the coord analyzer.		
		this.bufferedAffectedArea = affectedArea.buffer(buffer);
		
		this.coordAnalyzer = new CoordAnalyzer(bufferedAffectedArea);
		
		// identify affected links
		affectedLinks = new HashSet<Link>();
		for (Link link : network.getLinks().values()) {
			if (coordAnalyzer.isLinkAffected(link)) {
				affectedLinks.add(link);
			}
		}
		
		this.distanceFactors = new ConcurrentHashMap<Id, Double>();
		
		calculatePenaltyFactors();
	}
	
	public PenaltyCalculator getPenaltyCalculatorInstance() {
		return new PenaltyCalculator(distanceFactors, timePenaltyFactor);
	}
	
	/*
	 * Calculate penalty factory for every affected link.
	 */
	private void calculatePenaltyFactors() {
		Coordinate centerCoordinate = new Coordinate(EvacuationConfig.centerCoord.getX(), EvacuationConfig.centerCoord.getY());
		Geometry boundary = bufferedAffectedArea.getBoundary();
		Coordinate[] coords = boundary.getCoordinates();
		LineSegment[] boundarySegments = new LineSegment[coords.length - 1];
		for (int i = 0; i < boundarySegments.length; i++) {
			boundarySegments[i] = new LineSegment(coords[i], coords[i + 1]);
		}
		
		for (Link link : affectedLinks) {
			Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			double dx = link.getCoord().getX() - centerCoordinate.x;
			double dy = link.getCoord().getY() - centerCoordinate.y;
			
			// move the point along the line from the center to the link
			double x = centerCoordinate.x + 100000 * dx;
			double y = centerCoordinate.y + 100000 * dy;
			
			Coordinate lineEndCoordinate = new Coordinate(x, y);
			LineSegment line = new LineSegment(centerCoordinate, lineEndCoordinate);
			
			List<Coordinate> intersectionCoordinates = new ArrayList<Coordinate>();
			for (LineSegment boundarySegment : boundarySegments) {
				Coordinate intersectionCoordinate = line.intersection(boundarySegment);
				if (intersectionCoordinate != null) intersectionCoordinates.add(intersectionCoordinate);
			}
			
			double farestDistance = 0.0;
			Coordinate farestCoordinate = null;
			if (intersectionCoordinates.size() == 0) {
				log.warn("No intersections found for link " + link.getId());
				continue;
			} else {
				for (Coordinate intersectionCoordinate : intersectionCoordinates) {
					if (farestCoordinate == null) {
						farestCoordinate = intersectionCoordinate;
						farestDistance = farestCoordinate.distance(centerCoordinate);
					} else {
						double distance = intersectionCoordinate.distance(centerCoordinate);
						if (distance > farestDistance) {
							farestCoordinate = intersectionCoordinate;
							farestDistance = distance;
						}
					}
				}
			}
			
			double factor = 1 - (linkCoordinate.distance(centerCoordinate) / farestDistance);
			
			if (factor < 0.0 || factor > 1.0) log.warn("Unexpected penalty factor: " + factor);
			distanceFactors.put(link.getId(), factor);
		}
	}
}
