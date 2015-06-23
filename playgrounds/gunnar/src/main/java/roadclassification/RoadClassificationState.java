package roadclassification;

import org.matsim.api.core.v01.population.Population;

import floetteroed.utilities.math.Vector;
import opdytsintegration.AbstractMATSimState;

public class RoadClassificationState extends
		AbstractMATSimState {

	public RoadClassificationState(Population population,
			Vector vectorRepresentation) {
		super(population, vectorRepresentation);
	}

	@Override
	public Vector getReferenceToVectorRepresentation() {
		return super.getReferenceToVectorRepresentation();
	}

	@Override
	public void implementInSimulation() {
		super.implementInSimulation();
	}

}
