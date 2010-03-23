package playground.florian.OTFVis;

import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.executables.OTFVisController;

public class LiveStarter {
	
	private static String config = "./test/input/playground/florian/Equil/config_mvi.xml";
	
	public static void main(String[] args) {
		OTFVis.playConfig(config);
	}
}
