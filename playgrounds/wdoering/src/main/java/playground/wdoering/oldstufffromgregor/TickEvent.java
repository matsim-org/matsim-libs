package playground.wdoering.oldstufffromgregor;

import org.matsim.api.core.v01.events.Event;




public class TickEvent extends Event {

	public static final String type="tick";

	public TickEvent(double time) {
		super(time);
	}


	@Override
	public String getEventType() {
		return type;
	}




}
