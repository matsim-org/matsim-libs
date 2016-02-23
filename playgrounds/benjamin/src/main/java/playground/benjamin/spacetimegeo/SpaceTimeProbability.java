/* *********************************************************************** *
 * project: org.matsim.*
 * SpaceTimeProbability.java
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
package playground.benjamin.spacetimegeo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

/**
 * @author benjamin
 *
 */
public class SpaceTimeProbability {
	private static Logger logger = Logger.getLogger(SpaceTimeProbability.class);
	
	Map<Integer, ScenarioParams> scenarioId2Params;

	Integer scenarioBundleId = 1;
	Integer lastIteration = 99;
	
	String workingDirectory = "../../runs-svn/spacetimeGeo/";
	String tollLinksFile = workingDirectory + "input/tollLinksFile.xml";

	private void run() {
		ScenarioBundleSelector sbs = new ScenarioBundleSelector();
		sbs.selectScenarioBundle(scenarioBundleId, workingDirectory);
		scenarioId2Params = sbs.getScenarioId2Params();
		
		String fileName = this.workingDirectory + "output/analysis" + scenarioBundleId + ".txt";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.append("Scenario Nr. \t Route 3 (score) \t Route 9 (score) \t Route 11 (score) \n");

			for(Entry<Integer, ScenarioParams> entry: scenarioId2Params.entrySet()){

				Config config = specifyConfig(entry);

				Scenario scenario = ScenarioUtils.createScenario(config);

				createNetwork(scenario);
				createAgent(scenario);

				Controler controler = new Controler(scenario);
				controler.getConfig().controler().setOverwriteFileSetting(
						true ?
								OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
								OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
				controler.getConfig().controler().setCreateGraphs(false);


                final LinkLeaveCountHandler handler = new LinkLeaveCountHandler();
				StartupListener startupListener = new StartupListener() {
					@Override
					public void notifyStartup(StartupEvent event) {
						event.getServices().getEvents().addHandler(handler);
					}
				};
				controler.addControlerListener(startupListener);
                controler.setModules(new ControlerDefaultsWithRoadPricingModule());
                controler.run();
				
				Double link3Score = null;
				Double link9Score = null;
				Double link11Score = null;
				
				Person worker = scenario.getPopulation().getPersons().get(Id.create("worker", Person.class));
				for(Plan plan : worker.getPlans()){
					for(PlanElement pe : plan.getPlanElements()){
						if(pe instanceof Leg){
							NetworkRoute route = (NetworkRoute) ((Leg) pe).getRoute();
							for(Id<Link> id : route.getLinkIds()){
								if(id.equals(Id.create(3, Link.class))){
									link3Score = plan.getScore();
								} else if(id.equals(Id.create(9, Link.class))){
									link9Score = plan.getScore();
								} else if(id.equals(Id.create(11, Link.class))){
									link11Score = plan.getScore();
								}
							}
						}
					}
				}
				
				Double link3Fraction = handler.getLink3Counter() / (controler.getConfig().controler().getLastIteration() + 1);
				Double link9Fraction = handler.getLink9Counter() / (controler.getConfig().controler().getLastIteration() + 1);
				Double link11Fraction = handler.getLink11Counter() / (controler.getConfig().controler().getLastIteration() + 1);
				
				bw.append(entry.getKey().toString());
				bw.append("\t");
				bw.append(link3Fraction.toString() + " (" + link3Score.toString() + ")");
				bw.append("\t");
				bw.append(link9Fraction.toString() + " (" + link9Score.toString() + ")");
				bw.append("\t");
				bw.append(link11Fraction.toString() + " (" + link11Score.toString() + ")");
				bw.newLine();
			}
			bw.close();
			logger.info("Finished writing output to " + fileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Config specifyConfig(Entry<Integer, ScenarioParams> entry) {
		
		Config config = ConfigUtils.createConfig();
		
		// controlerConfigGroup
		ControlerConfigGroup ccg = config.controler();
		ccg.setFirstIteration(0);
		ccg.setLastIteration(this.lastIteration);
		ccg.setOutputDirectory(workingDirectory + "output/scenario" + entry.getKey() + "/");
		ccg.setMobsim(MobsimType.qsim.toString());
		Set<EventsFileFormat> set = new HashSet<EventsFileFormat>();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
		ccg.setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		ccg.setWriteEventsInterval(this.lastIteration);
		ccg.setWritePlansInterval(this.lastIteration);
		ccg.setWriteSnapshotsInterval(0);

		// qsimConfigGroup
		QSimConfigGroup qcg = config.qsim();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(24 * 3600.);
		qcg.setFlowCapFactor(1);
		qcg.setStorageCapFactor(1);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);

		// planCalcScoreConfigGroup
		PlanCalcScoreConfigGroup pcs = config.planCalcScore();

		ActivityParams act1Params = new ActivityParams("home");
		act1Params.setTypicalDuration(16 * 3600);
		pcs.addActivityParams(act1Params);

		ActivityParams act2Params = new ActivityParams("work");
		act2Params.setLatestStartTime(7 * 3600);
		act2Params.setTypicalDuration(8 * 3600);
		pcs.addActivityParams(act2Params);

		pcs.setBrainExpBeta(entry.getValue().getMu());
		pcs.setPerforming_utils_hr(entry.getValue().getBetaPerf());
		pcs.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(entry.getValue().getBetaTraveling());
		pcs.setMarginalUtilityOfMoney(entry.getValue().getBetaCost());
		pcs.setLateArrival_utils_hr(entry.getValue().getBetaLate());
//		pcs.setMonetaryDistanceCostRateCar(-0.0);

		// strategyConfigGroup
		StrategyConfigGroup scg = config.strategy();

		StrategySettings changePlan = new StrategySettings(Id.create("1", StrategySettings.class));
		changePlan.setStrategyName(entry.getValue().getChoiceModule());
		changePlan.setWeight(1.0);

//		StrategySettings reRoute = new StrategySettings(Id.create("2"));
//		reRoute.setModuleName("ReRoute");
//		reRoute.setProbability(0.99);
//		reRoute.setDisableAfter(2);
//		
//		scg.setMaxAgentPlanMemorySize(2);

		scg.addStrategySettings(changePlan);
//		scg.addStrategySettings(reRoute);
		
		// scenarioConfigGroup
		ScenarioConfigGroup sccg = config.scenario();

		//        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setUseRoadpricing(true);
		Logger.getLogger(this.getClass()).fatal("the above functionality does no longer exist.  please talk to me if needed. kai, sep'14") ;
		System.exit(-1) ;
		
        // roadPricingConfigGroup
        RoadPricingConfigGroup rpc = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
		rpc.setTollLinksFile(this.tollLinksFile);
		
		return config;
	}

	private void createAgent(Scenario scenario) {
		PopulationFactoryImpl pFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		Person person = pFactory.createPerson(Id.create("worker", Person.class));
		
		Id<Link> homeLinkId = Id.create("1", Link.class);
		Id<Link> workLinkId = Id.create("4", Link.class);
		
		for(int i=0; i<3; i++){
			Plan plan = pFactory.createPlan();
			
			Activity home = pFactory.createActivityFromLinkId("home", homeLinkId);
			home.setEndTime(6 * 3600);
			plan.addActivity(home);
			
			Leg leg1 = pFactory.createLeg(TransportMode.car);
			
			List<Id<Link>> routeLinkIds = new LinkedList<Id<Link>>();
			if(i == 0){
				Id<Link> id2 = Id.create("2", Link.class);
				Id<Link> id3 = Id.create("3", Link.class);
				routeLinkIds.add(id2);
				routeLinkIds.add(id3);
			} else if(i == 1){
				Id<Link> id2 = Id.create("2", Link.class);
				Id<Link> id8 = Id.create("8", Link.class);
				Id<Link> id9 = Id.create("9", Link.class);
				routeLinkIds.add(id2);
				routeLinkIds.add(id8);
				routeLinkIds.add(id9);
			} else if(i == 2){
				Id<Link> id2 = Id.create("2", Link.class);
				Id<Link> id10 = Id.create("10", Link.class);
				Id<Link> id11 = Id.create("11", Link.class);
				routeLinkIds.add(id2);
				routeLinkIds.add(id10);
				routeLinkIds.add(id11);
			} else {
				throw new RuntimeException();
			}
			
			NetworkRoute route1 = pFactory.createRoute(NetworkRoute.class, null, null);
			route1.setLinkIds(homeLinkId, routeLinkIds, workLinkId);
			leg1.setRoute(route1);
			logger.info(((NetworkRoute) leg1.getRoute()).getLinkIds());
			plan.addLeg(leg1);
			
			Activity work = pFactory.createActivityFromLinkId("work", workLinkId);
			work.setEndTime(home.getEndTime() + 1800 + 8 * 3600);
			plan.addActivity(work);
			
			Leg leg2 = pFactory.createLeg(TransportMode.car);
			
			List<Id<Link>> routeLinkIds2 = new LinkedList<Id<Link>>();
			Id<Link> id5 = Id.create("5", Link.class);
			Id<Link> id6 = Id.create("6", Link.class);
			Id<Link> id7 = Id.create("7", Link.class);
			routeLinkIds2.add(id5);
			routeLinkIds2.add(id6);
			routeLinkIds2.add(id7);

			NetworkRoute route2 = pFactory.createRoute(NetworkRoute.class, null, null);
			route2.setLinkIds(workLinkId, routeLinkIds2, homeLinkId);
			leg2.setRoute(route2);
			logger.info(((NetworkRoute) leg2.getRoute()).getLinkIds());
			plan.addLeg(leg2);
			
			plan.addActivity(home);
			plan.setScore(1000.0);
			person.addPlan(plan);
		}
		scenario.getPopulation().addPerson(person);
	}

	private void createNetwork(Scenario scenario) {
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		double x7 = -26000.0;
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(x7, 0.0));
		double x6 = -25000.0;
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(x6, 0.0));
		double x5 = -21000.0;
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(x5, 0.0));
		double x4 = -1000.0;
		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord(x4, 0.0));
		Node node5 = network.createAndAddNode(Id.create("5", Node.class), new Coord(0.0, 0.0));
		double x3 = -6500.0;
		double y2 = -5000.0;
		Node node6 = network.createAndAddNode(Id.create("6", Node.class), new Coord(x3, y2));
		double x2 = -19500.0;
		double y1 = -5000.0;
		Node node7 = network.createAndAddNode(Id.create("7", Node.class), new Coord(x2, y1));
		double x1 = -11000.0;
		Node node8 = network.createAndAddNode(Id.create("8", Node.class), new Coord(x1, 2500.0));
		double x = -11000.0;
		double y = -2500.0;
		Node node9 = network.createAndAddNode(Id.create("9", Node.class), new Coord(x, y));
		
		//base distance to work 5km; travel time 6min
		network.createAndAddLink(Id.create("2", Link.class), node2, node3, 4000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(Id.create("4", Link.class), node4, node5, 1000, 13.89, 3600, 1, null, null);

		//additional distance to work 20km; travel time 24min
		network.createAndAddLink(Id.create("3", Link.class), node3, node4, 20000, 13.89, 3600, 1, null, null);
		
		//additional distance to work 45km; travel time 54min
		network.createAndAddLink(Id.create("8", Link.class), node3, node8, 22500, 13.89, 3600, 1, null, null);
		network.createAndAddLink(Id.create("9", Link.class), node8, node4, 22500, 13.89, 3600, 1, null, null);
		
		//additional distance to work 70km; travel time 84min
		network.createAndAddLink(Id.create("10", Link.class), node3, node9, 35000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(Id.create("11", Link.class), node9, node4, 35000, 13.89, 3600, 1, null, null);
		
		//distance from work 5km; travel time 6min
		network.createAndAddLink(Id.create("5", Link.class), node5, node6, 1000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(Id.create("6", Link.class), node6, node7, 2000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(Id.create("7", Link.class), node7, node1, 1000, 13.89, 3600, 1, null, null);
		network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1, 13.89, 3600, 1, null, null);
	}

	public static void main(String[] args) {
		SpaceTimeProbability stp = new SpaceTimeProbability();
		stp.run();
	}
}
