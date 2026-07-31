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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Smoke test for {@link ElevationDataParser} against known Berlin reference
 * points sampled from Sonny's DTM Germany 50m v3b.
 *
 * <p>By default this test uses the small Berlin cutout shipped at
 * {@code contribs/bicycle/test/input/org/matsim/contrib/bicycle/network/sonny-dtm-50m-berlin-cutout.tif}
 * (see the README in that directory for source and license details).
 * A different DTM can be used via {@code -Ddem.path=<path-to.tif>}, e.g.:
 * <pre>
 *     mvn -pl contribs/bicycle test \
 *         -Dtest=ElevationDataParserTest \
 *         -Ddem.path="C:/path/to/full-germany-dtm.tif"
 * </pre>
 *
 * <p>If the default file is missing and no override is set, the test is
 * skipped via {@link org.junit.jupiter.api.Assumptions#assumeTrue}, so it
 * is safe in CI even on machines without the file.
 *
 * <p>Reference data:
 * <ul>
 *   <li>DTM: Sonny's DTM Germany 50m v3b (https://sonny.4lima.de/), CC BY 4.0</li>
 *   <li>DTM CRS: EPSG:32632 (UTM 32N)</li>
 *   <li>Sample points are given in WGS84 (EPSG:4326), longitude first.</li>
 *   <li>Reference elevations recorded on 2026-05-08, in meters above sea level.</li>
 * </ul>
 *
 * <p>The chosen tolerance is ±2 m, which is generous: it covers the DTM's own
 * vertical resolution (0.1 m), nearest-neighbor sampling artifacts at pixel
 * boundaries, and any small adjustments Sonny may push in a future v3c.
 *
 * @author smetzler
 */
public class ElevationDataParserTest {

	private static final String DEM_PROPERTY = "dem.path";
	private static final String DEFAULT_DEM_PATH =
		"test/input/org/matsim/contrib/bicycle/network/sonny-dtm-50m-berlin-cutout.tif";

	private static final String DEM_CRS = "EPSG:32632";       // UTM 32N
	private static final String SAMPLE_CRS = "EPSG:4326";      // WGS84

	/**
	 * ±2 m around the reference value -- covers DTM resolution + interpolation noise.
	 */
	private static final double ELEVATION_TOLERANCE_M = 2.0;

	private static ElevationDataParser parser;


	@BeforeAll
	static void setUp() {
		// Default: small Berlin cutout in the test inputs of this package.
		// Can be overridden via -Ddem.path=<path-to.tif> for testing against a
		// different DTM (e.g. the full Germany version).
		String demPath = System.getProperty(DEM_PROPERTY, DEFAULT_DEM_PATH);

		Path dem = Paths.get(demPath);
		assumeTrue(Files.exists(dem),
			"DEM not found at " + demPath + " -- skipping.");

		parser = new ElevationDataParser(dem.toString(), SAMPLE_CRS, DEM_CRS);
	}


	// =========================================================================
	// Berlin reference points
	// =========================================================================

	@Test
	void teufelsberg() {
		// Highest natural point in central Berlin (rubble hill, ~115 m a.s.l.)
		assertElevation(13.2407, 52.4971, 112.4);
	}

	@Test
	void tempelhoferFeld() {
		// Former airport, large flat area in southern Berlin
		assertElevation(13.3989, 52.4755, 45.7);
	}

	@Test
	void mueggelsee() {
		// Berlin's largest lake; surface elevation
		assertElevation(13.6354, 52.4334, 32.3);
	}

	@Test
	void mueggelberg() {
		// Highest natural point in Berlin
		assertElevation(13.64048, 52.41594, 94.5);
	}

	@Test
	void alexanderplatz() {
		assertElevation(13.40993, 52.52191, 36.6);
	}

	@Test
	void kreuzberg() {
		// "Kreuzberg" the actual hill in Viktoriapark
		assertElevation(13.379491, 52.487610, 57.8);
	}

	@Test
	void hermannplatz() {
		assertElevation(13.422301, 52.486477, 36.8);
	}

	@Test
	void uBahnhofBoddinstrasse() {
		assertElevation(13.423210, 52.480278, 52.3);
	}


	// =========================================================================
	// Missing data
	// =========================================================================

	/**
	 * A point outside the raster used to raise {@link ArrayIndexOutOfBoundsException}
	 * from deep inside the raster access, with an index in the billions and nothing
	 * saying which coordinate caused it.
	 */
	@Test
	void returnsNaNOutsideTheRaster() {
		// Munich - inside the projection but far outside a Berlin cutout
		assertTrue(Double.isNaN(parser.getElevation(11.5820, 48.1351)),
			"a coordinate outside the DEM must yield NaN, not an exception and not a number");
	}

	/**
	 * The failure this guards against: a DEM read with the wrong CRS answers every query
	 * with its no-data value, which is a perfectly ordinary double. Averaged into a
	 * gradient it produces a network that looks flat rather than one that looks broken.
	 */
	@Test
	void reportsMissingDataInsteadOfTheNoDataValue() {
		long before = parser.getMissingCount();

		double value = parser.getElevation(11.5820, 48.1351);

		assertTrue(Double.isNaN(value));
		assertEquals(before + 1, parser.getMissingCount(), "missing samples are counted");
	}

	@Test
	void coverageCheckPassesForPointsInsideTheDem() {
		assertDoesNotThrow(() -> parser.requireCoverageOf(
			List.of(new Coord(13.40993, 52.52191), new Coord(13.379491, 52.487610)), 1.0));
	}

	@Test
	void coverageCheckFailsWithADiagnosisForPointsOutside() {

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> parser.requireCoverageOf(
				List.of(new Coord(11.5820, 48.1351), new Coord(11.6, 48.2)), 0.5));

		// the message has to carry enough to act on: what was wrong, and what to compare
		assertTrue(e.getMessage().contains("--dem-crs"), "should name the likely cause");
		assertTrue(e.getMessage().contains(DEM_CRS), "should name the DEM CRS in use");
		assertTrue(e.getMessage().contains("DEM extent"), "should name the extent to compare against");
	}

	@Test
	void coverageCheckAcceptsPartialCoverageWithinTheRatio() {
		// one inside, one outside - fine at 50 %, not at 100 %
		List<Coord> mixed = List.of(new Coord(13.40993, 52.52191), new Coord(11.5820, 48.1351));

		assertDoesNotThrow(() -> parser.requireCoverageOf(mixed, 0.5));
		assertThrows(IllegalArgumentException.class, () -> parser.requireCoverageOf(mixed, 1.0));
	}


	// =========================================================================
	// helpers
	// =========================================================================

	/**
	 * Asserts that the parser returns the expected elevation (±tolerance) for
	 * the given lon/lat point.
	 */
	private static void assertElevation(double lon, double lat, double expectedM) {
		double actual = parser.getElevation(lon, lat);
		assertEquals(expectedM, actual, ELEVATION_TOLERANCE_M,
			"elevation at (" + lon + ", " + lat + ")");
	}
}
