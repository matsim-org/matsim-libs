package playground.florian.OTFVis;

import org.matsim.run.OTFVis;

public class LiveStarter {
	
	private static String config = "./test/input/playground/florian/Equil/config_mvi.xml";
	
	public static void main(String[] args) {
		OTFVis.playConfig(config);
	}
}
