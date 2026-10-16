package org.matsim.dsim.simulation.net;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.StringJoiner;

public class DefaultWait2Link implements Wait2Link {

	private static final Logger log = LogManager.getLogger(DefaultWait2Link.class);

	/**
	 * Diagnostic for two vehicles swapping which of them enters traffic first on one link between two otherwise identical runs. The queue
	 * per link is a FIFO, so a swap means {@link #accept} was called in a different order or at a different time. Enable with
	 * {@code -Ddsim.debug.wait2link=<linkId>@<fromSecond>-<toSecond>}, which logs every hand-in and every release on that link in that
	 * window, together with the queue as it stands. Costs nothing when the property is unset. Remove once the question is settled.
	 */
	private final Trace trace = Trace.fromSystemProperty();

	private final Map<Id<Link>, Queue<Waiting>> waitingVehicles = new HashMap<>();
	private final EventsManager em;

	private double now;

	@Inject
	public DefaultWait2Link(EventsManager em) {
		this.em = em;
	}

	@Override
	public boolean accept(DistributedMobsimVehicle vehicle, SimLink link, double now) {
		Queue<Waiting> queue = waitingVehicles.computeIfAbsent(link.getId(), _ -> new ArrayDeque<>());
		queue.add(new Waiting(vehicle, link));

		if (trace != null && trace.covers(link.getId(), now))
			log.info("wait2link accept  t={} link={} vehicle={} driver={} queue=[{}]",
				now, link.getId(), vehicle.getId(), vehicle.getDriver().getId(), describe(queue));

		return true;
	}

	@Override
	public void moveWaiting(double now) {
		this.now = now;
		var it = waitingVehicles.values().iterator();
		while (it.hasNext()) {
			var q = it.next();
			while (!q.isEmpty()) {
				var link = q.peek().link();
				var vehicle = q.peek().vehicle();

				if (moveWaiting(vehicle, link, now)) {
					q.remove();

					if (trace != null && trace.covers(link.getId(), now))
						log.info("wait2link release t={} link={} vehicle={} driver={} remaining=[{}]",
							now, link.getId(), vehicle.getId(), vehicle.getDriver().getId(), describe(q));
				} else {
					if (trace != null && trace.covers(link.getId(), now))
						log.info("wait2link blocked t={} link={} vehicle={} driver={} queue=[{}]",
							now, link.getId(), vehicle.getId(), vehicle.getDriver().getId(), describe(q));
					break;
				}
			}

			if (q.isEmpty()) {
				it.remove();
			}
		}
	}

	@Override
	public void afterMobsim() {
		for (var q : waitingVehicles.values()) {
			for (var veh : q) {
				em.processEvent(
					new PersonStuckEvent(now, veh.vehicle().getDriver().getId(), veh.link().getId(), veh.vehicle().getDriver().getMode()));
				for (var p : veh.vehicle().getPassengers()) {
					em.processEvent(new PersonStuckEvent(now, p.getId(), veh.link().getId(), veh.vehicle().getDriver().getMode()));
				}
			}
		}
		// clear in case this is called from multiple places
		waitingVehicles.clear();
	}

	private boolean moveWaiting(DistributedMobsimVehicle vehicle, SimLink link, double now) {
		SimLink.LinkPosition position = vehicle.getDriver().isWantingToArriveOnCurrentLink()
			? SimLink.LinkPosition.QEnd
			: SimLink.LinkPosition.Buffer;

		if (link.isAccepting(position, now)) {
			em.processEvent(new VehicleEntersTrafficEvent(
				now, vehicle.getDriver().getId(), link.getId(), vehicle.getId(),
				vehicle.getDriver().getMode(), 1.0)
			);
			link.pushVehicle(vehicle, position, now);
			return true;
		} else {
			return false;
		}
	}

	private static String describe(Queue<Waiting> queue) {
		StringJoiner joiner = new StringJoiner(", ");
		for (Waiting waiting : queue) {
			joiner.add(waiting.vehicle().getId().toString());
		}
		return joiner.toString();
	}

	public record Waiting(DistributedMobsimVehicle vehicle, SimLink link) {
	}

	/**
	 * Window to trace, see {@link #TRACE}.
	 */
	record Trace(String linkId, double from, double to) {

		static final String PROPERTY = "dsim.debug.wait2link";

		static Trace fromSystemProperty() {
			String value = System.getProperty(PROPERTY);
			if (value == null || value.isBlank())
				return null;

			try {
				String[] linkAndWindow = value.split("@", 2);
				String[] window = linkAndWindow[1].split("-", 2);
				Trace trace = new Trace(linkAndWindow[0], Double.parseDouble(window[0]), Double.parseDouble(window[1]));
				log.info("tracing wait2link on link {} between {} and {}", trace.linkId, trace.from, trace.to);
				return trace;
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Expected -D%s=<linkId>@<fromSecond>-<toSecond>, got '%s'".formatted(PROPERTY, value), e);
			}
		}

		boolean covers(Id<Link> linkId, double now) {
			return now >= from && now <= to && linkId.toString().equals(this.linkId);
		}
	}
}
