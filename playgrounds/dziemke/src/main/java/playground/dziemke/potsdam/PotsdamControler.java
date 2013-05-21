package playground.dziemke.potsdam;

// now (02-13) use other controler
// import org.matsim.run.Controler;
import org.matsim.core.controler.Controler;

import playground.kai.run.KaiAnalysisListener;

public class PotsdamControler {

	public static void main(final String[] args) {
		// String configFile = "./input/potsdam/Config_test.xml" ;
		String configFile = "D:/Workspace/container/potsdam-pg/config/Config_test5.xml" ;
		
		Controler controler = new Controler( configFile ) ;
		
		// (02-13) outcommented:
		// controler.setOverwriteFiles(true) ;
		
		
		// new
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		controler.run() ;
		// (original 2011) outcommented:
		/// controler.addControlerListener
		}
}