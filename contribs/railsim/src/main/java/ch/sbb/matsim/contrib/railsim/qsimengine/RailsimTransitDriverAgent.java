package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * Railsim specific transit driver that can be re-routed.
 */
public final class RailsimTransitDriverAgent extends TransitDriverAgentImpl {

	private static final Logger log = LogManager.getLogger(RailsimTransitDriverAgent.class);

	public RailsimTransitDriverAgent(Umlauf umlauf, String transportMode, TransitStopAgentTracker thisAgentTracker, InternalInterface internalInterface) {
		super(umlauf, transportMode, thisAgentTracker, internalInterface);
	}

	/**
	 * Add a detour to this driver schedule.
	 * @param currentIdx current route index
	 * @param start start of detour
	 * @param end end of the detour
	 * @return whether this detour is accepted and feasible.
	 */
	public boolean addDetour(int currentIdx, int start, int end, List<RailLink> detour) {

		TransitStopFacility nextStop = getNextTransitStop();


		// TODO:

		return true;
	}
}
