package org.matsim.contrib.drt.stops;

public class DefaultPassengerStopDurationProvider extends StaticPassengerStopDurationProvider {
	public DefaultPassengerStopDurationProvider(double stopDuration) {
		super(stopDuration, 0.0);
	}
}
