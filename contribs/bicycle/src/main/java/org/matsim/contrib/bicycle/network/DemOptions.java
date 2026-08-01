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

import picocli.CommandLine.Option;

import java.nio.file.Path;

/**
 * The {@code --dem} / {@code --dem-crs} pair shared by the network-building commands, as
 * a picocli mixin: declared once, embedded via {@code @CommandLine.Mixin}, so the
 * commands cannot drift apart in names, defaults or help texts.
 *
 * <p>The DEM is optional. {@link #validate()} enforces that a given DEM also brings its
 * CRS and should run before any real work — the parser itself is sometimes built only
 * after minutes of reading inputs, and the mistake has to surface immediately.
 */
final class DemOptions {

	@Option(names = "--dem",
		description = "DEM GeoTIFF. Optional: without it the network is built without "
			+ "elevation metrics. Requires --dem-crs when given.")
	private Path dem;

	@Option(names = "--dem-crs",
		description = "CRS of the DEM, e.g. EPSG:32632 for Sonny's Germany-wide DTMs. "
			+ "Required only with --dem.")
	private String demCRS;

	/** Whether a DEM was given; without one the elevation metrics are skipped. */
	boolean isSet() {
		return dem != null;
	}

	/** Fails fast on {@code --dem} without {@code --dem-crs}. */
	void validate() {
		if (dem != null && demCRS == null) {
			throw new IllegalArgumentException("--dem-crs is required when --dem is given.");
		}
	}

	/**
	 * The parser for the given DEM; check {@link #isSet()} first.
	 *
	 * @param queryCRS CRS of the coordinates the parser will be queried with
	 */
	ElevationDataParser createParser(String queryCRS) {
		if (dem == null) {
			throw new IllegalStateException("No --dem given; check isSet() first.");
		}
		validate();
		return new ElevationDataParser(dem.toString(), queryCRS, demCRS);
	}
}
