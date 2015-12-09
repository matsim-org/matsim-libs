package playground.tschlenther.analysis.modules.taxiTrips;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxi.evaluation.TaxiCustomerWaitTimeAnalyser;
import playground.jbischoff.taxi.evaluation.TravelDistanceTimeEvaluator;
import playground.vsp.analysis.VspAnalyzer;
import playground.vsp.demandde.counts.TSBASt2Count;

public class TaxiAnalysisTest {

	private static final Logger logger = Logger.getLogger(TaxiAnalysisTest.class);

	static final String outputDir = "C:/Users/Tille/WORK/TaxiAnalysisNEU";
	static final String eventsFile ="C:/Users/Tille/WORK/shared-svn/projects/audi_av/scenario/events.out.xml.gz";
	static final String networkFile ="C:/Users/Tille/WORK/shared-svn/projects/audi_av/scenario/networkc.xml.gz";

	public static void main(String[] args){
		Config config = ConfigUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(networkFile);
		Network net = scenario.getNetwork();
//		VspAnalyzer analyzer = new VspAnalyzer(outputDir,eventsFile);
//		analyzer.addAnalysisModule(new TaxiTripsAnalyzer(net));						
//
//		logger.info("----STARTING TO RUN ANALYZER----");
//		analyzer.run();
//		logger.info("VSP-analyzer finished..now prepare to run old version of jbischoff to compare");
		
		EventsManager manager = EventsUtils.createEventsManager();
		TaxiCustomerWaitTimeAnalyser tcwta = new TaxiCustomerWaitTimeAnalyser(scenario);
		manager.addHandler(tcwta);
		TravelDistanceTimeEvaluator evaluator = new TravelDistanceTimeEvaluator(net, 24*3600);
		manager.addHandler(evaluator);
		
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		
		logger.info("starting to read eventsfile");
		reader.readFile(eventsFile);
		
		String currentPath = outputDir + "/bischoff/customerStats/audi_av";
		logger.info("writing bischoff's customer stats to" + currentPath);
		tcwta.writeCustomerWaitStats(currentPath);
		
		currentPath = outputDir + "/bischoff/travelDistance/audi_av";
		logger.info("writing bischoff's travelDistance stats to" + currentPath);
		evaluator.writeTravelDistanceStatsToFiles(currentPath);
		
		logger.info("finito");
		
	}
	
	
}
