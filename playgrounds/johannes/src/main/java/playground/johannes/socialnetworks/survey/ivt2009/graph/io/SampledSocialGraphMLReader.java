/* *********************************************************************** *
 * project: org.matsim.*
 * SocialGraphMLReader.java
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

import java.io.File;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sna.graph.io.AbstractGraphMLReader;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphML;
import org.matsim.contrib.sna.snowball.io.SampledGraphML;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphML;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialVertex;

/**
 * @author illenberger
 * 
 */
public class SampledSocialGraphMLReader
		extends
		AbstractGraphMLReader<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> {

	private SampledSocialGraphBuilder builder;

	private Scenario scenario;

	private String baseDir;

	@Override
	public SampledSocialGraph readGraph(String file) {
		baseDir = new File(file).getParent();
		return super.readGraph(file);
	}

	@Override
	protected SampledSocialEdge addEdge(SampledSocialVertex v1,
			SampledSocialVertex v2, Attributes attrs) {
		return builder.addEdge(getGraph(), v1, v2);
	}

	@Override
	protected SampledSocialVertex addVertex(Attributes attrs) {
		/*
		 * get the person's id
		 */
		Id id = scenario.createId(attrs.getValue(SocialGraphML.PERSON_ID_ATTR));
		if (id == null)
			throw new RuntimeException("Id must not be null!");
		/*
		 * get the person itself
		 */
		Person person = scenario.getPopulation().getPersons().get(id);
		if (person == null)
			throw new RuntimeException(String.format(
					"Person for id=%1$s does not exist!", id.toString()));
		/*
		 * create a social person and social vertex
		 */
		SocialPerson sPerson = new SocialPerson((PersonImpl) person);
		SampledSocialVertex vertex = builder.addVertex(getGraph(), sPerson, SpatialGraphML.newPoint(attrs));
		/*
		 * set snowball attributes
		 */
		SampledGraphML.applyDetectedState(vertex, attrs);
		SampledGraphML.applySampledState(vertex, attrs);
		
		return vertex;
	}

	@Override
	protected SampledSocialGraph newGraph(Attributes attrs) {
		String popFile = attrs.getValue(SocialGraphML.POPULATION_FILE_ATTR);
		if (popFile == null)
			throw new RuntimeException("Population file must not be null!");

		scenario = new ScenarioImpl();
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(baseDir + "/" + popFile);

		builder = new SampledSocialGraphBuilder(SpatialGraphML.newCRS(attrs));

		return builder.createGraph();
	}

	public static void main(String args[]) {
		SampledSocialGraphMLReader reader = new SampledSocialGraphMLReader();
		SampledSocialGraph graph = reader
				.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/tmp.graphml");
		System.out.println("Graph has " + graph.getVertices().size()
				+ " vertices");
	}

}
