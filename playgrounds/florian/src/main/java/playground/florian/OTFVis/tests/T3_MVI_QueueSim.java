package playground.florian.OTFVis.tests;

import org.matsim.run.OTFVis;

public class T3_MVI_QueueSim {

	private static final String mviFile = "./Output/OTFVisTests/Sim/ITERS/it.1/1.otfvis.mvi";
	
	public static void main(String[] args) {
		String[] movies = new String[1];
		movies[0] = mviFile;
		OTFVis.playMVI(movies);
	}
}
