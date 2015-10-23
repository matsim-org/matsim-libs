/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityCostModule;
import playground.benjamin.scenarios.munich.exposure.EmissionResponsibilityTravelDisutilityCalculatorFactory;
import playground.benjamin.scenarios.munich.exposure.GridTools;
import playground.benjamin.scenarios.munich.exposure.InternalizeEmissionResponsibilityControlerListener;
import playground.benjamin.scenarios.munich.exposure.ResponsibilityGridTools;

/**
 * @author benjamin, amit
 *
 */

@RunWith(Parameterized.class)
public class EquilEmissionExposureTest {

	private static final Logger logger = Logger.getLogger(EquilEmissionExposureTest.class);

	private String inputPath = "../../../../repos/shared-svn/projects/detailedEval/emissions/";
	private String outputDir = "../../../../repos/shared-svn/projects/detailedEval/emissions/testScenario/output/";

	private String emissionInputPath = inputPath+"/hbefaForMatsim/";
	private String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	private String emissionVehicleFile = inputPath + "/testScenario/input/emissionVehicles_1pct.xml.gz";

	private String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	private String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";

	private boolean isUsingDetailedEmissionCalculation = true;
	private String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
	private String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";

	private boolean isConsideringCO2Costs ;

	public EquilEmissionExposureTest (boolean isConsideringCO2Costs) {
		this.isConsideringCO2Costs = isConsideringCO2Costs;
		logger.info("Each parameter will be used in all the tests i.e. all tests will be run while inclusing and excluding CO2 costs.");
	}

	@Parameters
	public static List<Object> considerCO2 () {
		Object[] considerCO2 = new Object [] { true, false };
		return Arrays.asList(considerCO2);
	}

