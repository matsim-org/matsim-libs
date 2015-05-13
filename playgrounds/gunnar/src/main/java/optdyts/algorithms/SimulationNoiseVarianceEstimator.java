package optdyts.algorithms;

import java.util.List;

import optdyts.DecisionVariable;
import optdyts.SimulatorState;
import optdyts.surrogatesolutions.Transition;
import floetteroed.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SimulationNoiseVarianceEstimator<X extends SimulatorState<X>, U extends DecisionVariable> {

	private Double estimatedSimulationNoise = null;

	public SimulationNoiseVarianceEstimator() {
	}

	public Double computeEstimatedSimulationNoise(
			final List<Transition<X, U>> stationaryTransitions) {

		final Vector averageState = new Vector(stationaryTransitions.get(0)
				.getToState().getReferenceToVectorRepresentation().size());
		for (Transition<X, U> transition : stationaryTransitions) {
			averageState.add(transition.getToState()
					.getReferenceToVectorRepresentation());
		}
		averageState.mult(1.0 / stationaryTransitions.size());

		double sumOfSquareDifferences = 0;
		for (Transition<X, U> transition : stationaryTransitions) {
			final Vector diff = transition.getToState()
					.getReferenceToVectorRepresentation().copy();
			diff.add(averageState, -1.0);
			sumOfSquareDifferences += diff.innerProd(diff);
		}

		this.estimatedSimulationNoise = sumOfSquareDifferences
				/ (stationaryTransitions.size() - 1.0);
		return this.estimatedSimulationNoise;
	}

	public Double getEstimatedSimulationNoise() {
		return this.estimatedSimulationNoise;
	}

}
