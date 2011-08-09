package playground.gregor.sim2d_v2.events;

import org.matsim.core.events.handler.EventHandler;

public interface Sim2DDebugEventHandler extends EventHandler {
	
	public void handleEvent(Sim2DDebugEvent event);
	


}
