package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.List;

/**
 * Disposition, handling route and track reservations.
 */
public interface TrainDisposition {


	/**
	 * Method invoked when a train is departing.
	 */
	void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route);

	/**
	 * Train is reaching the given links and is trying to block them.
	 *
	 * @return links of the request that are exclusively blocked for the train.
	 */
	List<RailLink> blockRailSegment(double time, MobsimDriverAgent driver, List<RailLink> segment);

	/**
	 * Inform the resource manager that the train has passed a link that can now be unblocked.
	 * This needs to be called after track states have been updated already.
	 */
	void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link);


}
