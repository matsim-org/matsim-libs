package playground.gregor.sim2d_v3.events;

import org.matsim.core.events.handler.EventHandler;

public interface TickEventHandler extends EventHandler {

	public void handleEvent(TickEvent event);

}
