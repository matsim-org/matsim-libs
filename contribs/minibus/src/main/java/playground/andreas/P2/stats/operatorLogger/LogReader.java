/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.andreas.P2.stats.operatorLogger;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;

/**
 * Reads a pLogger file omitting the header and all totals.
 * 
 * @author aneumann
 *
 */
public final class LogReader implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(LogReader.class);

	private final TabularFileParserConfig tabFileParserConfig;
	private final ArrayList<LogElement> processedLines = new ArrayList<>();
	private final LogElementSinkImpl sink = new LogElementSinkImpl();
	private int linesRejected = 0;

	static interface LogElementSink {
		void process(LogElement logElement);
	}

	class LogElementSinkImpl implements LogElementSink {
		@Override
		public void process(LogElement logElement) {
			processedLines.add(logElement);
		}
	}

	private LogReader(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
	}

	public static ArrayList<LogElement> readFile(String filename){
		LogReader reader = new LogReader(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.processedLines.size() + " lines");
		return reader.processedLines;		
	}	

	private void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("iter")){
			if(!row[3].trim().startsWith("===")){
				try {
					LogElement logElement = new LogElement();
					
					logElement.setIteration(Integer.parseInt(row[0]));
					logElement.setOperatorId(row[1]);
					logElement.setStatus(row[2]);
					logElement.setPlanId(row[3]);
					logElement.setCreatorId(row[4]);
					logElement.setnVeh(Integer.parseInt(row[5]));
					logElement.setnPax(Integer.parseInt(row[6]));
					logElement.setScore(Double.parseDouble(row[7]));
					logElement.setBudget(Double.parseDouble(row[8]));
					logElement.setStartTime(Time.parseTime(row[9]));
					logElement.setEndTime(Time.parseTime(row[10]));
					
					String nodes = row[11];
					nodes = nodes.substring(1, nodes.length() - 1);
					String[] n = nodes.split(",");
					logElement.setStopsToBeServed(n);
					
					sink.process(logElement);
					
				} catch (NumberFormatException e) {
					this.linesRejected++;
					log.info("Ignoring line : " + Arrays.asList(row));
				}

			} else {
				StringBuffer tempBuffer = new StringBuffer();
				for (String string : row) {
					tempBuffer.append(string);
					tempBuffer.append(", ");
				}
				this.linesRejected++;
				log.info("Ignoring: " + tempBuffer);
			}
		}
	}

}