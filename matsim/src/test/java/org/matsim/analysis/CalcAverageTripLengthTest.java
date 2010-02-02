package org.matsim.analysis;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.testcases.MatsimTestUtils;

public class CalcAverageTripLengthTest {

	@Test
	public void testWithRoute() {
		Scenario scenario = new ScenarioImpl();
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		NetworkFactory nf = network.getFactory();
		network.addNode(nf.createNode(scenario.createId("1"), scenario.createCoord(0, 0)));
		network.addNode(nf.createNode(scenario.createId("2"), scenario.createCoord(50, 0)));
		network.addNode(nf.createNode(scenario.createId("3"), scenario.createCoord(100, 0)));
		network.addNode(nf.createNode(scenario.createId("4"), scenario.createCoord(200, 0)));
		network.addNode(nf.createNode(scenario.createId("5"), scenario.createCoord(400, 0)));
		Link l1 = nf.createLink(scenario.createId("1"), scenario.createId("1"), scenario.createId("2"));
		Link l2 = nf.createLink(scenario.createId("2"), scenario.createId("2"), scenario.createId("3"));
		Link l3 = nf.createLink(scenario.createId("3"), scenario.createId("3"), scenario.createId("4"));
		Link l4 = nf.createLink(scenario.createId("4"), scenario.createId("4"), scenario.createId("5"));
		l1.setLength(50);
		l2.setLength(100);
		l3.setLength(200);
		l4.setLength(400);
		network.addLink(l1);
		network.addLink(l2);
		network.addLink(l3);
		network.addLink(l4);

		PopulationFactory pf = population.getFactory();

		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("h", l1.getId());
		Leg leg = pf.createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(l1, l3);
		ArrayList<Link> links = new ArrayList<Link>();
		links.add(l2);
		route.setLinks(l1, links, l3);
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromLinkId("w", l3.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		// test simple route, startLink should not be included, endLink should
		CalcAverageTripLength catl = new CalcAverageTripLength(network);
		Assert.assertEquals(0.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
		catl.run(plan);
		Assert.assertEquals(300.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);

		// extend route by one link, test again
		links.add(l3);
		route.setLinks(l1, links, l4);
		((ActivityImpl) act2).setLinkId(l4.getId());

		catl = new CalcAverageTripLength(network);
		catl.run(plan);
		Assert.assertEquals(500.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);

		// don't reset catl, modify route, test average
		links.remove(1);
		route.setLinks(l1, links, l3);
		((ActivityImpl) act2).setLinkId(l3.getId());

		catl.run(plan);
		Assert.assertEquals(400.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testWithRoute_OneLinkRoute() {
		Scenario scenario = new ScenarioImpl();
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		NetworkFactory nf = network.getFactory();
		network.addNode(nf.createNode(scenario.createId("1"), scenario.createCoord(0, 0)));
		network.addNode(nf.createNode(scenario.createId("2"), scenario.createCoord(50, 0)));
		network.addNode(nf.createNode(scenario.createId("3"), scenario.createCoord(100, 0)));
		Link l1 = nf.createLink(scenario.createId("1"), scenario.createId("1"), scenario.createId("2"));
		Link l2 = nf.createLink(scenario.createId("2"), scenario.createId("2"), scenario.createId("3"));
		l1.setLength(50);
		l2.setLength(100);
		network.addLink(l1);
		network.addLink(l2);

		PopulationFactory pf = population.getFactory();

		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("h", l1.getId());
		Leg leg = pf.createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(l1, l2);
		route.setLinks(l1, new ArrayList<Link>(0), l2);
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromLinkId("w", l2.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		// test simple route, startLink should not be included, endLink should be
		CalcAverageTripLength catl = new CalcAverageTripLength(network);
		catl.run(plan);
		Assert.assertEquals(100.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testWithRoute_StartEndOnSameLink() {
		Scenario scenario = new ScenarioImpl();
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		NetworkFactory nf = network.getFactory();
		network.addNode(nf.createNode(scenario.createId("1"), scenario.createCoord(0, 0)));
		network.addNode(nf.createNode(scenario.createId("2"), scenario.createCoord(50, 0)));
		Link l1 = nf.createLink(scenario.createId("1"), scenario.createId("1"), scenario.createId("2"));
		l1.setLength(50);
		network.addLink(l1);

		PopulationFactory pf = population.getFactory();

		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("h", l1.getId());
		Leg leg = pf.createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(l1, l1);
		route.setLinks(l1, new ArrayList<Link>(0), l1);
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromLinkId("w", l1.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		// test simple route, none of the links should be included, as there is no real traffic
		CalcAverageTripLength catl = new CalcAverageTripLength(network);
		catl.run(plan);
		Assert.assertEquals(0.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
	}

}
