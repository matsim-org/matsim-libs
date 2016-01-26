package floetteroed.opdyts.example.roadpricing;

import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadpricingStateFactory implements MATSimStateFactory<TollLevels> {

	private final TimeDiscretization timeDiscretization;

	private final double occupancyScale;

	private final double tollScale;

	public RoadpricingStateFactory(final TimeDiscretization timeDiscretization,
			final double occupancyScale, final double tollScale) {
		this.timeDiscretization = timeDiscretization;
		this.occupancyScale = occupancyScale;
		this.tollScale = tollScale;
	}

	public MATSimState newState(final Population population,
			final Vector stateVector, final TollLevels decisionVariable) {
		return new RoadpricingState(population, stateVector, decisionVariable,
				this.timeDiscretization, this.occupancyScale, this.tollScale);
	}

}
