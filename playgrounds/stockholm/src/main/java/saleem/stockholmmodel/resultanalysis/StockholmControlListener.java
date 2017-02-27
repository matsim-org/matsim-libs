package saleem.stockholmmodel.resultanalysis;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
/**
 * A control listener for stuck vehicles and persons.
 * 
 * @author Mohammad Saleem
 *
 */
public class StockholmControlListener implements StartupListener, IterationEndsListener{
	HandleStuckVehicles handler;
	/**
	 * Notifies all observers of the Controler that an iteration is finished
	 * @param event
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		handler.printStuckPersonsAndVehicles();
		// TODO Auto-generated method stub
		
	}
	/**
	 * Notifies all observers that the controler is initialized and they should do the same
	 *
	 * @param event
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		handler = new HandleStuckVehicles();
		event.getServices().getEvents().addHandler(handler);
		
	}

}
