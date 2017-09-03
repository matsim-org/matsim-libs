/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWriterHandlerImplV4Test.java
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

package org.matsim.core.population.io;

import java.util.List;
import java.util.Stack;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.testcases.MatsimTestUtils;
import org.xml.sax.Attributes;

/**
 * @author mrieser
 */
public class PopulationWriterHandlerImplV5Test {

	@Rule public MatsimTestUtils util = new MatsimTestUtils();

	@Test
	public void test_writeNetworkRoute_sameStartEndLink() {
		doTestWriteNetworkRoute("1", "", "1", "1");
		
		// round trip
		doTestWriteNetworkRoute("1", "2 3", "1", "1 2 3 1");
	}

	@Test
	public void test_writeNetworkRoute_consequentLinks() {
		doTestWriteNetworkRoute("1", "", "2", "1 2");
	}

	@Test
	public void test_writeNetworkRoute_regularCase() {
		doTestWriteNetworkRoute("1", "2", "3", "1 2 3");
		doTestWriteNetworkRoute("1", "2 3", "4", "1 2 3 4");
	}

	private NetworkRoute doTestWriteNetworkRoute(final String startLinkId, final String linkIds, final String endLinkId, final String expectedRouteSerialization) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(this.util.loadConfig((String) null));
		Id<Link> idFrom = Id.create(startLinkId, Link.class);
		Id<Link> idTo = Id.create(endLinkId, Link.class);

		MutableScenario tmpScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop = tmpScenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		Person person = pb.createPerson(Id.create(1, Person.class));
		Plan plan = (Plan) pb.createPlan();
		plan.setPerson(person);
		plan.addActivity(pb.createActivityFromLinkId("h", idFrom));
		Leg leg = pb.createLeg(TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(idFrom, idTo);
		List<Id<Link>> routeLinkIds = NetworkUtils.getLinkIds(linkIds);
		route.setLinkIds(idFrom, routeLinkIds, idTo);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", idTo));
		person.addPlan(plan);
		pop.addPerson(person);

		String filename = this.util.getOutputDirectory() + "population.xml";
		new PopulationWriter(pop, null).writeV5(filename);

		Population pop2 = scenario.getPopulation();
		RouteInterceptingPopulationReader popReader = new RouteInterceptingPopulationReader(new PopulationReaderMatsimV5(scenario));
		popReader.readFile(filename);
		Person person2 = pop2.getPersons().get(Id.create(1, Person.class));
		Leg leg2 = (Leg) person2.getPlans().get(0).getPlanElements().get(1);
		Route route2 = leg2.getRoute();
		Assert.assertEquals(expectedRouteSerialization, popReader.interceptedRouteContent.trim());
		Assert.assertTrue("read route is of class " + route2.getClass().getCanonicalName(), route2 instanceof NetworkRoute);
		NetworkRoute nr = (NetworkRoute) route2;
		Assert.assertEquals("wrong start link", startLinkId, nr.getStartLinkId().toString());
		Assert.assertEquals("wrong end link", endLinkId, nr.getEndLinkId().toString());
		Assert.assertEquals("wrong number of links in route", routeLinkIds.size(), nr.getLinkIds().size());
		for (int i = 0; i < routeLinkIds.size(); i++) {
			Assert.assertEquals("wrong link in route at position " + i, routeLinkIds.get(i), nr.getLinkIds().get(i));
		}
		return nr;
	}
	
	@Test
	public void testWriteGenericRouteRoute() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(this.util.loadConfig((String) null));
		String startLinkId = "1";
		String endLinkId = "4";
		Id<Link> idFrom = Id.create(startLinkId, Link.class);
		Id<Link> idTo = Id.create(endLinkId, Link.class);
		MutableScenario tmpScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop = tmpScenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		Person person = pb.createPerson(Id.create(1, Person.class));
		Plan plan = (Plan) pb.createPlan();
		plan.setPerson(person);
		plan.addActivity(pb.createActivityFromLinkId("h", idFrom));
		Leg leg = pb.createLeg(TransportMode.walk);
		Route route = RouteUtils.createGenericRouteImpl(idFrom, idTo);
		final double travTime = 60.0 * 60.0;
		route.setTravelTime(travTime); 
		final double dist = 100.0;
		route.setDistance(dist); 
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", Id.create("2", Link.class)));
		person.addPlan(plan);
		pop.addPerson(person);

		String filename = this.util.getOutputDirectory() + "population.xml";
		new PopulationWriter(pop, null).writeV5(filename);

		Population pop2 = scenario.getPopulation();
		PopulationReaderMatsimV5 popReader = new PopulationReaderMatsimV5(scenario);
		popReader.readFile(filename);
		Person person2 = pop2.getPersons().get(Id.create(1, Person.class));
		Leg leg2 = (Leg) person2.getPlans().get(0).getPlanElements().get(1);
		Route route2 = leg2.getRoute();
//		Assert.assertTrue("read route is of class " + route2.getClass().getCanonicalName(), route2 instanceof GenericRouteImpl);
		Assert.assertEquals("wrong start link", startLinkId, route2.getStartLinkId().toString());
		Assert.assertEquals("wrong end link", endLinkId, route2.getEndLinkId().toString());
		Assert.assertEquals("wrong travel time", travTime, route2.getTravelTime(), 1e-9);
		Assert.assertEquals("wrong distance", dist, route2.getDistance(), 1e-9);
	}

	private static final class RouteInterceptingPopulationReader extends MatsimXmlParser implements MatsimReader {
		private final MatsimXmlParser delegate;
		/*package*/ String interceptedRouteContent = null;

		public RouteInterceptingPopulationReader(final MatsimXmlParser delegate) {
			this.delegate = delegate;
		}

		@Override
		public void startTag(String name, Attributes atts, Stack<String> context) {
			this.delegate.startTag(name, atts, context);
		}

		@Override
		public void endTag(String name, String content, Stack<String> context) {
			if (name.equals("route")) {
				this.interceptedRouteContent = content;
			}
			this.delegate.endTag(name, content, context);
		}
	}
}
