/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.demandmodeling;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

/**
 * Reads a simple initial demand from a tab separated ascii file
 * that has to contain the following columns:
 * PersonId	HomeLocation	Age	Gender	Income	PrimaryActivityType	PrimaryActivityLocation
 *
 * A simple example of such a table can be seen in:
 * test/input/org/matsim/demandmodeling/PopulationAsciiFileReader/asciipopulation.txt
 *
 * @author dgrether
 */
public class PopulationAsciiFileReader implements TabularFileHandler {

	private static final Logger log = Logger.getLogger(PopulationAsciiFileReader.class);

	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};

	private static final double SIXOCLOCK = 6.0 * 3600.0;
	private static final double TWOHOURS = 2.0  * 3600.0;
	private static final double WORKDURATION = 8.0 * 3600.0;
	private static final String ACTTYPE_HOME = "h";

	private final TabularFileParserConfig tabFileParserConfig;

	private final Population plans;

	private boolean isFirstLine = true;

	private final ZoneLayer zoneLayer;

	public PopulationAsciiFileReader(final ZoneLayer zoneLayer) {
		this.zoneLayer = zoneLayer;
		this.tabFileParserConfig = new TabularFileParserConfig();
		this.plans = new Population(Population.NO_STREAMING);
	}

	public void startRow(final String[] row) throws IllegalArgumentException {
		if (this.isFirstLine) {
			boolean equalsHeader = true;
			int i = 0;
			for (String s : row) {
				if (!s.equalsIgnoreCase(HEADER[i])) {
					equalsHeader = false;
					break;
				}
				i++;
			}
			if (!equalsHeader) {
				log.warn("#######################################################################");
				log.warn("#######################################################################");
				log.warn("#######################################################################");
				log.warn("Not even the header of the files has correct names, please check semantical correctness of data in the file to ensure correct plan creation!");
				log.warn("Header should be: ");
				for (String g : HEADER) {
					System.out.print(g + " ");
				}
				System.out.println();
				log.warn("#######################################################################");
				log.warn("#######################################################################");
				log.warn("#######################################################################");
			}
			this.isFirstLine = false;
		}
		else {
			Person p = new Person(new IdImpl(row[0]));
			p.setAge(Integer.parseInt(row[2]));
			p.setSex(row[3]);
			log.warn("Income is not supported by the current version of MATSim. Column 5 will be ignored");
			Plan plan = p.createPlan(true);
			Zone homeZone = (Zone)this.zoneLayer.getLocation(new IdImpl(row[1]));
			Coord homeCoord = WorldUtils.getRandomCoordInZone(homeZone, this.zoneLayer);
			Zone primaryZone = (Zone)this.zoneLayer.getLocation(new IdImpl(row[6]));
			Coord primaryCoord = WorldUtils.getRandomCoordInZone(primaryZone, this.zoneLayer);
			double homeEndTime = SIXOCLOCK + MatsimRandom.random.nextDouble() * TWOHOURS;

			Act act1 = plan.createAct(ACTTYPE_HOME, homeCoord);
			act1.setEndTime(homeEndTime);

			plan.createLeg(BasicLeg.Mode.car);

			Act act2 = plan.createAct(row[5], primaryCoord);
			act2.setDur(WORKDURATION);

			plan.createLeg(BasicLeg.Mode.car);

			/*Act act3 = */plan.createAct(ACTTYPE_HOME, homeCoord);
			this.plans.addPerson(p);
		}
	}

	public Population readFile(final String filename) throws IOException {
		log.warn("#######################################################################");
		log.warn("This tool is not able to check the semantical correctness of data a better solution would be usage of xml.");
		log.warn("The correctnes of the resulting plans file depends on the correct usage of the input format, that will not be checked by this tool, please take care");
		log.warn("#######################################################################");
		this.tabFileParserConfig.setFileName(filename);
		this.tabFileParserConfig.setDelimiterTags(new String[] {"\t"});
		new TabularFileParser().parse(this.tabFileParserConfig, this);
		return this.plans;
	}

}