	@Test
	public void emissionTollTest () {

		Scenario sc = createConfig();
		createNetwork(sc);
		createActiveAgents(sc);
		createPassiveAgents(sc);

		emissionSettings(sc);

		Controler controler = new Controler(sc);
		String outputDirectory = outputDir + "/emissionToll/" + (isConsideringCO2Costs ? "considerCO2Costs/" : "notConsiderCO2Costs/");
		sc.getConfig().controler().setOutputDirectory(outputDirectory);

		EmissionModule emissionModule = new EmissionModule(sc);
		emissionModule.setEmissionEfficiencyFactor( 1.0 );
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		EmissionCostModule emissionCostModule = new EmissionCostModule( 1.0, isConsideringCO2Costs );
		final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emissionTducf);
			}
		});
		controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		
		MyPersonMoneyEventHandler personMoneyHandler = new MyPersonMoneyEventHandler();
		controler.getEvents().addHandler(personMoneyHandler);

		controler.run();

		//TODO dont know how to catch emission event during simulation.
		EventsManager events = EventsUtils.createEventsManager();
		EmissionEventsReader reader = new EmissionEventsReader(events);
		MyEmissionEventHandler emissEventHandler = new MyEmissionEventHandler();
		events.addHandler(emissEventHandler);
		int lastIt = sc.getConfig().controler().getLastIteration();
		reader.parse(outputDirectory + "/ITERS/it."+lastIt+"/"+lastIt+".emission.events.xml.gz");
		
		// first check for cold emission, which are generated only on departure link.
		for (ColdEmissionEvent e : emissEventHandler.coldEvents ) {
			if( ! ( e.getLinkId().equals(Id.createLinkId(12)) || e.getLinkId().equals(Id.createLinkId(45)) ) ) {
				throw new RuntimeException("Cold emission event can occur only on departure link.");	
			}
		}
		
		// first coldEmission event is on departure link, thus compare money event and coldEmissionEvent
		
		double firstMoneyEventToll = personMoneyHandler.events.get(0).getAmount();

		Map<ColdPollutant, Double> coldEmiss = emissEventHandler.coldEvents.get(0).getColdEmissions();
		double totalColdEmissAmount = 0.;
		/*
		 * departure time is 21600, so at this time. distance = 1.0km, parking duration = 12h, vehType="PASSENGER_CAR;petrol (4S);&gt;=2L;PC-P-Euro-0"
		 * Thus, coldEmission levels => FC 22.12, HC 5.41, CO 99.97, NO2 -0.00122764240950346, NOx -0-03, PM 0, NMHC 5.12
		 * Thus coldEmissionCost = 0 * 384500. / (1000. * 1000.) + 45.12 * 1700. / (1000. * 1000.) + -0.03 * 9600. / (1000. * 1000.) = 0.008416 
		 */
		
		for (ColdPollutant cp :coldEmiss.keySet() ){
			totalColdEmissAmount += EmissionCostFactors.getCostFactor(cp.toString()) * coldEmiss.get(cp);
		}

		Assert.assertEquals("Cold emission toll from emission event and emission cost factors does not match from manual calculation.",totalColdEmissAmount, 0.008416, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Cold emission toll from emission event and emission cost factors does not match from money event.",firstMoneyEventToll, - totalColdEmissAmount, MatsimTestUtils.EPSILON);
		
		/*
		 * There are two routes --> 12-23-38-84-45 and 12-23-39-94-45. 12 is departure link --> no warmEmissionevent. 
		 * Thus, in both routes, 2nd warmEmission event is on the link with length 5000m. On this link, only free flow warm emission is thrown.
		 * warmEmission --> linkLength = 5km, FC 77.12, HC 0.08, CO 1.0700000000000001, NO2 0.01, NOX 0.36, CO2(total) 241.78, PM 0.00503000011667609, NMHC 0.07, SO2 0.00123396399430931
		 * Thus if co2Costs is not considered --> warmEmissCost = 0.36 * 5 * 9600. / (1000. * 1000.) +  0.07 * 5 * 1700. / (1000. * 1000.) + 0.00123396399430931* 5 * 11000. / (1000. * 1000.)
		 *  +   0.00503000011667609 * 5 * 384500. / (1000. * 1000.) = 0.027613043
		 *  if co2costs are considerd, then warmEmissCost = 0.01787566 +  241.78 * 5 * 70. / (1000. * 1000.) = 0.112236043
		 */
		Map<WarmPollutant, Double> warmEmiss = emissEventHandler.warmEvents.get(2).getWarmEmissions();
		double warmEmissTime = emissEventHandler.warmEvents.get(2).getTime();
		double totalWarmEmissAmount = 0.;
		for ( WarmPollutant wp : warmEmiss.keySet() ) {
			if( wp.toString().equalsIgnoreCase(WarmPollutant.CO2_TOTAL.toString()) && !isConsideringCO2Costs ) {
				//nothing to do 
			} else {
				totalWarmEmissAmount += EmissionCostFactors.getCostFactor(wp.toString()) * warmEmiss.get(wp);	
			}
		}
		double tollFromMoneyEvent = 0.; 
		for (PersonMoneyEvent e : personMoneyHandler.events) {
			// Money event at the time of 2nd warm emission event.
			if ( e.getTime() == warmEmissTime ) tollFromMoneyEvent = e.getAmount();
		}
		
		DecimalFormat df = new DecimalFormat("#.#####");
		if ( isConsideringCO2Costs ) {
			Assert.assertEquals( "Warm emission toll from emission event and emission cost factors does not match from manual calculation.", df.format( 0.112236043 ), df.format( totalWarmEmissAmount ) );
			Assert.assertEquals("Warm emission toll from emission event and emission cost factors does not match from money event.",tollFromMoneyEvent, - totalWarmEmissAmount, MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Warm emission toll from emission event and emission cost factors does not match from manual calculation.", df.format( 0.027613043 ), df.format( totalWarmEmissAmount ) );
			Assert.assertEquals("Warm emission toll from emission event and emission cost factors does not match from money event.",tollFromMoneyEvent, - totalWarmEmissAmount, MatsimTestUtils.EPSILON);
		}
	}

	@Test
	public void exposureTollTest () {

		Scenario sc = createConfig();
		createNetwork(sc);
		createActiveAgents(sc);
		createPassiveAgents(sc);

		emissionSettings(sc);

		Controler controler = new Controler(sc);
		sc.getConfig().controler().setOutputDirectory(outputDir + "/exposureToll/" + (isConsideringCO2Costs ? "considerCO2Costs/" : "notConsiderCO2Costs/"));

		EmissionModule emissionModule = new EmissionModule(sc);
		emissionModule.setEmissionEfficiencyFactor( 1.0 );
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		Double xMin = 0.0;
		Double xMax = 20000.0;
		Double yMin = 0.0;
		Double yMax = 12500.0;

		Integer noOfXCells = 32;
		Integer noOfYCells = 20;

		Integer noOfTimeBins =1;
		Double timeBinSize = sc.getConfig().qsim().getEndTime() / noOfTimeBins;

		GridTools gt = new GridTools(sc.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		Map<Id<Link>, Integer> links2xCells = gt.mapLinks2Xcells(noOfXCells);
		Map<Id<Link>, Integer> links2yCells = gt.mapLinks2Ycells(noOfYCells);

		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule( 1.0, isConsideringCO2Costs, rgt, links2xCells, links2yCells);
		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emfac);
			}
		});

		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, links2xCells, links2yCells));
		
		MyPersonMoneyEventHandler personMoneyHandler = new MyPersonMoneyEventHandler();
		controler.getEvents().addHandler(personMoneyHandler);
		
		controler.run();

		// checks
		Person activeAgent = sc.getPopulation().getPersons().get(Id.create("567417.1#12424", Person.class));
		Plan selectedPlan = activeAgent.getSelectedPlan();

		// check with the first leg
		LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) ( (Leg) selectedPlan.getPlanElements().get(1) ).getRoute() ;
		// Agent should take longer route to avoid exposure toll
		Assert.assertTrue("Wrong route is selected. Agent should have used route with link 38 (longer) instead.", route.getLinkIds().contains(Id.create("38", Link.class)));

	}

	private void emissionSettings(Scenario scenario){

		Config config = scenario.getConfig();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
		ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		ecg.setEmissionVehicleFile(emissionVehicleFile);

		ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);

		ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);

		config.addModule(ecg);
	}

	private Scenario createConfig(){
		Config config = ConfigUtils.createConfig();

		config.strategy().setMaxAgentPlanMemorySize(11);

		ControlerConfigGroup ccg = config.controler();
		ccg.setWriteEventsInterval(2);
		ccg.setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		ccg.setCreateGraphs(false);		
		ccg.setFirstIteration(0);
		ccg.setLastIteration(20);
		ccg.setMobsim("qsim");

		Set<EventsFileFormat> set = new HashSet<EventsFileFormat>();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);

		QSimConfigGroup qcg = config.qsim();		
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(0.1);
		qcg.setStorageCapFactor(0.3);

		PlanCalcScoreConfigGroup pcs = config.planCalcScore();

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration( 6*3600 );
		pcs.addActivityParams(home);

		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration( 9*3600 );
		pcs.addActivityParams(work);

		pcs.setMarginalUtilityOfMoney(0.0789942);
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.0);
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(new Double("-3.0E-4"));
		pcs.setLateArrival_utils_hr(0.0);
		pcs.setPerforming_utils_hr(0.96);

		StrategyConfigGroup scg  = config.strategy();

		StrategySettings strategySettingsR = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettingsR.setStrategyName("ReRoute");
		strategySettingsR.setWeight(0.999);
		strategySettingsR.setDisableAfter(10);
		scg.addStrategySettings(strategySettingsR);

		StrategySettings strategySettings = new StrategySettings(Id.create("2", StrategySettings.class));
		strategySettings.setStrategyName("ChangeExpBeta");
		strategySettings.setWeight(0.001);
		scg.addStrategySettings(strategySettings);

		// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		VspExperimentalConfigGroup vcg = config.vspExperimental() ;
		vcg.setWritingOutputEvents(false) ;

		return ScenarioUtils.loadScenario(config);
	}

	/**
	 * Exposure to these agents will result in toll for active agent.
	 */
	private void createPassiveAgents(Scenario scenario) {
		PopulationFactoryImpl pFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		// passive agents' home coordinates are around node 9 (12500, 7500)
		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){

				String idpart = i.toString()+j.toString();

				Person person = pFactory.createPerson(Id.create("passive_"+idpart, Person.class)); 

				double xCoord = 6563. + (i+1)*625;
				double yCoord = 7188. + (j-1)*625;
				Plan plan = pFactory.createPlan(); 

				Coord coord = new Coord(xCoord, yCoord);
				Activity home = pFactory.createActivityFromCoord("home", coord );
				home.setEndTime(1.0);
				Leg leg = pFactory.createLeg(TransportMode.walk);
				Coord coord2 = new Coord(xCoord, yCoord + 1.);
				Activity home2 = pFactory.createActivityFromCoord("home", coord2);

				plan.addActivity(home);
				plan.addLeg(leg);
				plan.addActivity(home2);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
	}

	/**
	 * This agent is traveling and thus will be charged for exposure/emission toll.
	 */
	private void createActiveAgents(Scenario scenario) {
		PopulationFactory pFactory = scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(Id.create("567417.1#12424", Person.class));
		Plan plan = pFactory.createPlan();

		Coord homeCoords = new Coord(1.0, 10000.0);
		Activity home = pFactory.createActivityFromCoord("home", homeCoords);
		//Activity home = pFactory.createActivityFromLinkId("home", scenario.createId("12"));
		home.setEndTime(6 * 3600);
		plan.addActivity(home);

		Leg leg1 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg1);

		Coord workCoords = new Coord(19999.0, 10000.0);
		Activity work = pFactory.createActivityFromCoord("work" , workCoords);
		//		Activity work = pFactory.createActivityFromLinkId("work", scenario.createId("45"));
		work.setEndTime(home.getEndTime() + 600 + 8 * 3600);
		plan.addActivity(work);

		Leg leg2 = pFactory.createLeg(TransportMode.car);
		plan.addLeg(leg2);

		home = pFactory.createActivityFromCoord("home", homeCoords);
		//		home = pFactory.createActivityFromLinkId("home", scenario.createId("12"));
		plan.addActivity(home);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}

	/**
	 * Simplified form of Equil network.
	 */
	private void createNetwork(Scenario scenario) {
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(1.0, 10000.0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(2500.0, 10000.0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(4500.0, 10000.0));
		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord(17500.0, 10000.0));
		Node node5 = network.createAndAddNode(Id.create("5", Node.class), new Coord(19999.0, 10000.0));
		Node node6 = network.createAndAddNode(Id.create("6", Node.class), new Coord(19999.0, 1500.0));
		Node node7 = network.createAndAddNode(Id.create("7", Node.class), new Coord(1.0, 1500.0));
		Node node8 = network.createAndAddNode(Id.create("8", Node.class), new Coord(12500.0, 12499.0));
		Node node9 = network.createAndAddNode(Id.create("9", Node.class), new Coord(12500.0, 7500.0));


		//freeSpeed is 100km/h for roadType 22.
		network.createAndAddLink(Id.create("12", Link.class), node1, node2, 1000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("23", Link.class), node2, node3, 2000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("45", Link.class), node4, node5, 2000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("56", Link.class), node5, node6, 1000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("67", Link.class), node6, node7, 1000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("71", Link.class), node7, node1, 1000, 100.00/3.6, 3600, 1, null, "22");

		// two similar path from node 3 to node 4 - north: route via node 8, south: route via node 9
		network.createAndAddLink(Id.create("38", Link.class), node3, node8, 5000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("39", Link.class), node3, node9, 5000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("84", Link.class), node8, node4, 5000, 100.00/3.6, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("94", Link.class), node9, node4, 4999, 100.00/3.6, 3600, 1, null, "22");

		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){
				String idpart = i.toString()+j.toString();

				double xCoord = 6563. + (i+1)*625;
				double yCoord = 7188. + (j-1)*625;

				// add a link for each person
				Node nodeA = network.createAndAddNode(Id.create("node_"+idpart+"A", Node.class), new Coord(xCoord, yCoord));
				Node nodeB = network.createAndAddNode(Id.create("node_"+idpart+"B", Node.class), new Coord(xCoord, yCoord + 1.));
				network.createAndAddLink(Id.create("link_p"+idpart, Link.class), nodeA, nodeB, 10, 30.0, 3600, 1);
			}
		}
	}
	
	private class MyPersonMoneyEventHandler implements PersonMoneyEventHandler  {

		List<PersonMoneyEvent> events = new ArrayList<PersonMoneyEvent>();
		
		@Override
		public void reset(int iteration) {
			events.clear();
		}

		@Override
		public void handleEvent(PersonMoneyEvent event) {
			events.add(event);
		}
	}
	
	private class MyEmissionEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

		List<WarmEmissionEvent> warmEvents = new ArrayList<WarmEmissionEvent>();
		List<ColdEmissionEvent> coldEvents = new ArrayList<ColdEmissionEvent>();
		
		@Override
		public void reset(int iteration) {
			warmEvents.clear();
			coldEvents.clear();
		}

		@Override
		public void handleEvent(ColdEmissionEvent event) {
			coldEvents.add(event);
		}

		@Override
		public void handleEvent(WarmEmissionEvent event) {
			warmEvents.add(event);
		}
	}
}