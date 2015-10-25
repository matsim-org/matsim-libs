package roadclassification;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import floetteroed.opdyts.ObjectBasedObjectiveFunction;
import floetteroed.opdyts.SimulatorState;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class RoadClassificationObjectiveFunction implements
		ObjectBasedObjectiveFunction {

	// -------------------- MEMBERS --------------------

	private final Counts<Link> counts;

	// -------------------- CONSTRUCTION --------------------

	RoadClassificationObjectiveFunction(final Counts counts) {
		this.counts = counts;
	}

	// --------------- IMPLEMENTATION of ObjectiveFunction ---------------

	@Override
	public double value(SimulatorState state) {
		RoadClassificationState roadClassificationState = (RoadClassificationState) state;
		double result = 0.0;
		for (Map.Entry<Id<Link>, Count<Link>> linkId2measuredVolumes : this.counts
				.getCounts().entrySet()) {
			final int[] simulatedVolumes = roadClassificationState.getLinkId2simulatedVolumes()
					.get(linkId2measuredVolumes.getKey());
			for (Map.Entry<Integer, Volume> hour2measuredVolume : linkId2measuredVolumes
					.getValue().getVolumes().entrySet()) {
				final int h = hour2measuredVolume.getKey();
				if (simulatedVolumes == null || h > simulatedVolumes.length - 1){
					continue;
				}
				final double measuredVolume = hour2measuredVolume.getValue().getValue();
				final double residual = measuredVolume - simulatedVolumes[h];
				result += residual * residual;
			}
		}
		return result;
	}

}
