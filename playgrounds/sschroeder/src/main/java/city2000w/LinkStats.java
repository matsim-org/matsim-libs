package city2000w;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

public class LinkStats {
	
	static class MyEventsHandler extends EventsManagerImpl {
		
	}
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Scenario scen = ScenarioUtils.loadScenario(config);
		
		new MatsimNetworkReader(scen).readFile("/Users/stefan/Documents/workspace/contrib/freight/test/input/org/matsim/contrib/freight/mobsim/EquilWithCarrierTest/testMobsimWithCarrier/network.xml");
		EventsManager manager = new MyEventsHandler();
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
		TravelTimeCalculator ttCalc = new TravelTimeCalculator(scen.getNetwork(), config.travelTimeCalculator());
		
		manager.addHandler(ttCalc);
		
		new MatsimEventsReader(manager).readFile("/Users/stefan/Documents/workspace/matsim/output/ITERS/it.2/2.events.xml.gz");
		
		getTT(ttCalc,"6");
		getTT(ttCalc,"15");
		getTT(ttCalc,"20");
		
	}

	private static void getTT(TravelTimeCalculator ttCalc, String string) {
		System.out.println(string);
		for(int time=21600;time<26000;time+=200){
			System.out.println(time + "; " + ttCalc.getLinkTravelTime(makeId(string), time));
		}
		System.out.println();
		
	}

	private static void getTT(TravelTimeCalculator ttCalc, String from, String to) {
		System.out.println(from+"->"+to);
		for(int time=21600;time<26000;time+=200){
			System.out.println(time + "; " + ttCalc.getLinkToLinkTravelTime(makeId(from), makeId(to), time));
		}
		System.out.println();
	}

	private static Id makeId(String string) {
		return new IdImpl(string);
	}

}
