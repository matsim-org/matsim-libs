package org.matsim.core.network.turnRestrictions;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import com.google.common.base.Verify;

import jakarta.annotation.Nullable;

/**
 * @author hrewald
 */
class TurnRestrictionsNetworkCleanerTest {

	private static final String CAR = "car";
	private static final String BUS = "bus";
	private static final String BIKE = "bike";
	private static final Set<String> MODES = Set.of(CAR, BUS, BIKE);
	private static final List<String> LINK_SUFFIXES_WITH_DNL = List.of("23", "32", "12");

	@TempDir
	private static File tempFolder;

	@ParameterizedTest
	@ValueSource(strings = { CAR, BUS, BIKE })
	void cleanTest(String dnlMode) {

		Network network = crossingWithForbiddenUTurn(MODES, dnlMode);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, dnlMode);

		// * --------------------------------------------------

		Network expectedNetwork = crossingWithForbiddenUTurn(MODES, dnlMode);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
	}

	@Test
	void testNoChange() {

		Network network = createNetwork();
		addDisallowedNextLinks(network);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addDisallowedNextLinks(expectedNetwork);

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
	}

	@Test
	void testNoChangeWithOtherMode() {

		Network network = createNetwork();
		addDisallowedNextLinks(network);
		network.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addDisallowedNextLinks(expectedNetwork);
		expectedNetwork.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
	}

	@Test
	void testWithIsland() {

		Network network = createNetwork();
		addNetworkIsland(network);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void testWithIslandAndOtherMode() {

		Network network = createNetwork();
		addNetworkIsland(network);
		network.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addNetworkIsland(expectedNetwork);
		expectedNetwork.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("78")), TransportMode.car);
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("87")), TransportMode.car);

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void testWithIslandAndOtherModeAndOtherDnl() {

		Network network = createNetwork();
		addNetworkIsland(network);
		network.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("01")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("12")));
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("12")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("23")));
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addNetworkIsland(expectedNetwork);
		expectedNetwork.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		NetworkUtils.getOrCreateDisallowedNextLinks(expectedNetwork.getLinks().get(Id.createLinkId("01")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("12")));
		NetworkUtils.getOrCreateDisallowedNextLinks(expectedNetwork.getLinks().get(Id.createLinkId("12")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("23")));
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("78")), TransportMode.car);
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("87")), TransportMode.car);

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void testNoExit() {

		Network network = createNetwork();
		addNoExit(network);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addNoExit(expectedNetwork);
		// some elements are removed as node 8 is not reachable anymore
		expectedNetwork.removeNode(Id.createNodeId("8"));
		expectedNetwork.removeLink(Id.createLinkId("78"));
		expectedNetwork.removeLink(Id.createLinkId("87"));
		Link l67 = expectedNetwork.getLinks().get(Id.createLinkId("67"));
		NetworkUtils.removeDisallowedNextLinks(l67);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void testNoExitWithOtherMode() {

		Network network = createNetwork();
		addNoExit(network);
		network.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addNoExit(expectedNetwork);
		expectedNetwork.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		// some modes are removed, but bus stays everywhere
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("78")), TransportMode.car);
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("87")), TransportMode.car);
		NetworkUtils.removeDisallowedNextLinks(expectedNetwork.getLinks().get(Id.createLinkId("67")));
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void testNoExitWithOtherModeAndOtherDnl() {

		Network network = createNetwork();
		addNoExit(network);
		network.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("67")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("78")));
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("87")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("78")));
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, TransportMode.car);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addNoExit(expectedNetwork);
		expectedNetwork.getLinks().values().forEach(link -> NetworkUtils.addAllowedMode(link, "bus"));
		NetworkUtils.getOrCreateDisallowedNextLinks(expectedNetwork.getLinks().get(Id.createLinkId("67")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("78")));
		NetworkUtils.getOrCreateDisallowedNextLinks(expectedNetwork.getLinks().get(Id.createLinkId("87")))
				.addDisallowedLinkSequence("bus", List.of(Id.createLinkId("78")));
		// some modes are removed, but bus stays everywhere
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("78")), TransportMode.car);
		NetworkUtils.removeAllowedMode(expectedNetwork.getLinks().get(Id.createLinkId("87")), TransportMode.car);
		NetworkUtils.getDisallowedNextLinks(expectedNetwork.getLinks().get(Id.createLinkId("67")))
				.removeDisallowedLinkSequences(TransportMode.car);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void doubleUturnSingleMode() {
		String dnlMode = BUS;
		Set<String> modes = Set.of(BUS);

		Network network = doubleUturn(modes, dnlMode);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, dnlMode);

		// * --------------------------------------------------

		Network expectedNetwork = doubleUturn(modes, dnlMode);
		expectedNetwork.removeLink(Id.createLinkId("23"));
		expectedNetwork.removeLink(Id.createLinkId("32"));
		expectedNetwork.removeNode(Id.createNodeId("3"));
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
	}

	@ParameterizedTest
	@ValueSource(strings = { CAR, BUS, BIKE })
	void doubleUturn(String dnlMode) {

		Network network = doubleUturn(MODES, dnlMode);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, dnlMode);

		// * --------------------------------------------------

		Network expectedNetwork = doubleUturn(MODES, dnlMode);
		Link l23 = expectedNetwork.getLinks().get(Id.createLinkId("23"));
		NetworkUtils.removeAllowedMode(l23, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l23);
		Link l32 = expectedNetwork.getLinks().get(Id.createLinkId("32"));
		NetworkUtils.removeAllowedMode(l32, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l32);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
	}

	@Test
	void trippleUturnSingleMode() {
		String dnlMode = CAR;
		Set<String> modes = Set.of(CAR);

		Network network = trippleUturn(modes, dnlMode);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, dnlMode);

		// * --------------------------------------------------

		Network expectedNetwork = trippleUturn(modes, dnlMode);
		expectedNetwork.removeLink(Id.createLinkId("23"));
		expectedNetwork.removeLink(Id.createLinkId("32"));
		expectedNetwork.removeNode(Id.createNodeId("3"));
		expectedNetwork.removeLink(Id.createLinkId("12"));
		expectedNetwork.removeLink(Id.createLinkId("21"));
		expectedNetwork.removeNode(Id.createNodeId("2"));
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
	}

	@ParameterizedTest
	@ValueSource(strings = { CAR, BUS, BIKE })
	void trippleUturn(String dnlMode) {

		Network network = trippleUturn(MODES, dnlMode);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network, dnlMode);

		// * --------------------------------------------------

		Network expectedNetwork = trippleUturn(MODES, dnlMode);
		Link l23 = expectedNetwork.getLinks().get(Id.createLinkId("23"));
		NetworkUtils.removeAllowedMode(l23, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l23);
		Link l32 = expectedNetwork.getLinks().get(Id.createLinkId("32"));
		NetworkUtils.removeAllowedMode(l32, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l32);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));
		Link l12 = expectedNetwork.getLinks().get(Id.createLinkId("12"));
		NetworkUtils.removeAllowedMode(l12, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l12);
		Link l21 = expectedNetwork.getLinks().get(Id.createLinkId("21"));
		NetworkUtils.removeAllowedMode(l21, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l21);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
	}

	@ParameterizedTest
	@MethodSource("provideTrippleUturnNetworkPaths")
	void trippleUturn(String networkPath, String dnlMode) {

		Network input = NetworkUtils.readNetwork(networkPath);
		Verify.verify(DisallowedNextLinksUtils.isValid(input));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(input, dnlMode);

		// * --------------------------------------------------

		Network expected = NetworkUtils.readNetwork(networkPath);
		Link l23 = expected.getLinks().get(findLinkIdWithSuffix(expected, "23"));
		NetworkUtils.removeAllowedMode(l23, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l23);
		Link l32 = expected.getLinks().get(findLinkIdWithSuffix(expected, "32"));
		NetworkUtils.removeAllowedMode(l32, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l32);
		Verify.verify(DisallowedNextLinksUtils.isValid(expected));
		Link l12 = expected.getLinks().get(findLinkIdWithSuffix(expected, "12"));
		NetworkUtils.removeAllowedMode(l12, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l12);
		Link l21 = expected.getLinks().get(findLinkIdWithSuffix(expected, "21"));
		NetworkUtils.removeAllowedMode(l21, dnlMode);
		NetworkUtils.removeDisallowedNextLinks(l21);
		Verify.verify(DisallowedNextLinksUtils.isValid(expected));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(input));
		Assertions.assertTrue(NetworkUtils.compare(expected, input));

	}

	// Helpers

	static Stream<Arguments> provideTrippleUturnNetworkPaths() {
		List<Arguments> arguments = new ArrayList<>();
		// add multimodal network, shuffling the order of links with DNL
		for (String dnlMode : MODES) {
			List<Arguments> argumentsOfDnlMode = new ArrayList<>();
			for (String networkPath : trippleUturnShuffled(MODES, dnlMode, tempFolder)) {
				argumentsOfDnlMode.add(Arguments.of(networkPath, dnlMode));
			}
			arguments.addAll(argumentsOfDnlMode);
		}
		return arguments.stream();
	}

	static Network trippleUturn(Set<String> modes, String dnlMode) {
		return createTrippeUturnWithShuffledLinkIds(modes, dnlMode, LINK_SUFFIXES_WITH_DNL);
	}

	static List<String> trippleUturnShuffled(Set<String> modes, String dnlMode, File tempFolder) {
		List<String> linkIdPrefixes = List.of("a", "b", "c");

		List<List<String>> linkIdsWithDnlList = new ArrayList<>();
		for (List<String> linkIdPrefixPermutation : permutate(linkIdPrefixes)) {
			List<String> linkIdsWithDnl = new ArrayList<>();
			for (int i = 0; i < linkIdPrefixes.size(); ++i) {
				linkIdsWithDnl.add(linkIdPrefixPermutation.get(i) + LINK_SUFFIXES_WITH_DNL.get(i));
			}
			linkIdsWithDnlList.add(linkIdsWithDnl);
		}

		List<String> networkPaths = new ArrayList<>();
		for (List<String> linkIdsWithDnl : linkIdsWithDnlList) {
			Network network = createTrippeUturnWithShuffledLinkIds(modes, dnlMode, linkIdsWithDnl);
			File tempFile = new File(tempFolder, "network_" + StringUtils.join(modes, ",") + "_" + dnlMode + "_"
					+ StringUtils.join(linkIdsWithDnl, ",") + "_.xml");
			String networkPath = tempFile.toString();
			NetworkUtils.writeNetwork(network, networkPath);
			networkPaths.add(networkPath);
		}

		return networkPaths;
	}

	static Network createTrippeUturnWithShuffledLinkIds(Set<String> modes, String dnlMode,
			List<String> permutation) {
		Verify.verify(modes.contains(dnlMode));

		Network network = NetworkUtils.createNetwork();

		Node n0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, 0));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, 1));
		Node n2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0, 2));
		Node n3 = NetworkUtils.createNode(Id.createNodeId("3"), new Coord(0, 3));
		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);

		Link l01 = NetworkUtils.createLink(Id.createLinkId("01"), n0, n1, network, 1, 1, 300, 1);
		Link l10 = NetworkUtils.createLink(Id.createLinkId("10"), n1, n0, network, 1, 1, 300, 1);
		Link l12 = NetworkUtils.createLink(Id.createLinkId(permutation.get(2)), n1, n2, network, 1, 1, 300, 1);
		Link l21 = NetworkUtils.createLink(Id.createLinkId("21"), n2, n1, network, 1, 1, 300, 1);
		Link l23 = NetworkUtils.createLink(Id.createLinkId(permutation.get(0)), n2, n3, network, 1, 1, 300, 1);
		Link l32 = NetworkUtils.createLink(Id.createLinkId(permutation.get(1)), n3, n2, network, 1, 1, 300, 1);
		List<Link> links = List.of(l01, l10, l12, l21, l23, l32).stream()
				.sorted(Comparator.comparing(link -> link.getId().toString()))
				.toList();
		links.forEach(network::addLink);

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(modes);
		}

		// no uturns at 23 and 32 vice versa
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId(permutation.get(0))))
				.addDisallowedLinkSequence(dnlMode, List.of(Id.createLinkId(permutation.get(1)))); // no uturn
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId(permutation.get(1))))
				.addDisallowedLinkSequence(dnlMode, List.of(Id.createLinkId(permutation.get(0)))); // no uturn
		// no uturn at end of 12
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId(permutation.get(2))))
				.addDisallowedLinkSequence(dnlMode, List.of(Id.createLinkId("21"))); // no uturn

		return network;
	}

	static Network doubleUturn(Set<String> modes, String dnlMode) {
		Verify.verify(modes.contains(dnlMode));

		Network network = NetworkUtils.createNetwork();

		Node n0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, 0));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, 1));
		Node n2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0, 2));
		Node n3 = NetworkUtils.createNode(Id.createNodeId("3"), new Coord(0, 3));
		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);

		Link l01 = NetworkUtils.createLink(Id.createLinkId("01"), n0, n1, network, 1, 1, 300, 1);
		Link l10 = NetworkUtils.createLink(Id.createLinkId("10"), n1, n0, network, 1, 1, 300, 1);
		Link l12 = NetworkUtils.createLink(Id.createLinkId("12"), n1, n2, network, 1, 1, 300, 1);
		Link l21 = NetworkUtils.createLink(Id.createLinkId("21"), n2, n1, network, 1, 1, 300, 1);
		Link l23 = NetworkUtils.createLink(Id.createLinkId("23"), n2, n3, network, 1, 1, 300, 1);
		Link l32 = NetworkUtils.createLink(Id.createLinkId("32"), n3, n2, network, 1, 1, 300, 1);
		network.addLink(l01);
		network.addLink(l10);
		network.addLink(l12);
		network.addLink(l21);
		network.addLink(l23);
		network.addLink(l32);

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(modes);
		}

		// no uturns at 23 and 32 vice versa
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("23")))
				.addDisallowedLinkSequence(dnlMode, List.of(Id.createLinkId("32"))); // no uturn
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("32")))
				.addDisallowedLinkSequence(dnlMode, List.of(Id.createLinkId("23"))); // no uturn

		return network;
	}

	static Network crossingWithForbiddenUTurn(Set<String> modes, String dnlMode) {
		Verify.verify(modes.contains(dnlMode));

		Network network = NetworkUtils.createNetwork();

		Node n0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, 0));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(-1, 0));
		Node n2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0, -1));
		Node n3 = NetworkUtils.createNode(Id.createNodeId("3"), new Coord(1, 0));
		Node n4 = NetworkUtils.createNode(Id.createNodeId("4"), new Coord(0, 1));
		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);

		Link l01 = NetworkUtils.createLink(Id.createLinkId("01"), n0, n1, network, 1, 1, 300, 1);
		Link l10 = NetworkUtils.createLink(Id.createLinkId("10"), n1, n0, network, 1, 1, 300, 1);
		Link l02 = NetworkUtils.createLink(Id.createLinkId("02"), n0, n2, network, 1, 1, 300, 1);
		Link l20 = NetworkUtils.createLink(Id.createLinkId("20"), n2, n0, network, 1, 1, 300, 1);
		Link l03 = NetworkUtils.createLink(Id.createLinkId("03"), n0, n3, network, 1, 1, 300, 1);
		Link l30 = NetworkUtils.createLink(Id.createLinkId("30"), n3, n0, network, 1, 1, 300, 1);
		Link l04 = NetworkUtils.createLink(Id.createLinkId("04"), n0, n4, network, 1, 1, 300, 1);
		Link l40 = NetworkUtils.createLink(Id.createLinkId("40"), n4, n0, network, 1, 1, 300, 1);
		network.addLink(l01);
		network.addLink(l10);
		network.addLink(l02);
		network.addLink(l20);
		network.addLink(l03);
		network.addLink(l30);
		network.addLink(l04);
		network.addLink(l40);

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(modes);
		}

		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("10")))
				.addDisallowedLinkSequence(dnlMode, List.of(Id.createLinkId("01"))); // no uturn

		return network;
	}

	static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();

		Node n0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, -0));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, -1));
		Node n2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(-1, -2));
		Node n3 = NetworkUtils.createNode(Id.createNodeId("3"), new Coord(-1, -3));
		Node n4 = NetworkUtils.createNode(Id.createNodeId("4"), new Coord(0, -4));
		Node n5 = NetworkUtils.createNode(Id.createNodeId("5"), new Coord(1, -1));
		Node n6 = NetworkUtils.createNode(Id.createNodeId("6"), new Coord(0, -6));

		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		network.addNode(n5);
		network.addNode(n6);

		// * n0
		// l01 l10 (0110)
		// * n1 - l15 l51 (1551) - n5
		// l12 l21 (1221) \
		// * n2
		// l23 l32 (2332) | l14 l41 (1441)
		// * n3
		// l34 l43 (3443) /
		// * n4
		// l46 l64 (4664)
		// * n6

		Link l01 = NetworkUtils.createLink(Id.createLinkId("01"), n0, n1, network, 1, 1, 300, 1);
		Link l10 = NetworkUtils.createLink(Id.createLinkId("10"), n1, n0, network, 1, 1, 300, 1);

		Link l12 = NetworkUtils.createLink(Id.createLinkId("12"), n1, n2, network, 1, 1, 300, 1);
		Link l21 = NetworkUtils.createLink(Id.createLinkId("21"), n2, n1, network, 1, 1, 300, 1);

		Link l14 = NetworkUtils.createLink(Id.createLinkId("14"), n1, n4, network, 1, 1, 300, 1);
		Link l41 = NetworkUtils.createLink(Id.createLinkId("41"), n4, n1, network, 1, 1, 300, 1);

		Link l23 = NetworkUtils.createLink(Id.createLinkId("23"), n2, n3, network, 1, 1, 300, 1);
		Link l32 = NetworkUtils.createLink(Id.createLinkId("32"), n3, n2, network, 1, 1, 300, 1);

		Link l34 = NetworkUtils.createLink(Id.createLinkId("34"), n3, n4, network, 1, 1, 300, 1);
		Link l43 = NetworkUtils.createLink(Id.createLinkId("43"), n4, n3, network, 1, 1, 300, 1);

		Link l15 = NetworkUtils.createLink(Id.createLinkId("15"), n1, n5, network, 1, 1, 300, 1);
		Link l51 = NetworkUtils.createLink(Id.createLinkId("51"), n5, n1, network, 1, 1, 300, 1);

		Link l46 = NetworkUtils.createLink(Id.createLinkId("46"), n4, n6, network, 1, 1, 300, 1);
		Link l64 = NetworkUtils.createLink(Id.createLinkId("64"), n6, n4, network, 1, 1, 300, 1);

		network.addLink(l01);
		network.addLink(l10);
		network.addLink(l12);
		network.addLink(l21);
		network.addLink(l14);
		network.addLink(l41);
		network.addLink(l23);
		network.addLink(l32);
		network.addLink(l34);
		network.addLink(l43);
		network.addLink(l15);
		network.addLink(l51);
		network.addLink(l46);
		network.addLink(l64);

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(Set.of(TransportMode.car));
		}

		// * add some turn restrictions

		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		DisallowedNextLinks dnl34 = NetworkUtils.getOrCreateDisallowedNextLinks(l34);
		dnl34.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("41")));

		return network;
	}

	static void addDisallowedNextLinks(Network network) {
		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));
	}

	static void addNetworkIsland(Network network) {

		Node n7 = NetworkUtils.createNode(Id.createNodeId("7"), new Coord(0, -7));
		Node n8 = NetworkUtils.createNode(Id.createNodeId("8"), new Coord(0, -8));

		network.addNode(n7);
		network.addNode(n8);

		Link l78 = NetworkUtils.createLink(Id.createLinkId("78"), n7, n8, network, 1, 1, 300, 1);
		Link l87 = NetworkUtils.createLink(Id.createLinkId("87"), n8, n7, network, 1, 1, 300, 1);
		l78.setAllowedModes(Set.of(TransportMode.car));
		l87.setAllowedModes(Set.of(TransportMode.car));

		network.addLink(l78);
		network.addLink(l87);
	}

	static void addNoExit(Network network) {

		Node n6 = network.getNodes().get(Id.createNodeId("6"));
		Node n7 = NetworkUtils.createNode(Id.createNodeId("7"), new Coord(0, -7));
		Node n8 = NetworkUtils.createNode(Id.createNodeId("8"), new Coord(0, -8));

		network.addNode(n7);
		network.addNode(n8);

		Link l67 = NetworkUtils.createLink(Id.createLinkId("67"), n6, n7, network, 1, 1, 300, 1);
		Link l76 = NetworkUtils.createLink(Id.createLinkId("76"), n7, n6, network, 1, 1, 300, 1);
		l67.setAllowedModes(Set.of(TransportMode.car));
		l76.setAllowedModes(Set.of(TransportMode.car));

		Link l78 = NetworkUtils.createLink(Id.createLinkId("78"), n7, n8, network, 1, 1, 300, 1);
		Link l87 = NetworkUtils.createLink(Id.createLinkId("87"), n8, n7, network, 1, 1, 300, 1);
		l78.setAllowedModes(Set.of(TransportMode.car));
		l87.setAllowedModes(Set.of(TransportMode.car));

		network.addLink(l67);
		network.addLink(l76);
		network.addLink(l78);
		network.addLink(l87);

		DisallowedNextLinks dnl67 = NetworkUtils.getOrCreateDisallowedNextLinks(l67);
		dnl67.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("78")));
	}

	static @Nullable Id<Link> findLinkIdWithSuffix(Network network, String suffix) {
		return network.getLinks().keySet().stream()
				.filter(linkId -> linkId.toString().endsWith(suffix))
				.findFirst().orElse(null);
	}

	static <T> List<List<T>> permutate(List<T> elements) {
		List<List<T>> permutations = new ArrayList<>();
		permutations.add(new ArrayList<>());

		for (T e : elements) {
			List<List<T>> newPermutations = new ArrayList<>();
			for (List<T> permutation : permutations) {
				for (int i = 0; i <= permutation.size(); i++) {
					List<T> newPermutation = new ArrayList<>(permutation);
					newPermutation.add(i, e);
					newPermutations.add(newPermutation);
				}
			}
			permutations = newPermutations;
		}

		return permutations;
	}
}
