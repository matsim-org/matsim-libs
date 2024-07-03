package org.matsim.core.network.algorithms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// @formatter:off
/**
 * In this test, we restrict the allowed modes of a link. The base case is the equil scenario.
 * <p>
 *       /...                   ...\
 * ------o------------o------------o-----------
 * l1  (n2)   l6    (n7)   l15  (n12)   l20
 */
// @formatter:on
class NetworkModeRestrictionTest {

	/**
	 * Test that all links and nodes are removed if the network is not connected any more for every mode.
	 */
	@Test
	void testRemoveWholeNetwork() {
		Network network = getNetwork();

		Map<Id<Link>, Set<String>> changes = new HashMap<>();
		changes.put(Id.createLinkId(1), Set.of("car","bike"));

		new NetworkModeRestriction(changes).run(network);

		assertTrue(network.getLinks().isEmpty());
		assertTrue(network.getNodes().isEmpty());
	}

	/**
	 * Test that mode is removed from each node if there is not connected any more for this mode.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"car", "bike"})
	void testRemoveWholeMode(String modeToRemove) {
		Network network = getNetwork();

		int linksBefore = network.getLinks().size();
		int nodesBefore = network.getNodes().size();

		Map<Id<Link>, Set<String>> changes = new HashMap<>();
		changes.put(Id.createLinkId(1), Set.of(modeToRemove));

		new NetworkModeRestriction(changes).run(network);

		assertEquals(linksBefore, network.getLinks().size());
		assertEquals(nodesBefore, network.getNodes().size());

		Assertions.assertTrue(network.getLinks().values().stream()
									 .allMatch(l -> l.getAllowedModes().size() == 1 && l.getAllowedModes().contains(getOtherMode(modeToRemove))));
	}

	/**
	 * Test that removing a mode from link 6 or 15 also removes it from the other link.
	 */
	@ParameterizedTest
	@CsvSource({"6,car", "6,bike", "15,car", "15,bike"})
	void testRemoveModePartially(int link, String modeToRemove) {
		Network network = getNetwork();

		int linksBefore = network.getLinks().size();
		int nodesBefore = network.getNodes().size();

		//prepare removal
		Map<Id<Link>, Set<String>> changes = new HashMap<>();
		changes.put(Id.createLinkId(link), Set.of(modeToRemove));

		//remove
		new NetworkModeRestriction(changes).run(network);

		//links and nodes should all still be there
		assertEquals(linksBefore, network.getLinks().size());
		assertEquals(nodesBefore, network.getNodes().size());

		Set<String> link6Modes = network.getLinks().get(Id.createLinkId(6)).getAllowedModes();
		Set<String> link15Modes = network.getLinks().get(Id.createLinkId(15)).getAllowedModes();

		//link 6 and 15 should have the mode removed
		Assertions.assertEquals(1, link6Modes.size());
		Assertions.assertEquals(1, link15Modes.size());
		Assertions.assertTrue(link6Modes.contains(getOtherMode(modeToRemove)));
		Assertions.assertTrue(link15Modes.contains(getOtherMode(modeToRemove)));
	}

	/**
	 * Test that removing each mode from link 6 or 15 also removes both links at all.
	 */
	@ParameterizedTest
	@ValueSource(ints = {6, 15})
	void testRemovePartialNetwork(int link) {
		Network network = getNetwork();

		int linksBefore = network.getLinks().size();
		int nodesBefore = network.getNodes().size();

		//prepare removal
		Map<Id<Link>, Set<String>> changes = new HashMap<>();
		changes.put(Id.createLinkId(link), Set.of("car", "bike"));

		//remove
		new NetworkModeRestriction(changes).run(network);

		//both links and the node in between should be removed
		assertEquals(linksBefore - 2, network.getLinks().size());
		assertEquals(nodesBefore - 1, network.getNodes().size());

		//check node
		Assertions.assertNull(network.getNodes().get(Id.createNodeId(7)));

		//check links
		Assertions.assertNull(network.getLinks().get(Id.createLinkId(6)));
		Assertions.assertNull(network.getLinks().get(Id.createLinkId(15)));
	}


	private static Network getNetwork() {
		Network network = NetworkUtils.readNetwork(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml").toString());
		network.getLinks().values().forEach(l -> l.setAllowedModes(Set.of("car", "bike")));
		return network;
	}

	private String getOtherMode(String mode) {
		return mode.equals("car") ? "bike" : "car";
	}
}
