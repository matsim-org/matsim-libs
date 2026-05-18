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
 * Country-specific knobs for {@link BicycleInfraClassifier}.
 *
 * <p>Everything in this interface is hard-coded to Germany in
 * {@link BicycleCountryProfileGermany} (the default). The interface exists so
 * that other countries — France, UK, …  — can be added without forking the
 * classifier. Only knobs that are actually used today are exposed; further
 * methods will be added when the first non-DE profile lands.
 *
 * <p><b>Scope.</b> Two kinds of knobs live here:
 * <ul>
 *   <li><b>Traffic-sign predicates.</b> The OSM {@code traffic_sign} tag
 *       carries country-specific codes (DE:244 = Fahrradstraße, DE:240 =
 *       gemeinsamer Geh-/Radweg, …). Each method below takes the raw value
 *       of the tag (the classifier does no parsing) and returns whether it
 *       matches the country's sign for that category.</li>
 *   <li><b>Driving direction.</b> {@link #isRightHandTraffic()} drives the
 *       {@code cycleway:right} vs {@code cycleway:left} interpretation.
 *       Currently always {@code true} for DE; the classifier still wires
 *       directionality based on the {@code Direction} parameter and assumes
 *       right-hand traffic in a number of places — flipping this to
 *       {@code false} will not magically produce correct UK results. See
 *       TODO in {@link BicycleInfraClassifier} for the list of sites that
 *       need to be revisited when the first left-hand profile is added.</li>
 * </ul>
 *
 * <p>The {@code traffic_sign} values passed in are the raw OSM strings,
 * including separators and concatenated subsigns (e.g.
 * {@code "DE:244,1020-30"}, {@code "DE:245,1022-10"}). Implementations
 * should match accordingly with {@code contains} / {@code startsWith}.
 *
 * @author smetzler
 */
public interface BicycleCountryProfile {

	// ------------------------------------------------------------------------
	// Driving direction
	// ------------------------------------------------------------------------

	/**
	 * Whether the country drives on the right. Right-hand traffic implies
	 * {@code cycleway:right} = "in direction of travel" and
	 * {@code cycleway:left} = "against direction of travel"; left-hand traffic
	 * (UK, IE, …) flips that.
	 */
	boolean isRightHandTraffic();

	// ------------------------------------------------------------------------
	// Bicycle road
	// ------------------------------------------------------------------------

	/** Sign declaring a bicycle road (DE: Fahrradstraße — DE:244). */
	boolean isBicycleRoadSign(String trafficSign);

	/**
	 * Additional sign upgrading a bicycle road to "vehicle traffic allowed as
	 * destination only" (DE: "Kfz frei" / "Anlieger frei" — subsign 1020-30
	 * plus textual variants).
	 */
	boolean isBicycleRoadVehicleDestinationSign(String trafficSign);

	// ------------------------------------------------------------------------
	// Cycleway / shared paths
	// ------------------------------------------------------------------------

	/**
	 * Sign declaring a dedicated cycleway (DE: "Radweg" — DE:237). Used by
	 * the separated-cycleway rule to upgrade ambiguous highway types when
	 * the sign is present.
	 */
	boolean isProtectedCyclewaySign(String trafficSign);

	/** Sign declaring a shared foot+bike path (DE: "Gemeinsamer Geh-/Radweg" — DE:240). */
	boolean isSharedFootCyclewaySign(String trafficSign);

	/** Sign declaring a segregated foot+bike path (DE: "Getrennter Geh-/Radweg" — DE:241). */
	boolean isSegregatedFootCyclewaySign(String trafficSign);

	/** Subsign declaring "bicycles allowed on this footway" (DE: 1022-10 — "Fahrrad frei"). */
	boolean isFootwayBicycleAllowedSign(String trafficSign);

	// ------------------------------------------------------------------------
	// Shared bus lanes
	// ------------------------------------------------------------------------

	/**
	 * Sign declaring a bus lane that explicitly permits bicycles
	 * (DE: Bussonderfahrstreifen + Fahrrad-frei subsign — DE:245 + 1022-10/14).
	 */
	boolean isSharedBusLaneBusWithBikeSign(String trafficSign);

	/**
	 * Sign declaring a cycleway that explicitly permits buses
	 * (DE: Radweg + Bus-frei / Linienverkehr-frei subsign — DE:237 + 1024-14/1026-32).
	 */
	boolean isSharedBusLaneBikeWithBusSign(String trafficSign);
}
