package ch.sbb.matsim.contrib.railsim.qsimengine.router;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailResourceManager;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.List;

/**
 * Calculates unblocked route between two {@link RailLink}.
 */
public final class TrainRouter {

	private final Network network;
	private final RailResourceManager resources;
	private final LeastCostPathCalculator lpc;

	@Inject
	public TrainRouter(QSim qsim, RailResourceManager resources) {
		this(qsim.getScenario().getNetwork(), resources);
	}

	public TrainRouter(Network network, RailResourceManager resources) {
		this.network = network;
		this.resources = resources;

		// TODO: filter rail network

		this.lpc = new FastDijkstraFactory(false).createPathCalculator(network, new DisUtility(), new FreeSpeedTravelTime());
	}

	/**
	 * Calculate the shortest path between two links.
	 */
	public List<RailLink> calcRoute(RailLink from, RailLink to) {

		Node fromNode = network.getLinks().get(from.getLinkId()).getToNode();
		Node toNode = network.getLinks().get(to.getLinkId()).getFromNode();

		LeastCostPathCalculator.Path path = lpc.calcLeastCostPath(fromNode, toNode, 0, null, null);

		return path.links.stream().map(l -> resources.getLink(l.getId())).toList();
	}

	private final class DisUtility implements TravelDisutility {
		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return resources.hasCapacity(link.getId()) ? 0 : 1;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 0;
		}
	}
}
