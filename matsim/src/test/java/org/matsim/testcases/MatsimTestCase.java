/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimTestCase.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.testcases;

import org.junit.Ignore;
import org.junit.Rule;

/**
 * @Deprecated This is the "old" infrastructure for providing some standardized helper methods for junit-testing (until junit 3)
 * Please use {@link MatsimTestUtils} instead (starting from junit 4)
 * ((Deprecation was done after a mtg with KN))
 */
@Ignore
@Deprecated (since = "Jan 23")
public class MatsimTestCase {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

}
