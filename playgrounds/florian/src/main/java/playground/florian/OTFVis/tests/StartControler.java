package playground.florian.OTFVis.tests;

import java.util.Arrays;

import org.matsim.core.controler.Controler;

public class StartControler {

	private static String config = "./test/input/playground/florian/OTFVis/config.xml";
	
	public static void main(String[] args) {
		Controler con = new Controler(config);
		con.setOverwriteFiles(true);
		con.getConfig().controler().setSnapshotFormat(Arrays.asList("otfvis"));
		con.getConfig().qsim().setSnapshotPeriod(60.0);
		con.getConfig().qsim().setSnapshotStyle("queue");
		con.run();
	}

}
