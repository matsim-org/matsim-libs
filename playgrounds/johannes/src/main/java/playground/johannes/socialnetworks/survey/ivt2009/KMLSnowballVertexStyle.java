/* *********************************************************************** *
 * project: org.matsim.*
 * KMLSnowballVertexStyle.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TDoubleObjectHashMap;
import net.opengis.kml._2.LinkType;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle;
import playground.johannes.socialnetworks.snowball2.SampledGraph;
import playground.johannes.socialnetworks.snowball2.SampledVertex;

/**
 * @author illenberger
 *
 */
public class KMLSnowballVertexStyle extends KMLVertexColorStyle<SampledGraph, SampledVertex> {

	/**
	 * @param vertexIconLink
	 */
	public KMLSnowballVertexStyle(LinkType vertexIconLink) {
		super(vertexIconLink);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle#getValues(playground.johannes.socialnetworks.graph.Graph)
	 */
	@Override
	protected TDoubleObjectHashMap<String> getValues(SampledGraph graph) {
		
		TDoubleObjectHashMap<String> values = new TDoubleObjectHashMap<String>();
		for(Object v : graph.getVertices()) {
			values.put(((SampledVertex) v).getIterationSampled(), String.valueOf(((SampledVertex) v).getIterationSampled()));
		}
		values.put(1.0, "1.0");
		
		return values;
	}

	@Override
	public String getObjectSytleId(SampledVertex object) {
		return String.valueOf(object.getIterationSampled());
	}

	/* (non-Javadoc)
	 * @see playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle#getValue(playground.johannes.socialnetworks.graph.Vertex)
	 */
	@Override
	protected double getValue(SampledVertex vertex) {
		// TODO Auto-generated method stub
		return 0;
	}

}
