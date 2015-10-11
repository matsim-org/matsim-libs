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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import org.matsim.counts.Counts;

import javax.inject.Inject;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RoadClassificationStateFactory
		implements
		MATSimStateFactory {

	// -------------------- MEMBERS --------------------

	@Inject
	void setVolumesAnalyzer(VolumesAnalyzer volumesAnalyzer) {
		this.volumesAnalyzer = volumesAnalyzer;
	}

	private VolumesAnalyzer volumesAnalyzer;

	private final Set<Id<Link>> relevantLinkIds;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	RoadClassificationStateFactory(final Scenario scenario) {
		Counts counts = (Counts) scenario.getScenarioElement(Counts.ELEMENT_NAME);
		this.relevantLinkIds = counts.getCounts().keySet();
	}

	// --------------- IMPLEMENTATION OF MATSimStateFactory ---------------

	@Override
	public MATSimState newState(Population population, Vector stateVector, DecisionVariable decisionVariable) {
		final Map<Id<Link>, int[]> linkId2simulatedCounts = new LinkedHashMap<Id<Link>, int[]>();
		for (Id<Link> linkId : this.relevantLinkIds) {
			linkId2simulatedCounts.put(linkId,
					this.volumesAnalyzer.getVolumesForLink(linkId));
		}
		return new RoadClassificationState(population, stateVector,
				linkId2simulatedCounts);
	}

}
