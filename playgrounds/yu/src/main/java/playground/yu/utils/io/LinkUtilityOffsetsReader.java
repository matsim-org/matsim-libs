/* *********************************************************************** *
 * project: org.matsim.*
 * LinkUtilityOffsetsReader.java
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

package playground.yu.utils.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class LinkUtilityOffsetsReader implements TabularFileHandler {
	private TabularFileParserConfig parserConfig = new TabularFileParserConfig();
	private Map<Integer/* timeBin */, Map<Id/* linkId */, Double/* utiliyOffset */>> linkUtiliyOffsets = new HashMap<Integer/* timeBin */, Map<Id/* linkId */, Double/* utiliyOffset */>>();

	public LinkUtilityOffsetsReader(String inputFilename) {
		// parserConfig.setCommentTags(new String[] { "?????" });
		parserConfig.setDelimiterRegex("\t");
		parserConfig.setFileName(inputFilename);
	}

	public Map<Integer, Map<Id, Double>> getLinkUtiliyOffsets() {
		return linkUtiliyOffsets;
	}

	@Override
	public void startRow(String[] row) {
		String linkId = row[1];
		int timeBin = Integer.parseInt(row[3]);
		double utilityOffset = Double.parseDouble(row[5]);
		Map<Id, Double> utilityOffsetsPerTimeBin = this.linkUtiliyOffsets
				.get(timeBin);
		if (utilityOffsetsPerTimeBin == null) {
			utilityOffsetsPerTimeBin = new HashMap<Id, Double>();
			this.linkUtiliyOffsets.put(timeBin, utilityOffsetsPerTimeBin);
		}
		utilityOffsetsPerTimeBin.put(new IdImpl(linkId), utilityOffset);
	}

	public void parse() {
		try {
			new TabularFileParser().parse(parserConfig, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String linkUtilityOffsetFilename = "../integration-demandCalibration1.0.1/test/output/prepare/linkIdTimeBinX.log";
		LinkUtilityOffsetsReader reader = new LinkUtilityOffsetsReader(
				linkUtilityOffsetFilename);
		reader.parse();
		System.out.println(reader.getLinkUtiliyOffsets());
	}
}
