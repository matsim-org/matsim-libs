/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractCHLeastCostPathCalculatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Intermediate base class for CH-based router tests.
 *
 * <p>Overrides {@link AbstractLeastCostPathCalculatorTest#testCalcLeastCostPath_withOptions()}
 * to accept any valid equal-cost intermediate node (3..11) in the equil network, since
 * the bidirectional CH search breaks ties differently from unidirectional Dijkstra.
 *
 * @author Steffen Axer
 */
public abstract class AbstractCHLeastCostPathCalculatorTest
        extends AbstractLeastCostPathCalculatorTestWithTurnRestrictions {

    @Override
    @Test
    void testCalcLeastCostPath_withOptions() throws SAXException, ParserConfigurationException, IOException {
        Config config = utils.loadConfig((String) null);
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");

        Node node1 = network.getNodes().get(Id.create("1", Node.class));
        Node node13 = network.getNodes().get(Id.create("13", Node.class));

        LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
        LeastCostPathCalculator.Path path = routerAlgo.calcLeastCostPath(node1, node13, 8.0 * 3600, null, null);

        assertEquals(5, path.nodes.size(), "number of nodes wrong.");
        assertEquals(4, path.links.size(), "number of links wrong.");
        assertThat(path.nodes.get(0)).isEqualTo(network.getNodes().get(Id.create("1", Node.class)));
        assertThat(path.nodes.get(1)).isEqualTo(network.getNodes().get(Id.create("2", Node.class)));
        // CH bidirectional search may break ties differently; any intermediate node 3..11 is valid.
        assertThat(path.nodes.get(2)).isIn(
                network.getNodes().get(Id.create("3", Node.class)),
                network.getNodes().get(Id.create("4", Node.class)),
                network.getNodes().get(Id.create("5", Node.class)),
                network.getNodes().get(Id.create("6", Node.class)),
                network.getNodes().get(Id.create("7", Node.class)),
                network.getNodes().get(Id.create("8", Node.class)),
                network.getNodes().get(Id.create("9", Node.class)),
                network.getNodes().get(Id.create("10", Node.class)),
                network.getNodes().get(Id.create("11", Node.class)));
        assertThat(path.nodes.get(3)).isEqualTo(network.getNodes().get(Id.create("12", Node.class)));
        assertThat(path.nodes.get(4)).isEqualTo(network.getNodes().get(Id.create("13", Node.class)));
        assertThat(path.links.get(0)).isEqualTo(network.getLinks().get(Id.create("1", Link.class)));
        // Any of the equal-cost links 2..10 (2->X) and 11..19 (X->12) are valid.
        assertThat(path.links.get(1)).isIn(
                network.getLinks().get(Id.create("2", Link.class)),
                network.getLinks().get(Id.create("3", Link.class)),
                network.getLinks().get(Id.create("4", Link.class)),
                network.getLinks().get(Id.create("5", Link.class)),
                network.getLinks().get(Id.create("6", Link.class)),
                network.getLinks().get(Id.create("7", Link.class)),
                network.getLinks().get(Id.create("8", Link.class)),
                network.getLinks().get(Id.create("9", Link.class)),
                network.getLinks().get(Id.create("10", Link.class)));
        assertThat(path.links.get(2)).isIn(
                network.getLinks().get(Id.create("11", Link.class)),
                network.getLinks().get(Id.create("12", Link.class)),
                network.getLinks().get(Id.create("13", Link.class)),
                network.getLinks().get(Id.create("14", Link.class)),
                network.getLinks().get(Id.create("15", Link.class)),
                network.getLinks().get(Id.create("16", Link.class)),
                network.getLinks().get(Id.create("17", Link.class)),
                network.getLinks().get(Id.create("18", Link.class)),
                network.getLinks().get(Id.create("19", Link.class)));
        assertThat(path.links.get(3)).isEqualTo(network.getLinks().get(Id.create("20", Link.class)));

        // Verify determinism: multiple calls must yield the same result.
        List<Node> expectedNodeList = path.nodes;
        List<Link> expectedLinkList = path.links;
        for (int i = 0; i < 20; i++) {
            path = routerAlgo.calcLeastCostPath(node1, node13, 8.0 * 3600, null, null);
            assertThat(path.nodes).isEqualTo(expectedNodeList);
            assertThat(path.links).isEqualTo(expectedLinkList);
        }
    }
}
