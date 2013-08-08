package playground.wdoering.oldstufffromgregor;

import org.matsim.core.events.handler.EventHandler;

public interface TickEventHandler extends EventHandler {

	public void handleEvent(TickEvent event);

}
