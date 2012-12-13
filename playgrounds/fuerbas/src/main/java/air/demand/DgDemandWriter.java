/* *********************************************************************** *
 * project: org.matsim.*
 * DgDemandWriter
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
package air.demand;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgDemandWriter {
	
	private static final Logger log = Logger.getLogger(DgDemandWriter.class);

	public void writeFlightODRelations(String outputFile, SortedMap<String, SortedMap<String, FlightODRelation>> odRelations) throws IOException {
		this.writeFlightODRelations(outputFile, odRelations, null, null);
	}

	public void writeFlightODRelations(String outputFile,
			SortedMap<String, SortedMap<String, FlightODRelation>> odRelations,
			Integer totalDirectFlights, Integer totalStucked) throws IOException {
		this.writeFlightODRelations(outputFile, odRelations, totalDirectFlights, totalStucked, false);
	}
	
	public void writeFlightODRelations(String outputFile,
			SortedMap<String, SortedMap<String, FlightODRelation>> odRelations, Integer totalDirectFlights,
			Integer totalStucked, boolean absoluteValues) throws IOException {
		SortedSet<String> entries = new TreeSet<String>();
		for (Entry<String, SortedMap<String, FlightODRelation>> e : odRelations.entrySet()){
			entries.add(e.getKey());
			for (String key : e.getValue().keySet()) {
				entries.add(key);
			}
		}
		StringBuilder header = new StringBuilder();
		header.append(",");
		for (String key : entries){
			header.append(key);
			header.append(",");
		}
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFile);
		bw.append(header.toString());
		bw.newLine();
		for (String key : entries){
			bw.append(key);
			bw.append(",");
			Map<String, FlightODRelation> relations = odRelations.get(key);
			if (relations != null){
				for (String key2 : entries){
					FlightODRelation rel = relations.get(key2);
					if (rel != null){
						if (rel.getNumberOfTrips() != null){
							double i = rel.getNumberOfTrips();
							if (absoluteValues){
								i = Math.abs(i);
							}
							bw.append(Double.toString(i));
						}
						else {
							bw.append("-");
						}
					}
					else {
						bw.append("-");
					}
					bw.append(",");
				}
				bw.newLine();
			}
			else {
				for (String key2 : entries){
					bw.append("-");
					bw.append(",");
				}
				bw.newLine();
			}
		}
		if (totalDirectFlights != null || totalStucked != null){
			bw.newLine();
			bw.newLine();
			bw.newLine();
			if (totalDirectFlights != null) {
				bw.append("Total direct flights:,");
				bw.append(Integer.toString(totalDirectFlights));
				bw.newLine();
			}
			if (totalStucked != null){
				bw.append("Total stucked:,");
				bw.append(Integer.toString(totalStucked));
				bw.newLine();
			}
 		}
		bw.close();
	}


	public void writeFlightODRelationsList(String odDiffOutput, List<FlightODRelation> diff) throws IOException {
		SortedMap<String, SortedMap<String, FlightODRelation>> fromAirportCodeToAirportCodeMap = DgDemandUtils.createFromAirportCodeToAirportCodeMap(diff);
		SortedMap<String, FlightODRelation>first = null;
		BufferedWriter bw = IOUtils.getBufferedWriter(odDiffOutput);
		SortedSet<String> keys = new TreeSet<String>(fromAirportCodeToAirportCodeMap.keySet());
		for (String key : keys){
			SortedMap<String, FlightODRelation> m2 = fromAirportCodeToAirportCodeMap.get(key);
			if (first == null){
				first = m2;
				StringBuilder header = new StringBuilder();
				header.append(",");
				for (String key1 : first.keySet()){
					header.append(key1);
					header.append(",");
				}
				log.error("header: " + header.toString());
				bw.append(header.toString());
				bw.newLine();
			}
			bw.append(key);
			bw.append(",");

			if (m2.size() > keys.size()){
				throw new RuntimeException("Row entry has more columns than first row entry, cannot write this!");
			}
			for (String k : keys){
				FlightODRelation od = m2.get(k);
				if (od != null){
					bw.append(Double.toString(od.getNumberOfTrips()));
				}
				else {
					bw.append("0");
				}
				bw.append(",");
			}
			bw.newLine();
		}
		bw.close();
	}


}
