/* *********************************************************************** *
 * project: org.matsim.*
 * TimeTest.java
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

package org.matsim.core.utils.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Time}.
 *
 * @author mrieser
 */
public class TimeTest {

	@Test
	void testFormats() {
		double time = 12*3600 + 34*60 + 56.789;
		assertEquals("12:34:56", Time.writeTime(time, Time.TIMEFORMAT_HHMMSS));
		assertEquals("12:34", Time.writeTime(time, Time.TIMEFORMAT_HHMM));
		assertEquals(Integer.toString((int)time), Time.writeTime(time, Time.TIMEFORMAT_SSSS));
		try { // test unknown format
			String str = Time.writeTime(time, "ABCD");
			fail("expected IllegalArgumentException, got result: " + str);
		} catch (IllegalArgumentException expected) {}
		try { // test null as format, must *not* result in NullPointerException
			String str = Time.writeTime(time, null);
			fail("expected IllegalArgumentException, got result: " + str);
		} catch (IllegalArgumentException expected) {}

		assertEquals(12*3600.0 + 34*60.0 + 56.0, Time.parseTime("12:34:56"), 0.0);//HHMMSS
		assertEquals(12*3600.0 + 34*60.0 + 56.78, Time.parseTime("12:34:56.78"), 0.0);//HHMMSS, support parts of seconds
		assertEquals(12*3600.0 + 34*60.0, Time.parseTime("12:34"), 0.0);//HHMM
		assertEquals(12.0, Time.parseTime("12"), 0.0);//SSSS
		assertEquals(123456.0, Time.parseTime("123456"), 0.0);//SSSS
		assertEquals(123456.78, Time.parseTime("123456.78"), 0.0);//SSSS, support parts of seconds

		try { // test bad time-string with HHMMSS, bad part in Seconds
			double t = Time.parseTime("12:34:-01");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
		try { // test bad time-string with HHMMSS, bad part in Seconds
			double t = Time.parseTime("12:34:60");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
		try { // test bad time-string with HHMMSS, bad part in Minutes
			double t = Time.parseTime("12:-01:56");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
		try { // test bad time-string with HHMMSS, bad part in Minutes
			double t = Time.parseTime("12:60:56");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
		try { // test bad time-string with HHMM, bad part in Minutes
			double t = Time.parseTime("12:-01");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
		try { // test bad time-string with HHMM, bad part in Minutes
			double t = Time.parseTime("12:60");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
		try { // test bad time-string with unknown format
			double t = Time.parseTime("12:34:56:78");
			fail("expected IllegalArgumentException, got result: " + t);
		} catch (IllegalArgumentException expected) {}
	}

	@Test
	void testSeparators() {
		// test writing
		double dTime = 12*3600 + 34*60 + 56.789;
		assertEquals("12:34:56", Time.writeTime(dTime, ':'));
		assertEquals("12/34/56", Time.writeTime(dTime, '/'));
		assertEquals("12-34-56", Time.writeTime(dTime, '-'));

		// test reading
		double iTime = 12*3600 + 34*60 + 56;
		assertEquals(iTime, Time.parseTime( "12:34:56", ':').seconds(), 0.0);
		assertEquals(iTime, Time.parseTime( "12/34/56", '/').seconds(), 0.0);
		assertEquals(iTime, Time.parseTime( "12-34-56", '-').seconds(), 0.0);

		// test reading negative times
		assertEquals(-iTime, Time.parseTime( "-12:34:56", ':').seconds(), 0.0);
		assertEquals(-iTime, Time.parseTime( "-12/34/56", '/').seconds(), 0.0);
		assertEquals(-iTime, Time.parseTime( "-12-34-56", '-').seconds(), 0.0);
	}

	@Test
	void testUndefined() {
		// test writing
		assertEquals("undefined", Time.writeTime(OptionalTime.undefined()));

		// test reading
		assertEquals(OptionalTime.undefined(), Time.parseOptionalTime("undefined"));
		assertEquals(OptionalTime.undefined(), Time.parseOptionalTime(""));
		assertEquals(OptionalTime.undefined(), Time.parseOptionalTime(null));
	}

	@Test
	void testSetDefault() {
		Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMMSS);
		assertEquals("12:34:56", Time.writeTime(12*3600 + 34*60 + 56.789));
		Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMM);
		assertEquals("12:34", Time.writeTime(12*3600 + 34*60 + 56.789));
		Time.setDefaultTimeFormat(Time.TIMEFORMAT_SSSS);
		assertEquals(Integer.toString(12*3600 + 34*60 + 56), Time.writeTime(12*3600 + 34*60 + 56.789));


		Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMMSSDOTSS);
		assertEquals("12:34:56.78", Time.writeTime(12*3600 + 34*60 + 56.789).substring(0, 11));
		// (conversion to double of .789 looks like .788999999999... thus that dirty trick.
		//  kai/gregor, nov'11)
}

	@Test
	void testWriting() {
		Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMMSS);
		assertEquals( "12:34:56", Time.writeTime( 12*3600 + 34*60 + 56.789));// positive
		assertEquals( "01:02:03", Time.writeTime(  1*3600 +  2*60 +  3.4)); // positive with leading zero
		assertEquals("-12:34:56", Time.writeTime(-12*3600 - 34*60 - 56.789)); // negative
		assertEquals("-01:02:03", Time.writeTime( -1*3600 -  2*60 -  3.4)); // negative with leading zero
		assertEquals( "00:00:00", Time.writeTime(0.0)); // zero
		/* Integer.MIN_VALUE is a special value in case time values would be represented by int's, and not double's.
		 * Math.abs(Integer.MIN_VALUE) returns Integer.MIN_VALUE again, what could lead to a infinite loop depending
		 * how negative times are handled internally. So this test should ensure this or a future implementation
		 * does not have problems with that.
		 */
		assertEquals("-596523:14:08", Time.writeTime(Integer.MIN_VALUE));
	}

	@Test
	void testParsing() {
		assertEquals( 12*3600.0 + 34*60.0 + 56.0, Time.parseTime( "12:34:56"), 0.0);
		assertEquals( 12*3600.0 + 34*60.0 + 56.7, Time.parseTime( "12:34:56.7"), 0.0);
		assertEquals(  1*3600.0 +  2*60.0 +  3.0, Time.parseTime( "01:02:03"), 0.0);
		assertEquals(-12*3600.0 - 34*60.0 - 56.0, Time.parseTime("-12:34:56"), 0.0);
		assertEquals(-12*3600.0 - 34*60.0 - 56.7, Time.parseTime("-12:34:56.7"), 0.0);
		assertEquals(0.0, Time.parseTime("00:00:00"), 0.0);
		assertEquals(Integer.MIN_VALUE, Time.parseTime("-596523:14:08"), 0.0);
		/* test for parsing hours greater than 2^31-1 (i.e. hour of type long)
		 */
		assertEquals(Long.MAX_VALUE, Time.parseTime("2562047788015215:28:07"), 0.0);
	}

	@Test
	void testConvertHHMMInteger() {
		assertEquals( 12*3600.0 + 34*60.0, Time.convertHHMMInteger(Integer.valueOf("1234")), 0.0);
		assertEquals(  1*3600.0 +  2*60.0, Time.convertHHMMInteger(Integer.valueOf("0102")), 0.0);
	}

}
