/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.mzilske.neo;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;

public class NeoPopulationFactory implements PopulationFactory {

	private GraphDatabaseService graphDb;

	private IndexService index;

	public NeoPopulationFactory(GraphDatabaseService graphDb, IndexService index) {
		super();
		this.graphDb = graphDb;
		this.index = index;
	}

	@Override
	public Activity createActivityFromCoord(String actType, Coord coord) {
		Node actNode = graphDb.createNode();
		NeoActivityImpl act = new NeoActivityImpl(actNode);
		act.setCoord(coord);
		return act;
	}

	@Override
	public Activity createActivityFromLinkId(String actType, Id linkId) {
		Node actNode = graphDb.createNode();
		NeoActivityImpl act = new NeoActivityImpl(actNode);
		// TODO: Connect to link
		return act;
	}

	@Override
	public Leg createLeg(String legMode) {
		Node legNode = graphDb.createNode();
		NeoLegImpl leg = new NeoLegImpl(legNode);
		leg.setMode(legMode);
		return leg;
	}

	@Override
	public Person createPerson(Id id) {
		Node personNode = graphDb.createNode();
		NeoPersonImpl person = new NeoPersonImpl(personNode);
		person.setId(id);
		index.index(personNode, NeoPersonImpl.KEY_ID, id.toString());
		return person;
	}

	@Override
	public Plan createPlan() {
		Node planNode = graphDb.createNode();
		NeoPlanImpl plan = new NeoPlanImpl(planNode);
		return plan;
	}

}
