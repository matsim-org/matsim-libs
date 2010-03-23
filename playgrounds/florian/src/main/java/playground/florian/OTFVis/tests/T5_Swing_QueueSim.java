package playground.florian.OTFVis.tests;

import org.matsim.vis.otfvis.OTFClientSwing;

public class T5_Swing_QueueSim {
	
	private static final String mviFile = "./Output/OTFVisTests/Sim/ITERS/it.1/1.otfvis.mvi";
	
	public static void main(String[] args) {
		new OTFClientSwing(mviFile).run();
	}

}
