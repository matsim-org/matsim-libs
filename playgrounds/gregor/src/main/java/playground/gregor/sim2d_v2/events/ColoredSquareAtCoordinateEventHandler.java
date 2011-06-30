package playground.gregor.sim2d_v2.events;

import org.matsim.core.events.handler.EventHandler;

public interface ColoredSquareAtCoordinateEventHandler extends EventHandler{

	public void handleEvent(ColoredSquareAtCoordinateEvent e);
}
