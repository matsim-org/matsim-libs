/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.santiago.colectivos;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * @author  jbischoff
 *
 */

public class CreateCollectivoFareAttributes {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final ObjectAttributes faresTable = new ObjectAttributes();
		TabularFileParserConfig parseconfig = new TabularFileParserConfig();
		parseconfig.setDelimiterTags(new String[]{";"});
		parseconfig.setFileName("C:/Users/Joschka/Desktop/santiago-felix/Fares.txt");
		new TabularFileParser().parse(parseconfig, new TabularFileHandler() {
			
			@Override
			public void startRow(String[] row) {
				double fare = Double.parseDouble(row[1]);
				faresTable.putAttribute("co"+row[0], "fare", fare);
				
			}
		});
		new ObjectAttributesXmlWriter(faresTable).writeFile("C:/Users/Joschka/Desktop/santiago-felix/fares.xml");
	}

}
