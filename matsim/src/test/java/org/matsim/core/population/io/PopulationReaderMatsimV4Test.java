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

package org.matsim.core.population.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.pt.routes.TransitPassengerRoute;
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
	void testReadRoute() throws SAXException, ParserConfigurationException, IOException {
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = scenario.getNetwork();
		final Population population = scenario.getPopulation();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");

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

		Assertions.assertEquals(2, population.getPersons().size(), "population size.");
		Person person1 = population.getPersons().get(Id.create("1", Person.class));
		Plan plan1 = person1.getPlans().get(0);
		Leg leg1a = (Leg) plan1.getPlanElements().get(1);
		Route route1a = leg1a.getRoute();
		Assertions.assertEquals(network.getLinks().get(Id.create("1", Link.class)).getId(), route1a.getStartLinkId(), "different startLink for first leg.");
		Assertions.assertEquals(network.getLinks().get(Id.create("20", Link.class)).getId(), route1a.getEndLinkId(), "different endLink for first leg.");
		Leg leg1b = (Leg) plan1.getPlanElements().get(3);
		Route route1b = leg1b.getRoute();
		Assertions.assertEquals(network.getLinks().get(Id.create("20", Link.class)).getId(), route1b.getStartLinkId(), "different startLink for second leg.");
		Assertions.assertEquals(network.getLinks().get(Id.create("20", Link.class)).getId(), route1b.getEndLinkId(), "different endLink for second leg.");
		Leg leg1c = (Leg) plan1.getPlanElements().get(5);
		Route route1c = leg1c.getRoute();
		Assertions.assertEquals(network.getLinks().get(Id.create("20", Link.class)).getId(), route1c.getStartLinkId(), "different startLink for third leg.");
		Assertions.assertEquals(network.getLinks().get(Id.create("1", Link.class)).getId(), route1c.getEndLinkId(), "different endLink for third leg.");

		Person person2 = population.getPersons().get(Id.create("2", Person.class));
		Plan plan2 = person2.getPlans().get(0);
		Leg leg2a = (Leg) plan2.getPlanElements().get(1);
		Route route2a = leg2a.getRoute();
		Assertions.assertEquals(network.getLinks().get(Id.create("2", Link.class)).getId(), route2a.getStartLinkId(), "different startLink for first leg.");
		Assertions.assertEquals(network.getLinks().get(Id.create("20", Link.class)).getId(), route2a.getEndLinkId(), "different endLink for first leg.");
		Leg leg2b = (Leg) plan2.getPlanElements().get(3);
		Route route2b = leg2b.getRoute();
		Assertions.assertEquals(network.getLinks().get(Id.create("20", Link.class)).getId(), route2b.getStartLinkId(), "different startLink for third leg.");
		Assertions.assertEquals(network.getLinks().get(Id.create("1", Link.class)).getId(), route2b.getEndLinkId(), "different endLink for third leg.");
	}

	/**
	 * Tests that reading in plans that contain route-elements, but the activities before or after do
	 * not contain any link-information, works. This can be the case if the given routes are part of
	 * a teleported transport mode, where links may not be required.
	 *
	 * @author mrieser
	 */
	@Test
	void testReadRouteWithoutActivityLinks() {
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Population population = scenario.getPopulation();

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

		Assertions.assertEquals(1, population.getPersons().size(), "population size.");
		Person person1 = population.getPersons().get(Id.create("981", Person.class));
		Plan plan1 = person1.getPlans().get(0);
		Leg leg1 = (Leg) plan1.getPlanElements().get(1);
		Route route1 = leg1.getRoute();
		Assertions.assertNotNull(route1);
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testReadActivity() {
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Network network = (Network) scenario.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0, 1000));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode, toNode, 1000.0, 10.0, 2000.0, (double) 1 );
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

		Assertions.assertEquals(1, population.getPersons().size());
		Person person = population.getPersons().get(Id.create("2", Person.class));
		Plan plan = person.getPlans().get(0);
		Assertions.assertEquals(link3.getId(), ((Activity) plan.getPlanElements().get(0)).getLinkId());
		Assertions.assertEquals(Id.create("2", Link.class), ((Activity) plan.getPlanElements().get(2)).getLinkId());
	}

	@Test
	void testReadingRoutesWithoutType() {
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		final Population population = scenario.getPopulation();

		String str = "<?xml version=\"1.0\" ?>"+
		"<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">"+
		"<plans>"+
		"<person id=\"1\">"+
		"	<plan>"+
		"		<act type=\"h\" x=\"-25000\" y=\"0\" end_time=\"06:00\" />"+
		"		<leg mode=\"car\">"+
		"     <route trav_time=\"00:05\">"+
		"     </route>"+
		"   </leg>"+
		"		<act type=\"w\" x=\"10000\" y=\"0\" end_time=\"15:00\"/>"+
		"		<leg mode=\"walk\">"+
		"     <route trav_time=\"00:15\">"+
		"     </route>"+
		"   </leg>"+
		"		<act type=\"s\" x=\"15000\" y=\"0\" end_time=\"15:30\"/>"+
		"		<leg mode=\"pt\">"+
		"     <route trav_time=\"00:15\">"+
		"       PT1===1===Blue Line===1to3===2a"+
		"     </route>"+
		"   </leg>"+
		"		<act type=\"h\" x=\"-25000\" y=\"0\" start_time=\"16:00\" />"+
		"	</plan>"+
		"</person>"+
		"</plans>";
		reader.parse(new ByteArrayInputStream(str.getBytes()));

		Plan plan = population.getPersons().get(Id.create(1, Person.class)).getSelectedPlan();
		Assertions.assertEquals(7, plan.getPlanElements().size());
		Assertions.assertTrue(plan.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(plan.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(2) instanceof Activity);
		Assertions.assertTrue(plan.getPlanElements().get(3) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(4) instanceof Activity);
		Assertions.assertTrue(plan.getPlanElements().get(5) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(6) instanceof Activity);
		
		Leg leg1 = (Leg) plan.getPlanElements().get(1);
		Route route1 = leg1.getRoute();
		Leg leg2 = (Leg) plan.getPlanElements().get(3);
		Route route2 = leg2.getRoute();
		Leg leg3 = (Leg) plan.getPlanElements().get(5);
		Route route3 = leg3.getRoute();
		
		Assertions.assertTrue(route1 instanceof NetworkRoute);
//		Assert.assertTrue(route2 instanceof GenericRouteImpl);
		Assertions.assertTrue(route3 instanceof TransitPassengerRoute);
	}

	@Test
	void testRepeatingLegs() {
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		final Population population = scenario.getPopulation();

		String str = "<?xml version=\"1.0\" ?>"+
		"<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">"+
		"<plans>"+
		"<person id=\"1\">"+
		"	<plan>"+
		"		<act type=\"h\" x=\"-25000\" y=\"0\" end_time=\"06:00\" />"+
		"		<leg mode=\"walk\" />"+
		"		<leg mode=\"pt\" />"+
		"		<leg mode=\"walk\" />"+
		"		<act type=\"w\" x=\"10000\" y=\"0\" dur=\"03:30\" />"+
		"	</plan>"+
		"</person>"+
		"</plans>";
		reader.parse(new ByteArrayInputStream(str.getBytes()));

		Plan plan = population.getPersons().get(Id.create(1, Person.class)).getSelectedPlan();
		Assertions.assertEquals(5, plan.getPlanElements().size());
		Assertions.assertTrue(plan.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(plan.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(2) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(3) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(4) instanceof Activity);
	}

	@Test
	void testRepeatingActs() {
		final MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		final Population population = scenario.getPopulation();

		String str = "<?xml version=\"1.0\" ?>"+
		"<!DOCTYPE plans SYSTEM \"http://www.matsim.org/files/dtd/plans_v4.dtd\">"+
		"<plans>"+
		"<person id=\"1\">"+
		"	<plan>"+
		"		<act type=\"h\" x=\"-25000\" y=\"0\" end_time=\"06:00\" />"+
		"		<leg mode=\"walk\" />"+
		"		<act type=\"w\" x=\"10000\" y=\"0\" dur=\"03:30\" />"+
		"		<act type=\"l\" x=\"10000\" y=\"0\" dur=\"00:30\" />"+
		"	</plan>"+
		"</person>"+
		"</plans>";
		reader.parse(new ByteArrayInputStream(str.getBytes()));

		Plan plan = population.getPersons().get(Id.create(1, Person.class)).getSelectedPlan();
		Assertions.assertEquals(4, plan.getPlanElements().size());
		Assertions.assertTrue(plan.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(plan.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(plan.getPlanElements().get(2) instanceof Activity);
		Assertions.assertTrue(plan.getPlanElements().get(3) instanceof Activity);
	}

	private static class XmlParserTestHelper {
		private final MatsimXmlParser parser;
		private final Stack<String> context = new Stack<String>();

		public XmlParserTestHelper(final MatsimXmlParser parser) {
			this.parser = parser;
		}

		public void startTag(final String name) {
			startTag(name, AttributesBuilder.getEmpty());
		}

		public void startTag(final String name, final String[][] atts) {
			AttributesBuilder builder = new AttributesBuilder();
			for (String[] attribute : atts) {
				builder.add(attribute[0], attribute[1]);
			}
			startTag(name, builder.get());
		}

		private void startTag(final String name, final Attributes atts) {
			this.parser.startTag(name, atts, this.context);
			this.context.push(name);
		}

		public void endTag() {
			endTag(null);
		}

		public void endTag(final String content) {
			String name = this.context.pop();
			this.parser.endTag(name, content, this.context);
		}
	}

}
