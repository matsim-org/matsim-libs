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
package playground.johannes.studies.sbsurvey.io;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetgen.sna.graph.io.AbstractGraphMLReader;
import org.matsim.contrib.socnetgen.sna.graph.social.*;
import org.matsim.contrib.socnetgen.sna.graph.social.io.SocialGraphML;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphML;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.Attributes;

import java.io.File;

/**
 * @author illenberger
 * 
 */
public class SocialSparseGraphMLReader
		extends
		AbstractGraphMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> {

	private SocialSparseGraphBuilder builder;

	private Scenario scenario;
	
	private Population population; 

	private String baseDir;

	public SocialSparseGraph readGraph(String file, Population population) {
		this.population = population;
		return readGraph(file);
	}
	
	@Override
	public SocialSparseGraph readGraph(String file) {
		baseDir = new File(file).getParent();
		return super.readGraph(file);
	}

	@Override
	protected SocialSparseEdge addEdge(SocialSparseVertex v1, SocialSparseVertex v2, Attributes attrs) {
		SocialSparseEdge edge = builder.addEdge(getGraph(), v1, v2);
		
		String val = attrs.getValue(SocialSparseGraphML.FREQUENCY_ATTR);
		if(val != null)
			edge.setFrequency(Double.parseDouble(val));
		
		val = attrs.getValue(SocialSparseGraphML.EDGE_TYPE_ATTR);
		if(val != null)
			edge.setType(val);
		
		return edge;
	}

	@Override
	protected SocialSparseVertex addVertex(Attributes attrs) {
		/*
		 * get the person's id
		 */
		Id<Person> id = Id.create(attrs.getValue(SocialGraphML.PERSON_ID_ATTR), Person.class);
		if (id == null)
			throw new RuntimeException("Id must not be null!");
		/*
		 * get the person itself
		 */
		Person person = population.getPersons().get(id);
		if (person == null)
			throw new RuntimeException(String.format(
					"Person for id=%1$s does not exist!", id.toString()));
		/*
		 * create a social person and social vertex
		 */
		SocialPerson sPerson = new SocialPerson(person);
		SocialSparseVertex vertex = builder.addVertex(getGraph(), sPerson, SpatialGraphML.newPoint(attrs));
		/*
		 * add citizenship attribute
		 */
		sPerson.setCitizenship(attrs.getValue(SocialSparseGraphML.CITIZENSHIP_ATTR));
		sPerson.setEducation(attrs.getValue(SocialSparseGraphML.EDUCATION_ATTR));
		String val = attrs.getValue(SocialSparseGraphML.INCOME_ATTR);
		if(val != null)
			sPerson.setIncome(Integer.parseInt(val));
		
		sPerson.setCivilStatus(attrs.getValue(SocialSparseGraphML.CIVI_STATUS_ATTR));
		
		return vertex;
	}

	@Override
	protected SocialSparseGraph newGraph(Attributes attrs) {
		String popFile = attrs.getValue(SocialGraphML.POPULATION_FILE_ATTR);
		if (popFile == null && population == null)
			throw new RuntimeException("Population file must not be null!");

		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		if(population == null) {
			PopulationReader reader = new PopulationReader(scenario);
			reader.readFile(baseDir + "/" + popFile);
			population = scenario.getPopulation();
		}

		builder = new SocialSparseGraphBuilder(SpatialGraphML.newCRS(attrs));

		return builder.createGraph();
	}

	public static void main(String args[]) {
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialSparseGraph graph = reader
				.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/tmp.graphml");
		System.out.println("Graph has " + graph.getVertices().size()
				+ " vertices");
	}

}
