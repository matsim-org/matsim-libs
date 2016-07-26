package saleem.stockholmscenario.teleportation.ptoptimisation;

import opdytsintegration.MATSimState;
import opdytsintegration.MATSimStateFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.utilities.math.Vector;

public class PTMatsimStateFactoryImpl<U extends DecisionVariable> implements
MATSimStateFactory<U> {
	Scenario scenario;
	public PTMatsimStateFactoryImpl(Scenario scenario) {
		 this.scenario = scenario;
	}

	@Override
	public MATSimState newState(final Population population,
		final Vector stateVector, final U decisionVariable) {
		return new PTMatsimState(population, stateVector, scenario, (PTSchedule)decisionVariable);
	}

}
