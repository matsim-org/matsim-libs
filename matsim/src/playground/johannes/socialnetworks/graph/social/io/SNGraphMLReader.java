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

import org.matsim.api.basic.v01.population.BasicPerson;
import org.matsim.api.basic.v01.population.BasicPlan;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.api.basic.v01.population.BasicPopulation;
import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

import org.xml.sax.Attributes;

import playground.johannes.socialnetworks.graph.SparseVertex;
import playground.johannes.socialnetworks.graph.io.AbstractGraphMLReader;
import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialNetworkFactory;
import playground.johannes.socialnetworks.graph.social.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNGraphMLReader<P extends BasicPerson<?>> extends AbstractGraphMLReader {

	private static final String WSPACE = " ";
	
	private BasicPopulation<P> population;
	
	private SocialNetworkFactory<P> factory;
	
	public SNGraphMLReader(BasicPopulation<P> population) {
		this.population = population;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public SocialNetwork<P> readGraph(String file) {
		return (SocialNetwork<P>) super.readGraph(file);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected SocialTie addEdge(SparseVertex v1, SparseVertex v2,
			Attributes attrs) {
		String created = attrs.getValue(SNGraphML.CREATED_TAG);
		SocialTie e = factory.addEdge((SocialNetwork<P>)graph, (Ego<P>)v1, (Ego<P>)v2, Integer.parseInt(created));
		
		String usage = attrs.getValue(SNGraphML.USAGE_TAG);
		for(String token : usage.split(WSPACE)) {
			e.use(Integer.parseInt(token));
		}
		
		return e;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Ego<P> addVertex(Attributes attrs) {
		String id = attrs.getValue(SNGraphML.PERSON_ID_TAG);
		P person = population.getPersons().get(new IdImpl(id));
		if(person == null)
			throw new NullPointerException(String.format("Person with id %1$s not found!", id));
		
		return factory.addVertex((SocialNetwork<P>)graph, person);
	}

	@Override
	protected SocialNetwork<P> newGraph(Attributes attrs) {
		factory = new SocialNetworkFactory<P>(population);
		return new SocialNetwork<P>();
	}

	@SuppressWarnings("deprecation")
	public static SocialNetwork<PersonImpl> loadFromConfig(String configFile, String socialnetFile) {
		Config config = Gbl.createConfig(new String[]{configFile});
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadPopulation();
		Scenario scenario = loader.getScenario();
		PopulationImpl population = scenario.getPopulation();
		SNGraphMLReader<PersonImpl> reader = new SNGraphMLReader<PersonImpl>(population);
		SocialNetwork<PersonImpl> g = reader.readGraph(socialnetFile);
		
		return g;
	}
	
	public static <P extends BasicPerson<? extends BasicPlan<? extends BasicPlanElement>>> Set<Ego<P>> readAnonymousVertices(SocialNetwork<P> socialnet, String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			Set<Ego<P>> egos = new HashSet<Ego<P>>();
			String line = null;
			while((line = reader.readLine()) != null) {
				boolean found = false;
				for(Ego<P> e : socialnet.getVertices()) {
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
