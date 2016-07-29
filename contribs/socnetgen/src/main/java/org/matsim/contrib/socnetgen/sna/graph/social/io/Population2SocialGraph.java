/* *********************************************************************** *
 * project: org.matsim.*
 * Population2SocialGraph.java
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
package org.matsim.contrib.socnetgen.sna.graph.social.io;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialPerson;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraphBuilder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author illenberger
 *
 */
public class Population2SocialGraph {

	public SocialSparseGraph read(String filename, CoordinateReferenceSystem crs) {
		Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(filename);
		Population populatio = scenario.getPopulation();
		
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(crs);
		GeometryFactory geoFactory = new GeometryFactory();
		
		SocialSparseGraph graph = builder.createGraph();
		for(Person person : populatio.getPersons().values()) {
			Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			SocialPerson sPerson = new SocialPerson(person);
			builder.addVertex(graph, sPerson, geoFactory.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY())));
		}
		
		return graph;
	}
}
