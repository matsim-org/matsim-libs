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

package playground.vsp.analysis.modules.ptTripAnalysis;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * 
 * Rereads the output of the pt trip analysis. Note that this is not safe with respect to changes in the code.
 * 
 * @author aneumann
 *
 */
public class PtTripAnalysisReader implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(PtTripAnalysisReader.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private PtTripAnalysisContainer ptTripAnalysisContainer = new PtTripAnalysisContainer();
	private int currentLine = 0;
	private int linesSkipped = 0;

	private PtTripAnalysisReader(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static PtTripAnalysisContainer readPtAnalysisCSV(String filename) {
		PtTripAnalysisReader reader = new PtTripAnalysisReader(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Skipped " + reader.linesSkipped + " lines");
		log.info("Imported " + reader.currentLine + " lines");
		return reader.ptTripAnalysisContainer;		
	}	

	private void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if (row.length != 5) {
			log.warn("Wrong length of row. Skipping: " + row);
			linesSkipped++;
			return;
		}
		if(!row[0].trim().contains("#")){
			try {
				currentLine++;
				
				for (int i = 1; i < row.length; i++) {
					this.ptTripAnalysisContainer.addData(currentLine - 1, i - 1, Double.parseDouble(row[i]));
				}
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
