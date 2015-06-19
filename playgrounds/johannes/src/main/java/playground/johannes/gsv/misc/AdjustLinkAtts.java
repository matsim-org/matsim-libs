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

package playground.johannes.gsv.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author johannes
 * 
 */
public class AdjustLinkAtts {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Map<String, String> ttmap = new HashMap<>();
		ttmap.put("0", "0");
		ttmap.put("30", "63");
		ttmap.put("40", "42");
		ttmap.put("33", "84");
		ttmap.put("71", "8");
		ttmap.put("41", "56");
		ttmap.put("32", "63");
		ttmap.put("13", "103");
		ttmap.put("49", "49");
		ttmap.put("34", "84");
		ttmap.put("48", "35");
		ttmap.put("10", "71");
		ttmap.put("37", "42");
		ttmap.put("1", "28");
		ttmap.put("14", "111");
		ttmap.put("38", "63");
		ttmap.put("11", "87");
		ttmap.put("12", "71");
		ttmap.put("36", "42");
		ttmap.put("35", "56");
		ttmap.put("31", "77");
		ttmap.put("42", "63");

		Map<String, String> capmap = new HashMap<>();
		capmap.put("0", "0");
		capmap.put("30", "20000");
		capmap.put("40", "10000");
		capmap.put("33", "40000");
		capmap.put("71", "5000");
		capmap.put("41", "15000");
		capmap.put("32", "35000");
		capmap.put("13", "50000");
		capmap.put("49", "20000");
		capmap.put("34", "45000");
		capmap.put("48", "15000");
		capmap.put("10", "35000");
		capmap.put("37", "30000");
		capmap.put("1", "25000");
		capmap.put("14", "60000");
		capmap.put("38", "40000");
		capmap.put("11", "40000");
		capmap.put("12", "45000");
		capmap.put("36", "20000");
		capmap.put("35", "25000");
		capmap.put("31", "30000");
		capmap.put("42", "20000");
		
		Map<String, String> typeMap = new HashMap<>();
		typeMap.put("31", "35");
		typeMap.put("30", "36");
		typeMap.put("32", "37");
		typeMap.put("34", "38");
		typeMap.put("41", "48");
		typeMap.put("42", "49");

		BufferedReader reader = new BufferedReader(new FileReader("/home/johannes/gsv/prognose-update/strecken2030.de.att"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/johannes/gsv/prognose-update/strecken2030.de.edit.att"));

		for (int i = 0; i < 13; i++) {
			writer.write(reader.readLine());
			writer.newLine();
		}

		String line = null;
		while ((line = reader.readLine()) != null) {
			if (!line.isEmpty()) {
				String[] tokens = line.split("\t");

				String typenr = typeMap.get(tokens[3]);
				if(typenr == null) { // no type to replace, use original
					typenr = tokens[3];
				}
				
				String replace = capmap.get(typenr);
				if (replace != null)
					tokens[7] = replace;

				replace = ttmap.get(typenr);
				if (replace != null)
					tokens[8] = replace;

				writer.write(StringUtils.join(tokens, "\t"));
				writer.newLine();
			}
		}
		reader.close();
		writer.close();
	}

}
