package playground.dziemke.other;

import org.matsim.core.controler.Controler;

public class PtControler {
	
	// just runs the pt tutorial

	public static void main(final String[] args) {
//		String configFile = "D:/Workspace/container/pt-example/input/config.xml" ;
		String configFile = "/Users/dominik/Workspace/data/pt-tutorial/0.config.xml" ;
		Controler controler = new Controler(configFile) ;
		controler.run() ;
	}
}