package playground.andreas.fixedHeadway;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;

import javax.inject.Inject;

/**
 * @author aneumann
 */
public class FixedHeadwayCycleUmlaufDriverFactory implements TransitDriverAgentFactory {

	private final InternalInterface internalInterface;
	private final TransitStopAgentTracker transitStopAgentTracker;

	@Inject
	public FixedHeadwayCycleUmlaufDriverFactory(Scenario scenario, EventsManager eventsManager, InternalInterface internalInterface, TransitStopAgentTracker transitStopAgentTracker) {
		this.internalInterface = internalInterface;
		this.transitStopAgentTracker = transitStopAgentTracker;
	}

	@Override
	public AbstractTransitDriverAgent createTransitDriver(Umlauf umlauf) {
		return new FixedHeadwayCycleUmlaufDriver(umlauf, transitStopAgentTracker, internalInterface);
	}

}
