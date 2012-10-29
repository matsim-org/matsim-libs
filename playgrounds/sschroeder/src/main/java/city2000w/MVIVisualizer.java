package city2000w;

import org.matsim.contrib.otfvis.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {


		String events = "/Users/schroeder/Documents/workspace/playgrounds/sschroeder/output/ITERS/it.1/1.events.xml.gz";
//		
//		String mvi =  "/Users/stefan/Documents/workspace/playgrounds/sschroeder/input/vrpScen/eventsGrid/it.100/100.otfvis.mvi";
		String mvi =  "/Users/schroeder/Documents/workspace/playgrounds/sschroeder/output/ITERS/it.1/1.otfvis.mvi";

		String network = "/Users/schroeder/Documents/workspace/sschroeder_vrp/input/grid10.xml";
		
//		OTFVis.playNetwork("/Users/schroeder/Documents/workspace/sschroeder_vrp/input/grid10.xml");

		String[] arguments = { "-convert",events, network, mvi, "60" };
		OTFVis.convert(arguments);

		OTFVis.playMVI(mvi);
		
		
		
	}
}
