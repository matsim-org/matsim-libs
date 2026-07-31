/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.contrib.bicycle.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.sumo.SumoNetworkHandler;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static org.matsim.contrib.bicycle.network.BicycleOsmTags.SMOOTHNESS;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.SURFACE;

/**
 * Finds the places where netconvert must not merge two edges, and writes them as a
 * keep-edges list.
 *
 * <p>netconvert's {@code geometry.remove} joins consecutive edges whose SUMO attributes
 * match. It has no idea about cycling infrastructure, so it happily merges a stretch with
 * a bike lane into one without — and the result claims the lane over the whole length,
 * with nothing in the network hinting otherwise. In a Leipzig extract 22.7 % of the
 * network length sat on edges built from more than one OSM way.
 *
 * <p>The fix is a second netconvert pass. This command produces its input:
 *
 * <ol>
 *   <li>Run netconvert <b>without</b> {@code geometry.remove}. Every edge then comes from
 *       exactly one OSM way, so its classification is unambiguous.</li>
 *   <li>Convert that with {@code network-from-sumo} and run this command on the result.
 *       It classifies every link and reports each pair of links whose categories differ
 *       across a node netconvert would otherwise dissolve.</li>
 *   <li>Run netconvert again <b>with</b> {@code geometry.remove} plus
 *       {@code --geometry.remove.keep-edges.input-file keep.txt}. Everything else still
 *       merges; only the category boundaries survive.</li>
 * </ol>
 *
 * <p>This reproduces what the reference pipeline's bicycle-aware simplifier did, using
 * netconvert's own machinery instead of a second simplifier — and it makes the merge a
 * scenario-level decision: skipping the whole two-pass dance simply yields a smaller
 * network with more {@code NEEDS_CLARIFICATION} links, which {@code bicycle-attributes}
 * counts.
 *
 * <p><b>The comparison is strictly directed.</b> Cycling infrastructure is tagged per side
 * of the road, so a boundary can exist in one direction and not in the other: a way
 * tagged {@code cycleway:right=lane} followed by an untagged one differs when travelling
 * along it, but not against it, where both sides are unset. Comparing whole junctions
 * instead of directed links would either miss those or invent boundaries that are not
 * there.
 *
 * @author smetzler
 */
