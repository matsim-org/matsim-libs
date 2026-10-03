/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.IncidentRecord;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.network.NetworkUtils;

/**
 * Unit tests for the incident-hotspot feature building
 * ({@link RemoteGuidanceAnalysisControlerListener#incidentHotspotFeatures}): per-link aggregation, geometry placement
 * at the link to-node, empty/unknown-CRS handling, and skipping links absent from the (sub)network.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentHotspotFeaturesTest {

	private static final String CRS = "EPSG:25832";

	private static Network networkWithLinks() {
		Network network = NetworkUtils.createNetwork();
		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 200));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("c"), new Coord(300, 400));
		NetworkUtils.createAndAddLink(network, Id.createLinkId("l1"), a, b, 100, 10, 1000, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("l2"), b, c, 100, 10, 1000, 1);
		return network;
	}

	private static IncidentRecord incident(String link, double queueDelay, double duration) {
		return new IncidentRecord(Id.create("v", DvrpVehicle.class), null, 1, Id.createLinkId(link),
				0, 0, duration, 0, duration, queueDelay > 0, queueDelay);
	}

	@Test
	void aggregatesPerLinkAtToNode() {
		List<IncidentRecord> incidents = List.of(
				incident("l1", 10, 100),
				incident("l1", 30, 300),   // l1: count 2, meanQueue 20, meanDur 200
				incident("l2", 0, 500));   // l2: count 1, meanQueue 0, meanDur 500

		Collection<SimpleFeature> features = RemoteGuidanceAnalysisControlerListener.incidentHotspotFeatures(
				incidents, networkWithLinks(), CRS);

		assertThat(features).hasSize(2);
		Map<Object, SimpleFeature> byLink = features.stream()
				.collect(Collectors.toMap(f -> f.getAttribute("link"), f -> f));

		SimpleFeature l1 = byLink.get("l1");
		assertThat(l1.getAttribute("count")).isEqualTo(2);
		assertThat((Double) l1.getAttribute("meanQueue")).isEqualTo(20.0);
		assertThat((Double) l1.getAttribute("meanDur")).isEqualTo(200.0);

		SimpleFeature l2 = byLink.get("l2");
		assertThat(l2.getAttribute("count")).isEqualTo(1);
		assertThat((Double) l2.getAttribute("meanDur")).isEqualTo(500.0);
	}

	@Test
	void emptyIncidentsYieldNoFeatures() {
		assertThat(RemoteGuidanceAnalysisControlerListener.incidentHotspotFeatures(
				List.of(), networkWithLinks(), CRS)).isEmpty();
	}

	@Test
	void unknownCrsYieldsNoFeatures() {
		assertThat(RemoteGuidanceAnalysisControlerListener.incidentHotspotFeatures(
				List.of(incident("l1", 10, 100)), networkWithLinks(), "Atlantis")).isEmpty();
	}

	@Test
	void linkNotInNetworkIsSkipped() {
		List<IncidentRecord> incidents = List.of(
				incident("l1", 10, 100),
				incident("missing", 5, 50)); // not in the network → skipped, not guessed

		Collection<SimpleFeature> features = RemoteGuidanceAnalysisControlerListener.incidentHotspotFeatures(
				incidents, networkWithLinks(), CRS);

		assertThat(features).hasSize(1);
		assertThat(features.iterator().next().getAttribute("link")).isEqualTo("l1");
	}
}
