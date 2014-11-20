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
package playground.agarwalamit.marginalTesting;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.agarwalamit.analysis.emission.EmissionCostFactors;
import playground.agarwalamit.siouxFalls.simulationInputs.EmissionVehicleGeneration;
import playground.ikaddoura.internalizationCar.MarginalCostPricing;
import playground.ikaddoura.internalizationCar.TollHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsAnalyzer;

/**
 * @author amit
 */
public class StraightTrackExperiment {
	final Logger log = Logger.getLogger(StraightTrackExperiment.class);
	createPseudoInputs2 pseudoInputs;

	private static final boolean considerCO2Costs=true;
	private static final double emissionCostFacotr=1.0;


	private static final double marginal_Utl_money=0.062;
	private static final double marginal_Utl_performing_sec=0.96/3600;
	private static final double marginal_Utl_traveling_car_sec=-0.0/3600;
	private static final double marginalUtlOfTravelTime = marginal_Utl_traveling_car_sec+marginal_Utl_performing_sec;
	private static final double vtts_car = marginalUtlOfTravelTime/marginal_Utl_money;

	public static void main(String[] args) {
		StraightTrackExperiment stTrEx = new StraightTrackExperiment();
		//				stTrEx.test4FlowCapFactorAndTotalDelays();
		//		stTrEx.test4MarginalEmissionsCosts();
		stTrEx.test4MarginalCongestionCosts();
	}

	//	public void test4FlowCapFactorAndTotalDelays() {
	//
	//		final int populationSize = 1000;
	//		final int populationInterval =10;
	//		final int minimumPopulationSize =00;
	//
	//		SortedMap<Double, Double> flowCap2Delays = new TreeMap<Double, Double>();
	//		Controler controller;
	//
	//		for(int i=minimumPopulationSize;i<populationSize+1;i=i+populationInterval){
	//			String outputDir = "./output/testC4/pop"+i+"/";
	//			pseudoInputs = new createPseudoInputs2();
	//			double flowCapFactor = ((double) i)/populationSize;
	//			double storageCapFactor = ((double) i)/populationSize;
	//			pseudoInputs.createPopulation(i);
	//			pseudoInputs.createConfig(flowCapFactor, storageCapFactor);
	//			pseudoInputs.config.controler().setOutputDirectory(outputDir);
	//			controller = new Controler(pseudoInputs.scenario);
	//			controller.setOverwriteFiles(true);
	//			controller.setCreateGraphs(true);
	//			controller.setDumpDataAtEnd(false);
	//
	//			//			TollHandler tollHandler = new TollHandler(controller.getScenario());
	//			//			TollDisutilityCalculatorFactory tdcf = new TollDisutilityCalculatorFactory(tollHandler);
	//			//			controller.setTravelDisutilityFactory(tdcf);
	//			//			controller.addControlerListener(new MarginalCostPricing((ScenarioImpl) controller.getScenario(), tollHandler));
	//
	//			controller.run();
	//			double tempDelays = getDelaysFromEvents(outputDir, (ScenarioImpl) controller.getScenario());
	//			flowCap2Delays.put(flowCapFactor, tempDelays);
	//		}
	//
	//		BufferedWriter writer = IOUtils.getBufferedWriter("./output/testC4/flowCapFactorVsDelays.txt");
	//		try {
	//			writer.write("flowCapacityFactor \t delays(inSeconds) \n");
	//			for(double d:flowCap2Delays.keySet()){
	//				writer.write(d+"\t"+(flowCap2Delays.get(d)*(1/d))+"\n");
	//			}
	//			writer.close();
	//		} catch (Exception e) {
	//			throw new RuntimeException("Data is not written to file. Reason : -"+e);
	//		}
	//	}

