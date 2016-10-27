package saleem.stockholmscenario.teleportation.gaming;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

public class FareControlListener implements StartupListener{

	@Override
	public void notifyStartup(StartupEvent event) {
		EventsManager eventmanager = event.getServices().getEvents();
		FareControlHandler farehandler = new FareControlHandler(eventmanager);
		event.getServices().getEvents().addHandler(farehandler);
	}

}
