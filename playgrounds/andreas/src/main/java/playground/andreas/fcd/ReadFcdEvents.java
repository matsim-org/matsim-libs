/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.fcd;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;


public class ReadFcdEvents implements TabularFileHandler {

	static interface FcdEventSink {
		void process(FcdEvent fcdEvent);
	}

	class ListAdder implements FcdEventSink {

		@Override
		public void process(FcdEvent fcdEvent) {
			fcdEventList.add(fcdEvent);
		}

	}

	private static final Logger log = Logger.getLogger(ReadFcdEvents.class);

	private TabularFileParserConfig tabFileParserConfig;
	private LinkedList<FcdEvent> fcdEventList = new LinkedList<FcdEvent>();
	private FcdEventSink sink = new ListAdder();

	public ReadFcdEvents(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
	}

	public static LinkedList<FcdEvent> readFcdEvents(String filename) throws IOException {

		ReadFcdEvents reader = new ReadFcdEvents(filename);

		//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		reader.parse();

		return reader.fcdEventList;		
	}	

	public void parse() {
		try {
			new TabularFileParser().parse(tabFileParserConfig, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void startRow(String[] row) {
		if(row[0].contains("#")){
			StringBuffer tempBuffer = new StringBuffer();
			for (String string : row) {
				tempBuffer.append(string);
				tempBuffer.append(", ");
			}
			log.info("Ignoring: " + tempBuffer);
		} else {
			try {
				double time = Time.parseTime(row[0].split(" ")[1]);
				Id linkId = new IdImpl(row[1]);
				double aveSpeed = Double.parseDouble(row[2]);
				double cover = Double.parseDouble(row[3]);
				Id vehId = new IdImpl(row[4]);
				int minuteOfWeek = Integer.parseInt(row[5]);

				FcdEvent fcdEvent = new FcdEvent(time, linkId, aveSpeed, cover, vehId, minuteOfWeek);
				sink.process(fcdEvent);
			} catch (NumberFormatException e) {
				System.out.println("Ignoring line : " + Arrays.asList(row));
			}
		}

	}

	public void setSink(FcdEventSink sink) {
		this.sink = sink;
	}


}
