package city2000w;

import org.matsim.run.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {
		String network = "/Users/stefan/Documents/workspace/Diplomarbeit_Matthias/input/grid.xml";
		String events = "../Diplomarbeit_Matthias/output/ITERS/it.0/0.events.xml.gz";
		String mvi = "../Diplomarbeit_Matthias/output/sandbox.mvi";

		
//		OTFVis.playNetwork(network);

//		String[] arguments = { "-convert",events, network, mvi, "10" };
//		OTFVis.convert(arguments);
		
		OTFVis.playMVI(mvi);

	}
}
