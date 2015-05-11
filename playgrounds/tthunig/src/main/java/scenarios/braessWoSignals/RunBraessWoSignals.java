package scenarios.braessWoSignals;

import java.io.File;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.analysis.AnalyzeBraessSimulation;

/**
 * Class to run a simulation of the braess scenario without signals.
 * It also analyzes the simulation with help of AnalyseBraessSimulation.java.
 * 
 * @author tthunig
 *
 */
public class RunBraessWoSignals {
	
	private static final Logger log = Logger.getLogger(RunBraessWoSignals.class);

	public static void main(String[] args) {
		String date = "2015-05-11";
		
		int numberOfAgents = 60;
		boolean sameStartTime = true;
		boolean initPlansWithAllRoutes = true;
		boolean initPlansWithZRoute = true;
		
		int iterations = 100;
		boolean writeEventsForAllIts = true; // needed for detailed analysis. remind the running time if true
		boolean writePlansForAllIts = false; // remind the running time if true
		
		double middleLinkTT = 200; // in seconds. for deleting use 200
		/* travel time of the link which is not at the middle route. */
		double linkTT3 = 20; // in seconds. former TT was 20
		double linkTT5 = 20; // in seconds. former TT was 20 
		
		boolean enforceZ = false;
		
		double capZ = 1800;
		double capOther = 1800;
		
		double increaseLinkTTBy = 0.0; // in seconds. additive. use 0.0 for former travel times. 15*60 e.g. for an increase of 30 min per path
		
		double performingUtils = 6.0; // +6 default
		double travelingUtils = -6.0; // -6 default
		double lateArrivalUtils = -18.0; // default -18
		boolean scoringDummy = true; // default true
		
		double propChangeExpBeta = 0.9;
		double propReRoute = 0.1;
		double propKeepLast = 0.0;
		double propSelectRandom = 0.0;
		double propSelectExpBeta = 0.0;
		double propBestScore = 0.0;
		
		double brainExpBeta = 1.0; // default: 1.0. DG used to use 2.0 - better results!?
		
		if (initPlansWithAllRoutes && propReRoute != 0.0)
			log.warn("ReRoute isn't needed if plans are initialized with all routes.");
		if (!initPlansWithAllRoutes && propReRoute == 0.0)
			log.warn("ReRoute is needed if plans aren't initialized with all routes. Please increase the reRoute weight!");
		
		String inputDir = DgPaths.SHAREDSVN + "studies/tthunig/scenarios/BraessWoSignals/";
		
		// create run name
		String info = numberOfAgents + "p";
		if (sameStartTime)
			info += "_sameTime";
		if (initPlansWithZRoute && !initPlansWithAllRoutes)
			info += "_initZRoute";
		info += "_" + iterations + "it";
//		if (writeEventsForAllIts)
//			info += "_allEvents";
		if (enforceZ)
			info += "_enforceZ";
		if (capZ == capOther)
			info += "_cap" + capZ;
		else
			info += "_capZ" + capZ + "_capOther" + capOther;		
		info += "_ttMid" + middleLinkTT + "s";
		if (linkTT3 != 20.0 || linkTT5 != 20.0)
			info += "_tt3-" + linkTT3 + "s" + "_tt5-" + linkTT5 + "s";
		if (increaseLinkTTBy != 0.0)
			info += "_increaseTT+" + increaseLinkTTBy + "s";
		if (performingUtils != 6.0)
			info += "_perfUtil" + performingUtils;
		if (travelingUtils != -6.0)
			info += "_travUtil" + travelingUtils;
		if (lateArrivalUtils != -18.0)
			info += "_lateArr" + lateArrivalUtils;
		if (!scoringDummy)
			info += "_dontScoreDummyAct";
		if (propChangeExpBeta != 0.0)
			info += "_chExpBeta" + propChangeExpBeta;
		if (propReRoute != 0.0)
			info += "_reRoute" + propReRoute;
		if (propKeepLast != 0.0)
			info += "_keepLast" + propKeepLast;
		if (propSelectRandom != 0.0)
			info += "_selRandom" + propSelectRandom;
		if (propSelectExpBeta != 0.0)
			info += "_selExpBeta" + propSelectExpBeta;
		if (propBestScore != 0.0)
			info += "_bestScore" + propBestScore;
		if (propSelectExpBeta != 0.0 || propChangeExpBeta != 0.0)
			info += "_beta" + brainExpBeta;		
		
		String outputDir = inputDir + "matsim-output/" + date + "_" + info + "/";
		String configFile = inputDir + "config.xml";
		
		// read config file
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, configFile);
		
		// adapt output directory
		config.controler().setOutputDirectory(outputDir);
		
		// adapt number of iterations
		config.controler().setLastIteration(iterations);
		
