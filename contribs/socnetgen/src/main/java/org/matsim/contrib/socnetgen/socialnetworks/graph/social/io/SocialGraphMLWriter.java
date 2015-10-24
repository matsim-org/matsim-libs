/* *********************************************************************** *
 * project: org.matsim.*
 * SocialGraphMLWriter.java
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
package org.matsim.contrib.socnetgen.socialnetworks.graph.social.io;

import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphMLWriter;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.socialnetworks.graph.social.SocialVertex;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import java.io.IOException;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class SocialGraphMLWriter extends SpatialGraphMLWriter {

	private String populationFile;
	
	@Override
	protected List<Tuple<String, String>> getGraphAttributes() {
		List<Tuple<String, String>> attrs = super.getGraphAttributes();
		
		attrs.add(new Tuple<String, String>(SocialGraphML.POPULATION_FILE_ATTR, populationFile));
		
		return attrs;
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		
		SocialVertex sVertex = (SocialVertex)v;
		attrs.add(new Tuple<String, String>(SocialGraphML.PERSON_ID_ATTR, sVertex.getPerson().getId().toString()));
		
		return attrs;
	}

	public void setPopulationFile(String file) {
		this.populationFile = file;
	}
	
	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SocialGraph) {
			/*
			 * extract the parent directory
			 */
			int idx = filename.lastIndexOf("/");
			String relativPath = filename;
			String baseDir = "";
			if(idx > -1) {
				relativPath = filename.substring(idx + 1, filename.length() - 1);
				baseDir = filename.substring(0, idx + 1);
			}
			
			/*
			 * if no population file is explicitly given, set it to *.pop.xml
			 */
			if(populationFile == null) {
				idx = relativPath.lastIndexOf(".");
				if(idx > -1) {
					populationFile = relativPath.substring(0, idx) + ".pop.xml";
				} else {
					populationFile = relativPath + ".pop.xml";
				}
				writePlansFile((SocialGraph) graph, baseDir + populationFile);
			}
			
			super.write(graph, filename);
		} else {
			throw new IllegalArgumentException("This is not a SocialGraph!");
		}
	}

	private void writePlansFile(SocialGraph graph, String filename) {
        ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population pop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
		for(SocialVertex vertex : graph.getVertices()) {
			pop.addPerson(vertex.getPerson().getPerson());
		}
		
		PopulationWriter writer = new PopulationWriter(pop, null);
		writer.write(filename);
	}
}
