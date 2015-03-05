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
package playground.smetzler.santiago.demand;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;

/**
 * Read trip table with header
 * 
 * "diseno777subida|diseno777bajada|mediahora|viaje_laboral_promedio|viajes_adulto|viajes_estudiante|viajes_1_etapa|viajes_2_etapas|viajes_3_etapas|viajes_4_o_mas_etapas|viajes_usan_metro|viajes_solo_metro " 
 * 
 * @author aneumann
 *
 */
public class ReadTripTable2012 implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadTripTable2012.class);
	static final double TIME_INTERVAL = 30 * 60; // 30 minutes

	private TabularFileParserConfig tabFileParserConfig;
	private List<TripTableEntry> tripTableEntries = new LinkedList<>();
	private int linesRejected = 0;

	private ReadTripTable2012(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"|"}); // \t
	}

	protected static List<TripTableEntry> readGenericCSV(String filename) {
		ReadTripTable2012 reader = new ReadTripTable2012(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.tripTableEntries.size() + " lines");
		return reader.tripTableEntries;		
	}	

	private void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if (row.length != 12) {
			log.warn("Wrong length of row. Skipping: " + rowToString(row));
			return;
		}
		if(!row[0].trim().contains("diseno777")){
			// valid entry - parse it
			
			try {
				final String boardingZone = row[0];
				final String alightingZone = row[1];
				
				// TODO pruefe problem mit den zonen 847 und 29!!!!
				if (boardingZone.equalsIgnoreCase("847") || alightingZone.equalsIgnoreCase("847") || boardingZone.equalsIgnoreCase("29") || alightingZone.equalsIgnoreCase("29")) {
					log.warn("Skipping line because there is no geometry for zone 847 or 29.: " + rowToString(row));
					linesRejected++;
					return;
				}
				
				final double timeOfBoarding = Time.parseTime(row[2]);
				final double avgNumberOfTripsPerWorkingDay = Double.parseDouble(row[3]);
				final double avgNumberOfTripsOfAdults = Double.parseDouble(row[4]);
				final double avgNumberOfTripsOfPupils = Double.parseDouble(row[5]);
				final double avgNumberOfTripsWith0Transfers = Double.parseDouble(row[6]);
				final double avgNumberOfTripsWith1Transfers = Double.parseDouble(row[7]);
				final double avgNumberOfTripsWith2Transfers = Double.parseDouble(row[8]);
				final double avgNumberOfTripsWith3Transfers = Double.parseDouble(row[9]);
				final double avgNumberOfTripsWith4orMoreTransfers = Double.NaN;
				final double avgNumberOfTripsIncludingSubway = Double.parseDouble(row[10]);
				final double avgNumberOfTripsWithSubwayOnly = Double.parseDouble(row[11]);
				
				TripTableEntry tripTableEntry = new TripTableEntry(boardingZone, alightingZone, timeOfBoarding, avgNumberOfTripsPerWorkingDay, avgNumberOfTripsOfAdults, avgNumberOfTripsOfPupils, avgNumberOfTripsWith0Transfers, avgNumberOfTripsWith1Transfers, avgNumberOfTripsWith2Transfers, avgNumberOfTripsWith3Transfers, avgNumberOfTripsWith4orMoreTransfers, avgNumberOfTripsIncludingSubway, avgNumberOfTripsWithSubwayOnly);
				this.tripTableEntries.add(tripTableEntry);
				
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