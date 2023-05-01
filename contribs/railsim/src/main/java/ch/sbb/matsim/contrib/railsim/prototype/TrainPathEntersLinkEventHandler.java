package ch.sbb.matsim.contrib.railsim.prototype;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Ihab Kaddoura
 */
public interface TrainPathEntersLinkEventHandler extends EventHandler {
	public void handleEvent(TrainPathEntersLink event);
}
