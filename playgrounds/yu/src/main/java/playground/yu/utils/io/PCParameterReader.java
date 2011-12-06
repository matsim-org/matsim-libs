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
	private TabularFileParserConfig parserConfig;

	/**
	 * @parameters {@code Map}<{@code String} parameterName,{@code List}<
	 *             {@code Double}> parameter values>
	 */
	private Map<String, List<Double>> parameters;

	public PCParameterReader() {
		this.parserConfig = new TabularFileParserConfig();
		parserConfig.setDelimiterTags(new String[] { "\t" });

		this.parameters = new TreeMap<String, List<Double>>();
	}

	public void readFile(String filename) {
		parserConfig.setFileName(filename);
		new TabularFileParser().parse(parserConfig, this);
	}

	public List<Double> getParameter(String parameterName) {
		return this.parameters.get(parameterName);
	}

	public Set<String> getParameterNames() {
		return this.parameters.keySet();
	}

	@Override
	public void startRow(String[] row) {
		int size = row.length;

		if (!row[0].startsWith("iter")) {
			int i = 0;
			for (Iterator<List<Double>> it = this.parameters.values()
					.iterator(); it.hasNext();) {
				it.next().add(Double.parseDouble(row[i]));
				i++;
			}
		} else/* start */{
			for (int i = 0; i < size; i++) {
				this.parameters.put(row[i], new ArrayList<Double>());
			}
		}
	}

}
