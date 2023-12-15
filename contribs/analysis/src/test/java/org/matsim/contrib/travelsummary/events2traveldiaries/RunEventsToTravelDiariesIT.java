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
package org.matsim.contrib.travelsummary.events2traveldiaries;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 */
public class RunEventsToTravelDiariesIT {
    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@SuppressWarnings("static-method")
	@Test
	final void test() {

        String[] str = {"../../examples/scenarios/equil/config.xml", "../../examples/scenarios/equil/output_events.xml.gz", "_test", utils.getOutputDirectory()};
        // This goes through the file system (nothing to do with resource paths etc.)
        // It's OK in an integration test, but keep in mind that it wouldn't work like that in a separate repository.

        RunEventsToTravelDiaries.main(str);

        // yy missing: something that compares output files to expectations. kai, may'15

    }

}
