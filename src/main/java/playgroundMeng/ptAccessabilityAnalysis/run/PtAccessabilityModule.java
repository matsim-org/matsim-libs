package playgroundMeng.ptAccessabilityAnalysis.run;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.AbstractModule;

import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.AllActivitiesAnalysis;
import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.ActivitiesAnalysisInterface;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.AreaSplit;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.GridBasedSplit;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfoCollector;

public class PtAccessabilityModule extends AbstractModule{
	
	private String configFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_config.xml";
	private String networkFile = "C:/Users/VW3RCOM/Desktop/outputNetworkFileFinal.xml";
	private String analysisNetworkFile = "C:/Users/VW3RCOM/Desktop/outputNetworkFileNew.xml";
	private String transitFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_transitSchedule.xml.gz";
	//private String transitFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/test-Transit.xml";
	private String shapeFile = "C:/Users/VW3RCOM/Documents/shp/Hannover_Stadtteile.shp";
	//private String eventFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN//0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_events.xml.gz";
	private String eventFile = "C:/Users/VW3RCOM/Documents/2019-10-19_13-50-24__vw280_100pct.output_events.xml.gz";

	@Override
	protected void configure() {

		// prepare Scenario dataBank
		Network network = NetworkUtils.readNetwork(networkFile);		
//		Config config = ConfigUtils.loadConfig(configFile);
		Config config = ConfigUtils.createConfig();
		config.qsim().setEndTime(36*3600);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
//		new PopulationReader(scenario).readFile(populationFile);
		Population population = scenario.getPopulation();
		new TransitScheduleReader(scenario).readFile(transitFile);
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		
	
		// configure the analysisConfig
		PtAccessabilityConfig ptAccessabilityConfig = new PtAccessabilityConfig();
		// shape or grid?
		//ptAccessabilityConfig.setShapeFile(shapeFile);
		ptAccessabilityConfig.setAnalysisGridSlice(500);
		// timeSlice?
		ptAccessabilityConfig.setAnalysisTimeSlice(3600);
		ptAccessabilityConfig.setBeginnTime(0);
		ptAccessabilityConfig.setEndTime(config.qsim().getEndTime());
		// modeDistance?
		Map<String, Double> modeDistance = new HashedMap();
		modeDistance.put("bus", 250.);
		modeDistance.put("tram", 500.);
		modeDistance.put("rail", 1000.);
		modeDistance.put("pt", 1200.);
		modeDistance.put("train", 1200.);
		ptAccessabilityConfig.setModeDistance(modeDistance);
		// modeScore
		Map<String, Double> modeScore = new HashedMap();
		modeScore.put("bus", 1.);
		modeScore.put("tram", 1.);
		modeScore.put("rail", 1.);
		modeScore.put("pt", 1.);
		modeScore.put("train", 1.);
		ptAccessabilityConfig.setModeScore(modeScore);
		//anaylsis Network
		ptAccessabilityConfig.setAnalysisNetworkFile(analysisNetworkFile);
			
		// print?
		ptAccessabilityConfig.setIfWriteLinksInfo(false);
		ptAccessabilityConfig.setIfWriteStopInfo(true);
		ptAccessabilityConfig.setWriteNetworkChangeEventForEachArea(false);
		ptAccessabilityConfig.setWriteArea2Time2Score(true);
		
		// output
		ptAccessabilityConfig.setOutputDirectory("C:/Users/VW3RCOM/Desktop/ptAnalysisOutputFileGrid_"+ptAccessabilityConfig.getAnalysisTimeSlice()+"_"+ptAccessabilityConfig.getAnalysisGridSlice()+"/");
		File theDir = new File(ptAccessabilityConfig.getOutputDirectory());
		if (!theDir.exists()) {
			theDir.mkdir();
		}
		//activities?
		ptAccessabilityConfig.setEventFile(eventFile);
		ptAccessabilityConfig.setConsiderActivities(true);
		ptAccessabilityConfig.setWriteArea2Time2Activities(true);
		
		
		// bind the injection
		bind(Network.class).toInstance(network);
		bind(Population.class).toInstance(population);
		bind(TransitSchedule.class).toInstance(transitSchedule);
		bind(Config.class).toInstance(config);
		bind(PtAccessabilityConfig.class).toInstance(ptAccessabilityConfig);
		bind(ActivitiesAnalysisInterface.class).to(AllActivitiesAnalysis.class);
		bind(RouteStopInfoCollector.class).asEagerSingleton();
		bind(AreaSplit.class).to(GridBasedSplit.class).asEagerSingleton();
		//bind(AreaSplit.class).to(DistrictBasedSplit.class);
		
		
	}


}
