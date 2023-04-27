package ch.sbb.matsim.contrib.railsim.prototype;

import org.matsim.core.events.handler.EventHandler;

/**
 * @author Ihab Kaddoura
 */
public interface TrainLeavesLinkEventHandler extends EventHandler {
	void handleEvent(TrainLeavesLink event);
}
