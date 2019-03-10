package org.matsim.core.mobsim.qsim.interfaces;

import java.util.Optional;

public interface RequiresBooking {
	void bookTrip();

	interface TripInfoBookingResponse {
		Optional<TripInfo> getTripInfo(); //if null then rejected????
		//some additional info???

		default boolean isRejected() {
			return !getTripInfo().isPresent();
		}
	}
}
