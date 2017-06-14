package opdytsintegration.pt;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import floetteroed.utilities.math.Vector;
import opdytsintegration.SimulationStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;

public class PTOccupancyAnalyzerFactory implements SimulationStateAnalyzer {

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Set<Id<TransitStopFacility>> relevantStops;

	private PTOccupancyAnalyser analyzer = null;

	// -------------------- CONSTRUCTION --------------------

	public PTOccupancyAnalyzerFactory(final TimeDiscretization timeDiscretization,
			final Set<Id<TransitStopFacility>> relevantStops) {
		this.timeDiscretization = timeDiscretization;
		this.relevantStops = relevantStops;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public String getStringIdentifier() {
		return "pt";
	}

	@Override
	public EventHandler newEventHandler() {
		this.analyzer = new PTOccupancyAnalyser(this.timeDiscretization, this.relevantStops);
		return this.analyzer;
	}

	@Override
	public Vector newStateVectorRepresentation() {
		return this.analyzer.newStateVectorRepresentation();
	}

	@Override
	public void beforeIteration() {
		this.analyzer.beforeIteration();
	}

}
