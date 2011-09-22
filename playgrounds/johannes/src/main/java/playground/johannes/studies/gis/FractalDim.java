/* *********************************************************************** *
 * project: org.matsim.*
 * FractalDim.java
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

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.contrib.sna.math.DescriptivePiStatistics;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.Histogram;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.johannes.coopsim.util.MatsimCoordUtils;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class FractalDim {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
////		String pointFile = args[0];
//		String pointFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.005.xml";
////		String polyFile = args[1];
//		String polyFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp";
////		String outFile = args[2];
//		String outFile = "/Users/jillenberger/Work/socialnets/spatialchoice/fdim.ego-point.fixed.txt";
////		String egoFile = args[3];
//		String egoFile = "/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/graph/graph.graphml";
//		
//		Set<Point> targetPoints = new HashSet<Point>();
//		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(pointFile);
//
//		for(SpatialVertex v : graph2.getVertices()) {	
////			if(Math.random() < 0.5)
//				targetPoints.add(v.getPoint());
//		}
//
//		Feature feature = FeatureSHP.readFeatures(polyFile).iterator().next();
//		Geometry geometry = feature.getDefaultGeometry();
//		geometry.setSRID(21781);
//		
//		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read(egoFile);
//		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
//		Set<? extends SpatialVertex> egos = SnowballPartitions.createSampledPartition(graph.getVertices());
//		Set<Point> startPoints = new HashSet<Point>();
//		for(SpatialVertex v : egos)
//			if(v.getPoint() != null)
//				startPoints.add(v.getPoint());
		
		
		Config config = new Config();
		config.addCoreModules();
//		MatsimConfigReader creader = new MatsimConfigReader(config);
//		creader.readFile(args[0]);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml");
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile("/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/facilities.leisure.xml");
		
		
		Map<String, List<Point>> pointMap = new HashMap<String, List<Point>>();
		for(ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			for(ActivityOption opt : fac.getActivityOptions().values()) {
				List<Point> points = pointMap.get(opt.getType());
				if(points == null) {
					points = new ArrayList<Point>(10000);
					pointMap.put(opt.getType(), points);
				}
				points.add(MatsimCoordUtils.coordToPoint(fac.getCoord()));
			}
		}
		
		for(Entry<String, List<Point>> entry : pointMap.entrySet()) {
			System.out.println("Calculating fractal dimension for type " + entry.getKey());
//			DescriptivePiStatistics stats = calcuate(entry.getValue(), null, null);
			TDoubleDoubleHashMap hist = calcuate(entry.getValue(), null, null);//Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 100, 1000), true);
			TXTWriter.writeMap(hist, "d", "n", "/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/facilities/fdim." + entry.getKey() + ".txt");
		}
	}

	public static TDoubleDoubleHashMap calcuate(List<Point> startPoints, Set<Point> targetPoints, Geometry boundary) {
//		DescriptivePiStatistics stats = new DescriptivePiStatistics();
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		Discretizer discretizer = new LinearDiscretizer(1000.0);

		int N = (int) (startPoints.size() * startPoints.size()/2.0);
		
		ProgressLogger.init(N, 1, 5);
		
		for(int i = 0; i < startPoints.size(); i++) {
			Point p1 = startPoints.get(i);
			double a = 1;
//			for(Point p2 : targetPoints) {
//				double d = dCalc.distance(p1, p2);
//				d = discretizer.index(d);
//				d = Math.max(d, 1);
//				a += Math.pow(d, -1.4);
//			}
		
			for(int j = i+1; j< startPoints.size(); j++) {
				Point p2 = startPoints.get(j);
				double d = dCalc.distance(p1, p2);
				d = discretizer.discretize(d);
//				stats.addValue(d, a);
				hist.adjustOrPutValue(d, 1, 1);
				ProgressLogger.step();
			}
		}
		
		return hist;
	}
	
	private static void moveGeometry(final double x, final double y, Geometry geometry) {
		for(Coordinate c : geometry.getCoordinates()) {
			c.x = c.x + x;
			c.y = c.y + y;
		}
		geometry.geometryChanged();
	}
}
