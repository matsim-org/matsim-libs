package playgroundMeng.ptAccessabilityAnalysis.run;

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
	private String networkFile = "C:/Users/VW3RCOM/git/matsim-code-examples/outputNetworkFileFinal.xml";
	private String analysisNetworkFile = "C:/Users/VW3RCOM/git/matsim-code-examples/outputNetworkFileNew.xml";
	private String transitFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_transitSchedule.xml.gz";
	//private String transitFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/test-Transit.xml";
	private String shapeFile = "C:/Users/VW3RCOM/Documents/shp/Hannover_Stadtteile.shp";
	//private String populationFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_plans.xml.gz";
	
	//String configFile = "W:\\08_Temporaere_Mitarbeiter\\082_K-GGSN\\0822_Praktikanten\\Meng\\VIA\\via-sampledata\\via-sampledata\\transit-tutorial\\config.xml";
	//String transitFile = "C:/Users/VW3RCOM/Desktop/pt_exsample/transitschedule.xml";
	//String networkFile = "W:\\08_Temporaere_Mitarbeiter\\082_K-GGSN\\0822_Praktikanten\\Meng\\VIA\\via-sampledata\\via-sampledata\\transit-tutorial\\outputNetworkFileTest.xml";

	@Override
	protected void configure() {

		// prepare Scenario dataBank
		Network network = NetworkUtils.readNetwork(networkFile);		
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.createScenario(config);
//		new PopulationReader(scenario).readFile(populationFile);
		Population population = scenario.getPopulation();
		new TransitScheduleReader(scenario).readFile(transitFile);
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		
	
		// configure the analysisConfig
		PtAccessabilityConfig ptAccessabilityConfig = new PtAccessabilityConfig();
		// shape or grid?
		//ptAccessabilityConfig.setShapeFile(shapeFile);
		ptAccessabilityConfig.setAnalysisGridSlice(30);
		// timeSlice?
		ptAccessabilityConfig.setAnalysisTimeSlice(3600);
		ptAccessabilityConfig.setBeginnTime(0);
		ptAccessabilityConfig.setEndTime(config.qsim().getEndTime());
		// analysisModes?
		List<String> modes = new ArrayList<String>();
		modes.add("bus"); modes.add("rail"); modes.add("tram");
		modes.add("pt"); modes.add("train");
		ptAccessabilityConfig.setAnalysisModes(modes);
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
		//minx miny maxx maxy
		Network network2 = NetworkUtils.readNetwork(analysisNetworkFile);
		LinkedList<Double> xLinkedList = new LinkedList<Double>();
		LinkedList<Double> yLinkedList = new LinkedList<Double>();
		for(org.matsim.api.core.v01.network.Node node : network2.getNodes().values()) {
			xLinkedList.add(node.getCoord().getX());
			yLinkedList.add(node.getCoord().getY());
		}
			ptAccessabilityConfig.setMinx(Collections.min(xLinkedList)+0.25*(Collections.max(xLinkedList)-Collections.min(xLinkedList)));  
			ptAccessabilityConfig.setMiny(Collections.min(yLinkedList)+0.25*(Collections.max(yLinkedList)-Collections.min(yLinkedList)));
			ptAccessabilityConfig.setMaxx(Collections.max(xLinkedList)-0.25*(Collections.max(xLinkedList)-Collections.min(xLinkedList)));
			ptAccessabilityConfig.setMaxy(Collections.max(yLinkedList)-0.25*(Collections.max(yLinkedList)-Collections.min(yLinkedList)));
			
//			ptAccessabilityConfig.setMinx(600018);  
//			ptAccessabilityConfig.setMiny(579000);
//			ptAccessabilityConfig.setMaxx(602000);
//			ptAccessabilityConfig.setMaxy(579600);
		// print?
		ptAccessabilityConfig.setIfWriteLinksInfo(false);
		ptAccessabilityConfig.setIfWriteStopInfo(true);
		ptAccessabilityConfig.setWriteNetworkChangeEventForEachArea(false);
		ptAccessabilityConfig.setWriteArea2Time2Score(true);
		
		// output
		ptAccessabilityConfig.setOutputDirectory("C:/Users/VW3RCOM/Desktop/ptAnalysisOutputFileGrid_"+ptAccessabilityConfig.getAnalysisTimeSlice()+"_"+ptAccessabilityConfig.getAnalysisGridSlice()*ptAccessabilityConfig.getAnalysisGridSlice()+"/");
		
		//activities?
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
		bind(AreaSplit.class).to(GridBasedSplit.class);
		//bind(AreaSplit.class).to(DistrictBasedSplit.class);
		
		
	}


}
