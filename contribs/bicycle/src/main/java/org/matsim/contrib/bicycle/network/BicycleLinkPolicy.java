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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_AREA;
import static org.matsim.contrib.bicycle.BicycleUtils.BICYCLE_INFRA;
import static org.matsim.contrib.bicycle.network.BicycleOsmTags.*;

/**
 * Applies bicycle-specific link attributes and access rules on top of the
 * generic network produced by {@link org.matsim.contrib.osm.networkReader.OsmBicycleReader}.
 *
 * <p>Intended to be plugged into the reader via
 * {@code .setAfterLinkCreated(policy::apply)}. For each freshly created link
 * this class:
 * <ol>
 *   <li>optionally copies selected raw OSM tags onto the link (via {@link TagCopier}),</li>
 *   <li>classifies the link's cycling infrastructure with {@link BicycleInfraClassifier}
 *       and writes it to the link attribute
 *       {@link org.matsim.contrib.bicycle.BicycleUtils#BICYCLE_INFRA},</li>
 *   <li>applies the access rules and acts on their verdict,</li>
 *   <li>cleans up bicycle-oneway links on path/cycleway/footway.</li>
 * </ol>
 * "Drop" here means the link stays in the graph but has no allowed modes and
 * zero capacity; the empty-mode link is then pruned by {@code cleanNetwork},
 * which is easier for the downstream simplifiers than removing it here.
 *
 * <p>Which access rules there are and in what order they fire is <em>not</em> decided
 * here: that lives in {@link BicycleAccessRules}, shared with the SUMO path so the two
 * cannot drift apart. Only the last step is this path's own — netconvert resolves
 * oneways itself, so the SUMO side has no counterpart to it.
 *
 * <p>When an {@link AreaMarker} is supplied, only the ways carrying that marker
 * tag run through the steps above; every other way keeps the reader's modes (so
 * bikes may still ride it) but has its bicycle detail stripped -- no
 * classification, no bike attributes, and no elevation metrics.
 *
 * @author smetzler, esarikaya
 */
public final class BicycleLinkPolicy {

	private static final String BICYCLE_MODE = TransportMode.bike;

	private final BicycleInfraClassifier classifier;
	private final TagCopier tagCopier;

	/** Selects the ways that get the full bicycle treatment; {@code null} = every way. */
	private final AreaMarker areaMarker;

	/**
	 * Minor way types — {@code track}, {@code path} and the like — dropped when the way
	 * turns out to carry no cycling infrastructure. Empty switches the rule off.
	 */
	private final Set<String> dropWaysWithoutInfra;

	public BicycleLinkPolicy(BicycleInfraClassifier classifier, TagCopier tagCopier) {
		this(classifier, tagCopier, null, Set.of());
	}

	public BicycleLinkPolicy(BicycleInfraClassifier classifier, TagCopier tagCopier, AreaMarker areaMarker) {
		this(classifier, tagCopier, areaMarker, Set.of());
	}

	/**
	 * @param areaMarker           restricts the full bicycle treatment to the ways carrying
	 *                             this OSM marker tag; every other way is reduced to a plain
	 *                             car link. {@code null} treats every way as cyclable.
	 * @param dropWaysWithoutInfra OSM highway types of minor ways (typically {@code track}
	 *                             and {@code path}) to drop where the way ended up with
	 *                             {@code bicycle_infra=NONE}; empty switches the rule off.
	 */
	public BicycleLinkPolicy(BicycleInfraClassifier classifier, TagCopier tagCopier, AreaMarker areaMarker,
							 Set<String> dropWaysWithoutInfra) {
		this.classifier = classifier;
		this.tagCopier = tagCopier;
		this.areaMarker = areaMarker;
		this.dropWaysWithoutInfra = dropWaysWithoutInfra == null ? Set.of() : dropWaysWithoutInfra;
	}

