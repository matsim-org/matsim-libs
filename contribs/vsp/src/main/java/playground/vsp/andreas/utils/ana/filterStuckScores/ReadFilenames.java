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
package playground.vsp.andreas.utils.ana.filterStuckScores;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ReadFilenames implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(ReadFilenames.class);

	private TabularFileParserConfig tabFileParserConfig;
	private List<String> filenames = new LinkedList<String>();
	private FilenameSink sink = new FilenameAdder();
	private int linesRejected = 0;

	static interface FilenameSink {
		void process(String filename);
	}

	class FilenameAdder implements FilenameSink {
		@Override
		public void process(String filename) {
			filenames.add(filename);
		}
	}

	public ReadFilenames(String filename) {
		tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(filename);
		tabFileParserConfig.setDelimiterTags(new String[] {";"}); // \t
	}

	public static List<String> readFilenames(String filename){
		ReadFilenames reader = new ReadFilenames(filename);
		log.info("Start parsing " + filename);
		reader.parse();
		log.info("Finished parsing " + filename);
		log.info("Rejected " + reader.linesRejected + " lines");
		log.info("Imported " + reader.filenames.size() + " lines");
		return reader.filenames;		
	}	

	public void parse() {
        new TabularFileParser().parse(tabFileParserConfig, this);
    }
	
	@Override
	public void startRow(String[] row) {
		if(!row[0].trim().startsWith("#")){
			try {
				String filename = row[0].trim();
				sink.process(filename);
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

	public void setSink(FilenameSink sink) {
		this.sink = sink;
	}
}