package playground.christoph.withinday2;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

public class MyControlerListener implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler() ;
		controler.setMobsimFactory(new MyMobsimFactory(controler.createTravelCostCalculator(), controler.getTravelTimeCalculator())) ;
	}

}
