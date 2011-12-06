/* *********************************************************************** *
 * project: org.matsim.*
 * PCParameterReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.utils.io;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * reader for parameter file for "parameter calibration"
 * 
 * @author Chen
 * 
 */
public class PCParameterReader implements TabularFileHandler {
	private final TabularFileParserConfig parserConfig;

	/**
	 * @parameters {@code Map}<{@code String} parameterName,{@code List}<
	 *             {@code Double}> parameter values>
	 */
	private final Map<String, List<Double>> parameters;

	public PCParameterReader() {
		parserConfig = new TabularFileParserConfig();
		parserConfig.setDelimiterTags(new String[] { "\t" });

		parameters = new TreeMap<String, List<Double>>();
	}

	public List<Double> getParameter(String parameterName) {
		return parameters.get(parameterName);
	}

	public Set<String> getParameterNames() {
		Set<String> keySet = parameters.keySet();
		Set<String> delegate = new TreeSet<String>();
		int i = 0;
		for (String key : keySet) {
			if (i > 0) {
				delegate.add(key);
			}
			i++;
		}
		return delegate;// first column
	}

	public void readFile(String filename) {
		parserConfig.setFileName(filename);
		new TabularFileParser().parse(parserConfig, this);
	}

	@Override
	public void startRow(String[] row) {
		int size = row.length;

		if (!row[0].startsWith("iter")) {
			int i = 0;
			for (Iterator<List<Double>> it = parameters.values().iterator(); it
					.hasNext();) {
				it.next().add(Double.parseDouble(row[i]));
				i++;
			}
		} else/* start */{
			for (int i = 0; i < size; i++) {
				parameters.put(row[i], new ArrayList<Double>());
			}
		}
	}

}
