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
	public void testSplit() {
		final String[] fields = CsvUtils.parseCsvLine( ',' , '"' , "stuff1,\"some spaced stuff\",\"some quoted, with commas!\",," );

		Assert.assertEquals(
				"wrong number of fields in "+Arrays.toString( fields ),
				5,
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
				"" );
		Assert.assertEquals(
				"unexpected line",
				"\"stuff1\",\"some spaced stuff\",\"some quoted, with commas!\",\"\",\"\"",
				line );
	}
}

