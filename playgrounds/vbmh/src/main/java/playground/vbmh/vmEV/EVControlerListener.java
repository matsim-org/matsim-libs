package playground.vbmh.vmEV;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

public class EVControlerListener implements IterationEndsListener,
		IterationStartsListener, StartupListener {
	
	private EVHandler evHandler = new EVHandler();

	@Override
	public void notifyStartup(StartupEvent event) {
		// TODO Auto-generated method stub
		event.getServices().getEvents().addHandler(getEvHandler());

	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		// TODO Auto-generated method stub
		this.evHandler.getEvControl().iterStart();

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// TODO Auto-generated method stub

	}

	public EVHandler getEvHandler() {
		return evHandler;
	}

}
