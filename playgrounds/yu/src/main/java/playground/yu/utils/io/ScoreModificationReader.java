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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class ScoreModificationReader implements TabularFileHandler {
	private String inputFilename;
	private TabularFileParserConfig parserConfig = new TabularFileParserConfig();
	/**
	 * Map<PersonId,scoreModification>
	 */
	private Map<Id<Person>, Double> personScoreModifications = new HashMap<>();

	public ScoreModificationReader(String inputFilename) {
		this.inputFilename = inputFilename;
		parserConfig.setCommentTags(new String[] { "personId" });
		parserConfig.setDelimiterRegex("\t");
		parserConfig.setFileName(this.inputFilename);
	}

	public double getPersonUtilityOffset(Id<Person> personId) {
		return this.personScoreModifications.get(personId);
	}

	public Map<Id<Person>, Double> getPersonScoreModifications() {
		return personScoreModifications;
	}

	@Override
	public void startRow(String[] row) {
		this.personScoreModifications.put(Id.create(row[0], Person.class)/* person Id */,
				Double.parseDouble(row[1])/* scoreModification */);
	}

	public void parse() {
        new TabularFileParser().parse(parserConfig, this);
    }
}
