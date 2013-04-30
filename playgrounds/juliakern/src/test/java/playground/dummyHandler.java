package playground;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

public class dummyHandler implements EventsManager {

	@Override
	public EventsFactory getFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processEvent(Event event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addHandler(EventHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeHandler(EventHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetCounter() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetHandlers(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearHandlers() {
		// TODO Auto-generated method stub

	}

	@Override
	public void printEventsCount() {
		// TODO Auto-generated method stub

	}

	@Override
	public void printEventHandlers() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initProcessing() {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterSimStep(double time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finishProcessing() {
		// TODO Auto-generated method stub

	}
	
	//TODO
	public Double getFirstParameter(){
		return 10.0;
	}

}
