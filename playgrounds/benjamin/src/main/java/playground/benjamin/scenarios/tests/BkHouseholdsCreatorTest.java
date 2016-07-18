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
package playground.benjamin.scenarios.tests;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsFactory;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income;

import playground.benjamin.BkPaths;
import playground.benjamin.scenarios.zurich.IncomeCalculatorKantonZurich;
import playground.benjamin.utils.IncomeStats;


/**
 * @author dgrether
 *
 */
public class BkHouseholdsCreatorTest {



	public static void createHHForTestCase() throws FileNotFoundException, IOException {
    MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
    Id<Household> hhId1 = Id.create("1", Household.class);
    Id<Household> hhId2 = Id.create("2", Household.class);
    Id<Person> id1 = Id.create("1", Person.class);
    Id<Person> id2 = Id.create("2", Person.class);
    Households hhs = sc.getHouseholds();
    HouseholdsFactory b = hhs.getFactory();

    Household hh = b.createHousehold(hhId1);
    hh.setIncome(b.createIncome(120000, Income.IncomePeriod.year));
    hh.getMemberIds().add(id1);
    hhs.getHouseholds().put(hhId1, hh);

    hh = b.createHousehold(hhId2);
    hh.setIncome(b.createIncome(40000, Income.IncomePeriod.year));
    hh.getMemberIds().add(id2);
    hhs.getHouseholds().put(hhId2, hh);

    HouseholdsWriterV10 hhwriter = new HouseholdsWriterV10(hhs);
    hhwriter.writeFile(BkPaths.SHAREDSVN + "test/input/playground/benjamin/BKickScoringTest/households.xml");
    System.out.println("Households written!");
	}

	public static void createHHForTestScenario() throws FileNotFoundException, IOException {
		String outdir = BkPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/";
		String plansFile = outdir + "plans.xml";
		String networkFile =outdir + "../oneRouteNoModeTest/network.xml";

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netreader = new MatsimNetworkReader(sc.getNetwork());
		netreader.readFile(networkFile);

    Population pop = sc.getPopulation();
    PopulationReader popReader = new PopulationReader(sc);
    popReader.readFile(plansFile);

    sc.getConfig().scenario().setUseHouseholds(true);
    Households hhs = sc.getHouseholds();
    HouseholdsFactory b = hhs.getFactory();

    IncomeCalculatorKantonZurich incomeCalculator = new IncomeCalculatorKantonZurich();

    for (Person p : pop.getPersons().values()){
    	Household hh = b.createHousehold(Id.create(p.getId().toString(), Household.class));
      hh.setIncome(b.createIncome(incomeCalculator.calculateIncome(46300), Income.IncomePeriod.year));
      hh.getMemberIds().add(p.getId());
      hhs.getHouseholds().put(hh.getId(), hh);
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
