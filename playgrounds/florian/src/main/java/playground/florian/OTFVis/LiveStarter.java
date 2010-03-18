package playground.florian.OTFVis;

import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.executables.OTFVisController;

public class LiveStarter {
	
//	private static String config = "./src/main/java/playground/florian/Equil/config_live.xml";
	private static String config=null;
	
	
	public static void main(String[] args) {
		OTFVis.playConfig(config);
	}
}
