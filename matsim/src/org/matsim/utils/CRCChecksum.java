/* *********************************************************************** *
 * project: org.matsim.*
 * CRCChecksum.java
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

package org.matsim.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;

public class CRCChecksum {

	private static long getCRCFromStream(final InputStream in) {
		long check = 0;
		if (in == null) return 0;
		try {
			CRC32 crc = new CRC32();
			CheckedInputStream cis = new CheckedInputStream(in, crc);
			byte[] buffer = new byte[4096];
			while (cis.read(buffer) != -1) {
			  /* Read until the end of the stream is reached. */
			}
			check = crc.getValue();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try { in.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		return check;
	}

	public static long getCRCFromFile(final String filename) {
		InputStream in = null;
	  try {
			in = new BufferedInputStream(new FileInputStream(filename));
			return getCRCFromStream(in);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		return 0L;
	}

	public static long getCRCFromGZFile(final String filename) {
		InputStream in = null;
	    try {
			if (filename.endsWith(".gz") || filename.endsWith(".zip")) {
				in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(filename)));
			} else {
			    in = new FileInputStream( filename );
			}
			return getCRCFromStream(in);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return 0L;
	}
}
