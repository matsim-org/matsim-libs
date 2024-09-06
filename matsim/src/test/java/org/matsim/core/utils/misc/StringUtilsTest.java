/* *********************************************************************** *
 * project: org.matsim.*
 * StringUtilsTest.java
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests the functionality of the class org.matsim.utils.misc.StringUtils.
 *
 * @author mrieser
 */
public class StringUtilsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Tests the method explode(String, char), which should always return the same as
	 * String.split(String) does.
	 */
	@Test
	void testExplode() {
		String[] testStrings = {"a:b", "ab:cd", "ab::cd", ":ab:cd", "ab:cd:", ":ab:cd:", "::ab::cd::", "a", "ab", ""};
		for (String test : testStrings) {
			String[] resultExplode = StringUtils.explode(test, ':');
			String[] resultSplit = test.split(":");
			assertEquals(resultSplit.length, resultExplode.length, "Different result lengths with test string \"" + test + "\"");
			for (int i = 0; i < resultExplode.length; i++) {
				assertEquals(resultSplit[i], resultExplode[i], "Different result part " + i + " when testing string: \"" + test + "\"");
			}
		}
	}

	/**
	 * Tests the method explode(String, char, int), which should always return the same as
	 * String.split(String, int) does.
	 */
	@Test
	void testExplodeLimit() {
		String[] testStrings = {"a:b", "a:b:c", "a:b:c:d", "a:::b:c", ":::::", "a", ""};
		for (String test : testStrings) {
			String[] resultExplode = StringUtils.explode(test, ':', 3);
			String[] resultSplit = test.split(":", 3);
			assertEquals(resultSplit.length, resultExplode.length, "Different result lengths with test string \"" + test + "\"");
			for (int i = 0; i < resultExplode.length; i++) {
				assertEquals(resultSplit[i], resultExplode[i], "Different result part " + i + " when testing string: \"" + test + "\"");
			}
		}
	}

}
