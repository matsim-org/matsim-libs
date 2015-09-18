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

package playground.johannes.synpop.source.mid2008.generator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public abstract class RowHandler {

	private String separator = "\t";
	
	private int offset = 1;
	
	protected abstract void handleRow(Map<String, String> attributes);
	
	public void setSeparator(String separator) {
		this.separator = separator;
	}
	
	public void setColumnOffset(int offset) {
		this.offset = offset;
	}
	
	public void read(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line = reader.readLine();
		String keys[] = line.split(separator, -1);
		Map<String, String> attributes = new HashMap<String, String>(keys.length);
		
		int lineCount = 1;
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(separator, -1);
			
			if(tokens.length - offset > keys.length) // -1 because rows are numbered
				throw new RuntimeException(String.format("Line %s has more fields (%s) than available keys (%s).", lineCount, tokens.length, keys.length));
			
			for(int i = offset; i < tokens.length; i++) {
				attributes.put(keys[i - offset], tokens[i]);
			}
	
			handleRow(attributes);
		}
		
		reader.close();
	}
}
