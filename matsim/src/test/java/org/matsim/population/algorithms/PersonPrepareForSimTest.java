/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / senozon
 */
public class PersonPrepareForSimTest {

	@Test
	public void testRun_MultimodalNetwork() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		Network net = sc.getNetwork();
		Link l1;
		{
			NetworkFactory nf = net.getFactory();
			Set<String> modes = new HashSet<String>();
			Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
			Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
			modes.add(TransportMode.car);
			l1.setAllowedModes(modes);
			Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
			modes.clear();
			modes.add(TransportMode.pt);
			l2.setAllowedModes(modes);
			net.addLink(l1);
			net.addLink(l2);
		}

		Population pop = sc.getPopulation();
		Person person;
		Activity a1;
		Activity a2;
		{
			PopulationFactory pf = pop.getFactory();
			person = pf.createPerson(Id.create("1", Person.class));
			Plan p = pf.createPlan();
			double y1 = -10;
			a1 = pf.createActivityFromCoord("h", new Coord((double) 10, y1));
			Leg l = pf.createLeg(TransportMode.walk);
			double y = -10;
			a2 = pf.createActivityFromCoord("w", new Coord((double) 1900, y));
			p.addActivity(a1);
			p.addLeg(l);
			p.addActivity(a2);
			person.addPlan(p);
			pop.addPerson(person);
		}

		new PersonPrepareForSim(new DummyRouter(), sc).run(person);

		Assert.assertEquals(l1.getId(), a1.getLinkId());
		Assert.assertEquals(l1.getId(), a2.getLinkId()); // must also be linked to l1, as l2 has no car mode
	}

	@Test
	public void testRun_MultimodalScenario() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network net = sc.getNetwork();
		Link l1;
		{
			NetworkFactory nf = net.getFactory();
			Set<String> modes = new HashSet<String>();
			Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
			Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
			modes.add(TransportMode.car);
			l1.setAllowedModes(modes);
			Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
			modes.clear();
			modes.add(TransportMode.pt);
			l2.setAllowedModes(modes);
			net.addLink(l1);
			net.addLink(l2);
		}
		
		Population pop = sc.getPopulation();
		Person person;
		Activity a1;
		Activity a2;
		{
			PopulationFactory pf = pop.getFactory();
			person = pf.createPerson(Id.create("1", Person.class));
			Plan p = pf.createPlan();
			double y1 = -10;
			a1 = pf.createActivityFromCoord("h", new Coord((double) 10, y1));
			Leg l = pf.createLeg(TransportMode.walk);
			double y = -10;
			a2 = pf.createActivityFromCoord("w", new Coord((double) 1900, y));
			p.addActivity(a1);
			p.addLeg(l);
			p.addActivity(a2);
			person.addPlan(p);
			pop.addPerson(person);
		}
		
		new PersonPrepareForSim(new DummyRouter(), sc).run(person);
		
		Assert.assertEquals(l1.getId(), a1.getLinkId());
		Assert.assertEquals(l1.getId(), a2.getLinkId()); // must also be linked to l1, as l2 has no car mode
	}

	private static class DummyRouter implements PlanAlgorithm {
		@Override
		public void run(final Plan plan) {
		}
	}
}
