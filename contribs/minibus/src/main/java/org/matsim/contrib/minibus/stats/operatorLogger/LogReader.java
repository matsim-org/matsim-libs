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

package org.matsim.contrib.minibus.stats.operatorLogger;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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
		tabFileParserConfig.setDelimiterTags(new String[] {LogElement.DELIMITER});
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
		if(!row[0].trim().startsWith(LogElement.COMMENTTAG) || !row[0].trim().startsWith("iter")){
			if(!row[3].trim().startsWith(LogElement.NOVALUE) || !row[3].trim().startsWith("===")){
				try {
					LogElement logElement = new LogElement();
					
					logElement.setIteration(Integer.parseInt(row[0]));
					logElement.setOperatorId(Id.create(row[1], Operator.class));
					
					String state = row[2];
					if (state.equalsIgnoreCase(PConstants.OperatorState.PROSPECTING.name())) {
						logElement.setStatus(PConstants.OperatorState.PROSPECTING);
					} else if (state.equalsIgnoreCase(PConstants.OperatorState.INBUSINESS.name())) {
						logElement.setStatus(PConstants.OperatorState.INBUSINESS);
					} else if (state.equalsIgnoreCase(PConstants.OperatorState.BANKRUPT.name())) {
						logElement.setStatus(PConstants.OperatorState.BANKRUPT);
					} else {
						log.error("Couldn't determine the operator state " + state);
					}
					
					logElement.setPlanId(Id.create(row[3], PPlan.class));
					logElement.setCreatorId(row[4]);
					logElement.setParentId(Id.create(row[5], PPlan.class));
					logElement.setnVeh(Integer.parseInt(row[6]));
					logElement.setnPax(Integer.parseInt(row[7]));
					logElement.setScore(Double.parseDouble(row[8]));
					logElement.setBudget(Double.parseDouble(row[9]));
					logElement.setStartTime(Time.parseTime(row[10]));
					logElement.setEndTime(Time.parseTime(row[11]));
					
					String stops = row[12];
					stops = stops.substring(1, stops.length() - 1); // remove brackets
					String[] stopArray = stops.split(","); // split the array
					ArrayList<Id<org.matsim.facilities.Facility>> stopIds = new ArrayList<Id<org.matsim.facilities.Facility>>();
					for (String stop : stopArray) {
						stopIds.add(Id.create(stop.trim(), org.matsim.facilities.Facility.class));
					}
					logElement.setStopsToBeServed(stopIds);
					
					String links = row[13];
					links = links.substring(1, links.length() - 1); // remove brackets
					String[] linkArray = links.split(","); // split the array
					ArrayList<Id<Link>> linkIds = new ArrayList<Id<Link>>();
					for (String link : linkArray) {
						linkIds.add(Id.create(link.trim(), Link.class));
					}
					logElement.setLinksServed(linkIds);
					
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
