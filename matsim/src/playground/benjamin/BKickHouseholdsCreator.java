/* *********************************************************************** *
 * project: org.matsim.*
 * BKickHouseholdsCreator
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
package playground.benjamin;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.households.BasicHousehold;
import org.matsim.core.basic.v01.households.BasicHouseholdBuilder;
import org.matsim.core.basic.v01.households.BasicHouseholds;
import org.matsim.core.basic.v01.households.BasicIncome;
import org.matsim.core.basic.v01.households.HouseholdsWriterV1;


/**
 * @author dgrether
 *
 */
public class BKickHouseholdsCreator {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
    BasicScenarioImpl sc = new BasicScenarioImpl();
    Id id1 = sc.createId("1");
    Id id2 = sc.createId("2");
    BasicHouseholds<BasicHousehold> hhs = sc.getHouseholds();
    BasicHouseholdBuilder b = hhs.getHouseholdBuilder();
    
    BasicHousehold hh = b.createHousehold(id1);
    hh.setIncome(b.createIncome(40000, BasicIncome.IncomePeriod.year));
    hh.getMemberIds().add(id1);
    hhs.getHouseholds().put(id1, hh);
    
    hh = b.createHousehold(id2);
    hh.setIncome(b.createIncome(120000, BasicIncome.IncomePeriod.year));
    hh.getMemberIds().add(id2);
    hhs.getHouseholds().put(id2, hh);
    
    HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(hhs);
    hhwriter.writeFile("test/input/playground/benjamin/BKickScoringTest/households.xml");
    System.out.println("Households written!");
    
	}

}
