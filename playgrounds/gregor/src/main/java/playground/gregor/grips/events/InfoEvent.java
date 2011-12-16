package playground.gregor.grips.events;

import org.matsim.core.events.EventImpl;

public class InfoEvent extends EventImpl {
	public static final String EVENT_TYPE = "InfoEvent";
	private final String info;
	public InfoEvent(double time, String info) {
		super(time);
		this.info = info;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getInfo(){
		return this.info;
	}

}
