/* *********************************************************************** *
 * project: org.matsim.*
 * DensityZrh.java
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
package playground.johannes.socialnetworks.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledVertexDecorator;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

/**
 * @author illenberger
 *
 */
public class DensityZrh {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws FactoryException 
	 * @throws TransformException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, FactoryException, TransformException {
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.01.xml");

		Set<Coord> coords = Population2Coordinates.getCoords(scenario.getPopulation());

		MathTransform transform = CRS.findMathTransform(DefaultGeographicCRS.WGS84, CRSUtils.getCRS(21781));
//		double source[] = new double[]{8.537995, 47.369046};
//		transform.transform(source, 0, source, 0, 1);
//		Coord center = new CoordImpl(source[0], source[1]);
//		
		Distribution distr = new Distribution();
//		for(Coord c : coords) {
//			double dx = c.getX() - center.getX();
//			double dy = c.getY() - center.getY();
//			
//			double d = Math.sqrt(dx*dx + dy*dy);
//			
//			distr.add(d);
//		}
//		
//		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
		
		for(SocialSampledVertexDecorator<SocialSparseVertex> v : graph.getVertices()) {
			if(v.isSampled()) {
			for(Coord c : coords) {
				double points[] = new double[]{v.getPoint().getX(), v.getPoint().getY()};
				transform.transform(points, 0, points, 0, 1);
				double dx = c.getX() - points[0];
				double dy = c.getY() - points[1];
				
				double d = Math.sqrt(dx*dx + dy*dy);
				
				distr.add(d);
			}
			}
		}
		
		Distribution.writeHistogram(distr.absoluteDistribution(1000), "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/distr.txt");
	}

}
