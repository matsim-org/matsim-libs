package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

class MyControler {
	
	public static void main ( String[] args ) {
		Logger.getLogger("blabla").warn("here") ;

		Controler controler = new Controler( args ) ;

		controler.setOverwriteFiles(true) ;
		
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		controler.run();
	
	}

}
