
/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
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
package playground.juliakern.internalizationExposure;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import playground.benjamin.scenarios.munich.exposure.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author julia after benjamin
 *
 */
public class TestExposurePricing {
	
	static String inputPath = "../../detailedEval/emissions/testScenario/input/";
	
	static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
	static String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	static String emissionVehicleFile = inputPath + "emissionVehicles_1pct.xml.gz";
	
	static String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	static String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
	
	static boolean isUsingDetailedEmissionCalculation = true;
	static String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
	static String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";
	
//	static String outputPath = "../../detailedEval/emissions/testScenario/output/";
	static String outputPath = "output/testEmissionPricing/";

	private static Logger logger;

	private static Double xMin = 0.0;

	private static Double xMax = 20000.;

	private static Double yMin = 0.0;

	private static Double yMax = 12500.;

	private static Map<Id<Link>, Integer> links2xCells;

	private static Integer noOfXCells = 32;

	private static Map<Id<Link>, Integer> links2yCells;

	private static Integer noOfYCells = 20;

	private static ResponsibilityGridTools rgt;

	private static Double timeBinSize = 3600.;

	private static int noOfTimeBins = 24;
	
	private static int numberOfIterations = 21;
	
	public static void main(String[] args) {
		
		logger = Logger.getLogger(TestExposurePricing.class);
		
		Config config = new Config();
		config.addCoreModules();
		config.setParam("strategy", "maxAgentPlanMemorySize", "2");
		Controler controler = new Controler(config);
		config.setParam("controler", "writeEventsInterval", "1");
		
		
	// controler settings	
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);

        // controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputPath);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(numberOfIterations);
		ccg.setMobsim("qsim");
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
//		ccg.setRunId("321");
		
	// qsimConfigGroup
		QSimConfigGroup qcg = controler.getConfig().qsim();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(0.1);
		qcg.setStorageCapFactor(0.3);
//		qcg.setFlowCapFactor(0.01);
//		qcg.setStorageCapFactor(0.03);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		qcg.setStuckTime(10.0);
		
	// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		Set<String> activities = new HashSet<String>();
		activities.add("unknown");
		activities.add("work");
		activities.add("pickup");
		activities.add("with adult");
		activities.add("other");
		activities.add("pvWork");
		activities.add("pvHome");
		activities.add("gvHome");
		activities.add("education");
		activities.add("business");
		activities.add("shopping");
		activities.add("private");
		activities.add("leisure");
		activities.add("sports");
		activities.add("home");
		activities.add("friends");
		
		for(String activity : activities){
			ActivityParams params = new ActivityParams(activity);
			if(activity.equals("home")){
				params.setTypicalDuration(6*3600);
			}else{
				if(activity.equals("work")){
					params.setTypicalDuration(9*3600);
				}else{
					params.setTypicalDuration(8 * 3600);
				}
			}
			pcs.addActivityParams(params);
		}
		
		pcs.setMarginalUtilityOfMoney(0.0789942);
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.0);
		pcs.getModes().get(TransportMode.car).setMonetaryDistanceRate(new Double("-3.0E-4"));
		pcs.setLateArrival_utils_hr(0.0);
		pcs.setPerforming_utils_hr(0.96);
		
		logger.info("mrg util of money " + pcs.getMarginalUtilityOfMoney() + " = 0.0789942?");

		

	// strategy
//		StrategyConfigGroup scg = controler.getConfig().strategy();
//		StrategySettings strategySettings = new StrategySettings(Id.create("1"));
//		strategySettings.setModuleName("BestScore");
//		strategySettings.setProbability(0.01);
//		scg.addStrategySettings(strategySettings);
//		StrategySettings strategySettingsR = new StrategySettings(Id.create("2"));
//		strategySettingsR.setModuleName("ReRoute");
//		strategySettingsR.setProbability(1.0);
//		strategySettingsR.setDisableAfter(10);
//		scg.addStrategySettings(strategySettingsR);
		
		StrategyConfigGroup scg = controler.getConfig().strategy();

		StrategySettings strategySettingsR = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettingsR.setStrategyName("ReRoute");
		strategySettingsR.setWeight(0.999);
		strategySettingsR.setDisableAfter(10);
		scg.addStrategySettings(strategySettingsR);
		
		StrategySettings strategySettings = new StrategySettings(Id.create("2", StrategySettings.class));
		strategySettings.setStrategyName("ChangeExpBeta");
		strategySettings.setWeight(0.001);
		scg.addStrategySettings(strategySettings);
		
	// network
		Scenario scenario = controler.getScenario();
		createNetwork(scenario);
		
	// plans
		createActiveAgents(scenario);
		createPassiveAgents(scenario);
		
	// define emission tool input files	
        EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
        controler.getConfig().addModule(ecg);
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
        ecg.setEmissionVehicleFile(emissionVehicleFile);
        
        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
        
        ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
		
	// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
