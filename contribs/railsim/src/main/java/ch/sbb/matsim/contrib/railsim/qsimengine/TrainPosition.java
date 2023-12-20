package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Interface allowing to query current train position and information.
 */
public interface TrainPosition {

	/**
	 * The driver, which can also be used as identifier.
	 */
	MobsimDriverAgent getDriver();

	/**
	 * Get transit driver agent, if this is a pt transit.
	 */
	@Nullable
	RailsimTransitDriverAgent getPt();

	/**
	 * The train type.
	 */
	TrainInfo getTrain();

	/**
	 * The link the where the head of the train is on. Can be null if not yet departed.
	 */
	@Nullable
	Id<Link> getHeadLink();

	/**
	 * The link the where the tail of the train is on. Can be null if not yet departed.
	 */
	@Nullable
	Id<Link> getTailLink();

	/**
	 * The position of the head of the train on the link, in meters.
	 */
	double getHeadPosition();

	/**
	 * The position of the tail of the train on the link, in meters.
	 */
	double getTailPosition();

	/**
	 * Current route index.
	 */
	int getRouteIndex();

	/**
	 * Total route size.
	 */
	int getRouteSize();

	/**
	 * Get part of the route.
	 */
	RailLink getRoute(int idx);

	/**
	 * Get part of the route.
	 *
	 * @param from from index, inclusive
	 * @param to   to index, exclusive
	 */
	List<RailLink> getRoute(int from, int to);

	/**
	 * Return the route until the next stop based on the current position.
	 */
	List<RailLink> getRouteUntilNextStop();

	/**
	 * Check whether to stop at certain link.
	 */
	boolean isStop(Id<Link> link);
}
