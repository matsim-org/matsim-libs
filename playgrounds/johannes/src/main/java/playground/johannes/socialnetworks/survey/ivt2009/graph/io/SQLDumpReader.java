/* *********************************************************************** *
 * project: org.matsim.*
 * SQLDumpReader.java
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public class SQLDumpReader {
	
	private static final Logger logger = Logger.getLogger(SQLDumpReader.class);
	
	private static final String EMPTY = "";
	
	private static final String SEMICOLON = ";";
	
	private static final int USER_ID_COL = 3;
	
	private static final int PARAM_COL = 1;
	
	private static final int VALUE_COL = 2;
	
	private static final int NUM_HOMELOCS = 20;
	
	private static final String HOME_LOC_KEY = "84967X53X128Loc";
	
	private static final String HOME_LOC_YEAR_KEY = "84967X53X128von";
	
	private static final String ALTER_LOC_KEY = "84967X54X338Loc";
	
	private static final String YEAR_KEY = "84967X53X157";
	
	private static final String ALTER_YEAR_KEY = "84967X54X137";
	
	private static final int SURVEY_YEAR = 2010;
	
	private final GeometryFactory geoFactory = new GeometryFactory();

	private Map<String, Map<String, String>> rawData;
	
	public SQLDumpReader(List<String> files) throws IOException {
		rawData = new HashMap<String, Map<String,String>>();
		
		for(String file : files) {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line;
		while ((line = reader.readLine()) != null) {
			String[] tokens = line.split(SEMICOLON);
			String egoId = trimString(tokens[USER_ID_COL]);
			if (!EMPTY.equalsIgnoreCase(egoId)) {

				Map<String, String> egoData = rawData.get(egoId);
				if (egoData == null) {
					egoData = new HashMap<String, String>();
					rawData.put(egoId, egoData);
				}

				String param = trimString(tokens[PARAM_COL]);
				if (!EMPTY.equalsIgnoreCase(param)) {
					String value = trimString(tokens[VALUE_COL]);

					if(!EMPTY.equalsIgnoreCase(value)) {
						String oldValue = egoData.put(param, value);
						if (oldValue != null)
							logger.warn(String.format(
												"Overwriting value for parameter %1$s (%2$s -> %3$s)! UserId = %4$s",
												param, oldValue, value, egoId));
					}
				}
			}
		}
		}
	}
	
	private String trimString(String str) {
		if(str.length() > 1)
			return str.substring(1, str.length() - 1).trim();
		return str;
	}
	
	private String getValue(String egoId, String key) {
		Map<String, String> egoData = rawData.get(egoId);
		String value = egoData.get(key);
		if((value != null) && EMPTY.equalsIgnoreCase(value.trim()))
			return null;
		return value;
	}
	
	private String makeKey(String alterKey, String paramKey) {
		StringBuilder builder = new StringBuilder(alterKey.length() + paramKey.length() + 1);
		builder.append(alterKey);
		builder.append("_");
		builder.append(paramKey);
		return builder.toString();
	}
	
	private Point makePoint(String location) {
		String[] tokens = location.split("@");
		if(tokens.length >= 2) {
			String latitude = tokens[0];
			String longitude = tokens[1];
			return geoFactory.createPoint(new Coordinate(Double.parseDouble(latitude), Double.parseDouble(longitude)));
		} else {
			logger.warn("Invalid coordinate string!");
			return null;
		}
	}
	
	public Point getEgoLocation(String egoId) {
		SortedMap<Integer, String> homeLocs = new TreeMap<Integer, String>();
		for(int i = NUM_HOMELOCS; i > 0; i--) {
			String year = getValue(egoId, HOME_LOC_YEAR_KEY + i);
			if(year != null) {
				String homeLoc = getValue(egoId, HOME_LOC_KEY + i);
				if(homeLoc != null)
					homeLocs.put(new Integer(year), homeLoc);
				else
					logger.warn(String.format("Missing home location string. egoId = %1$s, entry = %2$s", egoId, i));
			}
		}
		
		if(homeLocs.isEmpty())
			return null;
		
		String lastLoc = homeLocs.get(homeLocs.lastKey());
		return makePoint(lastLoc);
	}
	
	public Point getAlterLocation(String egoId, String alterKey) {
		String location = getValue(egoId, makeKey(alterKey, ALTER_LOC_KEY));
		if(location != null)
			return makePoint(location);
		else
			return null;
	}
	
	public int getEgoAge(String egoId) {
		String val = getValue(egoId, YEAR_KEY);
		if(val != null) {
			return SURVEY_YEAR - (Integer.parseInt(val) + 1919);
		} else
			return -1;
	}
	
	public int getAlterAge(String egoId, String alterKey) {
		String val = getValue(egoId, makeKey(alterKey, ALTER_YEAR_KEY));
		if(val != null) {
			return SURVEY_YEAR - Integer.parseInt(val);
		} else
			return -1;
	}
}
