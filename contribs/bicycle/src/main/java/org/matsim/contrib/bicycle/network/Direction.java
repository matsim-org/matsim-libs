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
 * Travel direction of a link relative to the OSM way it was derived from.
 *
 * <p>Cycling infrastructure is frequently tagged per side of the road
 * ({@code cycleway:right} / {@code cycleway:left}), so a single way yields
 * different {@link BicycleInfraCategory} values per direction —
 * {@link BicycleInfraClassifier} needs to know which one it is looking at.
 *
 * <p>Deliberately reader-neutral: the same classifier runs on links from the
 * {@code SupersonicOsmNetworkReader} (which has its own {@code Direction} enum,
 * mapped at the call site) and on links from a SUMO-converted network, where
 * the direction comes from the sign of the link id. Keeping this enum here is
 * what frees the classifier from depending on any particular reader.
 */
public enum Direction {

	/** Along the OSM way's node order. */
	FORWARD,

	/** Against the OSM way's node order. */
	REVERSE
}
