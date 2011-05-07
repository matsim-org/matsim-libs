package tutorial;

import org.matsim.core.controler.Controler;

public class myFirstControler {
	public static void main(String[] args) {
		String configFile = args[0] ;
		Controler controler = new Controler( configFile ) ;
		//controler.setOverwriteFiles(true) ;
		controler.run() ;
	}
}
