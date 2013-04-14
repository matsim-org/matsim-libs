/* *********************************************************************** *
 * project: org.matsim.*
 * RoadTypeParser.java
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

package playground.southafrica.utilities.openstreetmap.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

public class RoadTypeParser {
	private final Logger log = Logger.getLogger(RoadTypeParser.class);
	
	public RoadTypeParser() {
	}
	
	public Map<Long, String> parseRoadType(String file) throws FileNotFoundException{
		File f = new File(file);
		if(!f.exists()){
			throw new FileNotFoundException("Could not find " + file);
		}

		RoadTypeSink rts = new RoadTypeSink();
		XmlReader xr = new XmlReader(f, false, CompressionMethod.GZip);
		xr.setSink(rts);
		log.info("Parsing road types from " + file);
		xr.run();
		
		return rts.getHighwayTypeMap();
	}
}

