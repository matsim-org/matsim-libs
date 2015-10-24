/* *********************************************************************** *
 * project: org.matsim.*
 * SNGraphMLWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.sna.graph.spatial.io;

import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.core.utils.collections.Tuple;
import playground.johannes.sna.graph.Graph;
import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.io.GraphMLWriter;
import playground.johannes.sna.graph.spatial.SpatialGraph;
import playground.johannes.sna.graph.spatial.SpatialSparseGraph;
import playground.johannes.sna.graph.spatial.SpatialVertex;

import java.io.IOException;
import java.util.List;

/**
 * An extension to {@link GraphMLWriter} that includes writing spatial
 * information from spatial graphs.
 * 
 * @author illenberger
 * 
 */
public class SpatialGraphMLWriter extends GraphMLWriter {

	/**
	 * @see {@link GraphMLWriter#write(Graph, String)}
	 */
	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SpatialSparseGraph)
			super.write(graph, filename);
		else
			throw new ClassCastException("Graph must be of type SpatialGraph.");
	}

	/**
	 * @see {@link GraphMLWriter#getGraph()}
	 */
	@Override
	protected SpatialGraph getGraph() {
		return (SpatialGraph) super.getGraph();
	}

	/**
	 * Calls {@link GraphMLWriter#getGraphAttributes()} and adds a SRID-attribute:
	 * <ul>
	 * <li>{@link SpatialGraphML#SRID_ATTR} - srid</li>
	 * </ul>
	 * 
	 * @return a list of graph attributes stored in String-String-tuples.
	 */
	@Override
	protected List<Tuple<String, String>> getGraphAttributes() {
		List<Tuple<String, String>> attrs = super.getGraphAttributes();
		int srid = CRSUtils.getSRID(getGraph().getCoordinateReferenceSysten());
		attrs.add(new Tuple<String, String>(SpatialGraphML.SRID_ATTR, String.valueOf(srid)));
		
		return attrs;
	}

	/**
	 * Calls {@link GraphMLWriter#getVertexAttributes(Vertex)} and adds xy-attributes:
	 * <ul>
	 * <li>{@link SpatialGraphML#COORD_X_ATTR} - x</li>
	 * <li>{@link SpatialGraphML#COORD_Y_ATTR} - y</li>
	 * </ul>
	 * 
	 * @return a list of vertex attributes stored in String-String-tuples.
	 * 
	 */
	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		SpatialGraphML.addPointData((SpatialVertex) v, attrs);
//		attrs.add(new Tuple<String, String>(SpatialGraphML.COORD_X_ATTR, String.valueOf(((SpatialSparseVertex)v).getPoint().getCoordinate().x)));
//		attrs.add(new Tuple<String, String>(SpatialGraphML.COORD_Y_ATTR, String.valueOf(((SpatialSparseVertex)v).getPoint().getCoordinate().y)));
		return attrs;
	}
}
