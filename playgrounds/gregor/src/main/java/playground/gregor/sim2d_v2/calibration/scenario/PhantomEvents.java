package playground.gregor.sim2d_v2.calibration.scenario;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEvent;

public class PhantomEvents {

	private boolean init = false;

	private double[] times;
	private Event[] events;

	private final LinkedList<Event> claibrationAgentEvents = new LinkedList<Event>();

	private final List<Event> eventsList = new ArrayList<Event>();

	private Id calibrationId;

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

	public void setCalibrationAgentId(Id id) {
		this.calibrationId = id;
		this.init = false;
	}

	private void init() {
		this.claibrationAgentEvents.clear();
		int size = 0;
		for (int i = 0; i < this.eventsList.size(); i++) {
			Event e = this.eventsList.get(i);
			if (e instanceof PersonEvent) {
				PersonEvent pe = (PersonEvent)e;
				if (!pe.getPersonId().equals(this.calibrationId)) {
					size++;
				}
			}
		}
		this.events = new Event[size];
		this.times = new double[size];
		int pointer = 0;
		for (int i = 0; i < this.eventsList.size(); i++) {
			Event e = this.eventsList.get(i);
			if (e instanceof PersonEvent) {
				PersonEvent pe = (PersonEvent)e;
				if (!pe.getPersonId().equals(this.calibrationId)) {
					this.times[pointer] = e.getTime();
					this.events[pointer++] = e;
				} else {
					this.claibrationAgentEvents.add(e);
				}
			}
		}
		this.init = true;
	}

	public LinkedList<Event> getCalibrationAgentEvents() {
		return this.claibrationAgentEvents;
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
