package scenarios.braessWoSignals;

import java.io.File;
import java.util.Collection;

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

	public static void main(String[] args) {
		String date = "2015-05-05";
		
		int numberOfAgents = 60;
		boolean sameStartTime = true;
		boolean plansWithRoutes = false;
		
		int iterations = 100;
		boolean writeEventsForAllIts = false; // remind the running time if true
		
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
		
		double propChangeExpBeta = 0.9; // 0.9 before
		double propReRoute = 0.1; // 0.1 before
		double propKeepLast = 0.0; // 0.0 before
		
		String inputDir = DgPaths.SHAREDSVN + "studies/tthunig/scenarios/BraessWoSignals/";
		
		// create run name
		String info = "Braess";
		info += "_" + numberOfAgents + "p";
		if (sameStartTime)
			info += "_sameTime";
		if (plansWithRoutes)
			info += "_initRoutes";
		info += "_" + iterations + "it";
		if (writeEventsForAllIts)
			info += "_allEvents";
		if (enforceZ)
			info += "_enforceZ";
		info += "_capZ" + capZ + "_capOther" + capOther;		
		info += "_ttMid" + middleLinkTT + "s" +
				"_tt3-" + linkTT3 + "s" +
				"_tt5-" + linkTT5 + "s";
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
			info += "_expBeta" + propChangeExpBeta;
		if (propReRoute != 0.0)
			info += "_reRoute" + propReRoute;
		if (propKeepLast != 0.0)
			info += "_keepLast" + propKeepLast;
		
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
		
		// adapt strategies
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for (StrategySettings s : strategySettings){
			if (s.getName().equals("ReRoute")){
				s.setWeight(propReRoute);
				s.setDisableAfter(iterations/2);
			}
			if (s.getName().equals("ChangeExpBeta")){
				s.setWeight(propChangeExpBeta);
			}
		}
		
//		StrategySettings keepLastSelectedStrategy = new StrategySettings();
//		keepLastSelectedStrategy.setStrategyName("KeepLastSelected");
//		keepLastSelectedStrategy.setWeight(propKeepLast);
//		config.strategy().addStrategySettings(keepLastSelectedStrategy);
		
//		StrategySettings changeExpBetaStrategy = new StrategySettings();
//		changeExpBetaStrategy.setStrategyName("ChangeExpBeta");
//		changeExpBetaStrategy.setWeight(propChangeExpBeta);
//		config.strategy().addStrategySettings(changeExpBetaStrategy);
//		
//		StrategySettings reRouteStrategy = new StrategySettings();
//		reRouteStrategy.setStrategyName("ReRoute");
//		reRouteStrategy.setWeight(propReRoute);
//		reRouteStrategy.setDisableAfter(iterations/2);
//		config.strategy().addStrategySettings(reRouteStrategy);
		
		
		// adapt plans file
		if (sameStartTime){
			if (plansWithRoutes)
				config.plans().setInputFile(inputDir + "plans" + numberOfAgents + "sameStartTimeWithRoutes.xml");
			else
				config.plans().setInputFile(inputDir + "plans" + numberOfAgents + "sameStartTime.xml");
		}
		else{
			if (plansWithRoutes)
				config.plans().setInputFile(inputDir + "plans" + numberOfAgents + "WithRoutes.xml");
			else
				config.plans().setInputFile(inputDir + "plans" + numberOfAgents + ".xml");
		}			
		
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
			l3.setFreespeed(1);
			l5.setFreespeed(1);
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
		// analyze the last iteration detailed
		analyzer.analyzeLastIt();
		if (writeEventsForAllIts)
			// analyze all iterations in terms of route choice and travel time
			analyzer.analyzeAllIt();
	}

}
