package playgroundMeng.ptTravelTimeAnalysis;

import java.io.File;

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

import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.ActivitiesAnalysisInterface;
import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.AllActivitiesAnalysis;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.AreaSplit;
import playgroundMeng.ptAccessabilityAnalysis.areaSplit.GridBasedSplit;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfoCollector;

public class PtTravelTimeAnalysisRunModule extends AbstractModule{
	
	private String configFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_config.xml";
	private String networkFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_network.xml.gz";
	//private String analysisNetworkFile = "C:/Users/VW3RCOM/Desktop/outputNetworkFileNew.xml";
	private String transitFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_transitSchedule.xml.gz";
	private String eventFile =  "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_events.xml.gz";

	@Override
	protected void configure() {
		// prepare Scenario dataBank
		Network network = NetworkUtils.readNetwork(networkFile);
		//Config config = ConfigUtils.loadConfig(configFile);
		Config config = ConfigUtils.createConfig();
		config.qsim().setEndTime(36 * 3600);

		Scenario scenario = ScenarioUtils.createScenario(config);
//				new PopulationReader(scenario).readFile(populationFile);
		new TransitScheduleReader(scenario).readFile(transitFile);
		TransitSchedule transitSchedule = scenario.getTransitSchedule();
		
		TravelTimeConfig travelTimeConfig = new TravelTimeConfig();
		travelTimeConfig.setConfigFile(configFile);
		travelTimeConfig.setEndTime(36*3600);
		travelTimeConfig.setEventFile(eventFile);
		travelTimeConfig.setTimeSlice(6000);
		travelTimeConfig.setNumOfThread(16);
		travelTimeConfig.setOutputDirectory("C:/Users/VW3RCOM/Desktop/travelTime_"+travelTimeConfig.getTimeSlice()+"_"+travelTimeConfig.getGridSlice()+"/");
		File theDir = new File(travelTimeConfig.getOutputDirectory());
		if (!theDir.exists()) {
			theDir.mkdir();
		}
		
		SwissRailRaptorData data = SwissRailRaptorData.create(transitSchedule, RaptorUtils.createStaticConfig(config),network);
		DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), null);
		SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(config), new LeastCostRaptorRouteSelector(), stopFinder);
				
		// bind the injection
		bind(TravelTimeConfig.class).toInstance(travelTimeConfig);
		bind(Network.class).toInstance(network);
		bind(TransitSchedule.class).toInstance(transitSchedule);
		
		bind(SwissRailRaptor.class).toInstance(raptor);
		
	}

}
