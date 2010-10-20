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

import geo.google.datamodel.GeoCoordinate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import playground.johannes.socialnetworks.survey.ivt2009.graph.io.AlterTableReader.VertexRecord;
import playground.johannes.socialnetworks.survey.ivt2009.util.GoogleGeoCoder;

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
	
	private static final int NUM_CLIQUES = 40;
	
	private static final String HOME_LOC_KEY = "84967X53X128Loc";
	
	private static final String HOME_LOC_YEAR_KEY = "84967X53X128von";
	
	private static final String ALTER_LOC_KEY = "84967X54X338Loc";
	
	private static final String YEAR_KEY = "84967X53X106";
	
	private static final String ALTER_YEAR_KEY = "84967X54X137";
	
	private static final String ALTER_F2F_FREQ_KEY = "84967X54X145Unit";
	
	private static final String ALTER_F2F_UNIT_KEY = "84967X54X145Freq";
	
	private static final String SEX_KEY = "84967X53X107";
	
	private static final String ALTER_SEX_KEY = "84967X54X135";
	
	private static final String LICENSE_KEY = "84967X53X104";
	
	private static final String CAR_AVAIL_KEY = "84967X53X105";
	
	private static final String CITZEN_KEY = "84967X53X110";
	
	private static final String ALTER_CITIZEN_KEY = "84967X54X138";
	
	private static final String CLIQUE_KEY = "84967X59X249N";
	
	private static final String CIVIL_STATUS_KEY = "84967X53X118";
	
	private static final String EDUCATION_KEY = "84967X53X116";
	
	private static final String ALTER_EDU_KEY = "84967X54X131";
	
	private static final String EDGE_TYPE_KEY = "84967X54X140";
	
	private static final String INCOME_KEY = "84967X53X130";
	
	private static final int SURVEY_YEAR = 2010;
	
	private final GeometryFactory geoFactory = new GeometryFactory();

	private Map<String, Map<String, String>> rawData;
	
	private GoogleGeoCoder geoCoder;
	
	private Map<String, String> countries;
	
	public SQLDumpReader(List<String> files) throws IOException {
		geoCoder = new GoogleGeoCoder(500);
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
		Point point = null;
		String[] tokens = location.split("@");
		if(tokens.length >= 3) {
			String response = tokens[2];
			if(response.equalsIgnoreCase("200")) {
				String latitude = tokens[0];
				String longitude = tokens[1];
				point = geoFactory.createPoint(new Coordinate(Double.parseDouble(longitude), Double.parseDouble(latitude)));
			} else if(response.equalsIgnoreCase("602")) {
				logger.info("Response code 602 - requesting google geo-coder...");
				GeoCoordinate coord = geoCoder.requestCoordinate(tokens[6]);
				if(coord != null)
					point = geoFactory.createPoint(new Coordinate(coord.getLongitude(), coord.getLatitude()));
			}
		}
		
		if(point == null)
			logger.warn(String.format("Cannot decode location string: \"%1$s\".", location));
		
		return point;
	}
	
	public Point getEgoLocation(String egoId) {
		SortedMap<Integer, String> homeLocs = new TreeMap<Integer, String>();
		for(int i = NUM_HOMELOCS; i > 0; i--) {
			String year = getValue(egoId, HOME_LOC_YEAR_KEY + i);
			if(year != null) {
				String homeLoc = getValue(egoId, HOME_LOC_KEY + i);
				if(homeLoc != null)
					homeLocs.put(new Integer(year), homeLoc);
//				else
//					logger.warn(String.format("Missing home location string. egoId = %1$s, entry = %2$s", egoId, i));
			}
		}
		
		if(homeLocs.isEmpty())
			return null;
		
		String lastLoc = homeLocs.get(homeLocs.lastKey());
		return makePoint(lastLoc);
	}
	
	public Point getAlterLocation(Map<String, String> alterKeys) {
		for (Entry<String, String> entry : alterKeys.entrySet()) {
			String location = getValue(entry.getKey(), makeKey(entry.getValue(), ALTER_LOC_KEY));
			if (location != null)
				return makePoint(location);
		}
		return null;
	}
	
	public int getEgoAge(String egoId) {
		String val = getValue(egoId, YEAR_KEY);
		if(val != null) {
			return SURVEY_YEAR - Integer.parseInt(val);
//			return SURVEY_YEAR - (Integer.parseInt(val) + 1919);
		} else
			return -1;
	}
	
	public int getAlterAge(Map<String, String> alterKeys) {
		double sum = 0;
		int cnt = 0;
		for(Entry<String, String> entry : alterKeys.entrySet()) {
			String val = getValue(entry.getKey(), makeKey(entry.getValue(), ALTER_YEAR_KEY));
			if (val != null) {
				sum += SURVEY_YEAR - Integer.parseInt(val);
				cnt++;
			}
		}

		return (int) (sum/cnt);
	}
	
	public String getSex(VertexRecord record) {
		if(record.isEgo)
			return getEgoSex(record.egoSQLId);
		else
			return getAlterSex(record.alterKeys);
	}
	
	public String getEgoSex(String egoId) {
		String val = getValue(egoId, SEX_KEY);
		if("1".equalsIgnoreCase(val))
			return "m";
		else if("2".equalsIgnoreCase(val))
			return "f";
		else
			return null;
	}
	
	public String getAlterSex(Map<String, String> alterKeys) {
		for(Entry<String, String> entry : alterKeys.entrySet()) {
			String val = getValue(entry.getKey(), makeKey(entry.getValue(), ALTER_SEX_KEY));
			if (val != null) {
				if ("1".equalsIgnoreCase(val))
					return "m";
				else if ("2".equalsIgnoreCase(val))
					return "f";
			}
		}
		
		return null;
	}
	
	public String getLicense(VertexRecord record) {
		String val = getValue(record.egoSQLId, LICENSE_KEY);
		if("1".equalsIgnoreCase(val))
			return "yes";
		else if("2".equalsIgnoreCase(val))
			return "no";
		else
			return null;
	}
	
	public String getCarAvail(VertexRecord record) {
		String val = getValue(record.egoSQLId, CAR_AVAIL_KEY);
		if("1".equalsIgnoreCase(val))
			return "always";
		else if ("2".equalsIgnoreCase(val))
//			return "often";
			return "always";
		else if ("3".equalsIgnoreCase(val))
			return "sometimes";
		else if ("4".equalsIgnoreCase(val))
			return "never";
		else
			return null;
	}
	
	public String getCitizenship(VertexRecord record) {
		if(countries == null)
			countries = loadCountries();
		
		if(record.isEgo) {
			String val = getValue(record.egoSQLId, CITZEN_KEY);
			if(val != null)
				return countries.get(val);
		} else {
			for(Entry<String, String> entry : record.alterKeys.entrySet()) {
				String val = getValue(entry.getKey(), makeKey(entry.getValue(), ALTER_CITIZEN_KEY));
				if(val != null)
					return countries.get(val);
			}
		}
		
		return null;
	}
	
	public String getCivilStatus(VertexRecord record) {
		String val = null;
		
		if(record.isEgo) {
			val = getValue(record.egoSQLId, CIVIL_STATUS_KEY);
		}
		
		return val;
	}
	
	
	public String getEducation(VertexRecord record) {
		String val = null;
		
		if(record.isEgo)
			val = getValue(record.egoSQLId, EDUCATION_KEY);
		else {
			for(Entry<String, String> entry : record.alterKeys.entrySet()) {
				val = getValue(entry.getKey(), makeKey(entry.getValue(), ALTER_EDU_KEY));
				if(val != null)
					break;
			}
		}
		
		return val;
	}
	
	public String getEdgeType(VertexRecord v1, VertexRecord v2) {
		VertexRecord ego = null;
		VertexRecord alter = null;
		
		if(v1.isEgo) {
			ego = v1;
			alter = v2;
		} else if(v2.isEgo) {
			ego = v2;
			alter = v1;
		} else {
			logger.error("Either one vertex must be an ego.");
		}
		
		String alterKey = alter.alterKeys.get(ego.egoSQLId);
		if(alterKey == null) {
			logger.info("Alter key not found!");
		}
		String val = getValue(ego.egoSQLId, makeKey(alterKey, EDGE_TYPE_KEY));
		
		return val;
	}
	
	public double getF2FFrequencey(String egoId, String alterKey) {
		String freq = getValue(egoId, makeKey(alterKey, ALTER_F2F_FREQ_KEY));
		String unit = getValue(egoId, makeKey(alterKey, ALTER_F2F_UNIT_KEY));
		
		if(freq != null && unit != null) {
			double factor = 0;
			if(unit.equalsIgnoreCase("0"))
				return 0;
			else if(unit.equalsIgnoreCase("1"))
				factor = 365;
			else if(unit.equalsIgnoreCase("2"))
				factor = 52;
			else if(unit.equalsIgnoreCase("3"))
				factor = 12;
			else if(unit.equalsIgnoreCase("4"))
				factor = 1;
			else
				return 0;
			
			int idx = freq.indexOf("-");
			if(idx > 0) {
				freq = freq.substring(0, idx);
			}
			
			freq = freq.replace(",", ".");
			
			double frequency = 0;
			try {
				frequency = Double.parseDouble(freq);
			} catch (NumberFormatException e) {
				logger.warn(String.format("Cannot parse frequency! (%1$s)", freq));
			}
			return  frequency * factor;
		}
		
		return 0;
	}
	
	public int getIncome(VertexRecord record) {
		if(record.isEgo) {
			String val = getValue(record.egoSQLId, INCOME_KEY);
			if(val != null)
				return Integer.parseInt(val);
		}
		
		return -1;
	}
	
	public List<Set<String>> getCliques(VertexRecord record) {
		List<Set<String>> list = new ArrayList<Set<String>>();
		for(int i = 1; i <= NUM_CLIQUES; i++) {
			String val = getValue(record.egoSQLId, CLIQUE_KEY + i);
			if(val != null) {
				String[] tokens = val.split(",");
				if(tokens.length > 0) {
					Set<String> alters = new HashSet<String>();
					for(String token : tokens) {
						int num = Integer.parseInt(token);
						if(num >= 1 && num <= 29) {
							alters.add("84967X55X143A" + num);
						} else if(num >= 30 && num <= 40) {
							alters.add("84967X55X144B" + (num - 29));
						}
					}
					list.add(alters);
				}
			}
		}
		
		return list;
	}
	
	public Map<String, VertexRecord> getFullAlterKeyMappping(Collection<VertexRecord> records) {
		Map<String, VertexRecord> map = new HashMap<String, VertexRecord>();
		for(VertexRecord record : records) {
			for(Entry<String, String> alterKey : record.alterKeys.entrySet()) {
				map.put(alterKey.getKey()+alterKey.getValue(), record);
			}
		}
		return map;
	}
	
	private Map<String, String> loadCountries() {
		try {
			InputStream stream = ClassLoader.getSystemResourceAsStream("playground/johannes/socialnetworks/survey/ivt2009/graph/io/countries.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			Map<String, String> map = new HashMap<String, String>();
			while ((line = reader.readLine()) != null) {
				String tokens[] = line.split("\t");
				map.put(tokens[0], tokens[1]);
			}
			return map;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
