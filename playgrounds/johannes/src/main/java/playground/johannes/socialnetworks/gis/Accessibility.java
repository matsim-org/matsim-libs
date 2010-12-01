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
package playground.johannes.socialnetworks.gis;

import java.io.FileNotFoundException;
import java.io.IOException;

import gnu.trove.TObjectDoubleHashMap;

import net.opengis.kml._2.PlacemarkType;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.math.Distribution;

import playground.johannes.socialnetworks.graph.spatial.io.NumericAttributeColorizer;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class Accessibility {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Population2SpatialGraph reader = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialGraph graph = reader.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		GravityCostFunction costFunction = new GravityCostFunction(1.6, 0, new CartesianDistanceCalculator());
		
		TObjectDoubleHashMap<SpatialVertex> values = new TObjectDoubleHashMap<SpatialVertex>();
		Distribution distr = new Distribution();
		
		for(SpatialVertex v1 : graph.getVertices()) {
			double sum = 0;
			for(SpatialVertex v2 : graph.getVertices()) {
				if(v1 != v2) {
					double c = costFunction.costs(v1.getPoint(), v2.getPoint());
					sum += Math.exp(-c);
				}
			}
			
			values.put(v1, Math.log(sum));
			distr.add(Math.log(sum));
		}
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		NumericAttributeColorizer colorizer = new NumericAttributeColorizer(values);
		style.setVertexColorizer(colorizer);
		Descriptor d = new Descriptor();
		d.values = values;
		writer.setKmlVertexDetail(d);
		writer.setKmlVertexStyle(style);
		writer.addKMZWriterListener(style);
		
		writer.write(graph, "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.kmz");
		
		double bin = (distr.max() - distr.min())/100.0;
		Distribution.writeHistogram(distr.absoluteDistribution(bin), "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.txt");
	}
	
	private static class Descriptor implements KMLObjectDetail {

		private TObjectDoubleHashMap<SpatialVertex> values;
		/* (non-Javadoc)
		 * @see org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail#addDetail(net.opengis.kml._2.PlacemarkType, java.lang.Object)
		 */
		@Override
		public void addDetail(PlacemarkType kmlPlacemark, Object object) {
			kmlPlacemark.setDescription("value = " + values.get((SpatialVertex) object));
			
		}
		
	}
	
}
