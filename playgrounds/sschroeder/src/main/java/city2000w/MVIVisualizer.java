package city2000w;

import org.matsim.run.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {
		String network = "../playgrounds/sschroeder/vrp/grid1000.xml";
		String events = "output/ITERS/it.0/0.events.xml.gz";
		String mvi =  "../playgrounds/sschroeder/vrp/vrp_vcn2.mvi";

		
//		OTFVis.playNetwork(network);

//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);

		
		OTFVis.playMVI(mvi);

	}
}
