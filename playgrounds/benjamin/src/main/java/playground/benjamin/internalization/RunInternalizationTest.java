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
package playground.benjamin.internalization;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * @author benjamin
 *
 */
public class RunInternalizationTest {
	
	static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
	static String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	static String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	static String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
	static boolean isUsingDetailedEmissionCalculation = false;

	private final String outputDirectory = "../../detailedEval/internalization/test/";

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private Vehicles emissionVehicles;
	private EmissionModule emissionModule;
	private EmissionCostModule emissionCostModule;
	
	private void run() {
		
		this.config = new Config();
		this.config.addCoreModules();
		this.config.controler().setOutputDirectory(this.outputDirectory);

		this.scenario = ScenarioUtils.createScenario(this.config);
		
		createNetwork();
		createActiveAgents();
//		createPassiveAgents();
		createVehicles();
		
		this.controler = new Controler(this.scenario);
		specifyControler();

		emissionModule = new EmissionModule(scenario, this.emissionVehicles);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		emissionCostModule = new EmissionCostModule(1.0, Boolean.parseBoolean("true"));
		
//		installScoringFunctionFactory();
		installTravelCostCalculatorFactory();
		this.controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		this.controler.run();
	}

	private void installTravelCostCalculatorFactory() {
		final EmissionTravelDisutilityCalculatorFactory emissionTdcf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, 
				emissionCostModule, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emissionTdcf);
			}
		});
	}

	private void installScoringFunctionFactory() {
		EmissionScoringFunctionFactory emissionSff = new EmissionScoringFunctionFactory(controler);
		controler.setScoringFunctionFactory(emissionSff);
	}
	
	private void specifyControler() {
	// controler settings	
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);

        // controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputDirectory);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(0);
		ccg.setMobsim("qsim");
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
//		ccg.setRunId("321");
		ccg.setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		
	// qsimConfigGroup
		QSimConfigGroup qcg = config.qsim();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(1);
		qcg.setStorageCapFactor(1);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		
	// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
			
		ActivityParams act1Params = new ActivityParams("home");
		act1Params.setTypicalDuration(16 * 3600);
		pcs.addActivityParams(act1Params);
		
		ActivityParams act2Params = new ActivityParams("work");
		act2Params.setTypicalDuration(8 * 3600);
		pcs.addActivityParams(act2Params);
		
		pcs.setBrainExpBeta(1.0);
		final double traveling = -6.0;
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		pcs.setMarginalUtilityOfMoney(0.6);
		double monetaryDistanceRateCar = -0.0001;
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);

		// strategyConfigGroup
		StrategyConfigGroup scg = controler.getConfig().strategy();
		
		StrategySettings changePlan = new StrategySettings(Id.create("1", StrategySettings.class));
