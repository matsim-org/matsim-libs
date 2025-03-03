package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.parking.ParkingSearchTimeCalculator;

import java.util.ArrayList;
import java.util.Collection;

public class DefaultParkingSearchTime implements ParkingSearchTimeCalculator {
	private final Collection<ParkingSearchTimeCalculator> handlers = new ArrayList<>();

	@Inject
	DefaultParkingSearchTime() {
	}

	@Override
	public double calculateParkingSearchTime(double now, QVehicle vehicle, Link link) {
		double time = Double.NaN;

		for (ParkingSearchTimeCalculator handler : handlers) {
			double tmp = handler.calculateParkingSearchTime(now, vehicle, link);

			if (Double.isNaN(tmp)) {
				// Handler was not able to calculate parking search time
				continue;
			}

			if (!Double.isNaN(time)) {
				// Time is already calculated by another handler. This is not allowed.
				throw new RuntimeException("Two parking search time calculators feel responsible for vehicle; don't know what to do.");
			}

			time = tmp;
		}

		if (!Double.isNaN(time)) {
			return time;
		}

		return 0;
	}

	public final DefaultParkingSearchTime addHandler(ParkingSearchTimeCalculator handler) {
		handlers.add(handler);
		return this;
	}
}
