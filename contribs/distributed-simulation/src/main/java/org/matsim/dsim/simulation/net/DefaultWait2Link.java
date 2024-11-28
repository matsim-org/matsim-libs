package org.matsim.dsim.simulation.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;

@Log4j2
@RequiredArgsConstructor
public class DefaultWait2Link implements Wait2Link {

	private final Map<Id<Link>, Queue<Waiting>> waitingVehicles = new HashMap<>();
	private final EventsManager em;
	private final Consumer<Id<Link>> activateLink;

	@Override
	public boolean accept(DistributedMobsimVehicle vehicle, SimLink link, double now) {
		waitingVehicles
			.computeIfAbsent(link.getId(), _ -> new ArrayDeque<>())
			.add(new Waiting(vehicle, link));
		return true;
	}

	@Override
	public void moveWaiting(double now) {
		var it = waitingVehicles.values().iterator();
		while (it.hasNext()) {
			var q = it.next();
			while (!q.isEmpty()) {
				var link = q.peek().link();
				var vehicle = q.peek().vehicle();

				if (moveWaiting(vehicle, link, now)) {
					q.remove();
				} else {
					break;
				}
			}

			if (q.isEmpty()) {
				it.remove();
			}
		}
	}

	private boolean moveWaiting(DistributedMobsimVehicle vehicle, SimLink link, double now) {
		SimLink.LinkPosition position = vehicle.getDriver().isWantingToArriveOnCurrentLink() ? SimLink.LinkPosition.QEnd : SimLink.LinkPosition.Buffer;

		if (link.isAccepting(position, now)) {
			em.processEvent(new VehicleEntersTrafficEvent(
				now, vehicle.getDriver().getId(), link.getId(), vehicle.getId(),
				vehicle.getDriver().getMode(), 1.0)
			);
			link.pushVehicle(vehicle, position, now);
			activateLink.accept(link.getId());
			return true;
		} else {
			return false;
		}
	}

	public record Waiting(DistributedMobsimVehicle vehicle, SimLink link) {
	}
}
