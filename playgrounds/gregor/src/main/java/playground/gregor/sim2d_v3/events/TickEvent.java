package playground.gregor.sim2d_v3.events;

import org.matsim.core.events.EventImpl;




public class TickEvent extends EventImpl {

	public static final String type="tick";

	public TickEvent(double time) {
		super(time);
	}


	@Override
	public String getEventType() {
		return type;
	}




}
