package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;

/**
 * Railsim specific transit driver that can be re-routed.
 */
public final class RailsimTransitDriverAgent extends TransitDriverAgentImpl {
	public RailsimTransitDriverAgent(Umlauf umlauf, String transportMode, TransitStopAgentTracker thisAgentTracker, InternalInterface internalInterface) {
		super(umlauf, transportMode, thisAgentTracker, internalInterface);
	}
}
