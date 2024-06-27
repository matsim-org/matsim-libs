
/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.utils.gis;

import java.io.IOException;

import org.geotools.api.data.FeatureSource;
import org.geotools.feature.NameImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / senozon
 * @author nkuehnel / MOIA // add gpkg test
 */
public class GeoFileReaderTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Based on message on users-mailing list from 20Dec2012)
	 * @throws IOException
	 */
	@Test
	void testShp() throws IOException {
		String filename = "src/test/resources/" + utils.getInputDirectory() + "test+test.shp";
		FeatureSource fs = GeoFileReader.readDataFile(filename);
		Assertions.assertEquals(3, fs.getFeatures().size());
	}

	/**
	 * @throws IOException
	 */
	@Test
	void testGpkg() throws IOException {
		String filename = "src/test/resources/" + utils.getInputDirectory() + "test+test.gpkg";
		FeatureSource fs = GeoFileReader.readDataFile(filename, new NameImpl("testtest"));
		Assertions.assertEquals(3, fs.getFeatures().size());
	}
}
