/* *********************************************************************** *
 * project: org.matsim.*
 * ArgumentParserTest.java
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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests the functionality of the class org.matsim.utils.misc.ArgumentParser.
 *
 * @author mrieser
 */
public class ArgumentParserTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Test if regular arguments (no options) are correctly recognized.
	 */
	@Test
	void testArguments() {
		ArgumentParser parser = new ArgumentParser(
				new String[] {"a", "bc", "def=g", "hjk=lmn opq"});
		Iterator<String> iter = parser.iterator();
		assertEquals("a", iter.next());
		assertEquals("bc", iter.next());
		assertEquals("def=g", iter.next()); // '=' should not be parsed!
		assertEquals("hjk=lmn opq", iter.next());
		assertIteratorAtEnd(iter);
	}

	/**
	 * Test if short options (beginning with '-'), which can be written in a collapsed form, are correctly recognized.
	 */
	@Test
	void testShortOptions() {
		ArgumentParser parser = new ArgumentParser(
				new String[] {"-a", "-b", "-cd", "-efg", "-h=j", "-kl=m", "-nop=qrs", "-tu=vw xyz", "=", "-", "-=", "-1", "-2=", "-=3", "-=4=5"}, true);
		Iterator<String> iter = parser.iterator();
		assertEquals("-a", iter.next());
		assertEquals("-b", iter.next());
		assertEquals("-c", iter.next());
		assertEquals("-d", iter.next());
		assertEquals("-e", iter.next());
		assertEquals("-f", iter.next());
		assertEquals("-g", iter.next());
		assertEquals("-h", iter.next());
		assertEquals("j", iter.next());
		assertEquals("-k", iter.next());
		assertEquals("-l", iter.next());
		assertEquals("m", iter.next());
		assertEquals("-n", iter.next());
		assertEquals("-o", iter.next());
		assertEquals("-p", iter.next());
		assertEquals("qrs", iter.next());
		assertEquals("-t", iter.next());
		assertEquals("-u", iter.next());
		assertEquals("vw xyz", iter.next());
		assertEquals("=", iter.next());
		assertEquals("-", iter.next());
		assertEquals("-=", iter.next());
		assertEquals("-1", iter.next());
		assertEquals("-2", iter.next());
		assertEquals("", iter.next()); // empty argument after '-2='
		assertEquals("-=3", iter.next()); // interpret as string argument, as it makes no sense otherwise
		assertEquals("-=4=5", iter.next()); /* one could iterprete '=4' as param name and '5' as value to that param,
		 		but I decided against that. everybody who needs such crazy arguments should have some work left to do.... */
		assertIteratorAtEnd(iter);
	}

	/**
	 * Test if long options (beginning with '--') are correctly recognized.
	 */
	@Test
	void testLongOptions() {
		ArgumentParser parser = new ArgumentParser(
				new String[] {"--a", "--bc", "--def=ghj", "--kl=mn op", "=", "--qr=", "--=", "--=st", "--=uv=wxy z"}, true);
		Iterator<String> iter = parser.iterator();
		assertEquals("--a", iter.next());
		assertEquals("--bc", iter.next());
		assertEquals("--def", iter.next());
		assertEquals("ghj", iter.next());
		assertEquals("--kl", iter.next());
		assertEquals("mn op", iter.next());
		assertEquals("=", iter.next());
		assertEquals("--qr", iter.next());
		assertEquals("", iter.next()); // empty argument after '--qr='
		assertEquals("--=", iter.next());
		assertEquals("--=st", iter.next());
		assertEquals("--=uv=wxy z", iter.next()); // see comment in testShortArguments() for similar case
		assertIteratorAtEnd(iter);
	}

	/**
	 * Test if a mix of arguments and options are correctly recognized.
	 */
	@Test
	void testMixed() {
		// first test it with short arguments enabled
		ArgumentParser parser = new ArgumentParser(
				new String[] {"-xcf", "myfile.tgz", "file1", "file2", "--verbose"}, true);
		Iterator<String> iter = parser.iterator();
		assertEquals("-x", iter.next());
		assertEquals("-c", iter.next());
		assertEquals("-f", iter.next());
		assertEquals("myfile.tgz", iter.next());
		assertEquals("file1", iter.next());
		assertEquals("file2", iter.next());
		assertEquals("--verbose", iter.next());
		assertIteratorAtEnd(iter);

		// then test it with short arguments disabled
		parser = new ArgumentParser(
				new String[] {"-xcf", "myfile.tgz", "file1", "file2", "--verbose"}, false);
		iter = parser.iterator();
		assertEquals("-xcf", iter.next());
		assertEquals("myfile.tgz", iter.next());
		assertEquals("file1", iter.next());
		assertEquals("file2", iter.next());
		assertEquals("--verbose", iter.next());
		assertIteratorAtEnd(iter);

		// test special cases
		parser = new ArgumentParser(new String[] {"-xcf", "=", "=a", "=ab", "-=", "-=a", "--=", "--=a", "--a=b"}, false);
		iter = parser.iterator();
		assertEquals("-xcf", iter.next());
		assertEquals("=", iter.next());
		assertEquals("=a", iter.next());
		assertEquals("=ab", iter.next());
		assertEquals("-=", iter.next());
		assertEquals("-=a", iter.next());
		assertEquals("--=", iter.next());
		assertEquals("--=a", iter.next());
		assertEquals("--a", iter.next());
		assertEquals("b", iter.next());
		assertIteratorAtEnd(iter);
	}

	private void assertIteratorAtEnd(final Iterator<?> iter) {
		try {
			iter.next();
			fail("expected NoSuchElementException, didn't receive one.");
		} catch (NoSuchElementException e) {
			// everything is great! just as we expected
		} catch (Exception e) {
			fail("expected NoSuchElementException, got " + e.getClass().getName());
		}
	}

}
