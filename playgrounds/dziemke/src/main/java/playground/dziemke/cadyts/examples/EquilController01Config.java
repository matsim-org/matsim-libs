package playground.dziemke.cadyts.examples;

import org.matsim.core.controler.Controler;

public class EquilController01Config {

	public static void main(final String[] args) {
		String configFile = "D:/Workspace/container/examples/equil/input/config.xml" ;
		Controler controler = new Controler(configFile);
		controler.run() ;
	}
}