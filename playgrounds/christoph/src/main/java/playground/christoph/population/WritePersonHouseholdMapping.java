/* *********************************************************************** *
 * project: org.matsim.*
 * WritePersonHouseholdMapping.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.population;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;

public class WritePersonHouseholdMapping {

	public static void main(String[] args) {
		Households households = new HouseholdsImpl();
		new HouseholdsReaderV10(households).readFile("/data/matsim/cdobler/sandbox00/input_census2000V2/input_goesgen_cut/households.xml.gz");
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter("/data/matsim/cdobler/sandbox00/input_census2000V2/input_goesgen_cut/personHouseholdMap.txt");
			writer.write("personId");
			writer.write("\t");
			writer.write("householdIds");
			writer.write("\n");
			for (Household household : households.getHouseholds().values()) {
				for (Id personId : household.getMemberIds()){
					writer.write(personId.toString());
					writer.write("\t");
					writer.write(household.getId().toString());
					writer.write("\n");				
				}
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