//		changePlan.setModuleName("BestScore");
		changePlan.setStrategyName("ChangeExpBeta");
		changePlan.setWeight(0.7);
		
		StrategySettings reRoute = new StrategySettings(Id.create("2", StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.3);
		
		scg.addStrategySettings(changePlan);
		scg.addStrategySettings(reRoute);

	// define emission tool input files	
        EmissionsConfigGroup ecg = new EmissionsConfigGroup();
        controler.getConfig().addModule(ecg);

        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
		
	// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
		vcg.setWritingOutputEvents(false) ;
	}

	private void createPassiveAgents() {
		// TODO: make code homogeneous by using factories!
		for(int i=0; i<10; i++){
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			
			Activity home = PopulationUtils.createAndAddActivityFromLinkId(plan, (String) "home", Id.create("11", Link.class));
			home.setEndTime(6 * 3600);
			home.setCoord(new Coord(0.0, 0.0));
			
			PopulationUtils.createAndAddLeg( plan, (String) TransportMode.walk );
			
			Activity home2 = PopulationUtils.createAndAddActivityFromLinkId(plan, (String) "home", Id.create("11", Link.class));
			home2.setCoord(new Coord(0.0, 0.0));
			
			scenario.getPopulation().addPerson(person);
		}
	}

	private void createActiveAgents() {
		PopulationFactory pFactory = scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(Id.create("worker", Person.class));
		Plan plan = pFactory.createPlan();
		
		Activity home = pFactory.createActivityFromLinkId("home", Id.create("1", Link.class));
		home.setEndTime(6 * 3600);
		plan.addActivity(home);
		
		Leg leg1 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg1);
		
		Activity work = pFactory.createActivityFromLinkId("work", Id.create("4", Link.class));
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
		Id<VehicleType> vehTypeId = Id.create(HbefaVehicleCategory.PASSENGER_CAR.toString(), VehicleType.class);
		VehicleType vehicleType = this.emissionVehicles.getFactory().createVehicleType(vehTypeId);
		for(Person person : scenario.getPopulation().getPersons().values()){
			Vehicle vehicle = this.emissionVehicles.getFactory().createVehicle(Id.create(person.getId(), Vehicle.class), vehicleType);
			this.emissionVehicles.addVehicle( vehicle);
		}
	}

	private void createNetwork() {
		Network network = (Network) scenario.getNetwork();

		double x7 = -20000.0;
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x7, 0.0));
		double x6 = -17500.0;
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(x6, 0.0));
		double x5 = -15500.0;
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(x5, 0.0));
		double x4 = -2500.0;
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(x4, 0.0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(0.0, 0.0));
		double x3 = -5000.0;
		double y2 = -8660.0;
		Node node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(x3, y2));
		double x2 = -15000.0;
		double y1 = -8660.0;
		Node node7 = NetworkUtils.createAndAddNode(network, Id.create("7", Node.class), new Coord(x2, y1));
		double x1 = -7500.0;
		Node node8 = NetworkUtils.createAndAddNode(network, Id.create("8", Node.class), new Coord(x1, 2500.0));
		double x = -7500.0;
		double y = -2500.0;
		Node node9 = NetworkUtils.createAndAddNode(network, Id.create("9", Node.class), new Coord(x, y));
		final Node fromNode = node1;
		final Node toNode = node2;
        
        NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode1 = node2;
	final Node toNode1 = node3;
        NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 2000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode2 = node4;
	final Node toNode2 = node5;
//      network.createAndAddLink(Id.create("3", Link.class), node3, node4, 75000, 10.42, 3600, 1, null, "22");
        NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode2, toNode2, (double) 2000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode3 = node5;
	final Node toNode3 = node6;
        NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode3, toNode3, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode4 = node6;
	final Node toNode4 = node7;
        NetworkUtils.createAndAddLink(network,Id.create("6", Link.class), fromNode4, toNode4, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode5 = node7;
	final Node toNode5 = node1;
        NetworkUtils.createAndAddLink(network,Id.create("7", Link.class), fromNode5, toNode5, (double) 1000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode6 = node3;
	final Node toNode6 = node8;
        NetworkUtils.createAndAddLink(network,Id.create("8", Link.class), fromNode6, toNode6, (double) 5000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode7 = node3;
	final Node toNode7 = node9;
        NetworkUtils.createAndAddLink(network,Id.create("10", Link.class), fromNode7, toNode7, (double) 5000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode8 = node8;
	final Node toNode8 = node4;

        NetworkUtils.createAndAddLink(network,Id.create("9", Link.class), fromNode8, toNode8, (double) 5000, 27.78, (double) 3600, (double) 1, null, (String) "22");
	final Node fromNode9 = node9;
	final Node toNode9 = node4;
        NetworkUtils.createAndAddLink(network,Id.create("11", Link.class), fromNode9, toNode9, (double) 2500, 13.89, (double) 3600, (double) 1, null, (String) "22");
	}

	public static void main(String[] args) {
		RunInternalizationTest test = new RunInternalizationTest();
		test.run();
	}

}