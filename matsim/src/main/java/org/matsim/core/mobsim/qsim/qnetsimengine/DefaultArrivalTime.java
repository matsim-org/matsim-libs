package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Delegates arrival-time calculation to the registered specialized handlers.
 *
 * <p>At most one handler may claim a vehicle by returning a non-NaN value. If no
 * handler claims it, vehicles arrive without an additional delay.</p>
 */
public class DefaultArrivalTime implements ArrivalTimeCalculator {
	private final Collection<ArrivalTimeCalculator> handlers = new ArrayList<>();

	@Inject
	DefaultArrivalTime() {
	}

	@Override
	public double calculateArrivalTime(double now, QVehicle vehicle, Link link) {
		double time = Double.NaN;

		for (ArrivalTimeCalculator handler : handlers) {
			double tmp = handler.calculateArrivalTime(now, vehicle, link);

			if (Double.isNaN(tmp)) {
				continue;
			}

			if (!Double.isNaN(time)) {
				throw new RuntimeException("Two arrival time calculators feel responsible for vehicle; don't know what to do.");
			}

			time = tmp;
		}

		if (!Double.isNaN(time)) {
			return time;
		}

		return 0;
	}

	public final DefaultArrivalTime addHandler(ArrivalTimeCalculator handler) {
		handlers.add(handler);
		return this;
	}
}
