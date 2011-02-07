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
import org.matsim.planomat.utils.SelectedPlansScoreTest;
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

	@Override
	protected void tearDown() throws Exception {
		this.config = null;
		super.tearDown();
	}

	public void testTAMRouter() {

		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateAll6AM/expected_plans.xml.gz");
		
		this.config.strategy().addParam("ModuleProbability_1", "0.8");
		this.config.strategy().addParam("Module_2", "TimeAllocationMutator");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), 102.4771507401222);
		expectedPlanScores.put(new IdImpl(1033), 96.53444472767572);
		expectedPlanScores.put(new IdImpl(1072), 107.02180374798186);
		expectedPlanScores.put(new IdImpl(947), 102.3916865018528);

		this.runATest(expectedPlanScores);
		
	}
	
	public void testPlanomatOnly() {

		this.config.planomat().setJgapMaxGenerations(10);
		
		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarOnly/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.9");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), 94.6779217996309);
		expectedPlanScores.put(new IdImpl(1033), -22.113775783571782);
		expectedPlanScores.put(new IdImpl(1072), 104.22794325628031);
		expectedPlanScores.put(new IdImpl(947), 98.97406416124487);

		this.runATest(expectedPlanScores);
	}
	
	public void testPlanomatRouter() {
		
		this.config.planomat().setJgapMaxGenerations(10);

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
		this.config.planomat().setJgapMaxGenerations(10);
		
		this.config.plansCalcRoute().setPtSpeedFactor(1.5);
		
		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarPt/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.9");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.82511071811835);
		expectedPlanScores.put(new IdImpl(1072), 105.00036493948923);
		expectedPlanScores.put(new IdImpl(947), 103.22669814320673);

		this.runATest(expectedPlanScores);
	}
	
	public void testPlanomatRouterSamplingOnly() {

		this.config.planomat().setJgapMaxGenerations(0);
		
		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarOnly/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.8");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");

		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 94.57548202998221);
		expectedPlanScores.put(new IdImpl(1072), 90.78104651406623);
		expectedPlanScores.put(new IdImpl(947), 104.09682058923968);

		this.runATest(expectedPlanScores);

	}
	
	public void testPlanomatRouterManyGenerations() {

		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarOnly/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.8");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");

		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 94.37270436510693);
		expectedPlanScores.put(new IdImpl(1072), 94.90568335701548);
		expectedPlanScores.put(new IdImpl(947), 106.10466621258328);
		this.runATest(expectedPlanScores);

	}
	
	private void configurePtCostVariation(double ptSpeedFactor, double travelingPt) {

		this.config.planomat().setPossibleModes("car,pt");
		this.config.planomat().setJgapMaxGenerations(10);
		
		this.config.plansCalcRoute().setPtSpeedFactor(ptSpeedFactor);
		this.config.planCalcScore().setTravelingPt_utils_hr(travelingPt);
		
		this.config.plans().setInputFile("test/input/playground/meisterk/phd/GenerateEquilPopulationsTest/testGenerateRandomCarPt/expected_plans.xml.gz");

		this.config.strategy().addParam("ModuleProbability_1", "0.8");
		this.config.strategy().addParam("Module_2", "Planomat");
		this.config.strategy().addParam("ModuleProbability_2", "0.1");
		this.config.strategy().addParam("Module_3", "ReRoute");
		this.config.strategy().addParam("ModuleProbability_3", "0.1");

	}
	
	public void testPtSpeedFactor10() {

		this.configurePtCostVariation(1.0, this.config.planCalcScore().getTravelingPt_utils_hr());
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.83535858516643);
		expectedPlanScores.put(new IdImpl(1072), 79.86540409434511);
		expectedPlanScores.put(new IdImpl(947), 105.42101834952176);
		this.runATest(expectedPlanScores);

	}
	
	public void testPtSpeedFactor11() {

		this.configurePtCostVariation(1.1, this.config.planCalcScore().getTravelingPt_utils_hr());
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 104.19682933444355);
		expectedPlanScores.put(new IdImpl(1072), 105.0695479783633);
		expectedPlanScores.put(new IdImpl(947), 106.87181362970294);
		this.runATest(expectedPlanScores);

	}
	
	public void testPtSpeedFactor12() {

		this.configurePtCostVariation(1.2, this.config.planCalcScore().getTravelingPt_utils_hr());
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 103.24504429242117);
		expectedPlanScores.put(new IdImpl(1072), 105.04246369984388);
		expectedPlanScores.put(new IdImpl(947), 92.61820451723383);
		this.runATest(expectedPlanScores);

	}
	
	public void testPtSpeedFactor13() {

		this.configurePtCostVariation(1.3, this.config.planCalcScore().getTravelingPt_utils_hr());
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 87.5842021311285);
		expectedPlanScores.put(new IdImpl(1072), 104.26443813631997);
		expectedPlanScores.put(new IdImpl(947), 104.63519690371794);
		this.runATest(expectedPlanScores);

	}
	
	public void testPtSpeedFactor14() {

		this.configurePtCostVariation(1.4, this.config.planCalcScore().getTravelingPt_utils_hr());
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 83.03024603384561);
		expectedPlanScores.put(new IdImpl(1072), 105.0423906792287);
		expectedPlanScores.put(new IdImpl(947), 107.69313072553115);
		this.runATest(expectedPlanScores);

	}
	
	public void testPtSpeedFactor15() {

		this.configurePtCostVariation(1.5, this.config.planCalcScore().getTravelingPt_utils_hr());
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.82179507900221);
		expectedPlanScores.put(new IdImpl(1072), 101.55431613167895);
		expectedPlanScores.put(new IdImpl(947), 105.34074659066195);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPt0() {
		
		this.configurePtCostVariation(1.5, 0.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 111.35534110589401);
		expectedPlanScores.put(new IdImpl(1072), 108.37254712986397);
		expectedPlanScores.put(new IdImpl(947), 108.68190268803976);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPtMinus1() {
		
		this.configurePtCostVariation(1.5, -1.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 109.79505713065271);
		expectedPlanScores.put(new IdImpl(1072), 106.76339898557961);
		expectedPlanScores.put(new IdImpl(947), 107.151875323949);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPtMinus2() {
		
		this.configurePtCostVariation(1.5, -2.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 98.92661479958059);
		expectedPlanScores.put(new IdImpl(1072), 105.66601296327353);
		expectedPlanScores.put(new IdImpl(947), 105.85740228220659);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPtMinus3() {
		
		this.configurePtCostVariation(1.5, -3.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.6342050994094);
		expectedPlanScores.put(new IdImpl(1072), 104.26367086471274);
		expectedPlanScores.put(new IdImpl(947), 105.4358076390397);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPtMinus4() {
		
		this.configurePtCostVariation(1.5, -4.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.43684583314439);
		expectedPlanScores.put(new IdImpl(1072), 103.92248197107142);
		expectedPlanScores.put(new IdImpl(947), 105.38574659066195);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPtMinus5() {
		
		this.configurePtCostVariation(1.5, -5.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 94.69320800929532);
		expectedPlanScores.put(new IdImpl(1072), 101.4853713320467);
		expectedPlanScores.put(new IdImpl(947), 101.75631976839345);
		this.runATest(expectedPlanScores);

	}
	
	public void testTravelingPtMinus6() {
		
		this.configurePtCostVariation(1.5, -6.0);
		
		HashMap<Id, Double> expectedPlanScores = new HashMap<Id, Double>();
		expectedPlanScores.put(new IdImpl(1012), -203.22393647708262);
		expectedPlanScores.put(new IdImpl(1033), 106.82179507900221);
		expectedPlanScores.put(new IdImpl(1072), 101.55431613167895);
		expectedPlanScores.put(new IdImpl(947), 105.34074659066195);
		this.runATest(expectedPlanScores);

	}
	
	public void xtestUpgradeRouteToHighway() {
		fail("Not yet implemented.");
	}
	
	public void xtestIntroduceTollOnHighway() {
		fail("Not yet implemented.");
	}
	
	public void xtestIntroduceMobilityTools() {
		fail("Not yet implemented.");
	}
	
	private void runATest(HashMap<Id,Double> expectedPlanScores) {
		
		Controler testee = new PhDControler(this.config);
		testee.getConfig().controler().setOutputDirectory(this.getOutputDirectory());

		testee.addControlerListener(new SelectedPlansScoreTest(expectedPlanScores, PhDControlerTest.TEST_ITERATION));
		
		testee.setCreateGraphs(false);
		testee.setWriteEventsInterval(0);
		testee.run();
		
	}

}
