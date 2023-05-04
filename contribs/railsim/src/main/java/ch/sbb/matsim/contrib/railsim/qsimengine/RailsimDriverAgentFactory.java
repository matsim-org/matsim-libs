package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.AbstractTransitDriverAgent;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;

import javax.inject.Inject;
import java.util.Set;

/**
 * Factory to create specific drivers for the rail engine.
 */
public class RailsimDriverAgentFactory implements TransitDriverAgentFactory {

	private final Set<String> modes;

	@Inject
	public RailsimDriverAgentFactory(Config config) {
		this.modes = ConfigUtils.addOrGetModule(config, RailsimConfigGroup.class).getRailNetworkModes();
	}

	@Override
	public AbstractTransitDriverAgent createTransitDriver(Umlauf umlauf, InternalInterface internalInterface, TransitStopAgentTracker transitStopAgentTracker) {

		String mode = umlauf.getUmlaufStuecke().get(0).getRoute().getTransportMode();

		if (this.modes.contains(mode)) {
			// TODO: Can be a specific driver agent later
			return new TransitDriverAgentImpl(umlauf, mode, transitStopAgentTracker, internalInterface);
		}

		return new TransitDriverAgentImpl(umlauf, TransportMode.car, transitStopAgentTracker, internalInterface);
	}
}
