/* *********************************************************************** *
 * project: org.matsim.*
 * InvertertedNetworkLegRouterTest
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
package org.matsim.core.router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;

/**
 * Tests if the expected links are found when using the MultimodalLinkChooserDefaultImpl
 * @author simei94
 *
 */

public class MultimodalLinkChooserDefaultImplTest {

	@Test
	void testDecideOnLink() {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Network network = scenario.getNetwork();
        Network netWithoutLinks = NetworkUtils.createNetwork();
        Link networkLink;
        Link secondLink;

        NetworkFactory nf = network.getFactory();
        Node n1 = nf.createNode(Id.createNodeId(1), new Coord((double) 0, (double) 0));
        Node n2 = nf.createNode(Id.createNodeId(2), new Coord((double) 1000, (double) 0));
        Node n3 = nf.createNode(Id.createNodeId(3), new Coord((double) 1000, (double) 1000));
        networkLink = nf.createLink(Id.createLinkId("linkWithId"), n1, n2);
        secondLink = nf.createLink(Id.createLinkId("secondLink"), n2, n3);

        network.addNode(n1);
        network.addNode(n2);
        network.addNode(n3);
        network.addLink(networkLink);
        network.addLink(secondLink);

        ActivityFacilitiesFactory fac = new ActivityFacilitiesFactoryImpl();
        ActivityFacility facilityLinkIdNotNull = fac.createActivityFacility(Id.create("hasLinkId", ActivityFacility.class),
                new Coord(0,0),
                networkLink.getId());
        ActivityFacility facilityLinkIdNull = fac.createActivityFacility(Id.create("noLinkId", ActivityFacility.class),
                new Coord(0,0));

        MultimodalLinkChooser linkChooser = new MultimodalLinkChooserDefaultImpl();

        RoutingRequest request = DefaultRoutingRequest.of(facilityLinkIdNotNull, facilityLinkIdNull, 0, null, null);

        Link linkFromFacLinkId = linkChooser.decideAccessLink(request, TransportMode.car, network);

        Assertions.assertEquals(networkLink, linkFromFacLinkId);

        Link linkFromFacCoord = linkChooser.decideEgressLink(request, TransportMode.car, network);

        Assertions.assertEquals(networkLink, linkFromFacCoord);

        //not sure whether the following makes sense as we basically are some functionality of NetworkUtils (which is used in the linkChooser)
        //testing this with the decideOnLink method would mean causing a RuntimeException -sm 0622
        Link linkNotInNetwork = NetworkUtils.getNearestLink(netWithoutLinks, facilityLinkIdNull.getCoord());

        Assertions.assertNull(linkNotInNetwork);
    }
}
