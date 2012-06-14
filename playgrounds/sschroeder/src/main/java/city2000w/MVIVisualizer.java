package city2000w;

import org.matsim.contrib.otfvis.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {
//		String network = "/Users/stefan/Documents/workspace/contrib/freight/test/input/org/matsim/contrib/freight/mobsim/EquilWithCarrierTest/testMobsimWithCarrier/network.xml";
		String network = "/Volumes/projekte/2000-Watt-City/Berlin/network.xml";
		String events = "/Users/stefan/Documents/workspace/matsim/output/ITERS/it.100/100.events.xml.gz";
		
		String mvi =  "/Users/stefan/Documents/workspace/playgrounds/sschroeder/input/vrpScen/eventsGrid/it.100/100.otfvis.mvi";
//		String mvi =  "/Users/stefan/Documents/workspace/matsim/output/ITERS/it.100/100.otfvis.mvi";

		
//		/Users/stefan/Documents/workspace/playgrounds/mzilske/output/grid10.xml
		OTFVis.playNetwork(network);

//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);
		
		
//		
//		OTFVis.playMVI(mvi);
		
		
	}
}