//		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
//		vcg.setWritingOutputEvents(false) ;
//		
//		EmissionControlerListener ecl = new EmissionControlerListener(controler);
//		controler.addControlerListener(ecl);
//		services.setScoringFunctionFactory(new ResponsibilityScoringFunctionFactory(config, services.getNetwork(), ecl));
		//controler.setTravelDisutilityFactory(new ResDisFactory(ecl, ecl.emissionModule, new EmissionCostModule(1.0)));
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.setEmissionEfficiencyFactor(1.0);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		GridTools gt = new GridTools(scenario.getNetwork().getLinks(), xMin, xMax, yMin, yMax);
		links2xCells = gt.mapLinks2Xcells(noOfXCells);
		links2yCells = gt.mapLinks2Ycells(noOfYCells);
		
		rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, links2xCells, links2yCells, noOfXCells, noOfYCells);
		EmissionResponsibilityCostModule emissionCostModule = new EmissionResponsibilityCostModule(1.0,	false, rgt, links2xCells, links2yCells);
		final EmissionResponsibilityTravelDisutilityCalculatorFactory emfac = new EmissionResponsibilityTravelDisutilityCalculatorFactory(emissionModule, 
				emissionCostModule, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(emfac);
			}
		});
		controler.addControlerListener(new InternalizeEmissionResponsibilityControlerListener(emissionModule, emissionCostModule, rgt, links2xCells, links2yCells));
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		
		controler.run();
		
		Person activeAgent = scenario.getPopulation().getPersons().get(Id.create("567417.1#12424", Person.class));
		Double scoreOfSelectedPlan;
		Plan selectedPlan = activeAgent.getSelectedPlan();
		
		// check selected plan 
		scoreOfSelectedPlan = selectedPlan.getScore();
		for(PlanElement pe: selectedPlan.getPlanElements()){
				if(pe instanceof Leg){
					Leg leg = (Leg)pe;
					LinkNetworkRouteImpl lnri = (LinkNetworkRouteImpl) leg.getRoute();
					if(lnri.getLinkIds().contains(Id.create("39", Link.class))){
						logger.info("Selected route should not use link 39."); //System.out.println("39 contained");
					}else{
						if(lnri.getLinkIds().contains(Id.create("38", Link.class))){
							logger.info("Selected route avoids node 9 as it is supposed to. " +
									"It's score is " + selectedPlan.getScore());
						}
					}
				}
		}
		
		// check not selected plans - score should be worse if link 39 is used
		
		for(Plan p: activeAgent.getPlans()){			
				for(PlanElement pe: p.getPlanElements()){
					if(pe instanceof Leg){
						Leg leg = (Leg)pe;
						LinkNetworkRouteImpl lnri = (LinkNetworkRouteImpl) leg.getRoute();
						if(lnri.getLinkIds().contains(Id.create("39", Link.class))){
							logger.info("This plan uses node 9 and has score " + p.getScore()+ ". Selected = " + PersonUtils.isSelected(p));
						}
						if(lnri.getLinkIds().contains(Id.create("38", Link.class))){
							logger.info("This plan uses node 8 and has score " + p.getScore() + ". Selected = " + PersonUtils.isSelected(p));
						}
					}
				}
			
		}
		
		
		// check links
//		for(Id linkId: scenario.getNetwork().getLinks().keySet()){
//			logger.info("link id " + linkId.toString() + " cell " + links2xCells.get(linkId) + " , " + links2yCells.get(linkId));
//		}
//		logger.info("epsilon"+epsilon);
	}
	
	private static void createNetwork(Scenario scenario) {
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


		network.createAndAddLink(Id.create("12", Link.class), node1, node2, 1000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("23", Link.class), node2, node3, 2000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("45", Link.class), node4, node5, 2000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("56", Link.class), node5, node6, 1000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("67", Link.class), node6, node7, 1000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("71", Link.class), node7, node1, 1000, 60.00, 3600, 1, null, "22");
		
		// two similar path from node 3 to node 4 - north: route via node 8, south: route via node 9
		network.createAndAddLink(Id.create("38", Link.class), node3, node8, 5000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("39", Link.class), node3, node9, 5000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("84", Link.class), node8, node4, 5000, 60.00, 3600, 1, null, "22");
		network.createAndAddLink(Id.create("94", Link.class), node9, node4, 4999, 60.00, 3600, 1, null, "22");
		
		
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
	
	private static void createPassiveAgents(Scenario scenario) {
		PopulationFactoryImpl pFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		// passive agents' home coordinates are around node 9 (12500, 7500)
		for(Integer i=0; i<5; i++){ // x
			for(Integer j=0; j<4; j++){
				
				String idpart = i.toString()+j.toString();
				
				Person person = pFactory.createPerson(Id.create("passive_"+idpart, Person.class)); //new PersonImpl (Id.create(i));

				double xCoord = 6563. + (i+1)*625;
				double yCoord = 7188. + (j-1)*625;
				Plan plan = pFactory.createPlan(); //person.createAndAddPlan(true);

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
	
	private static void createActiveAgents(Scenario scenario) {
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
}