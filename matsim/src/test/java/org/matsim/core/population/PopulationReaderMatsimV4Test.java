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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.testcases.utils.AttributesBuilder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PopulationReaderMatsimV4Test {

	/**
	 * @author mrieser
	 *
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	@Test
	public void testReadRoute() throws SAXException, ParserConfigurationException, IOException {
		final ScenarioImpl scenario = new ScenarioImpl();
		final Network network = scenario.getNetwork();
		final PopulationImpl population = scenario.getPopulation();
		new MatsimNetworkReader(scenario).parse("test/scenarios/equil/network.xml");

		MatsimXmlParser parser = new PopulationReaderMatsimV4(scenario);
		XmlParserTestHelper tester = new XmlParserTestHelper(parser);

		tester.startTag("plans");

		tester.startTag("person", new String[][]{{"id", "1"}});
		tester.startTag("plan", new String[][]{{"selected", "no"}});
		tester.startTag("act", new String[][]{{"type", "h"}, {"x", "-25000"}, {"y", "0"}, {"link", "1"}, {"end_time", "06:00"}});
		tester.endTag();
		tester.startTag("leg", new String[][]{{"mode", "car"}});
		tester.startTag("route");
		tester.endTag("2 7 12");
		tester.endTag();
		tester.startTag("act", new String[][]{{"type", "w"}, {"x", "10000"}, {"y", "0"}, {"link", "20"}, {"dur", "00:10"}});
		tester.endTag();
		tester.startTag("leg", new String[][]{{"mode", "car"}});
		tester.startTag("route");
		tester.endTag(" ");
		tester.endTag();
		tester.startTag("act", new String[][]{{"type", "w"}, {"x", "10000"}, {"y", "0"}, {"link", "20"}, {"dur", "03:30"}});
		tester.endTag();
		tester.startTag("leg", new String[][]{{"mode", "car"}});
		tester.startTag("route");
		tester.endTag("13 14 15 1");
		tester.endTag();
		tester.startTag("act", new String[][]{{"type", "4"}, {"x", "-25000"}, {"y", "0"}, {"link", "1"}});
		tester.endTag();
		tester.endTag();
		tester.endTag();

		tester.startTag("person", new String[][]{{"id", "2"}});
		tester.startTag("plan", new String[][]{{"selected", "no"}});
		tester.startTag("act", new String[][]{{"type", "h"}, {"x", "-25000"}, {"y", "0"}, {"link", "2"}, {"end_time", "06:00"}});
		tester.endTag();
		tester.startTag("leg", new String[][]{{"mode", "car"}});
		tester.startTag("route");
		tester.endTag("6 12");
		tester.endTag();
		tester.startTag("act", new String[][]{{"type", "w"}, {"x", "10000"}, {"y", "0"}, {"link", "20"}, {"dur", "03:30"}});
		tester.endTag();
		tester.startTag("leg", new String[][]{{"mode", "car"}});
		tester.startTag("route");
		tester.endTag("13 14 15 1");
		tester.endTag();
		tester.startTag("act", new String[][]{{"type", "4"}, {"x", "-25000"}, {"y", "0"}, {"link", "1"}});
		tester.endTag();
		tester.endTag();
		tester.endTag();

		tester.endTag();

		Assert.assertEquals("population size.", 2, population.getPersons().size());
		Person person1 = population.getPersons().get(scenario.createId("1"));
		Plan plan1 = person1.getPlans().get(0);
		LegImpl leg1a = (LegImpl) plan1.getPlanElements().get(1);
		RouteWRefs route1a = leg1a.getRoute();
		Assert.assertEquals("different startLink for first leg.", network.getLinks().get(scenario.createId("1")).getId(), route1a.getStartLinkId());
		Assert.assertEquals("different endLink for first leg.", network.getLinks().get(scenario.createId("20")).getId(), route1a.getEndLinkId());
		LegImpl leg1b = (LegImpl) plan1.getPlanElements().get(3);
		RouteWRefs route1b = leg1b.getRoute();
		Assert.assertEquals("different startLink for second leg.", network.getLinks().get(scenario.createId("20")).getId(), route1b.getStartLinkId());
		Assert.assertEquals("different endLink for second leg.", network.getLinks().get(scenario.createId("20")).getId(), route1b.getEndLinkId());
		LegImpl leg1c = (LegImpl) plan1.getPlanElements().get(5);
		RouteWRefs route1c = leg1c.getRoute();
		Assert.assertEquals("different startLink for third leg.", network.getLinks().get(scenario.createId("20")).getId(), route1c.getStartLinkId());
		Assert.assertEquals("different endLink for third leg.", network.getLinks().get(scenario.createId("1")).getId(), route1c.getEndLinkId());

		Person person2 = population.getPersons().get(scenario.createId("2"));
		Plan plan2 = person2.getPlans().get(0);
		LegImpl leg2a = (LegImpl) plan2.getPlanElements().get(1);
		RouteWRefs route2a = leg2a.getRoute();
		Assert.assertEquals("different startLink for first leg.", network.getLinks().get(scenario.createId("2")).getId(), route2a.getStartLinkId());
		Assert.assertEquals("different endLink for first leg.", network.getLinks().get(scenario.createId("20")).getId(), route2a.getEndLinkId());
		LegImpl leg2b = (LegImpl) plan2.getPlanElements().get(3);
		RouteWRefs route2b = leg2b.getRoute();
		Assert.assertEquals("different startLink for third leg.", network.getLinks().get(scenario.createId("20")).getId(), route2b.getStartLinkId());
		Assert.assertEquals("different endLink for third leg.", network.getLinks().get(scenario.createId("1")).getId(), route2b.getEndLinkId());
	}

	/**
	 * Tests that reading in plans that contain route-elements, but the activities before or after do
	 * not contain any link-information, works. This can be the case if the given routes are part of
	 * a teleported transport mode, where links may not be required.
	 *
	 * @author mrieser
	 */
	@Test
	public void testReadRouteWithoutActivityLinks() {
		final ScenarioImpl scenario = new ScenarioImpl();
		final PopulationImpl population = scenario.getPopulation();

		PopulationReaderMatsimV4 parser = new PopulationReaderMatsimV4(scenario);

		Stack<String> context = new Stack<String>(); // seems to be not used by reader
		parser.startTag("plans", new AttributesBuilder().add("name", "").get(), context);
		parser.startTag("person", new AttributesBuilder().add("id", "981").get(), context);
		parser.startTag("plan", new AttributesBuilder().add("selected", "yes").get(), context);
		parser.startTag("act", new AttributesBuilder().add("type", "h").add("x", "125").add("y", "500").add("end_time", "08:00:00").get(), context);
		parser.endTag("act", null, context);
		parser.startTag("leg", new AttributesBuilder().add("mode", "pt").get(), context);
		parser.startTag("route", new AttributesBuilder().add("dist", "1980.11").get(), context);
		parser.endTag("route", "   ", context);
		parser.endTag("leg", null, context);
		parser.startTag("act", new AttributesBuilder().add("type", "w").add("x", "500").add("y", "1100").add("start_time", "10:05:00").get(), context);
		parser.endTag("act", null, context);
		parser.endTag("plan", null, context);
		parser.endTag("person", null, context);
		parser.endTag("plans", null, context);

		Assert.assertEquals("population size.", 1, population.getPersons().size());
		Person person1 = population.getPersons().get(scenario.createId("981"));
		Plan plan1 = person1.getPlans().get(0);
		LegImpl leg1 = (LegImpl) plan1.getPlanElements().get(1);
		RouteWRefs route1 = leg1.getRoute();
		Assert.assertNotNull(route1);
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testReadActivity() {
		final ScenarioImpl scenario = new ScenarioImpl();
		final NetworkLayer network = scenario.getNetwork();
		Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(0, 0));
		Node node2 = network.createAndAddNode(scenario.createId("2"), scenario.createCoord(0, 1000));
		Link link3 = network.createAndAddLink(scenario.createId("3"), node1, node2, 1000.0, 10.0, 2000.0, 1);
		final Population population = scenario.getPopulation();

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

		Assert.assertEquals(1, population.getPersons().size());
		Person person = population.getPersons().get(scenario.createId("2"));
		Plan plan = person.getPlans().get(0);
		Assert.assertEquals(link3.getId(), ((Activity) plan.getPlanElements().get(0)).getLinkId());
		Assert.assertEquals(scenario.createId("2"), ((Activity) plan.getPlanElements().get(2)).getLinkId());
	}

	private static class XmlParserTestHelper {
		private final MatsimXmlParser parser;
		private final Stack<String> context = new Stack<String>();

		public XmlParserTestHelper(MatsimXmlParser parser) {
			this.parser = parser;
		}

		public void startTag(String name) {
			startTag(name, AttributesBuilder.getEmpty());
		}

		public void startTag(String name, String[][] atts) {
			AttributesBuilder builder = new AttributesBuilder();
			for (String[] attribute : atts) {
				builder.add(attribute[0], attribute[1]);
			}
			startTag(name, builder.get());
		}

		private void startTag(String name, Attributes atts) {
			parser.startTag(name, atts, context);
			this.context.push(name);
		}

		public void endTag() {
			endTag(null);
		}

		public void endTag(String content) {
			String name = this.context.pop();
			parser.endTag(name, content, this.context);
		}
	}

}
