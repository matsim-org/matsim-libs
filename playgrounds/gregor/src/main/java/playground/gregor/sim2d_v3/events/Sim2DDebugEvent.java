package playground.gregor.sim2d_v3.events;

import org.matsim.core.api.experimental.events.Event;

public interface Sim2DDebugEvent {

	public double getVelocity();
	
	public double getTime();
	
}
