package playground.gregor.sim2d_v2.events;

import org.matsim.core.events.handler.EventHandler;

public interface TickEventHandler extends EventHandler {

	public void handleEvent(TickEvent event);

}
