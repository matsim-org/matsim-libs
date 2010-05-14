/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreModificationReader.java
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

package playground.yu.utils.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ScoreModificationReader implements TabularFileHandler {
	private String inputFilename;
	private TabularFileParserConfig parserConfig = new TabularFileParserConfig();
	/**
	 * Map<PersonId,scoreModification>
	 */
	private Map<Id, Double> personScoreModifications = new HashMap<Id, Double>();

	public ScoreModificationReader(String inputFilename) {
		this.inputFilename = inputFilename;
		parserConfig.setCommentTags(new String[] { "personId" });
		parserConfig.setDelimiterRegex("\t");
		parserConfig.setFileName(this.inputFilename);
	}

	public double getPersonUtilityOffset(Id personId) {
		return this.personScoreModifications.get(personId);
	}

	public Map<Id, Double> getPersonScoreModifications() {
		return personScoreModifications;
	}

	@Override
	public void startRow(String[] row) {
		this.personScoreModifications.put(new IdImpl(row[0])/* person Id */,
				Double.parseDouble(row[1])/* scoreModification */);
	}

	public void parse() {
		try {
			new TabularFileParser().parse(parserConfig, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
