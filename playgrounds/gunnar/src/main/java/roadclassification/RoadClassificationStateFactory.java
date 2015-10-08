package roadclassification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.inject.Provider;
import floetteroed.opdyts.DecisionVariable;
import opdytsintegration.MATSimState;
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
class RoadClassificationStateFactory
		implements
		MATSimStateFactory {

	// -------------------- MEMBERS --------------------

	private final Provider<VolumesAnalyzer> volumesAnalyzer;

	private final Set<Id<Link>> relevantLinkIds;

	// -------------------- CONSTRUCTION --------------------

	RoadClassificationStateFactory(final Provider<VolumesAnalyzer> volumesAnalyzer,
			final Set<Id<Link>> relevantLinkIds) {
		this.volumesAnalyzer = volumesAnalyzer;
		this.relevantLinkIds = relevantLinkIds;
	}

	// --------------- IMPLEMENTATION OF MATSimStateFactory ---------------

	@Override
	public MATSimState newState(Population population, Vector stateVector, DecisionVariable decisionVariable) {
		final Map<Id<Link>, int[]> linkId2simulatedCounts = new LinkedHashMap<Id<Link>, int[]>();
		for (Id<Link> linkId : this.relevantLinkIds) {
			linkId2simulatedCounts.put(linkId,
					this.volumesAnalyzer.get().getVolumesForLink(linkId));
		}
		return new RoadClassificationState(population, stateVector,
				linkId2simulatedCounts);
	}

}
