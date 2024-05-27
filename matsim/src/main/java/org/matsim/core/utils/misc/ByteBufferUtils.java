/* *********************************************************************** *
 * project: org.matsim.*
 * ByteBufferUtils.java
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Some helper methods to work with {@link ByteBuffer}s.
 *
 * @author mrieser
 */
public class ByteBufferUtils {

	/**
	 * Writes the given String to the ByteBuffer. First writes the length of the String as int,
	 * then writes the single characters. The ByteBuffer's position is incremented according
	 * to the length of the String.
	 *
	 * @param buffer
	 * @param string
	 */
	public static void putString(final ByteBuffer buffer, final String string) {
		buffer.putInt(string.length());
		for (int i = 0; i < string.length(); i++) {
			buffer.putChar(string.charAt(i));
		}
	}

	/**
	 * Reads a String from a ByteBuffer. Reads first an int for the length of the String,
	 * and then the corresponding number of characters. Increments the position of the
	 * ByteBuffer according to the length of the String.
	 *
	 * @param buffer
	 * @return the String at the buffer's current position
	 */
	public static String getString(final ByteBuffer buffer) {
		int length = buffer.getInt();
		char[] chBuffer = new char[length];
		for (int i = 0; i < length; i++) {
			chBuffer[i] = buffer.getChar();
		}
		return new String(chBuffer);
	}

	/**
	 * Writes the given Serializable to the ByteBuffer. First writes the length of the Serializable as int,
	 * then writes the single bytes of the serialized object. The ByteBuffer's position is incremented according
	 * to the length of the Serializable.
	 *
	 */
	public static void putObject(final ByteBuffer buffer, Serializable o){
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (ObjectOutput oout = new ObjectOutputStream(bos)) {
				oout.writeObject(o);
				byte[] laneBytes = bos.toByteArray();
				buffer.putInt(laneBytes.length);
				for (int i = 0; i < laneBytes.length; i++) {
					buffer.put(laneBytes[i]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads a Object (Serializable) from a ByteBuffer. Reads first an int for the length of the Object,
	 * and then the corresponding number of bytes. Increments the position of the
	 * ByteBuffer according to the length of the object's byte array.
	 */
	public static Object getObject(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = buffer.get();
		}
		try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
				 ObjectInputStream oin = new ObjectInputStream(bis)) {
			return oin.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

}
