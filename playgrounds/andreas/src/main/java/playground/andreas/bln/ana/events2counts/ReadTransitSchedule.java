package playground.andreas.bln.ana.events2counts;

import org.apache.log4j.Logger;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


public class ReadTransitSchedule {

	private static final Logger log = Logger.getLogger(ReadTransitSchedule.class);
	
	public static TransitSchedule readTransitSchedule(String networkFile, String transitScheduleFile) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario);
		matsimNetReader.readFile(networkFile);
		
		return ReadTransitSchedule.readTransitSchedule(scenario.getNetwork(), transitScheduleFile);
	}
	
	public static TransitSchedule readTransitSchedule(NetworkImpl network, String transitScheduleFile) {
		TransitSchedule transitSchedule = new TransitScheduleFactoryImpl().createTransitSchedule();
		TransitScheduleReaderV1 transitScheduleReaderV1 = new TransitScheduleReaderV1(transitSchedule, network);
		transitScheduleReaderV1.readFile(transitScheduleFile);
	
		return transitSchedule;
	}
	
}