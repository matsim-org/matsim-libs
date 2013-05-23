/* *********************************************************************** *
 * project: org.matsim.*
 * DgUTCOffsetsReader
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
package air.scenario.utcoffsets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class DgUTCOffsetsReader {
	
	private static final Logger log = Logger.getLogger(DgUTCOffsetsReader.class);
	
	public Map<String, Double> loadUtcOffsets(String inputfile) throws Exception, IOException {
		log.debug("Loading utc offsets from file: " + inputfile);
		Map<String, Double> utcOffset = new HashMap<String, Double>();
		BufferedReader brUtc = new BufferedReader(new FileReader(new File(inputfile)));
		while (brUtc.ready()) {
			String line = brUtc.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0].trim();
			double offset = Double.parseDouble(entries[1]);
			utcOffset.put(airportCode, offset);
		}
		brUtc.close();
		log.debug("loaded " + utcOffset.size() + " utc offsets ");
		return utcOffset;
	}
}
