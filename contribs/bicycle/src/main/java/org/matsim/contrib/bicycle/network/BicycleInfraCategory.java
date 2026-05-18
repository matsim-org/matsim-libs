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

/**
 * Bicycle infrastructure categories produced by {@link BicycleInfraClassifier}.
 *
 * <p>Every link in a bicycle network is assigned exactly one of these
 * categories. The {@code .name()} of the chosen value is what gets written to
 * the link attribute {@code bicycle_infra}, so the on-disk attribute values
 * are unchanged from the previous string-based implementation.
 *
 * <p><b>Compatibility — enum names are an on-disk contract.</b> The string
 * form is the {@code bicycle_infra} attribute value in MATSim network XML.
 * That means:
 * <ul>
 *   <li><b>Do not rename existing constants.</b> Renaming
 *       e.g. {@code CYCLEWAY_ADJOINING} to {@code CYCLEWAY_ADJACENT} silently
 *       breaks every previously written network XML — the value
 *       {@code "CYCLEWAY_ADJOINING"} will no longer parse via {@code valueOf}
 *       and the link distribution log will tally it under
 *       {@code "(unparseable)"}.</li>
 *   <li><b>Adding new constants is fine</b>, including at any position in the
 *       declaration list — enum ordering is informational (see next paragraph).
 *       Old XMLs continue to read with their existing values.</li>
 *   <li><b>Removing or repurposing a constant is a breaking change</b> and
 *       needs a deprecation cycle plus a migration note in the contrib's
 *       release notes.</li>
 * </ul>
 *
 * <p>The order of the values follows the precedence used by the classifier
 * (first match wins) and is informational only — runtime behaviour does not
 * depend on enum ordering. Sidepath variants are spelled out explicitly
 * (rather than computed from a base value plus a suffix) so that downstream
 * consumers — in particular bicycle disutility / scoring — can attach a
 * comfort weight to each variant via an {@code EnumMap}.
 *
 * @author smetzler
 */
public enum BicycleInfraCategory {

	// 1. Protected bike lane (physical separation)
	CYCLEWAY_ON_HIGHWAY_PROTECTED,

	// 2. Cycleway link
	CYCLEWAY_LINK,

	// 3. Crossings
	CROSSING,

	// 4. Bicycle road (Fahrradstraße)
	BICYCLE_ROAD,
	BICYCLE_ROAD_VEHICLE_DESTINATION,

	// 5. Shared bus/bike lanes
	SHARED_BUS_LANE_BUS_WITH_BIKE,
	SHARED_BUS_LANE_BIKE_WITH_BUS,

	// 6. Pedestrian area with bicycle allowed
	PEDESTRIAN_AREA_BICYCLE_YES,

	// 7. On-street cycling with motor traffic
	SHARED_MOTOR_VEHICLE_LANE,

	// 8. Cycle lane between motor lanes (Angstweiche)
	CYCLEWAY_ON_HIGHWAY_BETWEEN_LANES,

	// 9. On-highway lanes (advisory / exclusive / unspecified)
	CYCLEWAY_ON_HIGHWAY_ADVISORY,
	CYCLEWAY_ON_HIGHWAY_EXCLUSIVE,
	CYCLEWAY_ON_HIGHWAY_ADVISORY_OR_EXCLUSIVE,

	// 10. Separated cycleways
	CYCLEWAY_ADJOINING,
	CYCLEWAY_ISOLATED,
	CYCLEWAY_ADJOINING_OR_ISOLATED,

	// 11. Combined foot+bike paths (shared)
	FOOT_AND_CYCLEWAY_SHARED_ADJOINING,
	FOOT_AND_CYCLEWAY_SHARED_ISOLATED,
	FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED,

	// 11. Combined foot+bike paths (segregated)
	FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING,
	FOOT_AND_CYCLEWAY_SEGREGATED_ISOLATED,
	FOOT_AND_CYCLEWAY_SEGREGATED_ADJOINING_OR_ISOLATED,

	// 12. Footway with bicycle allowed
	FOOTWAY_BICYCLE_YES_ADJOINING,
	FOOTWAY_BICYCLE_YES_ISOLATED,
	FOOTWAY_BICYCLE_YES_ADJOINING_OR_ISOLATED,

	// 13. Matched the precedence but OSM tags were ambiguous
	NEEDS_CLARIFICATION,

	// 14. No cycling infrastructure
	NONE
}
