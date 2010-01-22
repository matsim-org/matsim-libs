/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationReaderMatsimV4Test.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.population;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.AttributesBuilder;
import org.xml.sax.SAXException;

public class PopulationReaderMatsimV4Test extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(PopulationReaderMatsimV4Test.class);

	/**
	 * @author mrieser
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void testReadRoute() throws SAXException, ParserConfigurationException, IOException {
		final ScenarioImpl scenario = new ScenarioImpl();
		final Network network = scenario.getNetwork();
		final PopulationImpl population = scenario.getPopulation();

		new MatsimNetworkReader(scenario).parse("test/scenarios/equil/network.xml");
		new PopulationReaderMatsimV4(scenario).parse(getInputDirectory() + "plans2.xml");

		assertEquals("population size.", 2, population.getPersons().size());
		Person person1 = population.getPersons().get(scenario.createId("1"));
		Plan plan1 = person1.getPlans().get(0);
		LegImpl leg1a = (LegImpl) plan1.getPlanElements().get(1);
		RouteWRefs route1a = leg1a.getRoute();
		assertEquals("different startLink for first leg.", network.getLinks().get(scenario.createId("1")).getId(), route1a.getStartLinkId());
		assertEquals("different endLink for first leg.", network.getLinks().get(scenario.createId("20")).getId(), route1a.getEndLinkId());
		LegImpl leg1b = (LegImpl) plan1.getPlanElements().get(3);
		RouteWRefs route1b = leg1b.getRoute();
		assertEquals("different startLink for second leg.", network.getLinks().get(scenario.createId("20")).getId(), route1b.getStartLinkId());
		assertEquals("different endLink for second leg.", network.getLinks().get(scenario.createId("20")).getId(), route1b.getEndLinkId());
		LegImpl leg1c = (LegImpl) plan1.getPlanElements().get(5);
		RouteWRefs route1c = leg1c.getRoute();
		assertEquals("different startLink for third leg.", network.getLinks().get(scenario.createId("20")).getId(), route1c.getStartLinkId());
		assertEquals("different endLink for third leg.", network.getLinks().get(scenario.createId("1")).getId(), route1c.getEndLinkId());

		Person person2 = population.getPersons().get(scenario.createId("2"));
		Plan plan2 = person2.getPlans().get(0);
		LegImpl leg2a = (LegImpl) plan2.getPlanElements().get(1);
		RouteWRefs route2a = leg2a.getRoute();
		assertEquals("different startLink for first leg.", network.getLinks().get(scenario.createId("2")).getId(), route2a.getStartLinkId());
		assertEquals("different endLink for first leg.", network.getLinks().get(scenario.createId("20")).getId(), route2a.getEndLinkId());
		LegImpl leg2b = (LegImpl) plan2.getPlanElements().get(3);
		RouteWRefs route2b = leg2b.getRoute();
		assertEquals("different startLink for third leg.", network.getLinks().get(scenario.createId("20")).getId(), route2b.getStartLinkId());
		assertEquals("different endLink for third leg.", network.getLinks().get(scenario.createId("1")).getId(), route2b.getEndLinkId());
	}

	public void testReadActivity() {
		final ScenarioImpl scenario = new ScenarioImpl();
		final NetworkLayer network = scenario.getNetwork();
		Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(0, 0));
		Node node2 = network.createAndAddNode(scenario.createId("2"), scenario.createCoord(0, 1000));
		Link link3 = network.createAndAddLink(scenario.createId("3"), node1, node2, 1000.0, 10.0, 2000.0, 1);
		final PopulationImpl population = scenario.getPopulation();

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);

		Stack<String> context = new Stack<String>(); // not sure the context is ever used in the reader...
		reader.startTag("plans", AttributesBuilder.getEmpty(), context);
		reader.startTag("person", new AttributesBuilder().add("id", "2").get(), context);
		reader.startTag("plan", new AttributesBuilder().add("selected", "no").get(), context);
		reader.startTag("act", new AttributesBuilder().add("type", "h").add("link", "3").get(), context);
		reader.endTag("act", "", context);
		reader.startTag("leg", new AttributesBuilder().add("mode", "car").get(), context);
		reader.endTag("leg", "", context);
		reader.startTag("act", new AttributesBuilder().add("type", "h").add("link", "2").get(), context);
		reader.endTag("plan", "", context);
		reader.endTag("person", "", context);
		reader.endTag("plans", "", context);

		assertEquals(1, population.getPersons().size());
		Person person = population.getPersons().get(scenario.createId("2"));
		Plan plan = person.getPlans().get(0);
		assertEquals(link3.getId(), ((Activity) plan.getPlanElements().get(0)).getLinkId());
		assertEquals(scenario.createId("2"), ((Activity) plan.getPlanElements().get(2)).getLinkId());
	}

}
