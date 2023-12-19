/* *********************************************************************** *
 * project: org.matsim.*
 * ByteBufferUtilsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ByteBufferUtilsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Tests {@link ByteBufferUtils#putString(java.nio.ByteBuffer, String)} and
	 * {@link ByteBufferUtils#getString(java.nio.ByteBuffer)}.
	 */
	@Test
	void testPutGetString() {
		final ByteBuffer buffer = ByteBuffer.allocate(100);
		buffer.putInt(5);
		ByteBufferUtils.putString(buffer, "foo bar");
		buffer.putChar('?');
		ByteBufferUtils.putString(buffer, "Hello World");
		buffer.putChar('!');

		buffer.flip();

		assertEquals(5, buffer.getInt());
		assertEquals("foo bar", ByteBufferUtils.getString(buffer));
		assertEquals('?', buffer.getChar());
		assertEquals("Hello World", ByteBufferUtils.getString(buffer));
		assertEquals('!', buffer.getChar());
		assertFalse(buffer.hasRemaining());
	}

	/**
	 * Tests {@link ByteBufferUtils#putObject(java.nio.ByteBuffer, Serializable)} and
	 * {@link ByteBufferUtils#getObject(java.nio.ByteBuffer)}.
	 */
	@Test
	void testPutGetObject() {
		final ByteBuffer buffer = ByteBuffer.allocate(100);
		buffer.putInt(5);
		ByteBufferUtils.putObject(buffer, "foo bar");
		buffer.putChar('?');
		ByteBufferUtils.putObject(buffer, "Hello World");
		buffer.putChar('!');

		buffer.flip();

		assertEquals(5, buffer.getInt());
		assertEquals("foo bar", ByteBufferUtils.getObject(buffer));
		assertEquals('?', buffer.getChar());
		assertEquals("Hello World", ByteBufferUtils.getObject(buffer));
		assertEquals('!', buffer.getChar());
		assertFalse(buffer.hasRemaining());
	}

}
