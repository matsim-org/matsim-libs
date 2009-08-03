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
package playground.benjamin.income;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.basic.BasicIncome;
import org.matsim.households.basic.HouseholdBuilder;
import org.matsim.households.basic.HouseholdsWriterV1;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class BKickHouseholdsCreator {

	
	
	public static void createHHForTestCase() throws FileNotFoundException, IOException {
    ScenarioImpl sc = new ScenarioImpl();
    Id id1 = sc.createId("1");
    Id id2 = sc.createId("2");
    Households hhs = sc.getHouseholds();
    HouseholdBuilder b = hhs.getBuilder();

    Household hh = b.createHousehold(id1);
    hh.setIncome(b.createIncome(120000, BasicIncome.IncomePeriod.year));
    hh.getMemberIds().add(id1);
    hhs.getHouseholds().put(id1, hh);
    
    hh = b.createHousehold(id2);
    hh.setIncome(b.createIncome(40000, BasicIncome.IncomePeriod.year));
    hh.getMemberIds().add(id2);
    hhs.getHouseholds().put(id2, hh);
    
    HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(hhs);
    hhwriter.writeFile(DgPaths.SHAREDSVN + "test/input/playground/benjamin/BKickScoringTest/households.xml");
    System.out.println("Households written!");
	}

	public static void createHHForTestScenario() throws FileNotFoundException, IOException {
		String outdir = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/";
		String plansFile = outdir + "plans.xml";
		String networkFile =outdir + "../oneRouteNoModeTest/network.xml";
		
		ScenarioImpl sc = new ScenarioImpl();
		NetworkLayer network = sc.getNetwork();
		MatsimNetworkReader netreader = new MatsimNetworkReader(network);
		netreader.readFile(networkFile);
		
    PopulationImpl pop = sc.getPopulation();
    MatsimPopulationReader popReader = new MatsimPopulationReader(pop, network);
    popReader.readFile(plansFile);
    
    Households hhs = sc.getHouseholds();
    HouseholdBuilder b = hhs.getBuilder();

    IncomeCalculatorKantonZurich incomeCalculator = new IncomeCalculatorKantonZurich();
    
    for (PersonImpl p : pop.getPersons().values()){
      Household hh = b.createHousehold(p.getId());
      hh.setIncome(b.createIncome(incomeCalculator.calculateIncome(46300), BasicIncome.IncomePeriod.year));
      hh.getMembers().put(p.getId(), p);
      p.setHousehold(hh);
      hhs.getHouseholds().put(p.getId(), hh);
    }
    
    HouseholdsWriterV1 hhwriter = new HouseholdsWriterV1(hhs);
    hhwriter.writeFile(outdir + "households.xml");
    System.out.println("Households written!");
    
    IncomeStats stats = new IncomeStats(hhs);
    
    
	}

	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
//    createHHForTestCase();
		createHHForTestScenario();
	}

}
