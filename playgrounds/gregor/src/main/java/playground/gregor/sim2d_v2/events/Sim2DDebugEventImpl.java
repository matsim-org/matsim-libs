package playground.gregor.sim2d_v2.events;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEventImpl;

public class Sim2DDebugEventImpl extends PersonEventImpl implements Sim2DDebugEvent {

	private static final String EVENT_TYPE = "Sim2DDebugEvent";
	
	public Sim2DDebugEventImpl(double time, Id personId) {
		super(time, personId);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public double getVelocity() {
		// TODO Auto-generated method stub
		return 0;
	}

}
