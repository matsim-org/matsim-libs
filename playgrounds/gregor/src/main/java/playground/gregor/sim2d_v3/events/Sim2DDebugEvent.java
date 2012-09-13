package playground.gregor.sim2d_v3.events;

import org.matsim.core.api.experimental.events.Event;

public interface Sim2DDebugEvent extends Event {

	public double getVelocity();
}
