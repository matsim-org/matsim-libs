package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedDepartureHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetworkModeDepartureHandler;
import org.matsim.dsim.DSimConfigGroup;

import java.util.HashSet;
import java.util.Set;

public class NetworkTrafficDepartureHandler implements DistributedDepartureHandler, NetworkModeDepartureHandler {

	private final SimNetwork simNetwork;
	private final Set<String> modes;
	private final ParkedVehicles parkedVehicles;
	private final Wait2Link wait2Link;
	private final EventsManager em;

	@Inject
	public NetworkTrafficDepartureHandler(SimNetwork simNetwork, DSimConfigGroup config, ParkedVehicles parkedVehicles, Wait2Link wait2Link, EventsManager em) {
		this.simNetwork = simNetwork;
		this.modes = new HashSet<>(config.getNetworkModes());
		this.parkedVehicles = parkedVehicles;
		this.wait2Link = wait2Link;
		this.em = em;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (!modes.contains(agent.getMode())) {
			return false;
		}

		if (!(agent instanceof MobsimDriverAgent driver)) {
			throw new RuntimeException("Only driver agents are supported");
		}

		var vehicle = parkedVehicles.unpark(driver.getPlannedVehicleId(), linkId);
		driver.setVehicle(vehicle);
		vehicle.setDriver(driver);
		em.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));

		Id<Link> currentRouteElement = agent.getCurrentLinkId();
		assert currentRouteElement != null : "Vehicle %s has no current route element".formatted(vehicle.getId());

		SimLink link = simNetwork.getLinks().get(currentRouteElement);
		assert link != null : "Link %s not found in partition on partition #%d".formatted(currentRouteElement, simNetwork.getPart());

		wait2Link.accept(vehicle, link, now);
		return true;
	}
}
