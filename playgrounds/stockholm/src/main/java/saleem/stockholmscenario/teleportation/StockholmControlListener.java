package saleem.stockholmscenario.teleportation;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

public class StockholmControlListener implements StartupListener, IterationEndsListener{
	HandleStuckVehicles handler;
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		handler.printStuckPersonsAndVehicles();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		handler = new HandleStuckVehicles();
		event.getControler().getEvents().addHandler(handler);		
		
	}

}
