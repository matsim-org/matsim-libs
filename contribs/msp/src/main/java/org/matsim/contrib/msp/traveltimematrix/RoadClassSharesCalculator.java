package org.matsim.contrib.msp.traveltimematrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

/**
 * Computes the {@link RoadClassShares} of a zone system: for every zone pair, how the free-speed travel
 * time of the free-speed path splits over the road classes.
 * <p>
 * It is the same computation the zonal free-speed matrix of {@code contribs/dvrp} does - one
 * {@link LeastCostPathTree} per origin zone over the central nodes - with one addition: for every
 * destination the links of the path are walked back and their free-speed travel times are added up per
 * class. Walking the tree back is {@code O(path length)} per destination, so the shares cost roughly as
 * much again as the matrix itself, and they are computed <b>once</b>.
 * <p>
 * Taking the <b>free-speed</b> path and not a congested one is a deliberate part of the approximation,
 * and it is where the method's known bias comes from: under congestion a driver would avoid the
 * congested road, so holding the free-speed path fixed and slowing it down overstates the travel time.
 * Level 3 of the ladder - evaluating the time-dependent travel time along the free-speed path - has
 * exactly the same bias, which is why the two can be compared against time-dependent routing on the
 * same footing.
 *
 * @see RoadClassShares what comes out
 */
public final class RoadClassSharesCalculator {

	private static final Logger log = LogManager.getLogger(RoadClassSharesCalculator.class);

	private RoadClassSharesCalculator() {
	}

	/**
	 * The shares over a free-speed network, which is what the default free-speed matrix is built on.
	 *
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()}, so that the free-speed times are
	 * rounded exactly as {@code FreeSpeedTravelTimeMatrix} rounds them
	 */
	public static RoadClassShares calculateFreeSpeedShares(Network network, ZoneSystem zoneSystem,
		RoadClassification roadClasses, int numberOfThreads, double qSimTimeStepSize) {
		return calculate(network, zoneSystem, roadClasses, new QSimFreeSpeedTravelTime(qSimTimeStepSize),
			numberOfThreads);
	}

	/**
	 * @param travelTime the travel times the paths are built and split by. Has to be the <b>same</b> one
	 * the free-speed matrix was built with, or the shares describe a different path than the travel time
	 * they are multiplied with.
	 */
	public static RoadClassShares calculate(Network network, ZoneSystem zoneSystem, RoadClassification roadClasses,
		TravelTime travelTime, int numberOfThreads) {

		if (roadClasses.isSingleClass()) {
			throw new IllegalArgumentException("There is nothing to split with a single road class - level 0 of the "
				+ "matrix needs no road class shares at all. Pass a classification with more than one class, or use "
				+ "TimeVaryingTravelTimeMatrix.global(...).");
		}

		Map<Zone, Node> centralNodes = ZoneSystemUtils.computeMostCentralNodes(network.getNodes().values(), zoneSystem);
		RoadClassShares shares = new RoadClassShares(zoneSystem, roadClasses.getRoadClasses());

		log.info("Computing road class shares for {} zones ({} pairs) over {} road classes {}. This needs {} MB.",
			centralNodes.size(), (long) centralNodes.size() * centralNodes.size(), roadClasses.size(),
			roadClasses.getRoadClasses(), shares.getSizeInBytes() / 1024 / 1024);

		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
		List<LeastCostPathTree> trees = IntStream.range(0, numberOfThreads)
			.mapToObj(i -> new LeastCostPathTree(SpeedyGraphBuilder.build(network), travelTime, travelDisutility))
			.toList();
		ExecutorServiceWithResource<LeastCostPathTree> executor = new ExecutorServiceWithResource<>(trees);
		Counter counter = new Counter("Road class shares: zone ", " / " + centralNodes.size());

		// how far the sum over the classes is from the travel time of the tree. They describe the same
		// path, so the two have to agree; a deviation means the path walked back is not the path the
		// travel time came from, which would silently corrupt every level 1 travel time.
		DoubleAdder deviation = new DoubleAdder();
		LongAdder unreachable = new LongAdder();

		executor.submitRunnablesAndWait(centralNodes.keySet().stream().map(fromZone -> (tree -> {
			counter.incCounter();
			computeForOriginZone(fromZone, centralNodes, roadClasses, travelTime, shares, tree, deviation, unreachable);
		})));

		counter.printCounter();
		executor.shutdown();

		long pairs = (long) centralNodes.size() * centralNodes.size();
		log.info("Road class shares done. Mean deviation between the sum over the classes and the travel time of "
				+ "the path: {} s over {} pairs; {} pairs had no path.",
			String.format(Locale.ROOT, "%.6f", deviation.sum() / Math.max(1, pairs - unreachable.sum())), pairs,
			unreachable.sum());

		return shares;
	}

	private static void computeForOriginZone(Zone fromZone, Map<Zone, Node> centralNodes,
		RoadClassification roadClasses, TravelTime travelTime, RoadClassShares shares, LeastCostPathTree tree,
		DoubleAdder deviation, LongAdder unreachable) {

		// departure time 0: the paths are free-speed paths, so they do not depend on when the trip starts
		Node fromNode = centralNodes.get(fromZone);
		tree.calculate(fromNode, 0, null, null);

		double[] timeByRoadClass = new double[roadClasses.size()];

		for (Map.Entry<Zone, Node> destination : centralNodes.entrySet()) {

			Arrays.fill(timeByRoadClass, 0);
			Node toNode = destination.getValue();

			double summed = 0;
			Iterator<Link> path = tree.getLinkPathIterator(toNode);
			while (path.hasNext()) {
				Link link = path.next();
				double linkTime = travelTime.getLinkTravelTime(link, 0, null, null);
				timeByRoadClass[roadClasses.indexOf(link)] += linkTime;
				summed += linkTime;
			}

			OptionalTime arrival = tree.getTime(tree.getNodeIndex(toNode));
			if (arrival.isUndefined()) {
				// not reachable from this origin at all. The zonal travel time matrix would throw here,
				// so the shares of such a pair are never read; they are set anyway, to the "nothing is
				// known" value, so that no byte of the matrix is left uninitialised.
				unreachable.increment();
			} else if (toNode != fromNode) {
				deviation.add(Math.abs(arrival.seconds() - summed));
			}

			shares.set(fromZone, destination.getKey(), timeByRoadClass);
		}
	}
}
