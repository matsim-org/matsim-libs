package saleem.gaming.scenariobuilding;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
/**
 * A control listener class aimed at enforcing PT fare. Notifies the fare hanlder class.
 * 
 * @author Mohammad Saleem
 */
public class FareControlListener implements StartupListener{

	@Override
	public void notifyStartup(StartupEvent event) {
		EventsManager eventmanager = event.getServices().getEvents();
		FareControlHandler farehandler = new FareControlHandler(eventmanager);
		event.getServices().getEvents().addHandler(farehandler);
	}

}
