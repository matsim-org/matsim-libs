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

package org.matsim.contrib.minibus.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * 
 * Rereads the output of the {@linkplain PStatsOverview}. Note that this is not safe with respect to changes in the code.
 * 
 * @author aneumann
 *
 */
public class PStatsOverviewReader implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(PStatsOverviewReader.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private List<PStatsOverviewDataContainer> pStatOverviewData = new ArrayList<>();
	private int currentLine = 0;
	private int linesSkipped = 0;

	private PStatsOverviewReader(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {PStatsOverviewDataContainer.DELIMITER}); // \t
	}

	public static List<PStatsOverviewDataContainer> readPStatsOverviewCSV(String filename) {
		PStatsOverviewReader reader = new PStatsOverviewReader(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Skipped " + reader.linesSkipped + " lines");
		log.info("Imported " + reader.currentLine + " lines");
		return reader.pStatOverviewData;		
	}	

	private void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if (row.length != PStatsOverviewDataContainer.nColumns) {
			log.warn("Wrong length of row. Skipping: " + row);
			linesSkipped++;
			return;
		}
		if(!row[0].trim().contains("#") && !row[0].trim().contains("iter")){
			try {
				currentLine++;
				
				PStatsOverviewDataContainer container = new PStatsOverviewDataContainer();
				
				for (int i = 0; i < row.length; i++) {
					container.addData(i, Double.parseDouble(row[i]));
				}
				
				this.pStatOverviewData.add(container);
				
			} catch (NumberFormatException e) {
				this.linesSkipped++;
				log.info("Ignoring line : " + Arrays.asList(row));
			}
			
		} else {
			// Header line
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			this.linesSkipped++;
			log.info("Ignoring: " + tempBuffer);
		}
	}
}
