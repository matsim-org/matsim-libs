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
package playground.julia.incomeScoring;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.emissions.ColdEmissionHandler;
import playground.benjamin.emissions.EmissionModule;
import playground.benjamin.emissions.WarmEmissionHandler;
import playground.benjamin.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import playground.benjamin.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import playground.benjamin.emissions.types.HbefaVehicleCategory;
import playground.benjamin.internalization.EmissionScoringFunctionFactory;
import playground.benjamin.internalization.EmissionTravelCostCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
import playground.julia.incomeScoring.RoadUsedHandler;

/**
 * @author benjamin
 * junit testclass for different routing options
 * 
 * This class has three methods which test routings for different generalized costs on a simple network.
 * The network consists of a main route (links 1, 2, 3, 4, 5, 6, 7) with three variants (links 8+9, 10+11, 12+13) witch differ in length and free flow speed.
 * 
 * testTimeRouting uses only the time as generalized costs and should use the 8+9 variant
 * testDistanceRouting uses only the distance as generalized costs and should use the 10+11 variant
 * testEmissionRouting  uses emissions, time and distance as generalized costs and should use the 12+13 variant
 *
 */
public class EmissionRoutingTest extends MatsimTestCase{
	
	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");
	
	//private final String outputDirectory = "../../detailedEval/internalization/test/";
	//private final String outputDirectory = this.getOutputDirectory()+"../../detailedEval/internalization/test/";
	

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private Vehicles emissionVehicles;
	private EmissionModule emissionModule;
	private String outputDirectory;

	public void testEmissionRouting() {
		
		this.outputDirectory = this.getOutputDirectory()+"../../detailedEval/internalization/test/";
		this.config = new Config();
		this.config.addCoreModules();
		this.config.controler().setOutputDirectory(this.outputDirectory);
		this.scenario = ScenarioUtils.createScenario(this.config);
		
		createNetwork();
		createActiveAgents();
		createVehicles();
		
		this.controler = new Controler(this.scenario);
		specifyControler();
		
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setTraveling_utils_hr(-6.0);
		pcs.setMarginalUtilityOfMoney(0.6);
		pcs.setMonetaryDistanceCostRateCar(-0.0001);
		
		emissionModule = new EmissionModule(scenario, this.emissionVehicles);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		installTravelCostCalculatorFactory();
		final RoadUsedHandler handler = new RoadUsedHandler(11) ;
						
		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				//link 9 time, link 11 distance, link 13 emissions and time and distance
				event.getControler().getEvents().addHandler(handler);
			}
		};
		
		this.controler.addControlerListener(startupListener);
		this.controler.run();
		
		assertTrue("Person was expected to be routed through link 11, but was " + handler.getLink(), RoadUsedHandler.getWasRoadSelected()==true);
	}



	public void testDistanceRouting() {
		
		this.outputDirectory = this.getOutputDirectory()+"../../detailedEval/internalization/test/";
		this.config = new Config();
		this.config.addCoreModules();
		this.config.controler().setOutputDirectory(this.outputDirectory);
		this.scenario = ScenarioUtils.createScenario(this.config);
		
		createNetwork();
		createActiveAgents();
		createVehicles();
		
		this.controler = new Controler(this.scenario);
		specifyControler();
		
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setTraveling_utils_hr(0.0);
		pcs.setMarginalUtilityOfMoney(1.0);
		pcs.setMonetaryDistanceCostRateCar(-0.0001);
		
		final RoadUsedHandler handler = new RoadUsedHandler(13) ;
						
		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				//link 9 time, link 11 distance, link 13 emissions and time and distance

				event.getControler().getEvents().addHandler(handler);
			}
		};
		
		this.controler.addControlerListener(startupListener);
		this.controler.run();
		
		assertTrue("Person was expected to be routed through link 13, but was " + handler.getLink(), RoadUsedHandler.getWasRoadSelected()==true);
		
	}



	public void testTimeRouting() {
		
		this.outputDirectory = this.getOutputDirectory()+"../../detailedEval/internalization/test/";
		this.config = new Config();
		this.config.addCoreModules();
		this.config.controler().setOutputDirectory(this.outputDirectory);

		this.scenario = ScenarioUtils.createScenario(this.config);
		
		createNetwork();
		createActiveAgents();
		createVehicles();
		
		this.controler = new Controler(this.scenario);
		specifyControler();
		
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		pcs.setTraveling_utils_hr(-6000.0);
		pcs.setMarginalUtilityOfMoney(1.0);
		pcs.setMonetaryDistanceCostRateCar(-0.0001);
		
		final RoadUsedHandler handler = new RoadUsedHandler(9) ;
		
		StartupListener startupListener = new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				//link 9 time, link 11 distance, link 13 emissions and time and distance

				event.getControler().getEvents().addHandler(handler);
			}
		};
		
		this.controler.addControlerListener(startupListener);
		this.controler.setDumpDataAtEnd(true);
		this.controler.run();
		assertTrue("Person was expected to be routed through link 9, but was " + handler.getLink(), RoadUsedHandler.getWasRoadSelected()==true);
	}
	
	

	private void installTravelCostCalculatorFactory() {
		EmissionTravelCostCalculatorFactory emissionTccf = new EmissionTravelCostCalculatorFactory(emissionModule);
		controler.setTravelCostCalculatorFactory(emissionTccf);
	}
	
	
	
	private void specifyControler() {
	// controler settings	
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		
	// controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputDirectory);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(10);
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
		 String emissionInputPath = (this.getClassInputDirectory()+"detailedEval/emissions/hbefaForMatsim/");
		 String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
		 String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
		 String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
		 boolean isUsingDetailedEmissionCalculation = false;
		 
		vcg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		
		vcg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		vcg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
		
	// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		vcg.setWritingOutputEvents(false) ;
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
        Node node10 = network.createAndAddNode(scenario.createId("10"), scenario.createCoord(-7500.0, -3000.0));
        
        network.createAndAddLink(scenario.createId("1"), node1, node2, 1000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("2"), node2, node3, 2000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("4"), node4, node5, 2000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("5"), node5, node6, 1000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("6"), node6, node7, 1000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("7"), node7, node1, 1000, 27.78, 3600, 1, null, "22");
        
        //three similar links
        network.createAndAddLink(scenario.createId("8"), node3, node8, 5000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("10"), node3, node9, 5000, 27.78, 3600, 1, null, "22");
        network.createAndAddLink(scenario.createId("12"), node3, node10, 5000, 27.78, 3600, 1, null, "22");
        
        //time
        network.createAndAddLink(scenario.createId("9"), node8, node4, 5000, 27.78, 3600, 1, null, "22");
        //distance
        network.createAndAddLink(scenario.createId("11"), node9, node4, 2500, 12.63, 3600, 1, null, "22");
        //time+emissions
        network.createAndAddLink(scenario.createId("13"), node10, node4, 3750, 20.83, 3600, 1, null, "22");        
	}
}