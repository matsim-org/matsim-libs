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
 *   <li>drops parking-lot aisles ({@code service=parking_aisle}) outright -- they
 *       are not a meaningful cycling route,</li>
 *   <li>enforces footway/pedestrian whitelist: bike is only allowed when the OSM
 *       tags explicitly permit it,</li>
 *   <li>drops the bike mode on links tagged {@code bicycle=no} (the link keeps
 *       any other mode, e.g. {@code car} on a {@code highway=primary}),</li>
 *   <li>drops links whose general {@code access} is restricted
 *       ({@code no} / {@code private} / {@code customer}), unless a bicycle-specific
 *       tag ({@code bicycle=yes} / {@code =designated}) overrides it,</li>
 *   <li>cleans up bicycle-oneway links on path/cycleway/footway.</li>
 * </ol>
 * "Drop" here means the link stays in the graph but has no allowed modes and
 * zero capacity; the empty-mode link is then pruned by {@code cleanNetwork},
 * which is easier for the downstream simplifiers than removing it here.
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

	public BicycleLinkPolicy(BicycleInfraClassifier classifier, TagCopier tagCopier) {
		this(classifier, tagCopier, null);
	}

	/**
	 * @param areaMarker restricts the full bicycle treatment to the ways carrying this
	 *                   OSM marker tag; every other way is reduced to a plain car link.
	 *                   {@code null} treats every way as cyclable (the default).
	 */
	public BicycleLinkPolicy(BicycleInfraClassifier classifier, TagCopier tagCopier, AreaMarker areaMarker) {
		this.classifier = classifier;
		this.tagCopier = tagCopier;
		this.areaMarker = areaMarker;
	}

	public void apply(Link link, Map<String, String> tags, OsmWayDirection direction) {

		// Bicycle-area gating: outside the marked area keep the reader's link as is --
		// its modes stay, so bikes may still ride it -- but strip the bicycle detail
		// (no classification, no bike attributes, no elevation later). Ways outside are
		// a pre-filtered major-road network, so the access rules below would barely
		// fire there anyway. No marker configured -> every way is treated as cyclable.
		if (areaMarker != null && !areaMarker.matches(tags)) {
			stripBicycleDetail(link);
			return;
		}

		// 0. copy selected raw OSM tags onto the link (no-op if TagCopier has no keys)
		tagCopier.copy(link, tags);

		// 1. classify cycling infrastructure
		BicycleInfraCategory infra = classifier.classify(tags, direction);
		link.getAttributes().putAttribute(BICYCLE_INFRA, infra.name());

		// 2. service=parking_aisle -> drop outright. Parking-lot aisles are no real
		//    cycling route; unlike the ServiceLinkCleaner (which keeps aisles that
		//    happen to bridge two road nodes) this drops them regardless of
		//    connectivity. Dropped here, removed later by cleanNetwork.
		if (SV_PARKING_AISLE.equals(tags.get(SERVICE))) {
			drop(link);
			return;
		}

		// 3. footway/pedestrian whitelist — drops the link if bike isn't explicitly allowed
		enforceFootwayPedestrianWhitelist(link, tags);
		if (link.getAllowedModes().isEmpty()) return;

		// 4. bicycle=no -> bikes forbidden, but the link itself stays open to the
		//    other modes. On a highway=primary etc. that means it survives as a
		//    car-only link; only where bike was the sole mode (cycleway, track,
		//    ...) does dropping it leave the link empty (removed downstream).
		if (NO.equals(tags.get(BICYCLE))) {
			removeMode(link, BICYCLE_MODE);
			return;
		}

		// 5. access=no/private/customer -> drop
		if (isAccessRestricted(tags)) {
			drop(link);
			return;
		}

		// 6. oneway cleaning on path/cycleway/footway:
		//    the "_bike-reverse" link generated by the reader gets dropped,
		//    the plain "r" reverse link loses the bike mode.
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

	private static void enforceFootwayPedestrianWhitelist(Link link, Map<String, String> tags) {
		String highway = tags.get(HIGHWAY);
		if (!(HW_FOOTWAY.equals(highway) || HW_PEDESTRIAN.equals(highway))) return;

		if (!bicycleExplicitlyAllowed(tags)) {
			drop(link);
		}
	}

	/**
	 * Whether the OSM tags carry a bicycle-specific permission that grants cycling
	 * over a more general restriction — the footway/pedestrian default, or a
	 * restricted {@code access} (OSM: the more specific tag wins). Only
	 * {@code bicycle=yes} / {@code =designated} count for now; we might want to
	 * keep {@code bicycle=permissive} / {@code =dismount} as well.
	 */
	private static boolean bicycleExplicitlyAllowed(Map<String, String> tags) {
		String bicycle = tags.get(BICYCLE);
		return YES.equals(bicycle) || DESIGNATED.equals(bicycle);
	}

	private static boolean isAccessRestricted(Map<String, String> tags) {
		// Null-safe: raw OSM tag maps usually have no access key. Constant on the
		// left to avoid an NPE.
		String access = tags.get(ACCESS);
		boolean restricted = NO.equals(access) || PRIVATE.equals(access) || CUSTOMER.equals(access);
		// A bicycle-specific permission overrides the general access restriction,
		// e.g. access=private + bicycle=designated stays cyclable.
		return restricted && !bicycleExplicitlyAllowed(tags);
	}

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
