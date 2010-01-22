package playground.gregor.analysis;

import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

public class MkLinkStats {
	
	public static void main(String [] args) {
		String outDir = "../../../arbeit/svn/runs-svn/run316/stage2/output";
		String it = "201";
		String net = outDir + "/output_network.xml.gz";
		String eventsFile = outDir + "/ITERS/it." + it + "/" + it + ".events.txt.gz";
		String stats =  outDir + "/ITERS/it." + it + "/" + it + ".linkstats.txt.gz";
		
		ScenarioImpl scenario = new ScenarioLoaderImpl(outDir + "/output_config.xml.gz").getScenario();
		
		NetworkLayer netzzz = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(net);
		
		EventsManagerImpl events = new EventsManagerImpl();
		VolumesAnalyzer h = new VolumesAnalyzer(60,5*3600,netzzz);
		CalcLinkStats ls = new CalcLinkStats(netzzz);
		events.addHandler(h);
		
		TravelTimeCalculator tt = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(netzzz, scenario.getConfig().travelTimeCalculator());
		events.addHandler(tt);
		new EventsReaderTXTv1(events).readFile(eventsFile);
		ls.addData(h, tt);
		ls.writeFile(stats);
		
		
	}

}