@Command(
	name = "bicycle-keep-edges",
	description = "Lists the SUMO edges netconvert must not merge, to preserve cycling infrastructure boundaries.",
	showDefaultValues = true,
	mixinStandardHelpOptions = true
)
public class SumoBicycleKeepEdges implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(SumoBicycleKeepEdges.class);

	@Option(names = "--network", required = true,
		description = "MATSim network from a netconvert run WITHOUT geometry.remove")
	private Path networkFile;

	@Option(names = "--sumo-network", required = true,
		description = "The matching sumo.net.xml, also from the run without geometry.remove")
	private Path sumoNetworkFile;

	@Option(names = "--osm", required = true, description = "The .osm file netconvert consumed")
	private Path osmFile;

	@Option(names = "--output", required = true,
		description = "Keep-edges list, one edge id per line, for "
			+ "netconvert --geometry.remove.keep-edges.input-file")
	private Path output;

	@Option(names = "--country", defaultValue = "de",
		description = "Country profile for traffic-sign interpretation: de, at, or generic.")
	private String country;

	public static void main(String[] args) {
		new SumoBicycleKeepEdges().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(networkFile.toString());
		SumoNetworkHandler sumo = SumoNetworkHandler.read(sumoNetworkFile.toFile());
		OsmWayTags wayTags = OsmWayTags.read(osmFile);

		Result result = collect(network, sumo, wayTags, country);

		Files.write(output, result.edgeIds());

		log.info("Checked {} directed link(s): {} continued into a mergeable neighbour, "
				+ "{} of those crossed an infrastructure boundary. Wrote {} edge id(s) to {}.",
			result.linksChecked(), result.mergeCandidates(), result.boundaries(),
			result.edgeIds().size(), output);

		if (result.linksWithoutClassification() > 0) {
			log.warn("{} link(s) could not be classified (no matching SUMO edge or no OSM way tags) "
				+ "and were skipped.", result.linksWithoutClassification());
		}
		if (result.multiWayLinks() > 0) {
			log.warn("{} link(s) already cover more than one OSM way. This network was built WITH "
				+ "geometry.remove, so the boundaries it would have found are already gone. Re-run "
				+ "netconvert without that option first.", result.multiWayLinks());
		}

		return 0;
	}

	/**
	 * The keep-edges list plus the counters behind it. Package-private so a test can
	 * drive this without touching the file system.
	 */
	record Result(List<String> edgeIds, int linksChecked, int mergeCandidates, int boundaries,
				  int linksWithoutClassification, int multiWayLinks) {
	}

	static Result collect(Network network, SumoNetworkHandler sumo, OsmWayTags wayTags, String country) {

		BicycleInfraClassifier classifier =
			new BicycleInfraClassifier(BicycleCountryProfiles.forCode(country));

		// Sorted so two runs on the same inputs produce the same file.
		TreeSet<String> keep = new TreeSet<>();
		int linksChecked = 0;
		int mergeCandidates = 0;
		int boundaries = 0;
		int unclassified = 0;
		int multiWay = 0;

		for (Link link : network.getLinks().values()) {

			linksChecked++;

			MatchKey here = matchKeyOf(link, sumo, wayTags, classifier);
			if (here == null) {
				unclassified++;
				continue;
			}
			if (here.multiWay()) multiWay++;

			Link next = continuationOf(link);
			if (next == null) continue;
			mergeCandidates++;

			MatchKey there = matchKeyOf(next, sumo, wayTags, classifier);
			if (there == null) {
				unclassified++;
				continue;
			}

			if (!here.sameAs(there)) {
				boundaries++;
				// Both ends, because netconvert needs to be told about the edges it must
				// not touch, not about the node between them.
				keep.add(link.getId().toString());
				keep.add(next.getId().toString());
			}
		}

		return new Result(new ArrayList<>(keep), linksChecked, mergeCandidates, boundaries,
			unclassified, multiWay);
	}

	/**
	 * The one link a vehicle would continue onto at this link's toNode, or {@code null}
	 * when there is a choice — in which case the node is a real junction and netconvert
	 * would not dissolve it anyway.
	 *
	 * <p>The turnaround back onto the same road does not count as a choice; without
	 * excluding it every ordinary two-way node would look like a junction.
	 */
	private static Link continuationOf(Link link) {

		Node toNode = link.getToNode();
		Link candidate = null;

		for (Link out : toNode.getOutLinks().values()) {
			if (out.getToNode().equals(link.getFromNode())) continue;  // the U-turn
			if (candidate != null) return null;                        // more than one option
			candidate = out;
		}
		return candidate;
	}

	/**
	 * What has to match for two links to be mergeable without losing information. Mirrors
	 * the reference pipeline's simplifier criteria, minus the ones netconvert already
	 * checks itself (allowed modes, lane count, speed).
	 */
	private record MatchKey(BicycleInfraCategory category, String surface, String smoothness,
							boolean multiWay) {

		boolean sameAs(MatchKey other) {
			return category == other.category
				&& Objects.equals(surface, other.surface)
				&& Objects.equals(smoothness, other.smoothness);
		}
	}

	private static MatchKey matchKeyOf(Link link, SumoNetworkHandler sumo, OsmWayTags wayTags,
									   BicycleInfraClassifier classifier) {

		SumoNetworkHandler.Edge edge = sumo.getEdges().get(link.getId().toString());
		if (edge == null) return null;

		List<Map<String, String>> ways = SumoBicycleAttributes.wayTagsOf(edge, wayTags);
		if (ways.isEmpty()) return null;

		// On a network built without geometry.remove there is exactly one way per edge.
		// If there are several the caller is warned; classifying the first is the most
		// useful thing left to do.
		Map<String, String> tags = ways.get(0);

		return new MatchKey(
			classifier.classify(tags, SumoBicycleAttributes.directionOf(link)),
			tags.get(SURFACE),
			tags.get(SMOOTHNESS),
			ways.size() > 1);
	}
}
