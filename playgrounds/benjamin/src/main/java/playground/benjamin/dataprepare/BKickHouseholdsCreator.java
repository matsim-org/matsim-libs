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
package playground.benjamin.dataprepare;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;

import playground.benjamin.BkPaths;


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
    HouseholdsFactory b = hhs.getFactory();

    Household hh = b.createHousehold(id1);
    hh.setIncome(b.createIncome(120000, Income.IncomePeriod.year));
    hh.getMemberIds().add(id1);
    hhs.getHouseholds().put(id1, hh);

    hh = b.createHousehold(id2);
    hh.setIncome(b.createIncome(40000, Income.IncomePeriod.year));
    hh.getMemberIds().add(id2);
    hhs.getHouseholds().put(id2, hh);

    HouseholdsWriterV10 hhwriter = new HouseholdsWriterV10(hhs);
    hhwriter.writeFile(BkPaths.SHAREDSVN + "test/input/playground/benjamin/BKickScoringTest/households.xml");
    System.out.println("Households written!");
	}

	public static void createHHForTestScenario() throws FileNotFoundException, IOException {
		String outdir = BkPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/";
		String plansFile = outdir + "plans.xml";
		String networkFile =outdir + "../oneRouteNoModeTest/network.xml";

		ScenarioImpl sc = new ScenarioImpl();
		MatsimNetworkReader netreader = new MatsimNetworkReader(sc);
		netreader.readFile(networkFile);

    Population pop = sc.getPopulation();
    MatsimPopulationReader popReader = new MatsimPopulationReader(sc);
    popReader.readFile(plansFile);

    sc.getConfig().scenario().setUseHouseholds(true);
    Households hhs = sc.getHouseholds();
    HouseholdsFactory b = hhs.getFactory();

    IncomeCalculatorKantonZurich incomeCalculator = new IncomeCalculatorKantonZurich();

    for (Person p : pop.getPersons().values()){
    	Household hh = b.createHousehold(p.getId());
      hh.setIncome(b.createIncome(incomeCalculator.calculateIncome(46300), Income.IncomePeriod.year));
      hh.getMemberIds().add(p.getId());
      hhs.getHouseholds().put(p.getId(), hh);
    }

    HouseholdsWriterV10 hhwriter = new HouseholdsWriterV10(hhs);
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
