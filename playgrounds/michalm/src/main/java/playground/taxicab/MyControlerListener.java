package playground.taxicab;

import org.matsim.core.controler.*;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;

public class MyControlerListener implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler() ;
		controler.setMobsimFactory(new MyMobsimFactory()) ;
	}

}
