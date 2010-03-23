package playground.florian.OTFVis.tests;

import org.matsim.run.OTFVis;

public class T2_LiveVis {
	
	private static String config = "./test/input/playground/florian/OTFVis/config-qsim.xml";
	
	public static void main(String[] args) {
		OTFVis.playConfig(config);
	}

}
