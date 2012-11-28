package playground.christoph.withinday2;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

public class MyControlerListener implements IterationStartsListener {

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler controler = event.getControler() ;
		controler.setMobsimFactory(new MyMobsimFactory(controler.getTravelDisutilityFactory(), controler.getMultiModalTravelTimes()));		
	}

}
