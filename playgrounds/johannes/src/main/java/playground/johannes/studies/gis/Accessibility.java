/* *********************************************************************** *
 * project: org.matsim.*
 * Accessibility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.studies.gis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.PointUtils;
import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.gis.TraveTimeCostFunction;
import playground.johannes.socialnetworks.gis.io.FeatureKMLWriter;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author illenberger
 *
 */
public class Accessibility {

	private static final Logger logger = Logger.getLogger(Accessibility.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		
		String popfile = config.getParam("plans", "inputPlansFile");
		String zonesFile = config.getParam("accessibility", "municipalityZones");
		String boundaryFile = config.getParam("accessibility", "countryZones");
		String outfile = config.getParam("accessibility", "output");
		
		Scenario scenario = new ScenarioImpl();
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario);
		reader.readFile(config.getParam("network", "inputNetworkFile"));
		
		boolean interzone = Boolean.parseBoolean(config.getParam("accessibility", "interzone"));
		
		double originGirdSize = Integer.parseInt(config.getParam("accessibility", "originGridSize"));
		double targetGirdSize = Integer.parseInt(config.getParam("accessibility", "targetGridSize"));
		boolean useOriginZones = Boolean.parseBoolean(config.getParam("accessibility", "originZones"));
		boolean useTargetZones = Boolean.parseBoolean(config.getParam("accessibility", "targetZones"));
		boolean useTravelTime = Boolean.parseBoolean(config.getParam("accessibility", "useTravelTime"));
		double beta = Double.parseDouble(config.getParam("accessibility", "beta"));
//		ObjectFactory kmlObjectFactory = new ObjectFactory();
//		KMZWriter kmzWriter = new KMZWriter("/Users/jillenberger/Work/socialnets/spatialchoice/output/network.kmz");
//		
//		KmlType mainKml = kmlObjectFactory.createKmlType();
//		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
//		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
//		KmlNetworkWriter netWriter = new KmlNetworkWriter(scenario.getNetwork(), new CH1903LV03toWGS84(), kmzWriter, mainDoc);
//		
//		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(netWriter.getNetworkFolder()));
//		
//		kmzWriter.writeMainKml(mainKml);
//		kmzWriter.close();
//		System.exit(0);
		
		logger.info(outfile);
		
		logger.info("Loading data...");
		Set<Point> points = loadPoints(popfile);
		Geometry boundary = FeatureSHP.readFeatures(boundaryFile).iterator().next().getDefaultGeometry();
		boundary.setSRID(21781);
		
		ZoneLayer<Double> startZones;
		logger.info("Initializing start zones...");
		if(useOriginZones) {
			startZones= ZoneLayerSHP.read(zonesFile);
			startZones.overwriteCRS(CRSUtils.getCRS(21781));
			startZones = createZoneStartLayer(startZones);		
		} else {
			startZones = createGridStartLayer(originGirdSize, boundary);
		}

		logger.info("Initializing target zones...");
		ZoneLayer<Set<Point>> targetZones;
		if(useTargetZones) {
			targetZones = ZoneLayerSHP.read(zonesFile);
			targetZones.overwriteCRS(CRSUtils.getCRS(21781));
			targetZones = createZoneTargetLayer(targetZones, points);
		} else {
			targetZones = createGridTargetLayer(targetGirdSize, boundary, points);
		}
			
		DistanceCalculator distCalc = new CartesianDistanceCalculator();
		SpatialCostFunction function;
		if(useTravelTime) {
			function = new TraveTimeCostFunction((NetworkImpl) scenario.getNetwork(), beta);
		} else {
			
			Discretizer discretizer = new LinearDiscretizer(1000.0);
			function = new GravityCostFunction(beta, 0.0, distCalc);
			((GravityCostFunction) function).setDiscretizer(discretizer);
		}
		
		logger.info("Calculating...");
		calculate(startZones, targetZones, function, distCalc, interzone);
		
		logger.info("Writing KMZ file...");
		FeatureKMLWriter writer = new FeatureKMLWriter();
		Set<Geometry> geometries = new HashSet<Geometry>();
		TObjectDoubleHashMap<Geometry> values = new TObjectDoubleHashMap<Geometry>();
		for(Zone<Double> zone : startZones.getZones()) {
			geometries.add(zone.getGeometry());
			values.put(zone.getGeometry(), zone.getAttribute());
		}
		
		
		writer.setColorizable(new NumericAttributeColorizer(values));
		writer.write(geometries, outfile);
		
