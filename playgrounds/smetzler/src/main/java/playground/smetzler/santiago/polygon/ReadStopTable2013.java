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
 * Read transit stop table with header
 * 
 * Codigo|CodigoUsuario|Comuna|Nombre|Sentido|FilaSuperior|FilaInferior|GrupoParada|X|Y|Latitud|Longitud|censal_1992|comunas|diseno_563|diseno_777|eod_2001|eod_2006|estraus_264|estraus_404|estraus_410|estraus_618|zonas_6
 * 
 * @author aneumann
 *
 */
public class ReadStopTable2013 implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadStopTable2013.class);

	private TabularFileParserConfig tabFileParserConfig;
	private Map<String, List<StopTableEntry>> ptZoneId2TransitStopCoordinates = new HashMap<>();
	private int linesRejected = 0;

	private ReadStopTable2013(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"|"}); // \t
	}

	protected static Map<String, List<StopTableEntry>> readGenericCSV(String filename) {
		ReadStopTable2013 reader = new ReadStopTable2013(filename);
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
		if (row.length != 23) {
			log.warn("Wrong length of row. Skipping: " + rowToString(row));
			return;
		}
		if(!row[0].trim().contains("Codigo")){
			
			try {
				String stopId = row[0];
				String stopIdPublic = row[1];
				String comuna = row[2];
				String name = row[3];
				String orientation = row[4];
				String crossingStreetA = row[5];
				String crossingStreetB = row[6];
				String stopArea = row[7];

				Coord coordCartesian = new Coord(Double.parseDouble(row[8]), Double.parseDouble(row[9]));

				Coord coordLatLong = new Coord(Double.parseDouble(row[10]), Double.parseDouble(row[11]));
				
				String censal1992 = row[12];
				String comunas = row[13];
				String diseno563 = row[14];
				String diseno777 = row[15];
				String eod2001 = row[16];
				String eod2006 = row[17];
				String estraus264 = row[18];
				String estraus404 = row[19];
				String estraus410 = row[20];
				String estraus618 = row[21];
				String zonas6 = row[22];
				
				StopTableEntry stopTableEntry = new StopTableEntry(stopId, stopIdPublic, comuna, name, orientation, crossingStreetA, crossingStreetB, stopArea, coordCartesian, coordLatLong, censal1992, comunas, diseno563, diseno777, eod2001, eod2006, estraus264, estraus404, estraus410, estraus618, zonas6);
				
				if (ptZoneId2TransitStopCoordinates.get(stopTableEntry.diseno777) == null) {
					ptZoneId2TransitStopCoordinates.put(stopTableEntry.diseno777, new ArrayList<StopTableEntry>());
				}
				
				ptZoneId2TransitStopCoordinates.get(stopTableEntry.diseno777).add(stopTableEntry);
				
			} catch (Exception e) {
				log.warn("Parsing failed for entry " + rowToString(row));
				linesRejected++;
				return;
			}
		} else {
			this.linesRejected++;
			log.info("Ignoring: " + rowToString(row));
		}
	}
	
	private String rowToString(String[] row) {
		StringBuffer tempBuffer = new StringBuffer();
		for (String string : row) {
			tempBuffer.append(string);
			tempBuffer.append(", ");
		}
		return tempBuffer.toString();
	}
}