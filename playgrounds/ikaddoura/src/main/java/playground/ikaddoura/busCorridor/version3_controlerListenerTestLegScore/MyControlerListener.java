package playground.ikaddoura.busCorridor.version3_controlerListenerTestLegScore;

import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;

public class MyControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	private FareEventHandler fareEventHandler;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		System.out.println("Startup-Event");
		
		this.fareEventHandler = new FareEventHandler(event.getControler().getEvents(), event.getControler().getPopulation());
		event.getControler().getEvents().addHandler(this.fareEventHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		System.out.println("Iteration-Ends-Event");

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		System.out.println("Shutdown-Event");
	}

}
