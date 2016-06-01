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

import java.text.ParseException;

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
	public final void test1() throws ParseException {

		Assert.assertEquals("Wrong date / time", "2016-03-31", DateTime.secToDateString(1.4593752E9));
		Assert.assertEquals("Wrong date / time", 1.4593752E9, DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 00:00:00"), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong date / time", 1.4593752E9, DateTime.parseDateTimeToDateTimeSeconds("2016-03-31"), MatsimTestUtils.EPSILON);
		
		Assert.assertEquals("Wrong date / time", "2016-04-01", DateTime.secToDateString(1.4593752E9 + (24 * 3600.)));
		Assert.assertEquals("Wrong date / time", "2016-03-31", DateTime.secToDateString(1.4593752E9 + (23 * 3600.)));
		
		// test String -> seconds -> String
		Assert.assertEquals("Wrong date / time", "2016-03-01", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-01")));
		
		// test String -> seconds + x -> String
		Assert.assertEquals("Wrong date / time", "2016-04-01", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31") + (24 * 3600.)));
		Assert.assertEquals("Wrong date / time", "2016-03-31", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31") + (23 * 3600.)));

		Assert.assertEquals("Wrong date / time", "2016-04-01", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 00:00:00") + (24 * 3600.)));
		Assert.assertEquals("Wrong date / time", "2016-03-31", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 00:00:00") + (23 * 3600.)));
		
		Assert.assertEquals("Wrong date / time", "2016-04-01", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 12:00:00") + (24 * 3600.)));
		Assert.assertEquals("Wrong date / time", "2016-03-31", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 00:30:00") + (23 * 3600.)));
		Assert.assertEquals("Wrong date / time", "2016-04-01", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 02:00:00") + (23 * 3600.)));
		
		Assert.assertEquals("Wrong date / time", "2016-03-05", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-05")));
		Assert.assertEquals("Wrong date / time", "2016-03-05", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-05") + (23 * 3600.)));
		
		Assert.assertEquals("Wrong date / time", "2016-05-27", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-05-27")));
		Assert.assertEquals("Wrong date / time", "2016-05-27", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-05-27") + (23 * 3600.)));
		
		Assert.assertEquals("Wrong date / time", "2016-03-27", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-27 00:00:10")));
		Assert.assertEquals("Wrong date / time", "2016-03-28", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-27") + (24 * 3600.)));
		
		Assert.assertEquals("winter time --> summer time!", "2016-03-28", DateTime.secToDateString(DateTime.parseDateTimeToDateTimeSeconds("2016-03-27") + (23 * 3600.)));

//		System.out.println(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31"));
//		System.out.println(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 00:00:00"));
//		System.out.println(DateTime.secToDateTimeString(1.4593752E9));
//		System.out.println(DateTime.secToDateString(1.4593752E9));
//		
//		System.out.println();
//		
//		System.out.println(DateTime.parseDateTimeToDateTimeSeconds("2016-03-31 01:00:00"));
//		System.out.println(DateTime.secToDateTimeString(1.4593788E9));
//		System.out.println(DateTime.secToDateString(1.4593788E9));
//	
//		System.out.println();
//		
//		System.out.println(DateTime.parseDateTimeToDateTimeSeconds("2016-04-01"));
//		
//		System.out.println();
				
	}
		
}
