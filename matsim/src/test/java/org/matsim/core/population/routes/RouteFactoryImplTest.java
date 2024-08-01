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

package org.matsim.core.population.routes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.routes.heavycompressed.HeavyCompressedNetworkRoute;
import org.matsim.core.population.routes.mediumcompressed.MediumCompressedNetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / senozon
 */
public class RouteFactoryImplTest {

	@Test
	void testConstructor_DefaultNetworkRouteType() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationFactory pf = scenario.getPopulation().getFactory();

		Id<Link> linkId = Id.create(1, Link.class);
		final Id<Link> startLinkId = linkId;
		final Id<Link> endLinkId = linkId;
		Assertions.assertEquals(GenericRouteImpl.class, pf.getRouteFactories().createRoute(Route.class, startLinkId, endLinkId).getClass());
	}

	@Test
	void testConstructor_LinkNetworkRouteType() {
		Config config = ConfigUtils.createConfig();
		config.plans().setNetworkRouteType(PlansConfigGroup.NetworkRouteType.LinkNetworkRoute);
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationFactory pf = scenario.getPopulation().getFactory();

		Id<Link> linkId = Id.create(1, Link.class);
		final Id<Link> startLinkId = linkId;
		final Id<Link> endLinkId = linkId;
		Assertions.assertEquals(LinkNetworkRouteImpl.class, pf.getRouteFactories().createRoute(NetworkRoute.class, startLinkId, endLinkId).getClass());
	}

	@Test
	void testConstructor_HeavyCompressedNetworkRouteType() {
		Config config = ConfigUtils.createConfig();
		config.plans().setNetworkRouteType(PlansConfigGroup.NetworkRouteType.HeavyCompressedNetworkRoute);
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationFactory pf = scenario.getPopulation().getFactory();

		Id<Link> linkId = Id.create(1, Link.class);
		final Id<Link> startLinkId = linkId;
		final Id<Link> endLinkId = linkId;
		Assertions.assertEquals(HeavyCompressedNetworkRoute.class, pf.getRouteFactories().createRoute(NetworkRoute.class, startLinkId, endLinkId).getClass());
	}

	@Test
	void testConstructor_MediumCompressedNetworkRouteType() {
		Config config = ConfigUtils.createConfig();
		config.plans().setNetworkRouteType(PlansConfigGroup.NetworkRouteType.MediumCompressedNetworkRoute);
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationFactory pf = scenario.getPopulation().getFactory();

		Id<Link> linkId = Id.create(1, Link.class);
		final Id<Link> startLinkId = linkId;
		final Id<Link> endLinkId = linkId;
		Assertions.assertEquals(MediumCompressedNetworkRoute.class, pf.getRouteFactories().createRoute(NetworkRoute.class, startLinkId, endLinkId).getClass());
	}

}
