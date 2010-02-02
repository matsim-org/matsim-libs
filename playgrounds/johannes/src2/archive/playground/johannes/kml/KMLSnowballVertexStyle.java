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

import org.matsim.contrib.sna.snowball.SampledGraph;
import org.matsim.contrib.sna.snowball.SampledVertex;

import gnu.trove.TDoubleObjectHashMap;
import net.opengis.kml._2.LinkType;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle;

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
//		for(Object v : graph.getVertices()) {
//			values.put(((SampledVertex) v).getIterationSampled(), String.valueOf(((SampledVertex) v).getIterationSampled()));
//		}
		for(int i = -1 ; i < 7; i++) {
			values.put(i, String.valueOf(i));
		}
//		values.put(0.0, "-1");
//		values.put(0.05, "0");
//		values.put(0.1, "1");
//		values.put(0.15, "2");
//		values.put(0.2, "3");
		
		return values;
	}

	@Override
	public String getObjectSytleId(SampledVertex object) {
		return String.valueOf(object.getIterationDetected());
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
