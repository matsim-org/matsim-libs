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
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;


public class ReadFcdEvents implements TabularFileHandler{

	private static final Logger log = Logger.getLogger(ReadFcdEvents.class);
	
	private TabularFileParserConfig tabFileParserConfig;
	private LinkedList<FcdEvent> fcdEventList = new LinkedList<FcdEvent>();
	
	public static LinkedList<FcdEvent> readFcdEvents(String filename) throws IOException {
		
		ReadFcdEvents reader = new ReadFcdEvents();
		
		reader.tabFileParserConfig = new TabularFileParserConfig();
		reader.tabFileParserConfig.setFileName(filename);
		reader.tabFileParserConfig.setDelimiterTags(new String[] {"\t"}); // \t
//		this.tabFileParserConfig.setDelimiterTags(new String[] {"D"});
		new TabularFileParser().parse(reader.tabFileParserConfig, reader);

		return reader.fcdEventList;		
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
			
			double time = Time.parseTime(row[0].split(" ")[1]);
			Id linkId = new IdImpl(row[1]);
			double aveSpeed = Double.parseDouble(row[2]);
			double cover = Double.parseDouble(row[3]);
			Id vehId = new IdImpl(row[4]);
			int minuteOfWeek = Integer.parseInt(row[5]);
						
			this.fcdEventList.add(new FcdEvent(time, linkId, aveSpeed, cover, vehId, minuteOfWeek));
		}
		
	}
	
}
