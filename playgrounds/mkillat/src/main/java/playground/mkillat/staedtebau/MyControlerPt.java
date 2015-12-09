package playground.mkillat.staedtebau;

import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFFileWriterFactory;


public class MyControlerPt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String configFile = "./input/staedtebau/config.xml" ;
		String configFile = "./input/staedtebau/configM.xml" ;
//		String configFile = "./input/bus_test2/config.xml" ;
		Controler controler1 = new Controler( configFile ) ;
		controler1.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler1.addOverridingModule(new OTFVisFileWriterModule());

		controler1.run();
		
		System.out.println("Die Simulation ist fertig");
		

	}

}
