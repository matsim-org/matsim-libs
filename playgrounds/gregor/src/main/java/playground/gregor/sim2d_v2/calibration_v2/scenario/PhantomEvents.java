package playground.gregor.sim2d_v2.calibration_v2.scenario;

import java.util.ArrayList;
import java.util.List;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEvent;

public class PhantomEvents {

	private boolean init = false;

	private double[] times;
	private Event[] events;


	private final List<Event> eventsList = new ArrayList<Event>();


	public double[] getTimesArray() {
		if (this.init == false) {
			init();
		}
		return this.times;
	}

	public Event[] getEventsArray() {
		if (this.init == false) {
			init();
		}
		return this.events;
	}


	private void init() {
		int size = this.eventsList.size();
		this.events = new Event[size];
		this.times = new double[size];
		int pointer = 0;
		for (int i = 0; i < this.eventsList.size(); i++) {
			Event e = this.eventsList.get(i);
			if (e instanceof PersonEvent) {
				PersonEvent pe = (PersonEvent)e;
				this.times[pointer] = e.getTime();
				this.events[pointer++] = e;
			}
		}
		this.init = true;
	}

	public void addEvent(Event e) {
		if (this.init == true) {
			throw new RuntimeException("already initialized");
		}
		if (this.eventsList.size() > 0 && this.eventsList.get(this.eventsList.size()-1).getTime() > e.getTime()){
			throw new RuntimeException("events have to be added in chronological order!");
		}
		this.eventsList.add(e);
	}

}
