/* *********************************************************************** *
 * project: org.matsim.*
 * SocialSparseGraphMLWriter.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.util.List;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 * 
 */
public class SocialSparseGraphMLWriter extends SocialGraphMLWriter {

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		String val = ((SocialSparseVertex) v).getPerson().getCitizenship();
		if (val != null) {
			Tuple<String, String> attr = new Tuple<String, String>(SocialSparseGraphML.CITIZENSHIP_ATTR, val);
			attrs.add(attr);
		}
		return attrs;
	}

	@Override
	protected List<Tuple<String, String>> getEdgeAttributes(Edge e) {
		List<Tuple<String, String>> attrs = super.getEdgeAttributes(e);
		Tuple<String, String> tuple = new Tuple<String, String>(SocialSparseGraphML.FREQUENCY_ATTR, String
				.valueOf(((SocialSparseEdge) e).getFrequency()));
		attrs.add(tuple);
		return attrs;
	}

}
