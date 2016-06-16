package sergio;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;

import floetteroed.utilities.Tuple;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AssignmentMatrixColumnInventor {

	// -------------------- MEMBERS --------------------

	private final Network network;

	private final ZonalSystem zonalSystem;

	private final Set<Tuple<Zone, Zone>> excludedOdPairs = new LinkedHashSet<>();

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	public AssignmentMatrixColumnInventor(final Network network,
			final ZonalSystem zonalSystem, final Random rnd) {
		this.network = network;
		this.zonalSystem = zonalSystem;
		this.rnd = rnd;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void registerUsedOdPair(final Zone origin, final Zone destination) {
		this.excludedOdPairs.add(new Tuple<>(origin, destination));
	}

	public Iterator<Trip> missingRoutesIterator() {
		return new TripIterator(this.network, this.zonalSystem,
				this.excludedOdPairs, this.rnd);
	}
}
