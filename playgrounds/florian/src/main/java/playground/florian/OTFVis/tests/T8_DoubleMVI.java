package playground.florian.OTFVis.tests;

import org.matsim.vis.otfvis.OTFDoubleMVI;

public class T8_DoubleMVI {
	
	private static String mviFile1 = "./Output/OTFVisTests/Sim/ITERS/it.1/1.otfvis.mvi";
	private static String mviFile2 = "./Output/OTFVisTests/QSim/ITERS/it.1/1.otfvis.mvi";

	public static void main(String[] args) {
		new OTFDoubleMVI(mviFile1, mviFile2).start();
	}

}
