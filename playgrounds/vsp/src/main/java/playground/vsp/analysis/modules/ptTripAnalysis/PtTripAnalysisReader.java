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
				switch (currentLine) {
				case 0:
					log.error("Should never get here. This seems to be the header.");
					break;
				case 1:
					this.ptTripAnalysisContainer.setSumTravelTime(row2double(row));
					break;
				case 2:
					this.ptTripAnalysisContainer.setAccesWalkCnt(row2int(row));
					break;
				case 3:
					this.ptTripAnalysisContainer.setAccesWaitCnt(row2int(row));
					break;
				case 4:
					this.ptTripAnalysisContainer.setEgressWalkCnt(row2int(row));
					break;
				case 5:
					this.ptTripAnalysisContainer.setSwitchWalkCnt(row2int(row));
					break;
				case 6:
					this.ptTripAnalysisContainer.setSwitchWaitCnt(row2int(row));
					break;
				case 7:
					this.ptTripAnalysisContainer.setLineCnt(row2int(row));
					break;
				case 8:
					this.ptTripAnalysisContainer.setAccesWalkTTime(row2double(row));
					break;
				case 9:
					this.ptTripAnalysisContainer.setAccesWaitTime(row2double(row));
					break;
				case 10:
					this.ptTripAnalysisContainer.setEgressWalkTTime(row2double(row));
					break;
				case 11:
					this.ptTripAnalysisContainer.setSwitchWalkTTime(row2double(row));
					break;
				case 12:
					this.ptTripAnalysisContainer.setSwitchWaitTime(row2double(row));
					break;
				case 13:
					this.ptTripAnalysisContainer.setLineTTime(row2double(row));
					break;
				case 14:
					this.ptTripAnalysisContainer.setLine1cnt(row2int(row));
					break;
				case 15:
					this.ptTripAnalysisContainer.setLine2cnt(row2int(row));
					break;
				case 16:
					this.ptTripAnalysisContainer.setLine3cnt(row2int(row));
					break;
				case 17:
					this.ptTripAnalysisContainer.setLine4cnt(row2int(row));
					break;
				case 18:
					this.ptTripAnalysisContainer.setLine5cnt(row2int(row));
					break;
				case 19:
					this.ptTripAnalysisContainer.setLine6cnt(row2int(row));
					break;
				case 20:
					this.ptTripAnalysisContainer.setLine7cnt(row2int(row));
					break;
				case 21:
					this.ptTripAnalysisContainer.setLine8cnt(row2int(row));
					break;
				case 22:
					this.ptTripAnalysisContainer.setLine9cnt(row2int(row));
					break;
				case 23:
					this.ptTripAnalysisContainer.setLine10cnt(row2int(row));
					break;
				case 24:
					this.ptTripAnalysisContainer.setLineGt10cnt(row2int(row));
					break;
				default:
					break;
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

	private double[] row2double(String[] in) {
		double[] out = new double[in.length - 1];
		for (int i = 1; i < in.length; i++) {
			out[i] = Double.parseDouble(in[i]);
		}
		return out;
	}
	
	private int[] row2int(String[] in) {
		int[] out = new int[in.length - 1];
		for (int i = 1; i < in.length; i++) {
			out[i] = Integer.parseInt(in[i]);
		}
		return out;
	}

}
