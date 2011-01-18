/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceProbaConst.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.VertexProperty;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.FixedSampleSizeDiscretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.snowball.SampledGraphProjectionBuilder;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.analysis.AttributePartition;
import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexPropertyWriter;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAccessibility;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class AcceptanceProbaConst implements VertexProperty {

	private DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();
	
	private Discretizer distanceDiscretizer = new LinearDiscretizer(1000.0);
	
	private Set<Point> points;
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.analysis.VertexProperty#values(java.util.Set)
	 */
	@Override
	public TObjectDoubleHashMap<Vertex> values(Set<? extends Vertex> vertices) {
		ObservedAccessibility access = new ObservedAccessibility();
		GravityCostFunction function = new GravityCostFunction(1.6, 0.0, distanceCalculator);
		TObjectDoubleHashMap<SpatialVertex> values = access.values((Set<? extends SpatialVertex>) vertices, function, points);
		
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(values.getValues(), 10));
		TDoubleObjectHashMap<Set<SpatialVertex>> partitions = partitioner.partition(values);
		
		TObjectDoubleHashMap<Vertex> constValues = new TObjectDoubleHashMap<Vertex>();
		
		TDoubleObjectIterator<Set<SpatialVertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			double constant = constant(it.value(), points);
			
			for(SpatialVertex v : it.value()) {
				constValues.put(v, constant);
			}
		} 
		return constValues;
	}

	private double constant(Set<? extends SpatialVertex> vertices, Set<Point> points) {
		TDoubleIntHashMap M_d = new TDoubleIntHashMap();
		for(SpatialVertex vertex : vertices) {
			Point p1 = vertex.getPoint();
			if(p1 != null) {
				for(Point p2 : points) {
					double d = distanceCalculator.distance(p1, p2);
					d = distanceDiscretizer.discretize(d);
					M_d.adjustOrPutValue(d, 1, 1);
				}
			}
		}
		
		TDoubleDoubleHashMap m_d = Distance.getInstance().distribution(vertices).absoluteDistribution(1000.0);
		
		TDoubleDoubleIterator it = m_d.iterator();
		double constant = 0;
		int cnt = 0;
		for(int i = 0; i < m_d.size(); i++) {
			it.advance();
			
			double d = it.key();
			double m = it.value();
			
			int M = M_d.get(d);
			if(M > 0) {
				constant += m / (Math.pow(d, -1.6) * M);
				cnt++;
			}
		}
		
		return constant/(double)cnt;
	}
	
	public static void main(String args[]) throws IOException {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> g = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
		
		Set<Point> choiceSet = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.005.xml");
		
		g.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		geometry.setSRID(21781);
		GraphFilter<SpatialGraph> filter = new GraphClippingFilter(new SocialSparseGraphBuilder(g.getDelegate().getCoordinateReferenceSysten()), geometry);
		filter.apply(g.getDelegate());
		SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		builder.synchronize(g);
		
		
		
		for(SpatialVertex v : graph2.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
		
		AcceptanceProbaConst vProp = new AcceptanceProbaConst();
		vProp.points = choiceSet;
		
		KMLVertexPropertyWriter writer = new KMLVertexPropertyWriter(vProp);
		writer.setDrawEdges(false);
		writer.write(g, "/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/analysis/konstants2.kmz");
	}
}
