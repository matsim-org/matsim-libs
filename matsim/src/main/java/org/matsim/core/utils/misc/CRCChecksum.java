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

package org.matsim.core.utils.misc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;

public class CRCChecksum {

	private static long getCRCFromStream(final InputStream in) {
		long check = 0;
		if (in == null) return 0;
		CRC32 crc = new CRC32();
		try (CheckedInputStream cis = new CheckedInputStream(in, crc)) {
			byte[] buffer = new byte[4096];
			while (cis.read(buffer) != -1) {
				/* Read until the end of the stream is reached. */
			}
			check = crc.getValue();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		return check;
	}

	/**
	 * Calculates the checksum of the content of the given file. If the filename ends in ".gz",
	 * the file is assumed to be gzipped and the checksum over the <em>uncompressed</em> content 
	 * will be calculated. If a file is not found at its expected place, it is searched via the class loader.
	 * <p/>
	 * Comments:<ul>
	 * <li> Some version of this method, possibly the variant with the class loader, does some caching: If I replace
	 * the original file in a test case, I need to restart eclipse before it works correctly.  ???  kai, feb'14
	 * </ul>
	 * 
	 * @param filename
	 * @return CRC32-Checksum of the file's content.
	 */
	public static long getCRCFromFile(final String filename) {
		InputStream in = null;
		try {
			if (new File(filename).exists()) {
				if (filename.endsWith(".gz")) {
					in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(filename)));
				} else {
					in = new BufferedInputStream(new FileInputStream(filename));
				}
			} else {
				// (the logic is: if the file cannot be found directly on the file system, this is some method to search
				// in generic locations (which ones?).  kai, feb'14)
				InputStream stream = CRCChecksum.class.getClassLoader().getResourceAsStream(filename);
				if (filename.endsWith(".gz")) {
					in = new GZIPInputStream(new BufferedInputStream(stream));
				} else {
					in = new BufferedInputStream(stream);
				}
			}
			return getCRCFromStream(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
