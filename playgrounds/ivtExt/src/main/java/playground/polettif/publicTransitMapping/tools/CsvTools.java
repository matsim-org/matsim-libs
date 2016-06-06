/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.publicTransitMapping.tools;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Methods to create and write csv data.
 *
 * @author polettif
 */
public class CsvTools {

	/**
	 * Converts a table with Tuple&lt;line, column&gt; as key to a list of csv lines.
	 */
	public static List<String> convertToCsvLines(Map<Tuple<Integer, Integer>, String> keyTable) {
		int maxCol = 0;

		// From <<line, column>, value> to <line, <column, value>>
		Map<Integer, Map<Integer, String>> lin_colVal = new TreeMap<>();
		for(Map.Entry<Tuple<Integer, Integer>, String> entry : keyTable.entrySet()) {
			Map<Integer, String> line = MapUtils.getMap(entry.getKey().getFirst(), lin_colVal);
			line.put(entry.getKey().getSecond(), entry.getValue());
			if(entry.getKey().getSecond() > maxCol) { maxCol = entry.getKey().getSecond(); }
		}

		// From <line, <column, value>> value> to <line, String>
		Map<Integer, String> csvLines = new TreeMap<>();
		for(Map.Entry<Integer, Map<Integer, String>> entry : lin_colVal.entrySet()) {
			String line = "";
			Map<Integer, String> cols = entry.getValue();
			for(int i=1; i <= maxCol; i++) {
				String value = (cols.get(i) == null ? "" : cols.get(i));
				line += value+";";
			}
			csvLines.put(entry.getKey(), line);
		}

		return new LinkedList<>(csvLines.values());
	}

	/**
	 * Writes a list of csvLines to a file
	 * @param csvLines
	 * @param filename
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	public static void writeToFile(List<String> csvLines, String filename) throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(filename, "UTF-8");

		csvLines.forEach(writer::println);

		writer.close();
	}
	
}