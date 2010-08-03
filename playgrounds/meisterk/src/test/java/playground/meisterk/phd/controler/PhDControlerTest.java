/* *********************************************************************** *
 * project: org.matsim.*
 * RunEquilRuns.java
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

package playground.meisterk.phd.controler;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.testcases.MatsimTestCase;

/**
 * Use scores of arbitrarily chosen agents after 10th iteration to verify correct working of the system.
 * 
 * @author meisterk
 *
 */
public class PhDControlerTest extends MatsimTestCase {

	public static final int TEST_ITERATION = 10;
	private static final Logger log = Logger.getLogger(PhDControlerTest.class);

	private Config config;
	
	@Override
	protected void setUp() throws Exception {

		super.setUp();
		
		String configFilename = this.getClassInputDirectory() + "config.xml";
		
		this.config = new Config();
		this.config.addCoreModules();
		new MatsimConfigReader(this.config).readFile(configFilename);
		
	}

	public void testTAMRouter() {

		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateAll6AM/expected_plans.xml.gz");
		
		this.config.strategy().addParam("ModuleProbability_1", "0.8");
		this.config.strategy().addParam("Module_2", "TimeAllocationMutator");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), 102.3995485156362);
		expectedPlanScores.put(new IdImpl(1033), 92.3175207822995);
		expectedPlanScores.put(new IdImpl(1072), 107.02180374798185);
		expectedPlanScores.put(new IdImpl(947), 102.39001983518614);

		this.runATest(expectedPlanScores);
		
	}
	
	public void testPlanomatOnly() {

		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarOnly/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.9");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), 99.02855025439848);
		expectedPlanScores.put(new IdImpl(1033), -22.11377578357179);
		expectedPlanScores.put(new IdImpl(1072), 104.98460992294699);
		expectedPlanScores.put(new IdImpl(947), 103.71560343846069);

		this.runATest(expectedPlanScores);
	}
	
	public void testPlanomatRouter() {
		
		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarOnly/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.8");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 99.5184077860448);
		expectedPlanScores.put(new IdImpl(1072), 103.6855356436988);
		expectedPlanScores.put(new IdImpl(947), 107.41882610816248);

		this.runATest(expectedPlanScores);
		
	}
	
	public void testPlanomatRouterCarPt() {

		this.config.planomat().setPossibleModes("car,pt");
		
		this.config.plansCalcRoute().setPtSpeedFactor(1.5);
		
		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarPt/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.9");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.53017916647772);
		expectedPlanScores.put(new IdImpl(1072), null);
		expectedPlanScores.put(new IdImpl(947), 103.45836480987339);

		this.runATest(expectedPlanScores);
	}
	
	public void xtestImprovePtTravelTime() {
		fail("Not yet implemented.");
	}
	
	public void xtestUpgradeRouteToHighway() {
		fail("Not yet implemented.");
	}
	
	public void xtestIntroduceTollOnHighway() {
		fail("Not yet implemented.");
	}
	
	public void xtestRealWorldFeatures() {
		fail("Not yet implemented.");
	}
	
	public void xtestVaryLeisureOpeningTimes() {
		fail("Not yet implemented.");
	}
	
	public void xtestIntroduceMobilityTools() {
		fail("Not yet implemented.");
	}
	
	private void runATest(HashMap<Id,Double> expectedPlanScores) {
		
		Controler testee = new PhDControler(this.config);
		testee.getConfig().controler().setOutputDirectory(this.getOutputDirectory());

		testee.addControlerListener(new ScoreChecker(expectedPlanScores));
		
		testee.setCreateGraphs(false);
		testee.setWriteEventsInterval(0);
		testee.run();
		
	}
	
	private class ScoreChecker implements BeforeMobsimListener {

		private final HashMap<Id,Double> expectedPlanScores;
		
		public ScoreChecker(HashMap<Id, Double> expectedPlanScores) {
			super();
			this.expectedPlanScores = expectedPlanScores;
		}

		public void notifyBeforeMobsim(BeforeMobsimEvent event) {

			int iterationNumber = event.getIteration();
			if (iterationNumber == PhDControlerTest.TEST_ITERATION) {
				
				for (Id personId : this.expectedPlanScores.keySet()) {
					Double expectedScore = this.expectedPlanScores.get(personId);
					Double actualScore = event.getControler().getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
					if (expectedScore == null) {
						assertNull(actualScore);
					} else {
						assertEquals(
								"Unexpected score for selected plan of agent with id " + personId.toString() + ".", 
								expectedScore, 
								actualScore, 
								MatsimTestCase.EPSILON);
					}
				}
				
			}
			
		}

	}


}
