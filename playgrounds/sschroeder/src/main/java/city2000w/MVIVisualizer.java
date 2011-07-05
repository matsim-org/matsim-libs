package city2000w;

import org.matsim.run.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {
		String network = "../playgrounds/sschroeder/networks/karlsruheNetwork.xml";
		String events = "output/ITERS/it.0/0.events.xml.gz";
		String mvi = "../Diplomarbeit_Matthias/output/sandbox.mvi";

		
//		OTFVis.playNetwork(network);

//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);
		
		
		OTFVis.playMVI(mvi);

	}
}
