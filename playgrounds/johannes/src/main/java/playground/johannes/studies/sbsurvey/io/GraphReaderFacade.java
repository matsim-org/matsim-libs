/* *********************************************************************** *
 * project: org.matsim.*
 * GraphReaderFacade.java
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
package playground.johannes.studies.sbsurvey.io;

import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseVertex;
import org.matsim.contrib.socnetgen.sna.snowball.io.SampledGraphProjMLReader;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjection;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjectionBuilder;

/**
 * @author illenberger
 *
 */
public class GraphReaderFacade {

	public static SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> read(String filename) {
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		return (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) reader.readGraph(filename);
	}
}
