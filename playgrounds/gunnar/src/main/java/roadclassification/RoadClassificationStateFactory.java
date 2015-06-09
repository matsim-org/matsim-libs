package roadclassification;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.MATSimStateFactory;

public class RoadClassificationStateFactory implements MATSimStateFactory<RoadClassificationState, AbstractRoadClassificationDecisionVariable> {

	public RoadClassificationStateFactory() {
	}

	@Override
	public RoadClassificationState newState(Population population,
			Vector stateVector, AbstractRoadClassificationDecisionVariable decisionVariable) {
		return new RoadClassificationState(population, stateVector);
	}

}
