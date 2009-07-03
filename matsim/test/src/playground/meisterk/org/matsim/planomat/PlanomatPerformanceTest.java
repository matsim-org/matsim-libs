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
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.events.Events;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.planomat.Planomat;
import org.matsim.planomat.PlanomatTest;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.FixedRouteLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatPerformanceTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(PlanomatTest.class);

	public void testPerformanceTest() {
		Config config = super.loadConfig(this.getInputDirectory() + "config.xml");

		NetworkLayer network = null;
		Population population = null;

		log.info("Reading network xml file...");
		network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		log.info("Reading network xml file...done.");
		Gbl.printMemoryUsage();

		log.info("Reading plans xml file...");
		population = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(population, network);
		plansReader.readFile(config.plans().getInputFile());
		log.info("Reading plans xml file...done.");
		Gbl.printMemoryUsage();

		log.info("Initializing TravelTimeCalculator...");
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(network, config.travelTimeCalculator());
		log.info("Initializing TravelTimeCalculator...done.");
		log.info("Initializing TravelCost...");
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		log.info("Initializing TravelCost...done.");
		log.info("Initializing DepartureDelayAverageCalculator...");
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(network, config.travelTimeCalculator().getTraveltimeBinSize());
		log.info("Initializing DepartureDelayAverageCalculator...done.");

		log.info("Reading events...");
		Events events = new Events();
		events.addHandler(tTravelEstimator);
		events.addHandler(depDelayCalc);
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		log.info("Reading events...done.");

		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(network, travelCostEstimator, tTravelEstimator);

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(tTravelEstimator, depDelayCalc);

		FixedRouteLegTravelTimeEstimator ltte = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 
				plansCalcRoute);

		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());

		Planomat testee = new Planomat(ltte, scoringFunctionFactory, config.planomat());
		Gbl.printMemoryUsage();

		log.info("Running evolution on 10% of the plans...");
		int personCounter = 0;
		int nextCounter = 1;
		Random rng = MatsimRandom.getLocalInstance();
		for (PersonImpl person : population.getPersons().values()) {
			if (rng.nextDouble() < 0.1) {
				PlanImpl plan = person.getRandomPlan();
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
