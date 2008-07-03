/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.demandmodeling;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.WorldUtils;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.io.tabularFileParser.TabularFileHandlerI;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.utils.misc.Time;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;


/**
 * Reads a simple initial demand from a tab seperated ascii file
 * that has to contain the following columns:
 * PersonId	HomeLocation	Age	Gender	Income	PrimaryActivityType	PrimaryActivityLocation
 * 
 * A simple example of such a table can be seen in: 
 * test/input/org/matsim/demandmodeling/PopulationAsciiFileReader/asciipopulation.txt
 * @author dgrether
 *
 */
public class PopulationAsciiFileReader implements TabularFileHandlerI {

	private static final Logger log = Logger.getLogger(PopulationAsciiFileReader.class);
	
	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private static final double SIXOCLOCK = 6.0 * 3600.0;
	private static final double TWOHOURS = 2.0  * 3600.0;
	private static final double WORKDURATION = 8.0 * 3600.0;

	
	private TabularFileParserConfig tabFileParserConfig;
	
	private Plans plans;
	
	private boolean isFirstLine = true;
	
	private ZoneLayer zoneLayer;
	
	public PopulationAsciiFileReader(ZoneLayer zoneLayer) {
		this.zoneLayer = zoneLayer;
		this.tabFileParserConfig = new TabularFileParserConfig();
		plans = new Plans(Plans.NO_STREAMING);
	}

	public void startRow(String[] row) throws IllegalArgumentException {
		if (isFirstLine) {
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
			CoordI homeCoord = WorldUtils.getRandomCoordInZone(homeZone, this.zoneLayer);
			Zone primaryZone = (Zone)this.zoneLayer.getLocation(new IdImpl(row[6]));
			CoordI primaryCoord = WorldUtils.getRandomCoordInZone(primaryZone, this.zoneLayer);
			double homeEndTime = SIXOCLOCK + Gbl.random.nextDouble() * TWOHOURS;
			String homeEndTimeString = Time.writeTime(homeEndTime, Time.TIMEFORMAT_HHMMSS);
			try {
				plan.createAct("h", homeCoord.getX(), homeCoord.getY(), null, null, homeEndTimeString, null, "false");
				plan.createLeg(Leg.CARMODE, 0.0, 0.0, 0.0);
				plan.createAct(row[5], primaryCoord.getX(), primaryCoord.getY(), null, null, Time.writeTime(WORKDURATION, Time.TIMEFORMAT_HHMMSS), null, "true");
				plan.createLeg(Leg.CARMODE, 0.0, 0.0, 0.0);
				plan.createAct("h", homeCoord.getX(), homeCoord.getY(), null, null, null, null, "false");
				this.plans.addPerson(p);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
		}
		
	}
	
	
	public Plans readFile(String filename) throws IOException {
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
