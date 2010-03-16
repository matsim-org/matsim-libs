package playground.florian.OTFVis.tests;

import org.matsim.core.controler.Controler;

public class StartControler {

	private static String config = "./src/main/java/playground/florian/OTFVis/tests/config.xml";
	
	public static void main(String[] args) {
		Controler con = new Controler(config);
		con.setOverwriteFiles(true);
		con.run();
	}

}
