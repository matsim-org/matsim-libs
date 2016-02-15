/* *********************************************************************** *
 * project: org.matsim.*
 * CsvUtilsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author thibautd
 */
public class CsvUtilsTest {
	@Test
	public void testConvertBackAndForth() {
		final String line = "stuff1,some spaced stuff,\"some quoted, with commas!\",,\"with a \\\" in the middle\",";
		final String[] fields = CsvUtils.parseCsvLine( ',' , '"' , line );
		final String newLine = CsvUtils.buildCsvLine( ',' , '"' , fields );

		Assert.assertEquals(
				"escaping not stable",
				line,
				newLine );
	}
	@Test
	public void testSplit() {
		final String[] fields = CsvUtils.parseCsvLine( ',' , '"' , "stuff1,\"some spaced stuff\",\"some quoted, with commas!\",,\"with a \\\" in the middle\"," );

		Assert.assertEquals(
				"wrong number of fields in "+Arrays.toString( fields ),
				6,
				fields.length );
		Assert.assertEquals(
				"wrong value in field",
				fields[ 0 ],
				"stuff1" );
		Assert.assertEquals(
				"wrong value in field",
				fields[ 1 ],
				"some spaced stuff" );
		Assert.assertEquals(
				"wrong value in field",
				fields[ 2 ],
				"some quoted, with commas!" );
		Assert.assertEquals(
				"wrong value in field",
				fields[ 3 ],
				"" );
		Assert.assertEquals(
				"wrong value in field",
				fields[ 4 ],
				"with a \" in the middle" );
		Assert.assertEquals(
				"wrong value in field",
				fields[ 5 ],
				"" );
	}

	@Test
	public void testMerge() {
		final String line =
			CsvUtils.buildCsvLine( ',' , '"' ,
				"stuff1",
				"some spaced stuff",
				"some quoted, with commas!",
				"",
				"\"" );
		Assert.assertEquals(
				"unexpected line",
				"stuff1,some spaced stuff,\"some quoted, with commas!\",,\"\\\"\"",
				line );
	}

	@Test
	public void testOnlyEscapeIfNecessary() {
		final String string = "some normal string";

		Assert.assertEquals(
				"string was escaped without reason",
				string,
				CsvUtils.escapeField( '\t' , '"' , string ) );
	}

	@Test
	public void testEscapeQuotes() {
		final String string = "some \"quoted\" string";

		Assert.assertEquals(
				"string was not escaped",
				"\"some \\\"quoted\\\" string\"",
				CsvUtils.escapeField( '\t' , '"' , string ) );
	}

	@Test
	public void testEscapeSeparator() {
		final String string = "some\tseparated\tstring";

		Assert.assertEquals(
				"string was not escaped",
				"\"some\tseparated\tstring\"",
				CsvUtils.escapeField( '\t' , '"' , string ) );
	}

	@Test
	public void testEscapeBackslash() {
		final String string = "some \\";

		Assert.assertEquals(
				"string was not escaped",
				"\"some \\\\\"",
				CsvUtils.escapeField( '\t' , '"' , string ) );
	}
}

