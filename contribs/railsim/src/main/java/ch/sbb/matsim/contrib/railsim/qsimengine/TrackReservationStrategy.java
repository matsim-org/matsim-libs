package ch.sbb.matsim.contrib.railsim.qsimengine;

import java.util.List;

/**
 * Component to define when and how many tracks should be reserved in advance.
 */
public interface TrackReservationStrategy {


	/**
	 * Calculate when the reservation function should be triggered.
	 * Should return {@link Double#POSITIVE_INFINITY} if this distance is far in the future and can be checked at later point.
	 *
	 * @param state current train state.
	 * @return travel distance after which reservations should be updated.
	 */
	double nextUpdate(RailLink currentLink, TrainState state);


	List<RailLink> retrieveLinksToReserve(double time, int idx, TrainState state) ;

}
