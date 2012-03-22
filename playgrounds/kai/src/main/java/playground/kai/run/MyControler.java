package playground.kai.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;

class MyControler {
	
	public static void main ( String[] args ) {

		Controler controler = new Controler( args ) ;

		controler.setOverwriteFiles(true) ;
		
		ControlerListener myControlerListener = new KaiAnalysisListener() ;
		controler.addControlerListener(myControlerListener) ;
		
		controler.run();
	
	}

}
