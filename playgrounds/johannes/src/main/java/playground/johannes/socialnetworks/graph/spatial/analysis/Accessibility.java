/* *********************************************************************** *
 * project: org.matsim.*
 * Accessability.java
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

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.PlacemarkType;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.gis.SpatialCostFunction;
import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.analysis.ObservedAccessibility;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class Accessibility {
	
	private static Accessibility instance;
	
	public static Accessibility getInstance() {
		if(instance == null)
			instance = new Accessibility();
		return instance;
	}
	
	public Distribution distribution(Set<? extends SpatialVertex> vertices, SpatialCostFunction costFunction, Set<Point> opportunities) {
		TObjectDoubleHashMap<SpatialVertex> values = values(vertices, costFunction, opportunities);
		TObjectDoubleIterator<SpatialVertex> it = values.iterator();
		Distribution distr = new Distribution();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			distr.add(it.value());
		}
		
		return distr;
	}

	@SuppressWarnings("unchecked")
	public TObjectDoubleHashMap<SpatialVertex> values(Set<? extends SpatialVertex> vertices, SpatialCostFunction costFunction, Set<Point> opportunities) {
		Set<SpatialVertex> spatialVertices = (Set<SpatialVertex>) vertices;
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>(spatialVertices.size());

		for (SpatialVertex vertex : spatialVertices) {
			if (vertex.getPoint() != null) {
				double sum = 0;
				for (Point point : opportunities) {
					if (point != null) {
						double c = costFunction.costs(vertex.getPoint(), point);
						sum += Math.exp(-c);
					}
				}
				values.put(vertex, Math.log(sum));
			}
		}

		return values;
	}
	
	public static void main(String args[]) {
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> g = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/graph/noH/graph.graphml");
		g.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		
		GravityCostFunction function = new GravityCostFunction(1.6, 0.0, new CartesianDistanceCalculator());
		
		Set<Point> choiceSet = new HashSet<Point>();
		SpatialSparseGraph graph2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.005.xml");
		for(SpatialVertex v : graph2.getVertices()) {	
			choiceSet.add(v.getPoint());
		}
		
		final TObjectDoubleHashMap<SpatialVertex> values = ObservedAccessibility.getInstance().values(g.getVertices(), function, choiceSet);
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		writer.setKmlPartitition(new KMLPartitions() {
			
			@Override
			public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
				List<Set<? extends SpatialVertex>> list = new ArrayList<Set<? extends SpatialVertex>>(1);
				Set<SpatialVertex> set = new HashSet<SpatialVertex>();
				for(Object vertex : values.keys()) {
					set.add((SpatialVertex) vertex);
				}
				list.add(set);
				return list;
			}
			
			@Override
			public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
				// TODO Auto-generated method stub
				
			}
		});
		KMLIconVertexStyle style = new KMLIconVertexStyle(g);
		style.setVertexColorizer(new NumericAttributeColorizer(values));
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		writer.setDrawEdges(false);
		writer.setKmlVertexDetail(new KMLObjectDetail() {
			
			@Override
			public void addDetail(PlacemarkType kmlPlacemark, Object object) {
				kmlPlacemark.setDescription(String.valueOf(values.get((SpatialVertex) object)));
			}
		});
		writer.write(g, "/Users/jillenberger/Work/socialnets/data/ivt2009/09-2010/analysis/acces.kmz");
	}
}
