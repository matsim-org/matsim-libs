package city2000w;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

public class MVIVisualizer {
	public static void main(String[] args) {
//		String network = "/Users/stefan/Documents/workspace/contrib/freight/test/input/org/matsim/contrib/freight/mobsim/EquilWithCarrierTest/testMobsimWithCarrier/network.xml";
		String network = "/Users/stefan/Documents/workspace/playgrounds/sschroeder/input/vrpScen/grid10Broadway.xml";
		String events = "/Users/stefan/Documents/workspace/matsim/output/ITERS/it.100/100.events.xml.gz";
		
		String mvi =  "/Users/stefan/Documents/workspace/playgrounds/sschroeder/input/vrpScen/eventsGrid/it.100/100.otfvis.mvi";
//		String mvi =  "/Users/stefan/Documents/workspace/matsim/output/ITERS/it.100/100.otfvis.mvi";

		
//		/Users/stefan/Documents/workspace/playgrounds/mzilske/output/grid10.xml
//		OTFVis.playNetwork(network);

//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);
		
		
//		
		OTFVis.playMVI(mvi);
		
		
	}
}
