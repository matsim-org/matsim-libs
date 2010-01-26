/* *********************************************************************** *
 * project: org.matsim.*
 * EgoAlterTableReader.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author illenberger
 *
 */
public class EgoAlterTableReader {

	public static class RespondentData {
		
		public Integer id;
		
		public Double latitude;
		
		public Double longitude;
		
		public Integer source;
	}
	
	private static final Logger logger = Logger.getLogger(EgoAlterTableReader.class);
	
	private static final String TAB = "\t";
	
	private static final String[] ignores = new String[]{"Testzugang", "#NV", "", "NICHT ANSCHREIBEN"};
	
	public Set<RespondentData> readEgoData(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));		
		String line = reader.readLine();
		String[] tokens = line.split(TAB);
		/*
		 * get column indices
		 */
		int idIdx = getIndex("Laufnr.", tokens);
		int statusIdx = getIndex("Tats�chlicher Teilnahmestatus FB", tokens);
		int longIdx = getIndex("long", tokens);
		int latIdx = getIndex("lat", tokens);
		
		if(idIdx < 0 || longIdx < 0 || latIdx < 0 || statusIdx < 0)
			throw new IllegalArgumentException("Header not found!");
		/*
		 * read data
		 */
		Set<RespondentData> respondentData = new LinkedHashSet<RespondentData>();
		while((line = reader.readLine()) != null) {
			tokens = line.split(TAB);
			/*
			 * check status
			 */
			if(!ignore(tokens[statusIdx])) {
				if(Integer.parseInt(tokens[statusIdx]) == 1) {
					RespondentData data = new RespondentData();
					/*
					 * parse id and source
					 */
					data.id = new Integer(tokens[idIdx]);
					/*
					 * parse coordinates
					 */
					try {
						data.longitude = new Double(tokens[longIdx]);
						data.latitude = new Double(tokens[latIdx]);
					} catch (NumberFormatException e) {
						logger.warn(String.format("Parsing coordinates failed (%1$s, %2$s)", tokens[longIdx], tokens[latIdx]));
					}
					/*
					 * store data
					 */
					respondentData.add(data);
				}
			}
		}
		logger.info(String.format("Parsed %1$s records.", respondentData.size()));
		return respondentData;
	}
	
	public Set<RespondentData> readAlterData(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename));		
		String line = reader.readLine();
		String[] tokens = line.split(TAB);
		/*
		 * get column indices
		 */
		int idIdx = getIndex("AlterLaufnummer", tokens);
		int longIdx = getIndex("long", tokens);
		int latIdx = getIndex("lat", tokens);
		int sourceIdIdx = getIndex("Excel", tokens);
		
		if(idIdx < 0 || longIdx < 0 || latIdx < 0 || sourceIdIdx < 0)
			throw new IllegalArgumentException("Header not found!");
		/*
		 * read data
		 */
		Set<RespondentData> respondentData = new LinkedHashSet<RespondentData>();
		int idPool = 1000000;
		int coordinateError = 0;
		while ((line = reader.readLine()) != null) {
			tokens = line.split(TAB);

			RespondentData data = new RespondentData();
			/*
			 * parse id and source
			 */
			if(ignore(tokens[idIdx]))
				data.id = new Integer(idPool++);
			else
				data.id = new Integer(tokens[idIdx]);
			
			if (!ignore(tokens[sourceIdIdx])) {
				String[] sourceId = tokens[sourceIdIdx].split(" ");
				data.source = new Integer(sourceId[sourceId.length - 1]);
				/*
				 * parse coordinates
				 */
				try {
					data.longitude = new Double(tokens[longIdx]);
					data.latitude = new Double(tokens[latIdx]);
				} catch (Exception e) {
					coordinateError++;
				}
				/*
				 * store data
				 */
				respondentData.add(data);
			} else {
				logger.warn(String.format("ID=%1$s is not valid.", tokens[sourceIdIdx]));
			}
		}
		logger.info(String.format("Parsed %1$s records.", respondentData.size()));
		logger.warn(String.format("Failed to parse coordinates %1$s times.", coordinateError));
		return respondentData;
	}
	
	private int getIndex(String header, String[] tokens) {
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equalsIgnoreCase(header))
				return i;
		}
		
		return -1;
	}
	
	private boolean ignore(String token) {
		for(String str : ignores)
			if(str.equalsIgnoreCase(token))
				return true;
		
		return false;
	}
}
