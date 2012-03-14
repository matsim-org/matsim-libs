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

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

public class ActivityToZoneMappingWriter {

	private final static Logger log = Logger.getLogger(ActivityToZoneMappingWriter.class);
	private final ActivityToZoneMapping mapping;

	public ActivityToZoneMappingWriter(final ActivityToZoneMapping mapping) {
		this.mapping = mapping;
	}

	public void writeFile(final String filename) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		try {
			for (Id agentId : this.mapping.getAgentIds()) {
				writer.write(agentId.toString());
				for (String zoneId : this.mapping.getAgentActivityZones(agentId)) {
					writer.write(" ");
					if (zoneId == null) {
						writer.write("null");
					} else {
						writer.write(zoneId);
					}
				}
				writer.write("\r\n");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			if (writer != null) {
				try { writer.close(); }
				catch (IOException e) { log.error("Could not close file " + filename, e); }
			}
		}
	}
}
