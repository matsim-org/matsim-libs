package playgroundMeng.publicTransitServiceAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.GridImp;
import playgroundMeng.publicTransitServiceAnalysis.gridAnalysis.GridCreator;
import playgroundMeng.publicTransitServiceAnalysis.kpiCalculator.GridCalculator;
import playgroundMeng.publicTransitServiceAnalysis.others.ConsoleProgressBar;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class PublicTransitServiceAnalysis {
	private static final Logger logger = Logger.getLogger(PublicTransitServiceAnalysis.class);
	static PtAccessabilityConfig ptAccessabilityConfig;;

	public static void main(String[] args) {
		configure(args);
		GridCreator gridCreator = GridCreator.getInstacne();

		int remain = 0;
		int total = gridCreator.getNum2Grid().values().size();
		String string = "kpiCalculateProgress";
		ConsoleProgressBar.progressPercentage(remain, total, string, logger);

		for (GridImp gridImp : gridCreator.getNum2Grid().values()) {
			GridCalculator.calculateTime2Score(gridImp);
			GridCalculator.calculateTime2Ratio(gridImp);
			GridCalculator.calculateTime2Kpi(gridImp);

			remain++;
			if (total / 10 != 0) {
				if (remain % (total / 10) == 0) {
					ConsoleProgressBar.progressPercentage(remain, total, string, logger);
				} else if (remain == total) {
					ConsoleProgressBar.progressPercentage(remain, total, string, logger);
				}
			}
		}

		try {
			print3DGrafikFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void configure(String[] args) {
		
		String directory = args[0];
		String configFile = directory + args[1];
		String networkFile = directory+args[2];
		String transitFile = directory + args[3];
		String eventFile = directory +args[4];
		int timeSlice = Integer.valueOf(args[5]);
		int girdSlice = Integer.valueOf(args[6]);
		String analysisNetworkFile = null;
		if(!args[7].isEmpty()) {
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

		ptAccessabilityConfig.setOutputDirectory(directory + ptAccessabilityConfig.getAnalysisTimeSlice() + "_"
				+ ptAccessabilityConfig.getAnalysisGridSlice() + "/");
		File theDir = new File(ptAccessabilityConfig.getOutputDirectory());
		if (!theDir.exists()) {
			theDir.mkdir();
		}
	}

	private static void print3DGrafikFile() throws IOException {
		File file1 = new File(ptAccessabilityConfig.getOutputDirectory() + "3dGrafikOrigin.csv");
		File file2 = new File(ptAccessabilityConfig.getOutputDirectory() + "3dGrafikDestination.csv");
		FileWriter fileWriter1 = new FileWriter(file1);
		BufferedWriter bufferedWriter1 = new BufferedWriter(fileWriter1);
		FileWriter fileWriter2 = new FileWriter(file2);
		BufferedWriter bufferedWriter2 = new BufferedWriter(fileWriter2);

		bufferedWriter1.write("District,Latitude,longitude,time,ratio,score,kpi,numOfTrips,numofNoPtTrips");
		bufferedWriter2.write("District,Latitude,longitude,time,ratio,score,kpi,numOfTrips,numofNoPtTrips");
		for (int x = 0; x < 24 * 3600; x += ptAccessabilityConfig.getAnalysisTimeSlice()) {
			int h = (int) (x / 3600);
			int m = (int) ((x - h * 3600) / 60);
			int s = (int) (x - h * 3600 - m * 60);
			String time = "," + timeConvert(h) + ":" + timeConvert(m) + ":" + timeConvert(s);

			for (String string : GridCreator.getInstacne().getNum2Grid().keySet()) {
				bufferedWriter1.newLine();
				bufferedWriter2.newLine();
				bufferedWriter1.write(string + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[0] + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[1] + time + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2RatioOfOrigin().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2Score().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2OriginKpi().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumTripsOfOrigin().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumNoPtTripsOfOrigin().get(x));

				bufferedWriter2.write(string + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[0] + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getCoordinate()[1] + time + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2RatioOfDestination().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2Score().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2DestinationKpi().get(x) + ","
						+ GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumTripsOfDestination().get(x)
						+ "," + GridCreator.getInstacne().getNum2Grid().get(string).getTime2NumNoPtTripsOfDestination()
								.get(x));
			}
		}
		bufferedWriter1.close();
		bufferedWriter2.close();
	}

	private static String timeConvert(int a) {
		if (a < 10) {
			return "0" + a;
		} else {
			return String.valueOf(a);
		}
	}

}
