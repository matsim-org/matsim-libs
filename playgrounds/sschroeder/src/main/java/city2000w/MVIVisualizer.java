package city2000w;

import org.matsim.run.OTFVis;

public class MVIVisualizer {
	public static void main(String[] args) {
		String network = "/Volumes/projekte/LogoTakt/SaWu/verkehrsangebot/germany_bigroads_fused.xml";
		String events = "output/ITERS/it.0/0.events.xml.gz";
		String mvi = "../logotakt_sebastian/output/stueckgut.mvi";

		
//		OTFVis.playNetwork(network);

//		String[] arguments = { "-convert",events, network, mvi, "60" };
//		OTFVis.convert(arguments);

		
		OTFVis.playMVI(mvi);

	}
}
