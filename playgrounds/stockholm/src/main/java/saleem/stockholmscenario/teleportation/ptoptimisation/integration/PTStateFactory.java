package saleem.stockholmscenario.teleportation.ptoptimisation.integration;

import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;
import opdytsintegration.pt.PTState;
import opdytsintegration.utils.TimeDiscretization;

import org.matsim.api.core.v01.population.Population;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

@Deprecated
public class PTStateFactory implements MATSimStateFactory<DecisionVariable>{
	private final TimeDiscretization timeDiscretization;
	private final double occupancyScale;


	public PTStateFactory(final TimeDiscretization timeDiscretization, 	final double occupancyScale) {
		this.timeDiscretization = timeDiscretization;
		this.occupancyScale=occupancyScale;
	}

	public MATSimState newState(final Population population,
			final Vector stateVector, final DecisionVariable decisionVariable) {
			return new PTState(population, stateVector, this.occupancyScale);
	}
}
