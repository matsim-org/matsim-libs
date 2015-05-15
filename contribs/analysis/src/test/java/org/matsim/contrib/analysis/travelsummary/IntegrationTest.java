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
package org.matsim.contrib.analysis.travelsummary;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class IntegrationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@SuppressWarnings("static-method")
	@Test
	public final void test() {

		String[] str = { "test/scenarios/equil/output_events.xml.gz" , "test/scenarios/equil/network.xml" } ;
		// (this works since those files are, in the matsim main repository, in the resources path. kai, may'15)
		
		RunEventsToTravelSummaryExample.main( str );
		
		// yy missing: something that compares output files to expectations. kai, may'15
		
	}

}
