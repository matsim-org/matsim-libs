/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtTester.java
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

package playground.marcel.kti.test;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.core.utils.misc.Counter;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;

public class KtiPtTester {

	private Config config;
	private ScenarioImpl data;

	public KtiPtTester(final String[] args) {
		
		ScenarioLoader loader = new ScenarioLoader(args[0]);
		loader.loadNetwork();
		this.data = loader.getScenario();
		this.config = this.data.getConfig();
	}

	public void run() {
		Gbl.startMeasurement();
		Gbl.printRoundTime();
		PopulationImpl population = this.data.getPopulation();
		Gbl.printRoundTime();
		FreespeedTravelTimeCost fttc = new FreespeedTravelTimeCost();
		Gbl.printRoundTime();
		
		KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
		ktiConfigGroup.setUsePlansCalcRouteKti(true);
		ktiConfigGroup.setPtHaltestellenFilename("/Volumes/Data/ETH/cvs/ivt/studies/switzerland/externals/ptNationalModel/Haltestellen.txt");
		ktiConfigGroup.setPtTraveltimeMatrixFilename("/Volumes/Data/ETH/cvs/ivt/studies/switzerland/externals/ptNationalModel/2005_OEV_Befoerderungszeit.mtx");
		ktiConfigGroup.setWorldInputFilename(this.config.world().getInputFile());
		
		PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo();
		plansCalcRouteKtiInfo.prepare(ktiConfigGroup, (NetworkLayer) this.data.getNetwork());

		Gbl.printRoundTime();

		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = new AStarLandmarksFactory(
				(NetworkLayer) this.data.getNetwork(), 
				new FreespeedTravelTimeCost(this.config.charyparNagelScoring()));
		
		PlansCalcRouteKti calcPtLeg = new PlansCalcRouteKti(
				this.config.plansCalcRoute(), 
				(NetworkLayer) this.data.getNetwork(), 
				fttc, 
				fttc, 
				leastCostPathCalculatorFactory, 
				plansCalcRouteKtiInfo);

		Gbl.printRoundTime();

		Counter counter = new Counter("handle person #");
		for (PersonImpl person : population.getPersons().values()) {
			counter.incCounter();
			calcPtLeg.run(person.getSelectedPlan());
		}
		counter.printCounter();
		Gbl.printRoundTime();
		new PopulationWriter(population, this.config.plans().getOutputFile(), this.config.plans().getOutputVersion()).write();
		Gbl.printRoundTime();
	}

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiPtTester config-file");
			System.out.println();
		} else {
			final KtiPtTester ktiPtTester = new KtiPtTester(args);
			ktiPtTester.run();
		}
		System.exit(0);
	}

}
