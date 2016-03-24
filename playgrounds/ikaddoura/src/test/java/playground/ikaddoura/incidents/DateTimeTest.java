/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.incidents;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */

public class DateTimeTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test1() {

		double dateInSec = 2016 * 12 * 30.436875 * 24 * 3600 + 3 * 30.436875 * 24 * 3600 + 1 * 24 * 3600;
		Assert.assertEquals("Wrong date / time", dateInSec, DateTime.parseDateTimeToDateTimeSeconds("2016-03-01"), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong date / time", "2016-03-01", DateTime.secToDateTimeString(dateInSec));
		Assert.assertEquals("Wrong date / time", "2016-03-01", DateTime.secToDateTimeString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-01")));		
	}
		
}
