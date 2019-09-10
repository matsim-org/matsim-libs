/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.vsp.openberlinscenario.cemdap.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author dziemke
 */
@Deprecated
public class CemdapPersonParser {

	private final static Logger LOG = Logger.getLogger(CemdapPersonParser.class);

	// Cemdap adults/children file columns
//	private static final int HH_ID = 0;
	private static final int P_ID = 1;
//	...
	

	public CemdapPersonParser() {
	}

	
	public final void parse(String cemdapPersonFile, List<Id<Person>> personsIds) {
		int lineCount = 0;

		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(cemdapPersonFile);
			String currentLine = null;

			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 1000000 == 0) {
					LOG.info("Line " + lineCount + ": " + personsIds.size() + " persons stores so far.");
					Gbl.printMemoryUsage();
				}
				Id<Person> personId = Id.create(Integer.parseInt(entries[P_ID]), Person.class);
				personsIds.add(personId);
			}
		} catch (IOException e) {
			LOG.error(e);
		}
		LOG.info(lineCount + " lines parsed.");
		LOG.info(personsIds.size() + " persons stored.");
	}
}