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

package playground.johannes.gsv.matrices.postprocess;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class ZoneIDMappings {

	public static Map<String, String> modena2gsv2008(String file) {
		try {
			Map<String, String> map = new HashMap<>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while((line = reader.readLine()) != null) {
				String[] tokens = line.split("\t");
				String modenaId = tokens[0];
				String gsvId = tokens[4];
				map.put(modenaId, gsvId);
			}
			reader.close();
			
			return map;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