		logger.info("Done.");
	}

	private static Set<Point> loadPoints(String file) {
		SpatialGraph g = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(file);
		Set<Point> points = new HashSet<Point>();
		for (SpatialVertex vertex : g.getVertices())
			points.add(vertex.getPoint());

		return points;
	}
	
	public static void calculate(ZoneLayer<Double> startLayer, ZoneLayer<Set<Point>> targetLayer, SpatialCostFunction function, DistanceCalculator distCalc, boolean interzone) {
		GeometryFactory factory = new GeometryFactory();
		/*
		 * cache centroids
		 */
		Map<Zone<?>, Point> startCentroids = centroids(startLayer);
		Map<Zone<?>, Point> targetCentroids = centroids(targetLayer);
//		Map<Zone<Set<Point>>, Point> targetCentroids = centersOfMass(targetLayer);
		/*
		 * calc
		 */
		int cnt = 0;
		int N = startLayer.getZones().size() * targetLayer.getZones().size();
		
		for(Zone<Double> start : startLayer.getZones()) {
			Point startPoint = startCentroids.get(start);
			double sum = 0;
			for(Zone<Set<Point>> target : targetLayer.getZones()) {
				if (target.getAttribute() != null) {
					Point targetPoint;
					if (interzone) {
						if (start.getGeometry().equals(target.getGeometry())) {
							/*
							 * inter-zone distance
							 */
							double d = PointUtils.avrDistance(target.getAttribute(), distCalc);
							if (!Double.isNaN(d))
								targetPoint = factory.createPoint(new Coordinate(startPoint.getX(), startPoint.getY() + d));
							else
								targetPoint = targetCentroids.get(target);
						} else {
							targetPoint = targetCentroids.get(target);
						}
					} else {
						targetPoint = targetCentroids.get(target);
					}
					double c = function.costs(startPoint, targetPoint);
					
					sum += target.getAttribute().size() * Math.exp(-c);
				}
				cnt++;
				if(cnt % 100000 == 0) {
					logger.info(String.format("Processed %1$s percent...", cnt/(double)N*100));
				}
			}
			
			double a = Math.log(sum);
			start.setAttribute(a);
		}
	}
	
	private static Map<Zone<?>, Point> centroids(ZoneLayer<?> layer) {
		Map<Zone<?>, Point> centroids = new HashMap<Zone<?>, Point>(layer.getZones().size());
		for(Zone<?> zone : layer.getZones()) {
			centroids.put(zone, zone.getGeometry().getCentroid());
		}
		return centroids;
	}
	
	private static Map<Zone<Set<Point>>, Point> centersOfMass(ZoneLayer<Set<Point>> layer) {
		Map<Zone<Set<Point>>, Point> centers = new HashMap<Zone<Set<Point>>, Point>(layer.getZones().size());
		for(Zone<Set<Point>> zone : layer.getZones()) {
			Set<Point> points = zone.getAttribute();
			if(points != null)
				centers.put(zone, PointUtils.centerOfMass(zone.getAttribute()));
			else
				centers.put(zone, zone.getGeometry().getCentroid());
		}
		return centers;
	}
	
	private static ZoneLayer<Double> createGridStartLayer(double resolution, Geometry boundary) {
		return createGridLayer(resolution, boundary);
	}
	
	private static ZoneLayer<Double> createZoneStartLayer(ZoneLayer<Double> layer) {
		return layer;
	}
	
	private static ZoneLayer<Set<Point>> createGridTargetLayer(double resolution, Geometry boundary, Set<Point> points) {
		ZoneLayer<Set<Point>> layer = createGridLayer(resolution, boundary);
		fillTargetLayer(points, layer);
		return layer;
	}
	
	private static ZoneLayer<Set<Point>> createZoneTargetLayer(ZoneLayer<Set<Point>> layer, Set<Point> points) {
		fillTargetLayer(points, layer);
		return layer;
	}
	
	public static <T> ZoneLayer<T> createGridLayer(double resolution, Geometry boundary) {
		GeometryFactory factory = new GeometryFactory();
		Set<Zone<T>> zones = new HashSet<Zone<T>>();
		Envelope env = boundary.getEnvelopeInternal();
		for(double x = env.getMinX(); x < env.getMaxX(); x += resolution) {
			for(double y = env.getMinY(); y < env.getMaxY(); y += resolution) {
				Point point = factory.createPoint(new Coordinate(x, y));
				if(boundary.contains(point)) {
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + resolution);
					coords[2] = new Coordinate(x + resolution, y + resolution);
					coords[3] = new Coordinate(x + resolution, y);
					coords[4] = point.getCoordinate();
					
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					polygon.setSRID(21781);
					Zone<T> zone = new Zone<T>(polygon);
					zones.add(zone);
				}
			}
		}
		
		ZoneLayer<T> layer = new ZoneLayer<T>(zones);
		return layer;
	}
	
	private static ZoneLayer<Set<Point>> fillTargetLayer(Set<Point> points, ZoneLayer<Set<Point>> layer) {
		int nozone = 0;
		for (Point point : points) {
			Zone<Set<Point>> zone = layer.getZone(point);
			if (zone != null) {
				Set<Point> zonePoints = zone.getAttribute();
				if(zonePoints == null) {
					zonePoints = new HashSet<Point>();
					zone.setAttribute(zonePoints);
				}
				zonePoints.add(point);
			} else{
				nozone++;
			}
		}

		logger.warn(String.format("%1$s points could not be assigned to a zone.", nozone)); 
		return layer;
	}
}
