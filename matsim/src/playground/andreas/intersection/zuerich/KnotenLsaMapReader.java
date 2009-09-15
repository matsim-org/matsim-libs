/* *********************************************************************** *
 * project: org.matsim.*
 * KnotenLsaMapReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.andreas.intersection.zuerich;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * @author dgrether
 *
 */
public class KnotenLsaMapReader implements TabularFileHandler {
	
	private static final Logger log = Logger.getLogger(KnotenLsaMapReader.class);
	
	private TabularFileParserConfig tabFileParserConfig;

	private Map<Integer, Integer> knotenLsaMap;
	
	public Map<Integer, Integer> readFile(String knotenLsaMappingFile) {
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.tabFileParserConfig.setFileName(knotenLsaMappingFile);
		this.tabFileParserConfig.setDelimiterTags(new String[] {" ", "\t"}); // \t

		this.knotenLsaMap = new LinkedHashMap<Integer, Integer>();
		try {
			new TabularFileParser().parse(this.tabFileParserConfig, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Read " + this.knotenLsaMap.size() + " knoten lsa mappings!");
		return this.knotenLsaMap;
	}

	public void startRow(String[] row) {
		String knotenIdString = row[0];
		String lsaIdString = row	[1];
		
		if (knotenIdString.matches("[\\d]+") && lsaIdString.matches("[\\d]+") ){
			Integer knotenId = Integer.valueOf(knotenIdString);
			Integer lsaId = Integer.valueOf(lsaIdString);
			this.knotenLsaMap.put(knotenId, lsaId);
		}
	}
}
