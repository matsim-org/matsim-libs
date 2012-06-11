package playground.mkillat.pt_test;

import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;


public class MyControlerPt {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String configFile = "./input/bus_test2/config.xml" ;		
		Controler controler1 = new Controler( configFile ) ;
		controler1.setOverwriteFiles(true) ;
		controler1.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());

		controler1.run();
		
		System.out.println("Die Simulation ist fertig");
		

	}

}
