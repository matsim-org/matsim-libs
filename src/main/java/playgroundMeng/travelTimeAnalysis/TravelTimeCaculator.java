package playgroundMeng.travelTimeAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator.Builder;
import org.matsim.vehicles.Vehicle;

public class TravelTimeCaculator implements TravelTime{
	private static final Logger logger = Logger.getLogger(TravelTimeCaculator.class);
	
	String eventsFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN//0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_events.xml.gz";
	String networkFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN//0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_network.xml.gz";
	String configFile = "W:/08_Temporaere_Mitarbeiter/082_K-GGSN/0822_Praktikanten/Meng/vw243_cadON_ptSpeedAdj.0.1/vw243_cadON_ptSpeedAdj.0.1.output_config.xml";
	
	TravelTimeCalculator ttCalculator;
	
	public TravelTimeCaculator() {
		this.run();
	}
	public void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getVehicles().getVehicleTypes().values();
				
		EventsManager eventsManager = EventsUtils.createEventsManager();
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
		
	
		Builder builder = new TravelTimeCalculator.Builder(network);
		int maxTime = (int)config.qsim().getEndTime();
		int binSize = 900;
		
		TravelTimeCalculatorConfigGroup ttcConfig = new TravelTimeCalculatorConfigGroup();
		ttcConfig.setMaxTime(maxTime);
		ttcConfig.setTraveltimeBinSize(binSize);
		
		builder.configure(ttcConfig );
		ttCalculator = builder.build();
		eventsManager.addHandler(ttCalculator);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		
		reader.readFile(eventsFile);	
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		
		return this.ttCalculator.getLinkTravelTimes().getLinkTravelTime(link, time, person, vehicle);
	}
	
//	public static void printInfo(String string, Network network, TravelTimeCalculator timeCalculator, double maxTime, double timeBin) {
//		
//		File file = new File(string);
//		try {
//			FileWriter fileWriter = new FileWriter(file);
//			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
//			
//			bufferedWriter.write("LinkId/TimeBeginn,");
//			
//			for(int a = 0; a < maxTime; a += timeBin) {
//				bufferedWriter.write(a+"-" + (a+timeBin) + ",");
//			}
//			
//			
//			for(Link link : network.getLinks().values()) {
//				bufferedWriter.newLine();
//				bufferedWriter.append(link.getId().toString());
//				for(int a = 0; a < maxTime; a += timeBin) {
//					double tt = timeCalculator.getLinkTravelTimes().getLinkTravelTime(link, a, null, null);
//					bufferedWriter.write(","+tt);
//				}
//			}
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} 
//	}


}
