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

package playground.mrieser.svi.converters;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.core.utils.io.IOUtils;

import playground.mrieser.svi.data.Zones;

/**
 * @author mrieser / senozon
 */
public class DynusTZonesWriter {

	private final static Logger log = Logger.getLogger(DynusTZonesWriter.class);
	
	private final Zones zones;
	
	public DynusTZonesWriter(final Zones zones) {
		this.zones = zones;
	}
	
	public void writeToDirectory(final String zonesFilename) {
		BufferedWriter writer = IOUtils.getBufferedWriter(zonesFilename);
		try {
			writer.write("bla"); // TODO
			
			for (Feature f : zones.getAllZones()) {
				
			}
			
		} catch (IOException e) {
			log.error("Could not write file " + zonesFilename);
		} finally {
			try {
				writer.close();
			} catch (IOException e2) {
				log.error("Could not close file " + zonesFilename);
			}
		}
	}
}
