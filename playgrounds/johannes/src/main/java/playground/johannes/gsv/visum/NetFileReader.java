/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.visum;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author johannes
 *
 */
public class NetFileReader {
	
	private final static Logger logger = Logger.getLogger(NetFileReader.class);

	public final static String VISION_IDENT = "$VISION";
	
	private final static String COMMENT = "*";
	
	private final static String TABLE_PREFIX = "$";
	
	private final static String TABLE_SUFFIX = ":";
	
	public static String FIELD_SEPARATOR = ";";//FIXME
	
	private Map<String, TableHandler> tableHandlers;
	
	public NetFileReader(Map<String, TableHandler> tableHandlers) {
		this.tableHandlers = tableHandlers;
	}
	
	public void read(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		if(!line.contains(VISION_IDENT)) {
			logger.warn("File missing $VISION entry string. Are your sure that this is a VISUM .net file?");
		}
		
		TableHandler tableHandler = null;
		
		while((line = reader.readLine()) != null) {
			if(line.startsWith(TABLE_PREFIX)) {
				/*
				 * parse table identifier
				 */
				int idx = line.indexOf(TABLE_SUFFIX);
				String identifier = line.substring(1, idx);
				
				tableHandler = tableHandlers.get(identifier);
				if(tableHandler != null) {
					logger.info(String.format("Parsing table $s...", identifier));
					tableHandler.parseKeys(line);
				}
				
			} else if (line.startsWith(COMMENT)) {
				// do nothing, invalidate table handler
				tableHandler = null;
			} else if (line.length() == 0) {
				// empty line, end of table, invalidate table handler
				tableHandler = null;
			} else {
				if(tableHandler != null) {
					// line to parse
					tableHandler.parseRow(line);
				}
			}
		}
		
		reader.close();
	}
	
	public static abstract class TableHandler {
		
		private String keys[];
		
		private void parseKeys(String line) {
			int idx = line.indexOf(TABLE_SUFFIX);
			line = line.substring(idx + 1, line.length());
			keys = line.split(FIELD_SEPARATOR);
		}
		
		private void parseRow(String line) {
			String tokens[] = line.split(FIELD_SEPARATOR, -1);
			if(tokens.length == keys.length) {
				Map<String, String> map = new HashMap<String, String>(keys.length);
				for(int i = 0; i < keys.length; i++) {
					map.put(keys[i], tokens[i]);
				}
				handleRow(map);
			} else {
				throw new RuntimeException("Number of columns differs.");
			}
		}
		
		public abstract void handleRow(Map<String,String> record);
	}
}
