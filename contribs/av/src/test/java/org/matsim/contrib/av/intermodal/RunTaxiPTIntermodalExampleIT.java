/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.av.intermodal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author jbischoff
 *
 */
public class RunTaxiPTIntermodalExampleIT {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testIntermodalExample() throws MalformedURLException {
		URL configUrl = new File(utils.getClassInputDirectory() + "config.xml").toURI().toURL();
		new RunTaxiPTIntermodalExample().run(configUrl, false);
	}
}
