package playground.mkillat.pt_test;

import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFFileWriterFactory;


public class MyControlerPt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFile = "./input/bus_berlin/config.xml" ;		
//		String configFile = "./input/bus_test2/config.xml" ;
		Controler controler1 = new Controler( configFile ) ;
		controler1.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler1.addOverridingModule(new OTFVisModule());

		controler1.run();
		
		System.out.println("Die Simulation ist fertig");
		

	}

}
