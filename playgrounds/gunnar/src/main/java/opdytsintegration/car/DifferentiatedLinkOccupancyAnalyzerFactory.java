package opdytsintegration.car;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.handler.EventHandler;

import floetteroed.utilities.math.Vector;
import opdytsintegration.SimulationStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DifferentiatedLinkOccupancyAnalyzerFactory implements SimulationStateAnalyzer {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Set<String> relevantModes;

	private final Set<Id<Link>> relevantLinks;

	private DifferentiatedLinkOccupancyAnalyzer linkOccupancyAnalyzer = null;
	
	// -------------------- CONSTRUCTION --------------------

	public DifferentiatedLinkOccupancyAnalyzerFactory(final TimeDiscretization timeDiscretization,
			final Set<String> relevantModes, final Set<Id<Link>> relevantLinks) {
		this.timeDiscretization = timeDiscretization;
		this.relevantModes = relevantModes;
		this.relevantLinks = relevantLinks;
	}

	// --------------- IMPLEMENTATION OF SimulationStateAnalyzer ---------------

	@Override
	public String getStringIdentifier() {
		return "network modes";
	}

	@Override
	public EventHandler newEventHandler() {
		this.linkOccupancyAnalyzer = new DifferentiatedLinkOccupancyAnalyzer(this.timeDiscretization, this.relevantModes, this.relevantLinks);
		return this.linkOccupancyAnalyzer;
	}

	@Override
	public Vector newStateVectorRepresentation() {
		return this.linkOccupancyAnalyzer.newStateVectorRepresentation();
	}

	@Override
	public void beforeIteration() {
		// TODO may need a non-null check before the first iteration?
		this.linkOccupancyAnalyzer.beforeIteration();
	}

}
