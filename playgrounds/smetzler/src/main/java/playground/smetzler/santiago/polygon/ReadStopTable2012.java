/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.smetzler.santiago.polygon;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * Read transit stop table with header "Codigo par,x,y,Zona 777" 
 * 
 * @author aneumann
 *
 */
public class ReadStopTable2012 implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadStopTable2012.class);

	private TabularFileParserConfig tabFileParserConfig;
	private Map<String, List<Coord>> ptZoneId2TransitStopCoordinates = new HashMap<>();
	private int linesRejected = 0;

	private ReadStopTable2012(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {","}); // \t
	}

	protected static Map<String, List<Coord>> readGenericCSV(String filename) {
		ReadStopTable2012 reader = new ReadStopTable2012(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.ptZoneId2TransitStopCoordinates.size() + " zones");
		return reader.ptZoneId2TransitStopCoordinates;		
	}	

	private void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if (row.length != 4) {
			log.warn("Wrong length of row. Skipping: " + row);
			return;
		}
		if(!row[0].trim().contains("Codigo par")){
			String zoneId = row[3];

			if (ptZoneId2TransitStopCoordinates.get(zoneId) == null) {
				ptZoneId2TransitStopCoordinates.put(zoneId, new ArrayList<Coord>());
			}

			ptZoneId2TransitStopCoordinates.get(zoneId).add(new Coord(Double.parseDouble(row[1]), Double.parseDouble(row[2])));
		} else {
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			this.linesRejected++;
			log.info("Ignoring: " + tempBuffer);
		}
	}
}