package playground.gregor.sim2d_v2.events;

import org.matsim.core.events.EventImpl;




public class TickEvent extends EventImpl {

	private static final String type="tick";

	public TickEvent(double time) {
		super(time);
	}


	@Override
	public String getEventType() {
		return type;
	}




}
