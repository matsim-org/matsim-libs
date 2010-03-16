package playground.florian.OTFVis;

import org.matsim.core.controler.Controler;
import org.matsim.run.OTFVis;
import org.matsim.vis.otfvis.OTFClientSwing;


public class MviStarter {

	private static String config = "./src/main/java/playground/florian/OTFVis/tests/config.xml";
	private static String mviFile = "./src/main/java/playground/florian/MVIs/500.events.mvi";
	
	public static void main(String[] args) {
//		Controler con = new Controler(config);
//		con.setOverwriteFiles(true);
//		con.run();
		String[] movies = new String[2];
		movies[0] = mviFile;
//		OTFVis.playMVI(movies);
		new OTFClientSwing(mviFile).start();
	}

}
