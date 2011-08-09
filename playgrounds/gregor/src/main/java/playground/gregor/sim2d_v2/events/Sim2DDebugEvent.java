package playground.gregor.sim2d_v2.events;

import org.matsim.core.api.experimental.events.PersonEvent;

public interface Sim2DDebugEvent extends PersonEvent {

	public double getVelocity();
}
