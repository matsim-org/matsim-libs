/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomaPerformanceTest.java
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

package playground.meisterk.org.matsim.planomat;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.Planomat;
import org.matsim.planomat.PlanomatTest;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

public class PlanomatPerformanceTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(PlanomatTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		super.loadConfig(this.getInputDirectory() + "config.xml");

	}

	public void testPerformanceTest() {

		NetworkLayer network = null;
		Population population = null;

		log.info("Reading network xml file...");
		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");
		Gbl.printMemoryUsage();

		log.info("Reading plans xml file...");
		population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");
		Gbl.printMemoryUsage();

		log.info("Initializing TravelTimeCalculator...");
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(network, 900);
		log.info("Initializing TravelTimeCalculator...done.");
		log.info("Initializing TravelCost...");
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		log.info("Initializing TravelCost...done.");
		log.info("Initializing DepartureDelayAverageCalculator...");
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(network, 900);
		log.info("Initializing DepartureDelayAverageCalculator...done.");

		log.info("Reading events...");
		Events events = new Events();
		events.addHandler(tTravelEstimator);
		events.addHandler(depDelayCalc);
		new MatsimEventsReader(events).readFile(Gbl.getConfig().events().getInputFile());
		log.info("Reading events...done.");

		LegTravelTimeEstimator ltte = new CetinCompatibleLegTravelTimeEstimator(tTravelEstimator, travelCostEstimator, depDelayCalc, network);
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());

		Planomat testee = new Planomat(ltte, scoringFunctionFactory);
		Gbl.printMemoryUsage();

		log.info("Running evolution on 10% of the plans...");
		int personCounter = 0;
		int nextCounter = 1;
		Random rng = MatsimRandom.getLocalInstance();
		for (Person person : population) {
			if (rng.nextDouble() < 0.1) {
				Plan plan = person.getRandomPlan();
				testee.run(plan);
				personCounter++;
				if (personCounter % nextCounter == 0) {
					log.info("handled " + personCounter + " persons.");
					nextCounter *= 2;
					Gbl.printMemoryUsage();
				}
			}
		}
		log.info("Population finished. Handled " + personCounter + " persons.");
		log.info("Running evolution on 10% of the plans...done.");

		log.info("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(population, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		log.info("Writing plans file...DONE.");

	}

}
