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
package playground.johannes.socialnetworks.graph.social.io;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author illenberger
 *
 */
public class Population2SocialGraph {

	public SocialSparseGraph read(String filename, CoordinateReferenceSystem crs) {
		Scenario scenario = new ScenarioImpl();
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(filename);
		Population populatio = scenario.getPopulation();
		
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(crs);
		GeometryFactory geoFactory = new GeometryFactory();
		
		SocialSparseGraph graph = builder.createGraph();
		for(Person person : populatio.getPersons().values()) {
			Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			SocialPerson sPerson = new SocialPerson((PersonImpl) person);
			builder.addVertex(graph, sPerson, geoFactory.createPoint(new Coordinate(act.getCoord().getX(), act.getCoord().getY())));
		}
		
		return graph;
	}
}
