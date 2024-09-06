/* *********************************************************************** *
 * project: org.matsim.*
 * TransformationFactoryTest.java
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

package org.matsim.core.utils.geometry.transformations;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.core.utils.geometry.CoordinateTransformation;

public class TransformationFactoryTest {

	private final static Logger log = LogManager.getLogger(TransformationFactoryTest.class);

	/**
	 * Test if a custom implemented, non-GeoTools coordinate transformation can
	 * be instantiated.
	 */
	@Test
	final void testKnownCustomTransformation() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.GK4, TransformationFactory.WGS84);
		assertNotNull(transformation);
		assertTrue(transformation instanceof GK4toWGS84);
	}

	/**
	 * Test if GeoTools handles the requested coordinate transformation.
	 */
	@Test
	final void testKnownGeotoolsTransformation() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84_UTM35S, TransformationFactory.WGS84);
		assertNotNull(transformation);
		assertTrue(transformation instanceof GeotoolsTransformation);
	}

	/**
	 * Test if a correct, GeoTools' Well-Known-Text (WKT) is correctly recognized,
	 * instead of our shortcut names.
	 */
	@Test
	final void testUnknownWKTTransformation() {
		final String wgs84utm35s = "PROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";
		final String wgs84 = "GEOGCS[\"WGS84\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\",0.017453292519943295], AXIS[\"Longitude\",EAST], AXIS[\"Latitude\",NORTH]]";
		CoordinateTransformation transformation1 = TransformationFactory.getCoordinateTransformation(wgs84utm35s, TransformationFactory.WGS84);
		assertNotNull(transformation1);
		assertTrue(transformation1 instanceof GeotoolsTransformation);
		CoordinateTransformation transformation2 = TransformationFactory.getCoordinateTransformation(wgs84utm35s, wgs84);
		assertNotNull(transformation2);
		assertTrue(transformation2 instanceof GeotoolsTransformation);
	}

	/**
	 * Test if a wrong, non-Well-Known-Text according to GeoTools is correctly rejected.
	 */
	@Test
	final void testUnknownBadTransformation() {
		// GeoTools recognize misspellings in WKTs, but not (possibly) missing parameters.
		// Don't be fooled by the many Strings in WKTs, many of them are just names, NOT identifiers!

		// check for misspelling
		/** first 'P' of PROJCS right at the beginning is missing */
		final String bad1 = "ROJCS[\"WGS_1984_UTM_Zone_35S\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Transverse_Mercator\"],PARAMETER[\"latitude_of_origin\",0],PARAMETER[\"central_meridian\",27],PARAMETER[\"scale_factor\",0.9996],PARAMETER[\"false_easting\",500000],PARAMETER[\"false_northing\",10000000],UNIT[\"Meter\",1]]";
		try {
			TransformationFactory.getCoordinateTransformation(bad1, TransformationFactory.WGS84);
			fail("expected Exception.");
		} catch (IllegalArgumentException expected) {
			log.info("Catched expected Exception: " + expected.getMessage());
		}

		// check for non-WKTs
		final String bad2 = "WGS84_UTM1234";
		try {
			TransformationFactory.getCoordinateTransformation(bad2, TransformationFactory.WGS84);
			fail("expected Exception.");
		} catch (IllegalArgumentException expected) {
			log.info("Catched expected Exception: " + expected.getMessage());
		}
	}

	@Test
	final void testIdentityTransformation() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.ATLANTIS, TransformationFactory.ATLANTIS);
		assertTrue(transformation instanceof IdentityTransformation);
	}

	@Test
	final void testToCH1903LV03() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);
		assertTrue(transformation instanceof WGS84toCH1903LV03);
	}

	@Test
	final void testFromCH1903LV03() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.CH1903_LV03, TransformationFactory.WGS84);
		assertTrue(transformation instanceof CH1903LV03toWGS84);
	}

	@Test
	final void testToCH1903LV03Plus() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03_Plus);
		assertTrue(transformation instanceof WGS84toCH1903LV03Plus);
	}

	@Test
	final void testFromCH1903LV03Plus() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.CH1903_LV03_Plus, TransformationFactory.WGS84);
		assertTrue(transformation instanceof CH1903LV03PlustoWGS84);
	}

	@Test
	final void testFromAtlantis() {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.ATLANTIS, TransformationFactory.WGS84);
		assertTrue(transformation instanceof AtlantisToWGS84);
	}
}
