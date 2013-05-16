/* *********************************************************************** *
 * project: org.matsim.*
 * RunInternalizationTest.java
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
package playground.misc;
/*
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionScoringFunctionFactory;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizationRoutingTestHandler;
import playground.vsp.analysis.modules.*;
import playground.vsp.emissions.*;
import playground.vsp.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import playground.vsp.emissions.types.HbefaColdEmissionFactor;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;
//import playground.benjamin.emissions.EmissionModule;
//import playground.benjamin.emissions.types.HbefaVehicleCategory;
/**
 * @author benjamin
 *
 */

//mit Benjamins version vergleichen. hier veraltete konstrukte, 
//TODO nach testColdEmission* migrieren, diese Klasse loeschen

/*
public class InternalizationRoutingTest extends MatsimTestCase{

	static boolean isUsingDetailedEmissionCalculation = false;

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private Vehicles emissionVehicles;
	private EmissionModule emissionModule;
	private EmissionCostModule emissionCostModule;
	private ColdEmissionAnalysisModule coldEmissionAnalysisModule;

	@Test
	public void testCalculateColdEmissionsAndThrowEvent() {
		this.config = new Config();
		this.config.addCoreModules();

		this.scenario = ScenarioUtils.createScenario(this.config);

		createNetwork();
		createActiveAgents();
		createVehicles();

		this.controler = new Controler(this.scenario);
		specifyControler();

		EventsManager dummyHandler = new dummyHandler();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		ColdEmissionAnalysisModuleParameter parameterObject = new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable);
		coldEmissionAnalysisModule = new ColdEmissionAnalysisModule(parameterObject, dummyHandler, 2.0);
		
		//TODO set veh infos
		HbefaVehicleAttributes hva = new HbefaVehicleAttributes();
		
		hva.setHbefaEmConcept("hbefaEmConcept");
		hva.setHbefaSizeClass("hbefaSizeClass");
		hva.setHbefaTechnology("hbefaTechnology");
		//TODO set all this stuff - does not work like this
		String vehicleInformationTuple = "PASSENGER_CAR;2005HotAverage[3.1];pass. car;2005;BAU (D);FC;MW;RUR/MW/100/Freeflow;0%;102.03;49.44";//new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(HbefaVehicleCategory.PASSENGER_CAR, hva);
		vehicleInformationTuple = "PASSENGER_CAR;3;211083;URB/MW-City/80/Satur.;61.5208;0.1568;0;48.63067627;0.3555;149.4417;153.1131;0.0575;0.01609385";
		//coldEmissionAnalysisModule.calculateColdEmissionsAndThrowEvent(coldEmissionEventLinkId, personId, startEngineTime, parkingDuration, accumulatedDistance, vehicleInformation);
		coldEmissionAnalysisModule.calculateColdEmissionsAndThrowEvent(new IdImpl("coldEmissionEventLinkId"), 
				new IdImpl("personId"),
				10.0,
				2.0,
				50.0,
				vehicleInformationTuple) ;
		
		Double expectedValue = 10.0;
		Double actualValue = ((playground.dummyHandler) dummyHandler).getFirstParameter();
		//Assert.assertTrue("something about the events did go wrong", ((playground.dummyHandler) dummyHandler).getFirstParameter(), expectedValue);
		Assert.assertEquals(expectedValue, actualValue);
		
	

		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setTraveling_utils_hr(-6.000);
		pcs.setMarginalUtilityOfMoney(1.0);
		pcs.setMonetaryDistanceCostRateCar(-0.0001);

		installEmissionDisutilityCalculatorFactory();
		
		//link 9 time, link 11 distance, link 13 emissions and time and distance
		int expectedRoad = 13;
		final InternalizationRoutingTestHandler handler = new InternalizationRoutingTestHandler(expectedRoad) ;

		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getEvents().addHandler(handler);
			}
		};

		this.controler.addControlerListener(startupListener);
		this.controler.setDumpDataAtEnd(true);
		this.controler.run();
		assertTrue("Person was expected to be routed through link " + expectedRoad + ", but was " + handler.getActualRoadSelected(), handler.expectedRoadSelected() == true);

	}
	

	@Test
	@Ignore
	public void calculateColdEmissionsTest(){
		this.config = new Config();
		this.config.addCoreModules();

		this.scenario = ScenarioUtils.createScenario(this.config);

		createNetwork();
		createActiveAgents();
		createVehicles();

		this.controler = new Controler(this.scenario);
		specifyControler();

		EventsManager dummyHandler = new dummyHandler();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		ColdEmissionAnalysisModuleParameter parameterObject = new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable);
		coldEmissionAnalysisModule = new ColdEmissionAnalysisModule(parameterObject, dummyHandler, 2.0);
		
		//TODO set veh infos
		HbefaVehicleAttributes hva = new HbefaVehicleAttributes();
		
		hva.setHbefaEmConcept("hbefaEmConcept");
		hva.setHbefaSizeClass("hbefaSizeClass");
		hva.setHbefaTechnology("hbefaTechnology");
		//TODO set all this stuff - does not work like this
		String vehicleInformationTuple = "PASSENGER_CAR;2005HotAverage[3.1];pass. car;2005;BAU (D);FC;MW;RUR/MW/100/Freeflow;0%;102.03;49.44";//new Tuple<HbefaVehicleCategory, HbefaVehicleAttributes>(HbefaVehicleCategory.PASSENGER_CAR, hva);
		vehicleInformationTuple = "PASSENGER_CAR;3;211083;URB/MW-City/80/Satur.;61.5208;0.1568;0;48.63067627;0.3555;149.4417;153.1131;0.0575;0.01609385";
		
		
	}
	
	private void installEmissionDisutilityCalculatorFactory() {
		EmissionTravelDisutilityCalculatorFactory emissiondcf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
		controler.setTravelDisutilityFactory(emissiondcf);
	}

	private void installScoringFunctionFactory() {
		EmissionScoringFunctionFactory emissionSff = new EmissionScoringFunctionFactory(controler);
		controler.setScoringFunctionFactory(emissionSff);
	}

	private void specifyControler() {
		// controler settings	
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);

		// controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setFirstIteration(0);
		//set to one iteration, use "ccg.setLastIteration(9)" for ten iterations
		ccg.setLastIteration(0);
		ccg.setMobsim("qsim");
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
		//		ccg.setRunId("321");
		ccg.setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);

		// qsimConfigGroup
		QSimConfigGroup qcg = new QSimConfigGroup();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(1);
		qcg.setStorageCapFactor(1);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		controler.getConfig().addQSimConfigGroup(qcg);

		// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();

		ActivityParams act1Params = new ActivityParams("home");
		act1Params.setTypicalDuration(16 * 3600);
		pcs.addActivityParams(act1Params);

		ActivityParams act2Params = new ActivityParams("work");
		act2Params.setTypicalDuration(8 * 3600);
		pcs.addActivityParams(act2Params);

		pcs.setBrainExpBeta(1.0);
		pcs.setTraveling_utils_hr(-6.0);
		pcs.setMarginalUtilityOfMoney(0.6);
		pcs.setMonetaryDistanceCostRateCar(-0.0001);

		// strategyConfigGroup
		StrategyConfigGroup scg = controler.getConfig().strategy();

		StrategySettings changePlan = new StrategySettings(new IdImpl("1"));
		//		changePlan.setModuleName("BestScore");
		changePlan.setModuleName("ChangeExpBeta");
		changePlan.setProbability(0.7);

		StrategySettings reRoute = new StrategySettings(new IdImpl("2"));
		reRoute.setModuleName("ReRoute");
		reRoute.setProbability(0.3);

		scg.addStrategySettings(changePlan);
		scg.addStrategySettings(reRoute);

		// define emission tool input files	
		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
		vcg.setEmissionRoadTypeMappingFile(this.getClassInputDirectory() + "roadTypeMapping.txt");

		vcg.setAverageWarmEmissionFactorsFile(this.getClassInputDirectory() + "EFA_HOT_vehcat_2005average.txt");
		vcg.setAverageColdEmissionFactorsFile(this.getClassInputDirectory() + "EFA_ColdStart_vehcat_2005average.txt");

		// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		vcg.setWritingOutputEvents(false) ;
	}

	private void createPassiveAgents() {
		// TODO: make code homogeneous by using factories!
		for(int i=0; i<10; i++){
			PersonImpl person = new PersonImpl (new IdImpl(i));
			PlanImpl plan = person.createAndAddPlan(true);

			ActivityImpl home = plan.createAndAddActivity("home", scenario.createId("11"));
			home.setEndTime(6 * 3600);
			home.setCoord(scenario.createCoord(0.0, 0.0));

			plan.createAndAddLeg(TransportMode.walk);

			ActivityImpl home2 = plan.createAndAddActivity("home", scenario.createId("11"));
			home2.setCoord(scenario.createCoord(0.0, 0.0));

			scenario.getPopulation().addPerson(person);
		}
	}

	private void createActiveAgents() {
		PopulationFactory pFactory = scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(scenario.createId("worker"));
		Plan plan = pFactory.createPlan();

		Activity home = pFactory.createActivityFromLinkId("home", scenario.createId("1"));
		home.setEndTime(6 * 3600);
		plan.addActivity(home);

		Leg leg1 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg1);

		Activity work = pFactory.createActivityFromLinkId("work", scenario.createId("4"));
		work.setEndTime(home.getEndTime() + 600 + 8 * 3600);
		plan.addActivity(work);

		Leg leg2 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg2);

		plan.addActivity(home);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	private void createVehicles() {
		this.emissionVehicles = VehicleUtils.createVehiclesContainer();
		Id vehTypeId = scenario.createId(HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleType vehicleType = this.emissionVehicles.getFactory().createVehicleType(vehTypeId);
		for(Person person : scenario.getPopulation().getPersons().values()){
			Vehicle vehicle = this.emissionVehicles.getFactory().createVehicle(person.getId(), vehicleType);
			this.emissionVehicles.getVehicles().put(vehicle.getId(), vehicle);
		}
	}

	private void createNetwork() {
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Node node1 = network.createAndAddNode(scenario.createId("1"), scenario.createCoord(-20000.0,     0.0));
		Node node2 = network.createAndAddNode(scenario.createId("2"), scenario.createCoord(-17500.0,     0.0));
		Node node3 = network.createAndAddNode(scenario.createId("3"), scenario.createCoord(-15500.0,     0.0));
		Node node4 = network.createAndAddNode(scenario.createId("4"), scenario.createCoord( -2500.0,     0.0));
		Node node5 = network.createAndAddNode(scenario.createId("5"), scenario.createCoord(     0.0,     0.0));
		Node node6 = network.createAndAddNode(scenario.createId("6"), scenario.createCoord( -5000.0, -8660.0));
		Node node7 = network.createAndAddNode(scenario.createId("7"), scenario.createCoord(-15000.0, -8660.0));
		Node node8 = network.createAndAddNode(scenario.createId("8"), scenario.createCoord( -7500.0,  2500.0));
		Node node9 = network.createAndAddNode(scenario.createId("9"), scenario.createCoord( -7500.0, -2500.0));
		Node node10 = network.createAndAddNode(scenario.createId("10"), scenario.createCoord( -7500.0, -5000.0));


		network.createAndAddLink(scenario.createId("1"), node1, node2, 1000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("2"), node2, node3, 2000, 27.78, 3600, 1, null, "22");
		//      network.createAndAddLink(scenario.createId("3"), node3, node4, 75000, 10.42, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("4"), node4, node5, 2000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("5"), node5, node6, 1000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("6"), node6, node7, 1000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("7"), node7, node1, 1000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("8"), node3, node8, 5000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("10"), node3, node9, 5000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("12"), node3, node10, 5000, 27.78, 3600, 1, null, "22");

		//9 time, 11 distance, 13 emissions
		network.createAndAddLink(scenario.createId("9"), node8, node4, 5000, 27.78, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("11"), node9, node4, 2500, 10.00, 3600, 1, null, "22");
		network.createAndAddLink(scenario.createId("13"), node10, node4, 3750, 20.80, 3600, 1, null, "22");
	}
}
*/
