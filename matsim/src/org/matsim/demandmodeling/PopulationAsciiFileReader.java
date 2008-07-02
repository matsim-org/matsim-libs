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
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.io.tabularFileParser.TabularFileHandlerI;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * @author dgrether
 *
 */
public class PopulationAsciiFileReader implements TabularFileHandlerI {

	private static final Logger log = Logger.getLogger(PopulationAsciiFileReader.class);
	
	private static final String[] HEADER = {"PersonId", "HomeLocation", "Age", "IsFemale", "Income", "PrimaryActivityType", "PrimaryActivityLocation"};
	
	private TabularFileParserConfig tabFileParserConfig;
	
	private Plans plans;
	
	private boolean isFirstLine = true;
	
	public PopulationAsciiFileReader() {
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
			try {
				plan.createAct("h", 0.0, 0.0, row[1], null, "06:00:00", null, "false");
				plan.createAct(row[5], 0.0, 0.0,row[6], null, null, null, "true");
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
	
	
	public static void main(String[] args) throws IOException {
		new PopulationAsciiFileReader().readFile(args[0]);
	}


}
