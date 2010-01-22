/* *********************************************************************** *
 * project: org.matsim.*
 * SNGraphMLReader.java
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
package playground.johannes.socialnetworks.graph.social.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.graph.io.AbstractGraphMLReader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialNetworkBuilder;
import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.graph.social.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNGraphMLReader extends AbstractGraphMLReader<SocialNetwork, Ego, SocialTie> {

	private static final String WSPACE = " ";
	
	private Population population;
	
	private SocialNetworkBuilder builder;
	
	public SNGraphMLReader(Population population) {
		this.population = population;
	}
	
	@Override
	public SocialNetwork readGraph(String file) {
		return super.readGraph(file);
	}

	@Override
	protected SocialTie addEdge(Ego v1, Ego v2,
			Attributes attrs) {
		String created = attrs.getValue(SNGraphML.CREATED_TAG);
		SocialTie e = builder.addEdge(getGraph(), v1, v2, Integer.parseInt(created));
		
		String usage = attrs.getValue(SNGraphML.USAGE_TAG);
		for(String token : usage.split(WSPACE)) {
			e.use(Integer.parseInt(token));
		}
		
		return e;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Ego addVertex(Attributes attrs) {
		String id = attrs.getValue(SNGraphML.PERSON_ID_TAG);
		PersonImpl person = (PersonImpl) population.getPersons().get(new IdImpl(id));
		if(person == null)
			throw new NullPointerException(String.format("Person with id %1$s not found!", id));
		
		return builder.addVertex(getGraph(), new SocialPerson(person));
	}

	@Override
	protected SocialNetwork newGraph(Attributes attrs) {
		builder = new SocialNetworkBuilder();
		return new SocialNetwork();
	}

	@SuppressWarnings("deprecation")
	public static SocialNetwork loadFromConfig(String configFile, String socialnetFile) {
		Config config = Gbl.createConfig(new String[]{configFile});
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadPopulation();
		ScenarioImpl scenario = loader.getScenario();
		PopulationImpl population = scenario.getPopulation();
		SNGraphMLReader reader = new SNGraphMLReader(population);
		SocialNetwork g = reader.readGraph(socialnetFile);
		
		return g;
	}
	
	public static <P extends Person> Set<Ego> readAnonymousVertices(SocialNetwork socialnet, String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			Set<Ego> egos = new HashSet<Ego>();
			String line = null;
			while((line = reader.readLine()) != null) {
				boolean found = false;
				for(Ego e : socialnet.getVertices()) {
					if(e.getPerson().getId().toString().equals(line)) {
						egos.add(e);
						found = true;
						break;
					}
				}
				if(!found)
					System.err.println("Person with id " + line + " not found!");
			}
			
			return egos;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
