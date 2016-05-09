package sergio;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.TupleIteratorWithExclusions;
import floetteroed.utilities.math.MathHelpers;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TripIterator implements Iterator<Trip> {

	// -------------------- MEMBERS --------------------

	private final ZonalSystem zonalSystem;

	private final TupleIteratorWithExclusions<Zone> zonePairIterator;

	private final Random rnd;

	private final Dijkstra dijkstra;

	// -------------------- CONSTRUCTION --------------------

	TripIterator(final Network network, final ZonalSystem zonalSystem,
			final Set<Tuple<Zone, Zone>> excludedOdPairs, final Random rnd) {
		this.zonalSystem = zonalSystem;
		final Set<Zone> allZones = new LinkedHashSet<>();
		for (Zone zone : zonalSystem.getId2zoneView().values()) {
			if ((zonalSystem.getNodes(zone) != null)
					&& (zonalSystem.getNodes(zone).size() > 0)) {
				allZones.add(zone);
			}
		}
		this.zonePairIterator = new TupleIteratorWithExclusions<>(allZones,
				excludedOdPairs);
		this.dijkstra = new Dijkstra(network, new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength() / link.getFreespeed();
			}
		}, new TravelTime() {
			@Override
			public double getLinkTravelTime(final Link link, final double time,
					final Person person, final Vehicle vehicle) {
				return 0.0;
			}

		});
		this.rnd = rnd;
	}

	// -------------------- IMPLEMENTATION OF Iterator --------------------

	@Override
	public boolean hasNext() {
		return this.zonePairIterator.hasNext();
	}

	@Override
	public Trip next() {

		final Tuple<Zone, Zone> nextOdPair = this.zonePairIterator.next();
		final Node fromNode = MathHelpers.draw(
				this.zonalSystem.getNodes(nextOdPair.getA()), this.rnd);
		final Node toNode = MathHelpers.draw(
				this.zonalSystem.getNodes(nextOdPair.getB()), this.rnd);
		final Path path = this.dijkstra.calcLeastCostPath(fromNode, toNode, 0.0,
				null, null);
		return new Trip(nextOdPair.getA(), nextOdPair.getB(), path.links);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
