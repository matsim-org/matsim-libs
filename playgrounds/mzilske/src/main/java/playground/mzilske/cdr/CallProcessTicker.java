package playground.mzilske.cdr;

import java.util.ArrayList;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Steppable;

public class CallProcessTicker implements BasicEventHandler {

	private EventsManager delegateEventsManager = new EventsManagerImpl();

	private ArrayList<Steppable> steppables = new ArrayList<Steppable>();

	private double currentTime = -1.0;

	public CallProcessTicker() {
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	public void addHandler(EventHandler handler) {
		delegateEventsManager.addHandler(handler);
	}

	@Override
	public void handleEvent(Event event) {
		while (currentTime < event.getTime() - 1) {
			currentTime = currentTime + 1.0;
			for(Steppable steppable : steppables) {
				steppable.doSimStep(currentTime);
			}
		}
		delegateEventsManager.processEvent(event);
	}
	
	public void addSteppable(Steppable steppable) {
		steppables.add(steppable);
	}

	public void finish() {
		currentTime = currentTime + 1.0;
		for(Steppable steppable : steppables) {
			steppable.doSimStep(currentTime);
		}
	}

}
