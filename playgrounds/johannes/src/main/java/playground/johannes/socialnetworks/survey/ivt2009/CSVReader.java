/* *********************************************************************** *
 * project: org.matsim.*
 * CSVReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author illenberger
 *
 */
public class CSVReader {
	
	private static final Logger logger = Logger.getLogger(CSVReader.class);
	
	private static final String SEMICOLON = ";";
	
	private static final String EMPTY = "";
	
	private static final int DATA_USER_ID_COL = 3;
	
	private static final int USER_ID_COL = 0;
	
	private static final int PARAM_COL = 1;
	
	private static final int VALUE_COL = 2;

	public Map<String, String[]> readUserData(String filename) throws IOException {
		logger.info("Loading user data...");
		int errors = 0;
		
		Map<String, String[]> users = new HashMap<String, String[]>();
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line;
		
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(SEMICOLON);
			String userId = trimString(tokens[USER_ID_COL]);
			if (!EMPTY.equalsIgnoreCase(userId)) {
				String[] attributes = new String[3];
				attributes[0] = trimString(tokens[1]); // surename
				attributes[1] = trimString(tokens[2]); // name
				attributes[2] = trimString(tokens[3]); // laufnummer
				if(!EMPTY.equals(attributes[0]) && !EMPTY.equals(attributes[1]) && !EMPTY.equals(attributes[2])) {
					if(users.put(userId, attributes) != null)
						logger.warn(String.format("Overwriting user attributes for usern %1$s", userId));
				}
			} else
				errors++;
		}
		
		if(errors > 0)
			logger.warn(String.format("Failed to read %1$s lines!", errors));
		
		return users;
	}
	
	public Map<String, Map<String, String>> readSnowballData(String filename) throws IOException {
		logger.info("Loading snowball data...");
		int idErrors = 0;
		int paramErrors = 0;
		int valueErrors = 0;
		
		Map<String, Map<String, String>> data = new HashMap<String, Map<String,String>>();
		
		BufferedReader reader = new BufferedReader(new FileReader(filename));		
		String line;
		
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(SEMICOLON);
			String userId = trimString(tokens[DATA_USER_ID_COL]);
			if (!EMPTY.equalsIgnoreCase(userId)) {

				Map<String, String> egoData = data.get(userId);
				if (egoData == null) {
					egoData = new HashMap<String, String>();
					data.put(userId, egoData);
				}

				String param = trimString(tokens[PARAM_COL]);
				if (!EMPTY.equalsIgnoreCase(param)) {
					String value = trimString(tokens[VALUE_COL]);

					if(!EMPTY.equalsIgnoreCase(value)) {
						String oldValue = egoData.put(param, value);
						if (oldValue != null)
							logger.warn(String.format(
								"Overwriting value for parameter %1$s (%2$s -> %3$s)! UserId = %4$s",
								param, oldValue, value, userId));
					} else {
//						valueErrors++; // empty strings seem to be ok.
					}
				} else {
					paramErrors++;
				}
			} else {
				idErrors++;
			}
		}
		
		if(idErrors > 0 || paramErrors > 0 || valueErrors > 0)
			logger.warn(String.format("%1$s errors parsing id, %2$s errors parsing parameter key, %3$s errors parsing parameter values!",
					idErrors, paramErrors, valueErrors));
		
		return data;
	}
	
	private String trimString(String str) {
		if(str.length() > 1)
			return str.substring(1, str.length() - 1).trim();
		return str;
	}
}
