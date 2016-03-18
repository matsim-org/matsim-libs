package saleem.stockholmscenario.teleportation.ptoptimisation;

import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;

public class PTStateFactory implements MATSimStateFactory<PTSchedule>{
	private final TimeDiscretization timeDiscretization;
	private final double occupancyScale;


	public PTStateFactory(final TimeDiscretization timeDiscretization, 	final double occupancyScale) {
		this.timeDiscretization = timeDiscretization;
		this.occupancyScale=occupancyScale;
	}

	public MATSimState newState(final Population population,
			final Vector stateVector, final PTSchedule decisionVariable) {
			return new PTState(population, stateVector, this.occupancyScale);
	}
}
