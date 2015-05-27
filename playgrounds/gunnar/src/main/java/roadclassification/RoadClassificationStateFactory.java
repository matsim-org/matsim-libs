package roadclassification;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimStateFactory;

public class RoadClassificationStateFactory implements MATSimStateFactory<RoadClassificationState, RoadClassificationDecisionVariable> {

	public RoadClassificationStateFactory() {
	}

	@Override
	public RoadClassificationState newState(Population population,
			Vector stateVector, RoadClassificationDecisionVariable decisionVariable) {
		return new RoadClassificationState(population, stateVector);
	}

}
