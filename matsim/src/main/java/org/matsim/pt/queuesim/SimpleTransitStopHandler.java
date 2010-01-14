package org.matsim.pt.queuesim;

import java.util.List;

import org.matsim.transitSchedule.api.TransitStopFacility;

public class SimpleTransitStopHandler implements TransitStopHandler {

	private TransitStopFacility lastHandledStop = null;

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now, List<PassengerAgent> leavingPassengers,
			List<PassengerAgent> enteringPassengers, PassengerAccessEgress handler) {
		int cntEgress = leavingPassengers.size();
		int cntAccess = enteringPassengers.size();
		double stopTime = 0;
		if ((cntAccess > 0) || (cntEgress > 0)) {
			stopTime = cntAccess * 4 + cntEgress * 2;
			if (this.lastHandledStop != stop) {
				stopTime += 15.0; // add fixed amount of time for door-operations and similar stuff
			}
			for (PassengerAgent passenger : leavingPassengers) {
				handler.handlePassengerLeaving(passenger, now);
			}
			for (PassengerAgent passenger : enteringPassengers) {
				handler.handlePassengerEntering(passenger, now);
			}
		}
		this.lastHandledStop = stop;
		return stopTime;
	}

}
