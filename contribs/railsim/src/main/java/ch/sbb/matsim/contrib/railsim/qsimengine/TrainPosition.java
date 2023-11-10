package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;

/**
 * Interface allowing to query current train position and information.
 */
public interface TrainPosition {

	/**
	 * The driver, which can also be used as identifier.
	 */
	MobsimDriverAgent getDriver();

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
	 * Check whether to stop at certain link.
	 */
	boolean isStop(Id<Link> link);
}
