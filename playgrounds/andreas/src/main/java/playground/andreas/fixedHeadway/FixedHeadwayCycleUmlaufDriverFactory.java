package playground.andreas.fixedHeadway;

import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriver;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverFactory;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;

/**
 * @author aneumann
 */
public class FixedHeadwayCycleUmlaufDriverFactory implements AbstractTransitDriverFactory {

	@Override
	public AbstractTransitDriver createTransitDriver(Umlauf umlauf, TransitStopAgentTracker thisAgentTrackerVehicle, InternalInterface internalInterface) {
		return new FixedHeadwayCycleUmlaufDriver(umlauf, thisAgentTrackerVehicle, internalInterface);
	}

}
