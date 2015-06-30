package roadclassification;

import java.util.Map;

import opdytsintegration.MATSimState;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RoadClassificationState extends MATSimState {

	// -------------------- MEMBERS --------------------

	private final Map<Id<Link>, int[]> linkId2simulatedVolumes;

	// -------------------- CONSTRUCTION --------------------

	RoadClassificationState(final Population population,
			final Vector vectorRepresentation,
			final Map<Id<Link>, int[]> linkId2simulatedVolumes) {
		super(population, vectorRepresentation);
		this.linkId2simulatedVolumes = linkId2simulatedVolumes;
	}

	// -------------------- GETTERS --------------------

	Map<Id<Link>, int[]> getLinkId2simulatedVolumes() {
		return this.linkId2simulatedVolumes;
	}

}
