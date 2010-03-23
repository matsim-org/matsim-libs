package playground.florian.OTFVis.tests;

import org.matsim.core.controler.Controler;

public class T0_StartControler {
	
	private static String config = "./test/input/playground/florian/OTFVis/config.xml";
	private static String config2 = "./test/input/playground/florian/OTFVis/config-qsim.xml";
	
	public static void main(String[] args) {
		Controler con = new Controler(config);
		con.setOverwriteFiles(true);
		con.run();
		System.out.println("\n Queue-Sim is done. Output:" + con.getConfig().controler().getOutputDirectory());
		con = new Controler(config2);
		con.setOverwriteFiles(true);
		con.run();
		System.out.println("\n QSim is done. Output:" + con.getConfig().controler().getOutputDirectory());
	}

}
