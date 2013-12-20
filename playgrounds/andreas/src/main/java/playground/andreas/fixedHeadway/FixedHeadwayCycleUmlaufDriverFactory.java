package playground.andreas.fixedHeadway;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;

/**
 * @author aneumann
 */
public class FixedHeadwayCycleUmlaufDriverFactory implements TransitDriverAgentFactory {

	@Override
	public AbstractTransitDriverAgent createTransitDriver(Umlauf umlauf, TransitStopAgentTracker thisAgentTrackerVehicle, InternalInterface internalInterface) {
		return new FixedHeadwayCycleUmlaufDriver(umlauf, thisAgentTrackerVehicle, internalInterface);
	}

}
