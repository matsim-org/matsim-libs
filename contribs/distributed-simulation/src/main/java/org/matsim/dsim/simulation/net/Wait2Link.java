package org.matsim.dsim.simulation.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Steppable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

@Log4j2
@RequiredArgsConstructor
class Wait2Link implements Steppable {

	private final Map<Id<Link>, Queue<Waiting>> waitingVehicles = new HashMap<>();
	private final EventsManager em;
	private final ActiveLinks activeLinks;

	void accept(SimVehicle vehicle, SimLink link) {

		waitingVehicles
			.computeIfAbsent(link.getId(), _ -> new ArrayDeque<>())
			.add(new Waiting(vehicle, link));
	}

	@Override
	public void doSimStep(double now) {
		//  use an iterator, as we might have to remove entries from the map
		var it = waitingVehicles.values().iterator();
		while (it.hasNext()) {
			var waitingQ = it.next();

			// try to push all vehicles from the queue onto the link.
			while (!waitingQ.isEmpty()) {
				var link = waitingQ.peek().link();
				var vehicle = waitingQ.peek().vehicle();
				var position = vehicle.getNextRouteElement() == null ? SimLink.LinkPosition.QEnd : SimLink.LinkPosition.Buffer;

				if (link.isAccepting(position, now)) {
					waitingQ.poll();
					pushVehicleOntoLink(vehicle, link, position, now);
				} else {
					break;
				}
			}

			// in case there are no waiting vehicles for the link,
			// remove the entry
			if (waitingQ.isEmpty()) {
				it.remove();
			}
		}
	}

	private void pushVehicleOntoLink(SimVehicle vehicle, SimLink link, SimLink.LinkPosition position, double now) {

		em.processEvent(new VehicleEntersTrafficEvent(
			now, vehicle.getDriver().getId(), link.getId(), vehicle.getId(),
			vehicle.getDriver().getCurrentLeg().getMode(), 1.0)
		);
		link.pushVehicle(vehicle, position, now);
		activeLinks.activate(link);
	}

	private record Waiting(SimVehicle vehicle, SimLink link) {
	}
}