	public void apply(Link link, Map<String, String> tags, OsmWayDirection direction) {

		// Bicycle-area gating: outside the marked area keep the reader's link as is --
		// its modes stay, so bikes may still ride it -- but strip the bicycle detail
		// (no classification, no bike attributes, no elevation later). Ways outside are
		// a pre-filtered major-road network, so the access rules below would barely
		// fire there anyway. No marker configured -> every way is treated as cyclable.
		// Which side a link fell on is recorded either way, so downstream can filter on
		// the area itself instead of inferring it from a missing category.
		if (areaMarker != null) {
			boolean inside = areaMarker.matches(tags);
			link.getAttributes().putAttribute(BICYCLE_AREA, inside);
			if (!inside) {
				stripBicycleDetail(link);
				return;
			}
		}

		// 0. copy selected raw OSM tags onto the link (no-op if TagCopier has no keys)
		tagCopier.copy(link, tags);

		// 1. classify cycling infrastructure
		BicycleInfraCategory infra = classifier.classify(tags, direction);
		link.getAttributes().putAttribute(BICYCLE_INFRA, infra.name());

		// 2. access rules -- the shared rule order, see BicycleAccessRules. Applying
		//    the verdict is all that is left here: which rules exist and in what order
		//    they fire is the one thing this path must not decide for itself.
		BicycleAccessRules.Verdict verdict =
			BicycleAccessRules.evaluate(tags, infra, dropWaysWithoutInfra);

		// bicycle=no -> bikes forbidden, but the link itself stays open to the other
		// modes. On a highway=primary etc. that means it survives as a car-only link;
		// only where bike was the sole mode (cycleway, track, ...) does dropping it
		// leave the link empty (removed downstream).
		if (verdict.bikeForbidden()) {
			removeMode(link, BICYCLE_MODE);
		}

		// "Drop" leaves the link in the graph with no modes and zero capacity;
		// cleanNetwork prunes it afterwards.
		if (verdict.dropped()) {
			drop(link);
			return;
		}

		// 3. oneway cleaning on path/cycleway/footway:
		//    the "_bike-reverse" link generated by the reader gets dropped,
		//    the plain "r" reverse link loses the bike mode.
		//    No SUMO counterpart -- netconvert resolves oneways itself.
		if (isBicycleOnewayRelevant(tags)) {
			if (link.getId().toString().endsWith("r")) {
				removeMode(link, BICYCLE_MODE);
			}
			if (link.getId().toString().endsWith("_bike-reverse")) {
				drop(link);
			}
		}
	}

	// ------------------------------------------------------------------------

	private static void drop(Link link) {
		link.setAllowedModes(Set.of());
		link.setCapacity(0);
	}

	/**
	 * Strip the bicycle-scoring detail from a link outside the marked bicycle area:
	 * remove the bike-specific attributes the reader stamped ({@code surface},
	 * {@code smoothness}, {@code cycleway}, {@code bicycle}) and, by returning before
	 * classification, leave {@code bicycle_infra} unset. The allowed modes are left
	 * untouched -- bikes may still ride the link -- and it gets no elevation metrics
	 * later, which key off {@code bicycle_infra}.
	 */
	private static void stripBicycleDetail(Link link) {
		link.getAttributes().removeAttribute(SURFACE);
		link.getAttributes().removeAttribute(SMOOTHNESS);
		link.getAttributes().removeAttribute(CYCLEWAY);
		link.getAttributes().removeAttribute(BICYCLE);
	}

	private static void removeMode(Link link, String mode) {
		var modes = new HashSet<>(link.getAllowedModes());
		modes.remove(mode);
		link.setAllowedModes(modes);
	}

	private static boolean isBicycleOnewayRelevant(Map<String, String> tags) {
		String highway = tags.get(HIGHWAY);
		boolean relevant = HW_PATH.equals(highway) || HW_CYCLEWAY.equals(highway) || HW_FOOTWAY.equals(highway);
		if (!relevant) return false;

		// Null-safe: the tag map is the raw OSM tag set, so oneway / oneway:bicycle
		// are usually absent. Keep the constant on the left to avoid an NPE.
		if (YES.equals(tags.get(ONEWAY_BICYCLE))) return true;
		if (YES.equals(tags.get(ONEWAY))) {
			return !NO.equals(tags.get(ONEWAY_BICYCLE));
		}
		return false;
	}

	/**
	 * Selects the OSM ways that should get the full bicycle treatment, parsed from
	 * the {@code --bike-area-marker} CLI value. A bare {@code "key"} matches any way
	 * carrying that tag key regardless of its value; {@code "key=value"} matches only
	 * that exact value.
	 */
	public record AreaMarker(String key, String value) {

		/** Parse a {@code key} or {@code key=value} spec; the value part is optional. */
		public static AreaMarker parse(String spec) {
			int eq = spec.indexOf('=');
			return eq < 0
				? new AreaMarker(spec.trim(), null)
				: new AreaMarker(spec.substring(0, eq).trim(), spec.substring(eq + 1).trim());
		}

		/** Whether the given raw OSM tag map carries this marker. */
		public boolean matches(Map<String, String> tags) {
			String v = tags.get(key);
			return v != null && (value == null || value.equals(v));
		}

		@Override
		public String toString() {
			return value == null ? key : key + "=" + value;
		}
	}
}
