package playgroundMeng.publicTransitServiceAnalysis.run;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import playgroundMeng.publicTransitServiceAnalysis.gridAnalysis.GridCreator;
import playgroundMeng.publicTransitServiceAnalysis.kpiCalculator.GridCalculator;

public class PublicTransitServiceAnalysis {
	private static final Logger logger = Logger.getLogger(PublicTransitServiceAnalysis.class);
	private static PtAccessabilityConfig ptAccessabilityConfig;
	
	private static void configure(String[] args) {

		String directory = args[0];
		String configFile = directory + args[1];
		String networkFile = directory + args[2];
		String transitFile = directory + args[3];
		String eventFile = directory + args[4];
		int timeSlice = Integer.valueOf(args[5]);
		int girdSlice = Integer.valueOf(args[6]);
		String analysisNetworkFile = null;
		if (args.length == 8) {
			analysisNetworkFile = directory + args[7];
		}

		ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
		ptAccessabilityConfig.setConfigFile(configFile);
		ptAccessabilityConfig.setNetworkFile(networkFile);
		ptAccessabilityConfig.setTransitFile(transitFile);
		ptAccessabilityConfig.setEventFile(eventFile);
		ptAccessabilityConfig.setAnalysisNetworkFile(analysisNetworkFile);

		ptAccessabilityConfig.setAnalysisGridSlice(girdSlice);
		// timeSlice?
		ptAccessabilityConfig.setAnalysisTimeSlice(timeSlice);
		ptAccessabilityConfig.setBeginnTime(0);
		ptAccessabilityConfig.setEndTime(36 * 3600);

		Network network = NetworkUtils.readNetwork(networkFile);
		Config config = ConfigUtils.createConfig();
		ptAccessabilityConfig.setNetwork(network);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(transitFile);
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		ptAccessabilityConfig.setTransitSchedule(transitSchedule);

		// modeDistance?
		Map<String, Double> modeDistance = new HashedMap();
		modeDistance.put("bus", 250.);
		modeDistance.put("tram", 500.);
		modeDistance.put("rail", 1000.);
		modeDistance.put("pt", 1200.);
		modeDistance.put("train", 1200.);
		ptAccessabilityConfig.setModeDistance(modeDistance);

		Map<String, Double> modeScore = new HashedMap();
		modeScore.put("bus", 1.);
		modeScore.put("tram", 1.);
		modeScore.put("rail", 1.);
		modeScore.put("pt", 1.);
		modeScore.put("train", 1.);
		ptAccessabilityConfig.setModeScore(modeScore);

		ptAccessabilityConfig.setParkingTime(5 * 60);
		List<String> consideredModes = new ArrayList<String>();
		consideredModes.add("pt");
		consideredModes.add("car");
		ptAccessabilityConfig.setConsideredModes(consideredModes);

		ptAccessabilityConfig.setOutputDirectory(directory + ptAccessabilityConfig.getAnalysisTimeSlice() + "_"
				+ ptAccessabilityConfig.getAnalysisGridSlice() + "/");
		File theDir = new File(ptAccessabilityConfig.getOutputDirectory());
		if (!theDir.exists()) {
			theDir.mkdir();
		}
	}
	
	public static void main(String[] args) throws Exception {
		configure(args);
		GridCreator gridCreator = GridCreator.getInstacne();
		GridCalculator.calculate(gridCreator);

		logger.info("beginn to print the result");
		ResultPrinter.print3DGrafikFile();
		ResultPrinter.printTripInfo();
		logger.info("finished");
	}


	

}
