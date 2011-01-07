/* *********************************************************************** *
 * project: org.matsim.*
 * PopRhoTest.java
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
package playground.johannes.socialnetworks.misc;

import gnu.trove.TDoubleDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.util.TXTWriter;
import org.opengis.referencing.crs.GeographicCRS;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class PopRhoTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		Scenario scenario = new ScenarioImpl();
//		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
//		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
//		
//		Set<Coord> coords = Population2Coordinates.getCoords(scenario.getPopulation());
//		
		Set<Point> points = new HashSet<Point>();
//		GeometryFactory factory = new GeometryFactory();
//		for(Coord c : coords) {
//			points.add(factory.createPoint(new Coordinate(c.getX(), c.getY())));
//		}

		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
		for(SpatialVertex vertex : graph.getVertices()) {
			if(((SampledVertex)vertex).isSampled()) {
				points.add(vertex.getPoint());
			}
		}
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		
		CartesianDistanceCalculator calculator = new CartesianDistanceCalculator();
		Discretizer discretizer = new LinearDiscretizer(1000.0);
		
//		Point source = factory.createPoint(new Coordinate(680000, 250000));
//		Set<Point> souces = new HashSet<Point>();
//		souces.add(source);
		Set<Point> sources = points;
		
		for(Point source : sources) {
		for(Point target : points) {
			double d = calculator.distance(source, target);
			d = discretizer.discretize(d);
			hist.adjustOrPutValue(d, 1.0, 1.0);
		}
		}
		TXTWriter.writeMap(hist, "d", "cnt", "/Users/jillenberger/Work/socialnets/spatialchoice/rho.txt");
	}

}