	public void test4MarginalEmissionsCosts(){
		Controler controller;
		final int maxPopulationSize = 30;
		final int populationInterval =30;
		final int minimumPopulationSize =30;
		createPseudoInputs2 pseudoInputs;
		SortedMap<Double, Double> numberOfAgentsVsEmissionsCosts = new TreeMap<Double, Double>();
		for(int i=minimumPopulationSize;i<maxPopulationSize+1;i=i+populationInterval){
			if(i==0) continue;
			String outputDir = "./output/testCP/pop"+i+"/";
			pseudoInputs = new createPseudoInputs2();
			double flowCapFactor = 1;
			double storageCapFactor = 1;
			pseudoInputs.createPopulation(i);
			pseudoInputs.createConfig(flowCapFactor, storageCapFactor);
			pseudoInputs.config.controler().setOutputDirectory(outputDir);
			pseudoInputs.config.qsim().setStuckTime(7200);

			String emissionVehicleFile =  "./input/emissionVehicleFile.xml";
			EmissionVehicleGeneration evg = new EmissionVehicleGeneration(pseudoInputs.scenario,emissionVehicleFile);
			evg.run();

			EmissionsConfigGroup ecg = new EmissionsConfigGroup();
			ecg.setAverageColdEmissionFactorsFile("./input/matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
			ecg.setAverageWarmEmissionFactorsFile("./input/matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
			ecg.setEmissionRoadTypeMappingFile("./input/roadTypeMapping.txt");
			ecg.setEmissionVehicleFile(emissionVehicleFile);
			ecg.setUsingDetailedEmissionCalculation(false);
			pseudoInputs.config.addModule(ecg);

			controller = new Controler(pseudoInputs.scenario);
			controller.setOverwriteFiles(true);
			controller.setCreateGraphs(true);
			controller.setDumpDataAtEnd(false);

			controller.addControlerListener(new MyEmissionControlerListner());
			controller.run();
			//			double emissionsCosts = getTotalEmissionsCostsFromEmissionsEvents(outputDir, controller.getScenario());
			double delaysCosts = getDelaysFromEvents(outputDir, (ScenarioImpl) controller.getScenario());
			//			numberOfAgentsVsEmissionsCosts.put((double) i, emissionsCosts);
		}
		BufferedWriter writer = IOUtils.getBufferedWriter("./output/testCP/numberOfAgentsVsEmissionsCosts.txt");
		try {
			writer.write("numberOfAgents \t emissionsCosts(inMonetaryUnits) \n");
			for(double d:numberOfAgentsVsEmissionsCosts.keySet()){
				writer.write(d+"\t"+(numberOfAgentsVsEmissionsCosts.get(d))+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason : -"+e);
		}
	}

	public void test4MarginalCongestionCosts(){
		Controler controller;
		final int maxPopulationSize = 10;
		final int populationInterval =10;
		final int minimumPopulationSize =10;

		SortedMap<Double, Double> numberOfAgentsVsDelaysCosts = new TreeMap<Double, Double>();
		for(int i=minimumPopulationSize;i<maxPopulationSize+1;i=i+populationInterval){
			i = 1000 ;
			if(i==0) continue;
			String outputDir = "./output/testCP2/pop"+i+"/";
			pseudoInputs = new createPseudoInputs2();
			double flowCapFactor = 1;
			double storageCapFactor = 1;
			pseudoInputs.createPopulation(i);
			pseudoInputs.createConfig(flowCapFactor, storageCapFactor);
			pseudoInputs.config.controler().setOutputDirectory(outputDir);
			controller = new Controler(pseudoInputs.scenario);
			controller.setOverwriteFiles(true);
			controller.setCreateGraphs(true);
			controller.setDumpDataAtEnd(false);
			
			TollHandler tollHandler = new TollHandler( controller.getScenario() ) ;
			controller.addControlerListener( new MarginalCostPricing( (ScenarioImpl) controller.getScenario(), tollHandler ) ) ;

			final boolean useOTFVis = true ;
			controller.setMobsimFactory( new MobsimFactory() {
				@Override		
				public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
					QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
					if ( useOTFVis ) {
						// otfvis configuration.  There is more you can do here than via file!
						final OTFVisConfigGroup otfVisConfig = ConfigUtils.addOrGetModule(qSim.getScenario().getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
						otfVisConfig.setDrawTransitFacilities(false) ; // this DOES work
						//				otfVisConfig.setShowParking(true) ; // this does not really work
						otfVisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.bvg) ;
						//					otfVisConfig.setShowTeleportedAgents(false) ;

						OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(sc.getConfig(), sc, eventsManager, qSim);
						OTFClientLive.run(sc.getConfig(), server);
					}
					return qSim ;
				}
			} );

			//			controller.addControlerListener(new MyEmissionControlerListner());
			controller.run();
			double delaysCosts = getDelaysFromEvents(outputDir, (ScenarioImpl)controller.getScenario());
			numberOfAgentsVsDelaysCosts.put((double) i, delaysCosts*vtts_car);
		}
		BufferedWriter writer = IOUtils.getBufferedWriter("./output/testCP/numberOfAgentsVsCongestionCosts.txt");
		try {
			writer.write("numberOfAgents \t delaysCosts(inMonetaryUnits) \n");
			for(double d:numberOfAgentsVsDelaysCosts.keySet()){
				writer.write(d+"\t"+(numberOfAgentsVsDelaysCosts.get(d))+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written to file. Reason : -"+e);
		}
	}

	private double getTotalEmissionsCostsFromEmissionsEvents(String outputDir, Scenario scenario){
		EmissionsAnalyzer analyzer	= new EmissionsAnalyzer(outputDir+"ITERS/it.0/0.emission.events.xml.gz");
		analyzer.init((ScenarioImpl) scenario);
		analyzer.preProcessData();
		analyzer.postProcessData();
		SortedMap<String, Double> totalEmissions = analyzer.getTotalEmissions();
		double totalEmissionCost =0;

		for(EmissionCostFactors ecf:EmissionCostFactors.values()) {
			String str = ecf.toString();
			if(str.equals("CO2_TOTAL") && !considerCO2Costs){
				// do not include CO2_TOTAL costs.
			} else {
				double emissionsCosts = ecf.getCostFactor() * totalEmissions.get(str);
				totalEmissionCost += emissionsCosts;
			}
		}

		//		for(String str:totalEmissions.keySet()){
		//			if(str.equals("NOX")) {
		//				double noxCosts = totalEmissions.get(str) * EURO_PER_GRAMM_NOX;
		//				totalEmissionCost += noxCosts;
		//			} else if(str.equals("NMHC")) {
		//				double nmhcCosts =totalEmissions.get(str) * EURO_PER_GRAMM_NMVOC;
		//				totalEmissionCost += nmhcCosts;
		//			} else if(str.equals("SO2")) {
		//				double so2Costs = totalEmissions.get(str) * EURO_PER_GRAMM_SO2;
		//				totalEmissionCost += so2Costs;
		//			} else if(str.equals("PM")) {
		//				double pmCosts = totalEmissions.get(str) * EURO_PER_GRAMM_PM2_5_EXHAUST;
		//				totalEmissionCost += pmCosts;
		//			} else if(str.equals("CO2_TOTAL")){
		//				if(considerCO2Costs) {
		//					double co2Costs = totalEmissions.get(str) * EURO_PER_GRAMM_CO2;
		//					totalEmissionCost += co2Costs;
		//				} else ; //do nothing
		//			}
		//			else ; //do nothing
		//		}
		return emissionCostFacotr*totalEmissionCost;
	}

	private double getDelaysFromEvents(String outputDir, ScenarioImpl sc){
		EventsManager eventManager = EventsUtils.createEventsManager();
		MarginalCongestionHandlerImplV4 congestionHandlerImplV4= new MarginalCongestionHandlerImplV4(eventManager, sc);

		eventManager.addHandler(congestionHandlerImplV4);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventManager);
		String inputEventsFile = outputDir+"/ITERS/it.0/0.events.xml.gz";
		eventsReader.readFile(inputEventsFile);
		congestionHandlerImplV4.writeCongestionStats(outputDir+"/ITERS/it.0/congestionStats.txt");

		return congestionHandlerImplV4.getTotalDelay();
	}

	//	private class createPseudoInputs {
	//		Scenario scenario;
	//		Config config;
	//		NetworkImpl network;
	//		Population population;
	//		Link link1;
	//		Link link2;
	//		Link link3;
	//		Link link4;
	//		Link link5;
	//		Link link6;
	//
	//		public createPseudoInputs(){
	//			this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	//			createNetwork();
	//		}
	//
	//		private void createNetwork (){
	//
	//			network = (NetworkImpl) this.scenario.getNetwork();
	//			final Set<String> allModesAllowed = new HashSet<String>();
	//			allModesAllowed.addAll(Arrays.asList("car","motorbike","pt", "bike", "walk"));
	//
	//			Node node1 = network.createAndAddNode(this.scenario.createId("1"), this.scenario.createCoord(0, 0)) ;
	//			Node node2 = network.createAndAddNode(this.scenario.createId("2"), this.scenario.createCoord(100, 100));
	//			Node node3 = network.createAndAddNode(this.scenario.createId("3"), this.scenario.createCoord(300, 90));
	//			Node node4 = network.createAndAddNode(this.scenario.createId("4"), this.scenario.createCoord(500, 200));
	//			Node node5 = network.createAndAddNode(this.scenario.createId("5"), this.scenario.createCoord(700, 150));
	//
	//			link1 = network.createAndAddLink(this.scenario.createId(String.valueOf("0")), node1, node2,10000.0,20.0,36000,1,null,"7");
	//			link1.setAllowedModes(allModesAllowed);
	//			link2 = network.createAndAddLink(this.scenario.createId(String.valueOf("1")), node2, node3,1000.0,20.0,360,1,null,"7");
	//			link2.setAllowedModes(allModesAllowed);
	//			link3 = network.createAndAddLink(this.scenario.createId(String.valueOf("2")), node3, node4,10.0,20.0,900,1,null,"7");
	//			link3.setAllowedModes(allModesAllowed);
	//			link4 = network.createAndAddLink(this.scenario.createId(String.valueOf("3")), node4, node5,10000.0,20.0,36000,1,null,"7");
	//			link4.setAllowedModes(allModesAllowed);
	//			//		new NetworkCleaner().run(network);
	//			//		new NetworkWriter(network).write("./input/TestNetwork.xml");
	//		}
	//		
	//		private void createPopulation(int numberOfPersons){
	//			population = this.scenario.getPopulation();
	//
	//			for(int i=0;i<numberOfPersons;i++){
	//				PersonImpl p = new PersonImpl(new IdImpl(i));
	//				PlanImpl plan = p.createAndAddPlan(true);
	//				ActivityImpl a1 = plan.createAndAddActivity("home",link1.getId());
	//				a1.setEndTime(8*3600+i);
	//				LegImpl leg = plan.createAndAddLeg(TransportMode.car);
	//
	//				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
	//				NetworkRoute route = (NetworkRoute) factory.createRoute(link1.getId(), link4.getId());
	//				List<Id> linkIds = new ArrayList<Id>();
	//				linkIds.add(link2.getId());
	//				linkIds.add(link3.getId());
	//				route.setLinkIds(link1.getId(), linkIds, link4.getId());
	//				leg.setRoute(route);
	//				plan.createAndAddActivity("work", link4.getId());
	//				population.addPerson(p);
	//			}
	//			//		new PopulationWriter(pop, network).write("./input/planTest.xml");
	//		}
	//
	//		private void createConfig(double flowCapFactor, double storageCapFactor){
	//			config = this.scenario.getConfig();
	//			config.qsim().setFlowCapFactor(flowCapFactor);
	//			config.qsim().setStorageCapFactor(storageCapFactor);
	//			config.qsim().setMainModes(Arrays.asList(TransportMode.car));
	//			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO.name());
	//			config.qsim().setEndTime(24*3600);
	//			config.qsim().setStuckTime(1*60);
	//			config.controler().setFirstIteration(0);
	//			config.controler().setLastIteration(0);
	//			config.controler().setWriteEventsInterval(1);
	//
	//			ActivityParams homeAct = new ActivityParams("home");
	//			homeAct.setTypicalDuration(1*3600);
	//			config.planCalcScore().addActivityParams(homeAct);
	//
	//			ActivityParams workAct =new ActivityParams("work");
	//			workAct.setTypicalDuration(1*3600);;
	//			config.planCalcScore().addActivityParams(workAct);
	//
	//			//			new ConfigWriter(config).write("./output/CI/config.xml");
	//		}
	//	}

	private class createPseudoInputs2 {
		Scenario scenario;
		Config config;
		NetworkImpl network;
		Population population;
		Link link1;
		Link link2;
		Link link3;
		Link link4;
		Link link5;
		Link link6;
		Link link7;
		Link link8;
		Link link9;
		public createPseudoInputs2(){
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			network =  (NetworkImpl) this.scenario.getNetwork();
			population = this.scenario.getPopulation();
			createNetwork();
		}

		private void createNetwork(){
			final Set<String> allModesAllowed = new HashSet<String>();
			allModesAllowed.addAll(Arrays.asList("car","motorbike","pt", "bike", "walk"));

			Node node1 = network.createAndAddNode(Id.createNodeId("1"), this.scenario.createCoord(0, 0)) ;
			Node node2 = network.createAndAddNode(Id.createNodeId("2"), this.scenario.createCoord(100, 100));
			Node node3 = network.createAndAddNode(Id.createNodeId("3"), this.scenario.createCoord(300, 90));
			Node node4 = network.createAndAddNode(Id.createNodeId("4"), this.scenario.createCoord(500, 200));
			Node node5 = network.createAndAddNode(Id.createNodeId("5"), this.scenario.createCoord(700, 150));
			Node node10 = network.createAndAddNode(Id.createNodeId("10"), this.scenario.createCoord(900, 200));
			Node node6 = network.createAndAddNode(Id.createNodeId("6"), this.scenario.createCoord(500, 20));
			Node node7 = network.createAndAddNode(Id.createNodeId("7"), this.scenario.createCoord(700, 100));
			Node node8 = network.createAndAddNode(Id.createNodeId("8"), this.scenario.createCoord(500, -100));
			Node node9 = network.createAndAddNode(Id.createNodeId("9"), this.scenario.createCoord(700, 10));

			link1 = network.createAndAddLink(Id.createLinkId(String.valueOf("1")), node1, node2,10000.0,20.0,36000,1,null,"7");
			link1.setAllowedModes(allModesAllowed);
			link2 = network.createAndAddLink(Id.createLinkId(String.valueOf("2")), node2, node3,100.0,20.0,18000,1,null,"7");
			link2.setAllowedModes(allModesAllowed);
			link3 = network.createAndAddLink(Id.createLinkId(String.valueOf("3")), node3, node4,10.0,20.0,3600,1,null,"7");
			link3.setAllowedModes(allModesAllowed);
			link4 = network.createAndAddLink(Id.createLinkId(String.valueOf("4")), node4, node5,10.0,20.0,360,1,null,"7");
			link4.setAllowedModes(allModesAllowed);
			link9 = network.createAndAddLink(Id.createLinkId(String.valueOf("9")), node5, node10,10000.0,20.0,36000,1,null,"7");
			link9.setAllowedModes(allModesAllowed);
			link5 = network.createAndAddLink(Id.createLinkId(String.valueOf("5")), node3, node6,10.0,20.0,360,1,null,"7");
			link5.setAllowedModes(allModesAllowed);
			link6 = network.createAndAddLink(Id.createLinkId(String.valueOf("6")), node6, node7,10000.0,20.0,36000,1,null,"7");
			link6.setAllowedModes(allModesAllowed);
			link7= network.createAndAddLink(Id.createLinkId(String.valueOf("7")), node3, node8,10.0,20.0,360,1,null,"7");
			link7.setAllowedModes(allModesAllowed);
			link8 = network.createAndAddLink(Id.createLinkId(String.valueOf("8")), node8, node9,10000.0,20.0,36000,1,null,"7");
			link8.setAllowedModes(allModesAllowed);
			//		new NetworkCleaner().run(network);
			//		new NetworkWriter(network).write("./input/TestNetwork.xml");
		}

		private void createPopulation(int numberOfPersons){

			for(int i=0;i<numberOfPersons;i++){
				PersonImpl p = new PersonImpl(Id.createPersonId(i));
				PlanImpl plan = p.createAndAddPlan(true);
				ActivityImpl a1 = plan.createAndAddActivity("home",link1.getId());
				a1.setEndTime(8*3600+i);
				LegImpl leg = plan.createAndAddLeg(TransportMode.car);

				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				if(i%2==0) {
					route= (NetworkRoute) factory.createRoute(link1.getId(), link9.getId());
					linkIds.add(link2.getId());
					linkIds.add(link3.getId());
					linkIds.add(link4.getId());
					route.setLinkIds(link1.getId(), linkIds, link9.getId());
					leg.setRoute(route);
					plan.createAndAddActivity("work", link9.getId());
				} else if(i%3==0) {
					route = (NetworkRoute) factory.createRoute(link1.getId(), link6.getId());
					linkIds.add(link2.getId());
					linkIds.add(link5.getId());
					route.setLinkIds(link1.getId(), linkIds, link6.getId());
					leg.setRoute(route);
					plan.createAndAddActivity("work", link6.getId());
				} else {
					route = (NetworkRoute) factory.createRoute(link1.getId(), link8.getId());
					linkIds.add(link2.getId());
					linkIds.add(link7.getId());
					route.setLinkIds(link1.getId(), linkIds, link8.getId());
					leg.setRoute(route);
					plan.createAndAddActivity("work", link8.getId());
				}
				population.addPerson(p);
			}
			//		new PopulationWriter(pop, network).write("./input/planTest.xml");
		}

		private void createConfig(double flowCapFactor, double storageCapFactor){
			config.qsim().setFlowCapFactor(flowCapFactor);
			config.qsim().setStorageCapFactor(storageCapFactor);
			config.qsim().setMainModes(Arrays.asList(TransportMode.car));
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.FIFO.name());
			config.qsim().setEndTime(24*3600);
			//		config.qsim().setStuckTime(1*60);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			config.controler().setWriteEventsInterval(1);

			ActivityParams homeAct = new ActivityParams("home");
			homeAct.setTypicalDuration(1*3600);
			config.planCalcScore().addActivityParams(homeAct);

			ActivityParams workAct =new ActivityParams("work");
			workAct.setTypicalDuration(1*3600);;
			config.planCalcScore().addActivityParams(workAct);

			//			new ConfigWriter(config).write("./output/CI/config.xml");
		}
	}
}
