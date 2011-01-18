/* *********************************************************************** *
 * project: org.matsim.*
 * KMLVertexPropertyWriter.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.PlacemarkType;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.analysis.VertexProperty;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;
import org.matsim.contrib.sna.graph.spatial.io.KMLPartitions;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;

/**
 * @author illenberger
 *
 */
public class KMLVertexPropertyWriter extends SpatialGraphKMLWriter {

	@Override
	public void write(SpatialGraph graph, String filename) {
		TObjectDoubleHashMap<Vertex> values = vertexProperty.values(graph.getVertices());
		
		KMLHelper helper = new KMLHelper();
		helper.values = values;
		
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		style.setVertexColorizer(new NumericAttributeColorizer(values));
		
		this.setKmlVertexStyle(style);
		this.setKmlPartitition(helper);
		this.setKmlVertexDetail(helper);
		this.addKMZWriterListener(style);
		super.write(graph, filename);
	}
	private VertexProperty vertexProperty;
	/**
	 * 
	 */
	public KMLVertexPropertyWriter(VertexProperty vertexProperty) {
		super();
		this.vertexProperty = vertexProperty;
	}
	
	private static class KMLHelper implements KMLPartitions, KMLObjectDetail {

		TObjectDoubleHashMap<Vertex> values;
		
		/* (non-Javadoc)
		 * @see org.matsim.contrib.sna.graph.spatial.io.KMLPartitions#addDetail(net.opengis.kml._2.FolderType, java.util.Set)
		 */
		@Override
		public void addDetail(FolderType kmlFolder, Set<? extends SpatialVertex> partition) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.matsim.contrib.sna.graph.spatial.io.KMLPartitions#getPartitions(org.matsim.contrib.sna.graph.spatial.SpatialGraph)
		 */
		@Override
		public List<Set<? extends SpatialVertex>> getPartitions(SpatialGraph graph) {
			List<Set<? extends SpatialVertex>> list = new ArrayList<Set<? extends SpatialVertex>>(1);
			Set<SpatialVertex> vertices = new HashSet<SpatialVertex>();
			TObjectDoubleIterator<Vertex> it = values.iterator();
			for(int i = 0; i < values.size(); i++) {
				it.advance();
				vertices.add((SpatialVertex) it.key());
			}
			list.add(vertices);
			return list;
		}

		/* (non-Javadoc)
		 * @see org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail#addDetail(net.opengis.kml._2.PlacemarkType, java.lang.Object)
		 */
		@Override
		public void addDetail(PlacemarkType kmlPlacemark, Object object) {
			kmlPlacemark.setDescription(String.valueOf(values.get((SpatialVertex) object)));
			
		}
		
	}

}
