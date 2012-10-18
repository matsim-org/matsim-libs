package city2000w;

import org.matsim.contrib.otfvis.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {


//		String events = "/Users/stefan/Documents/workspace/matsim/output/ITERS/it.100/100.events.xml.gz";
//		
//		String mvi =  "/Users/stefan/Documents/workspace/playgrounds/sschroeder/input/vrpScen/eventsGrid/it.100/100.otfvis.mvi";
//		String mvi =  "/Users/stefan/Documents/workspace/matsim/output/ITERS/it.100/100.otfvis.mvi";

		
		
		OTFVis.playNetwork("/Users/stefan/Documents/workspace/sschroeder_vrp/test/matsim/network.xml");

//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);
		
		
//		
//		OTFVis.playMVI(mvi);
		
		
		
	}
}
