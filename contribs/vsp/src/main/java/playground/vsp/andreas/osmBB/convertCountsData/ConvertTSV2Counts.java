/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.osmBB.convertCountsData;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

public class ConvertTSV2Counts {

	private static final Logger log = LogManager.getLogger(ConvertTSV2Counts.class);

	public static void main(String[] args) {

		String workingDir = "F:\\bln_counts\\";
		String startTag = "Di-Do";
		String countStationsFileName = workingDir + "DZS-Koordinaten_AN.csv";
		String countsOutFile = workingDir + startTag + "_counts.xml";

		log.info("Reading count stations from " + countStationsFileName + " ...");
		List<CountStationDataBox> countStations = ReadCountStations.readCountStations(countStationsFileName);

		log.info("Building count station map by reading " + countStations.size() + " stations");
		HashMap<String, CountStationDataBox> countStationsMap = new HashMap<String, CountStationDataBox>();
		for (CountStationDataBox countStation : countStations) {
			if(countStationsMap.get(countStation.getShortName()) == null) {
				countStationsMap.put(countStation.getShortName(), countStation);
			} else {
				log.info("Duplicate count station found: " + countStation.toString());
			}
		}
		log.info("Final map contains " + countStationsMap.size() + " stations");

		log.info("Reading counts...");
		Counts counts = new Counts();
		// set some nonsense, cause writer allows for empty fields, but reader complains
		counts.setYear(2009);
		counts.setName("hab ich nicht");


		for (CountStationDataBox countStation : countStationsMap.values()) {
			counts.createAndAddCount(Id.create(countStation.getShortName(), Link.class), countStation.getShortName());
			counts.getCount(Id.create(countStation.getShortName(), Link.class)).setCoord(countStation.getCoord());
			String filename = workingDir + "Wochenübersicht_" + countStation.getShortName() + ".tsv";
			ReadCountDataForWeek.readCountDataForWeek(filename, counts.getCount(Id.create(countStation.getShortName(), Link.class)), startTag);
		}

		Set<Id> countIds = new TreeSet<Id>(counts.getCounts().keySet());
		for (Id countId : countIds) {
			if(counts.getCount(countId).getVolumes().isEmpty() == true){
				counts.getCounts().remove(countId);
			}
		}

		log.info("Converted counts data for " + counts.getCounts().size() + " stations");

		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(countsOutFile);
		log.info("Counts written to " + countsOutFile);

		log.info("Finish...");

	}

}
