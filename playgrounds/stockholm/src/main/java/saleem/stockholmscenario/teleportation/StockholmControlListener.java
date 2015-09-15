package saleem.stockholmscenario.teleportation;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class StockholmControlListener implements IterationStartsListener, IterationEndsListener{
	HandleStuckVehicles handler;
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		handler.printStuckPersonsAndVehicles();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		handler = new HandleStuckVehicles();
		event.getControler().getEvents().addHandler(handler);		
	}

}
