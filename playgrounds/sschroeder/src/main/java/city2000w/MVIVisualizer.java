package city2000w;

import org.matsim.contrib.otfvis.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {


//		String events = "output/ITERS/it.20/20.events.xml.gz";
//		
//		String mvi =  "/Users/stefan/Documents/workspace/playgrounds/sschroeder/input/vrpScen/eventsGrid/it.100/100.otfvis.mvi";
		
		String mvi =  "output/ITERS/it.20/20.otfvis.mvi";

//		String network = "input/usecases/chessboard/network/grid9x9.xml";
//		
////		OTFVis.playNetwork("input/usecases/chessboard/network/grid9x9.xml");
//
//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);

		OTFVis.playMVI(mvi);
		
		
		
	}
}