		if (writeEventsForAllIts){
			// adapt events writing interval
			config.controler().setWriteEventsInterval(1);
		}
		if (writePlansForAllIts)
			config.controler().setWritePlansInterval(1);
		
		config.planCalcScore().setBrainExpBeta(brainExpBeta);
		
		// adapt strategies
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for (StrategySettings s : strategySettings){
			if (s.getStrategyName().equals("ReRoute")){
				s.setWeight(propReRoute);
				s.setDisableAfter(iterations/2);
			}
			if (s.getStrategyName().equals("ChangeExpBeta")){
				s.setWeight(propChangeExpBeta);
			}
		}
		
		StrategySettings keepLastSelectedStrategy = new StrategySettings();
		keepLastSelectedStrategy.setStrategyName("KeepLastSelected");
		keepLastSelectedStrategy.setWeight(propKeepLast);
		keepLastSelectedStrategy.setDisableAfter(iterations/2);
		config.strategy().addStrategySettings(keepLastSelectedStrategy);
		
		StrategySettings selectRandomStrategy = new StrategySettings();
		selectRandomStrategy.setStrategyName("SelectRandom");
		selectRandomStrategy.setWeight(propSelectRandom);
		selectRandomStrategy.setDisableAfter(iterations/2);
		config.strategy().addStrategySettings(selectRandomStrategy);
		
		StrategySettings selExpBetaStrategy = new StrategySettings();
		selExpBetaStrategy.setStrategyName("SelectExpBeta");
		selExpBetaStrategy.setWeight(propSelectExpBeta);
		config.strategy().addStrategySettings(selExpBetaStrategy);
		
		StrategySettings bestScoreStrategy = new StrategySettings();
		bestScoreStrategy.setStrategyName("BestScore");
		bestScoreStrategy.setWeight(propBestScore);
		config.strategy().addStrategySettings(bestScoreStrategy);
		
		
		// adapt plans file
		String plansFile = "plans" + numberOfAgents;
		if (sameStartTime){
			plansFile += "sameStartTime";
		}
		if (initPlansWithAllRoutes)
			plansFile += "AllRoutes";
		else if (initPlansWithZRoute)
			plansFile += "RouteZ";
		plansFile += ".xml";
		config.plans().setInputFile(inputDir + plansFile);
		Log.info("Use plans file " + plansFile);
					
		
		// adapt utils
		config.planCalcScore().setTraveling_utils_hr(travelingUtils);
		config.planCalcScore().setPerforming_utils_hr(performingUtils);
		config.planCalcScore().setLateArrival_utils_hr(lateArrivalUtils);
		
		config.planCalcScore().getActivityParams("dummy").setScoringThisActivityAtAll(scoringDummy);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// adapt the network
		Link l2 = scenario.getNetwork().getLinks().get(Id.create(2, Link.class));
		Link l3 = scenario.getNetwork().getLinks().get(Id.create(3, Link.class));
		Link l4 = scenario.getNetwork().getLinks().get(Id.create(4, Link.class));
		Link l5 = scenario.getNetwork().getLinks().get(Id.create(5, Link.class));
		Link l6 = scenario.getNetwork().getLinks().get(Id.create(6, Link.class));
		
		// set travel time at middle link (by setting link length)
		l4.setLength(l4.getFreespeed() * middleLinkTT); //instead of 0
		
		// set travel time at the links which are not on the middle route (by setting link length)
		l3.setLength(l3.getFreespeed() * linkTT3);
		l5.setLength(l5.getFreespeed() * linkTT5);
		
		// adapt capacity on all links
		l2.setCapacity(capZ);
		l3.setCapacity(capOther);
		l4.setCapacity(capZ);
		l5.setCapacity(capOther);
		l6.setCapacity(capZ);
		
		if (enforceZ){
			// force agents to use the middle link
			l3.setFreespeed(0.1);
			l5.setFreespeed(0.1);
		}
		
		// increase travel time on each link (by increasing link length)
		// except the middle link and the first and the last link
		l2.setLength(l2.getLength() + increaseLinkTTBy * l2.getFreespeed());
		l3.setLength(l3.getLength() + increaseLinkTTBy * l3.getFreespeed());
		l5.setLength(l5.getLength() + increaseLinkTTBy * l5.getFreespeed());
		l6.setLength(l6.getLength() + increaseLinkTTBy * l6.getFreespeed());
		
		
		Controler controler = new Controler(scenario);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.run();
		
		// analyze run
		String analyzeDir = outputDir + "analysis/";
		new File(analyzeDir).mkdir();
		AnalyzeBraessSimulation analyzer = new AnalyzeBraessSimulation(outputDir, iterations, analyzeDir);
		if (writeEventsForAllIts)
			// analyze all iterations in terms of route choice and travel time
			analyzer.analyzeAllIt();
		// analyze the last iteration more detailed
		analyzer.analyzeLastIt();
				
	}

}
