package roadclassification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import opdytsintegration.MATSimStateFactory;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadClassificationStateFactory<U extends RoadClassificationDecisionVariable>
		implements MATSimStateFactory<RoadClassificationState, U> {

	// -------------------- MEMBERS --------------------

	private final VolumesAnalyzer volumesAnalyzer;

	private final Set<Id<Link>> linkIds;

	// -------------------- CONSTRUCTION --------------------

	public RoadClassificationStateFactory(
			final VolumesAnalyzer volumesAnalyzer, final Set<Id<Link>> linkIds) {
		this.volumesAnalyzer = volumesAnalyzer;
		this.linkIds = linkIds;
	}

	// --------------- IMPLEMENTATION OF MATSimStateFactory ---------------

	@Override
	public RoadClassificationState newState(final Population population,
			final Vector stateVector, final U decisionVariable) {
		final Map<Id<Link>, int[]> linkId2simulatedCounts = new LinkedHashMap<Id<Link>, int[]>();
		for (Id<Link> linkId : this.linkIds) {
			linkId2simulatedCounts.put(linkId,
					this.volumesAnalyzer.getVolumesForLink(linkId));
		}
		return new RoadClassificationState(population, stateVector,
				linkId2simulatedCounts);
	}

}
