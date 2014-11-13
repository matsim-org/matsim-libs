/* *********************************************************************** *
 * project: org.matsim.*
 * ScorAttrReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.scoring.withAttrRecorder;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import playground.yu.integration.cadyts.CalibrationConfig;

import java.util.Map;

/**
 * reads scoring function attributes from scorAttr file, and saves them in
 * custom attributes of {@code Plan}
 * 
 * @author yu
 * 
 */
public class ScorAttrReader implements TabularFileHandler {
	private final TabularFileParserConfig parserConfig;
	private Population population = null;
	private String[] attrNames = null;

	public ScorAttrReader(String scorAttrFilename, Population population) {
		parserConfig = new TabularFileParserConfig();
		parserConfig.setFileName(scorAttrFilename);
		parserConfig.setDelimiterRegex("\t");
		// parserConfig.setStartTag("PersonId");
		this.population = population;
	}

	public void parser() {
		TabularFileParser parser = new TabularFileParser();
		parser.parse(parserConfig, this);
	}

	@Override
	public void startRow(String[] row) {
		if (!row[0].equals("PersonId")) {
			if (attrNames == null) {
				throw new RuntimeException(
						"There is not yet attributes name collection, was Filehead not read?");
			}

			Person person = population.getPersons().get(Id.create(row[0], Person.class));
			Plan plan = person.getPlans().get(Integer.parseInt(row[1]));
			Map<String, Object> attrs = plan.getCustomAttributes();
			for (int i = 2; i < row.length; i++) {
				attrs.put(attrNames[i], Double.parseDouble(row[i]));
			}
		} else/* is started */{
			attrNames = row;// first 2 element don't belong to
			// attrNames
		}
	}

	public static class ScorAttrReadListener implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			Controler ctl = event.getControler();

			String scorAttrFilename = ctl.getConfig().findParam(
					CalibrationConfig.BSE_CONFIG_MODULE_NAME,
					"scorAttrFilename");
			if (scorAttrFilename != null) {
				System.out
						.println("BEGINNING of loading scoring function attributes for each plan.");
                new ScorAttrReader(scorAttrFilename, ctl.getScenario().getPopulation())
						.parser();
				System.out
						.println("ENDING of loading scoring function attributes.");
			}
		}

	}
}
