package org.matsim.core.network;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NetworkCollectorTest {

    private static Network getNetworkFromExample() {

        var networkUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("berlin"), "network.xml.gz");
        var network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readURL(networkUrl);
        return network;
    }

	@Test
	void testWithSequentialStream() {

        var network = getNetworkFromExample();

        var collectedNetwork = network.getLinks().values().stream()
                .collect(NetworkUtils.getCollector());

        assertTrue(NetworkUtils.compare(network, collectedNetwork));
    }

	@Test
	void testWithParallelStream() {

        var network = getNetworkFromExample();

        var collectedNetwork = network.getLinks().values().parallelStream()
                .collect(NetworkUtils.getCollector());

        assertTrue(NetworkUtils.compare(network, collectedNetwork));
    }

	@Test
	void testWithFilter() {

        var network = getNetworkFromExample();
        // choose link 73 because both of its nodes have multiple in and out links
        var filterId = Id.createLinkId("73");

        var collectedNetwork = network.getLinks().values().stream()
                .filter(link -> !link.getId().equals(filterId))
                .collect(NetworkUtils.getCollector());

        // make sure the link is not in filtered network
        assertFalse(collectedNetwork.getLinks().containsKey(filterId));

        // also the internal bookkeeping of in and out links should be without the filtered link
        for (Node node : collectedNetwork.getNodes().values()) {
            assertFalse(node.getInLinks().containsKey(filterId));
            assertFalse(node.getOutLinks().containsKey(filterId));
        }

        // make sure the original network still contains the link
        assertTrue(network.getLinks().containsKey(filterId));
        assertTrue(network.getLinks().get(filterId).getFromNode().getOutLinks().containsKey(filterId));
        assertTrue(network.getLinks().get(filterId).getToNode().getInLinks().containsKey(filterId));
    }
}