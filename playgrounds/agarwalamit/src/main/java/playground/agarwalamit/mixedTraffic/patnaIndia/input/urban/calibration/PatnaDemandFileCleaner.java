/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.input.urban.calibration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * First, read the file and count unknown data for each label.
 * Get value randomly based on the PatnaCMP stats.
 * Write it back.
 * @author amit
 */

public class PatnaDemandFileCleaner {

	private final String [] unknownLabels = {"NA","9999","a",
			""//blanks
			};
	
	/**
	 * This will read the file, and then collect the missing info, get this info based on stats and then write it back to a file.
	 */
	public void processForUnknownData(String inputFile) {
		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setFileName(inputFile);
		config.setDelimiterTags(new String [] {"\t"});

		TabularFileHandler handler = new TabularFileHandler() {
			List<String> labels = new ArrayList<>();

			@Override
			public void startRow(String[] row) {
				List<String> strs = Arrays.asList(row);

				if( row[0].substring(0, 1).matches("[A-Za-z]") // labels 
						&& !row[0].startsWith("NA") // "NA" could also be the data 
						) {
					for (String s : strs){ 
						labels.add(s); 
					}
				} else { // main data
					
					String originWard ;
					
					
				}

			}
		};

	}

}


