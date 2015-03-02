/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author mrieser / senozon
 */
public class ActivityToZoneMappingReader {

	private final static Logger log = Logger.getLogger(ActivityToZoneMappingReader.class);
	private final ActivityToZoneMapping mapping;
	
	public ActivityToZoneMappingReader(ActivityToZoneMapping mapping) {
		this.mapping = mapping;
	}
	
	public void readFile(final String filename) {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		try {
			String line = null;
			while ((line = reader.readLine()) != null) {
				String[] parts = StringUtils.explode(line, ' ');
				if (parts.length >= 2) {
					String id = parts[0];
					String[] zones = new String[parts.length - 1];
					System.arraycopy(parts, 1, zones, 0, zones.length);
					this.mapping.addAgentActivityZones(Id.create(id, Person.class), zones);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (reader != null) {
				try { reader.close(); }
				catch (IOException e) { log.error("Could not close file " + filename); }
			}
		}
	}

}
