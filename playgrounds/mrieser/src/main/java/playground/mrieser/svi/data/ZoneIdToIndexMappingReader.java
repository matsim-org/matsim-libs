/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.StringUtils;

public class ZoneIdToIndexMappingReader {

	private final ZoneIdToIndexMapping mapping;
	
	public ZoneIdToIndexMappingReader(final ZoneIdToIndexMapping mapping) {
		this.mapping = mapping;
	}
	
	public void readFile(final String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		try {
			String line = reader.readLine(); // header, ignore
			while ((line = reader.readLine()) != null) {
				String[] parts = StringUtils.explode(line, ',');
				if (parts.length == 2) {
					int index = Integer.parseInt(parts[0]);
					String zoneId = parts[1];
					this.mapping.addMapping(zoneId, index);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
