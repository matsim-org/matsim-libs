package roadclassification;

import java.util.Map;

import optdyts.ObjectiveFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Counts;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoadClassificationObjectiveFunction implements
		ObjectiveFunction<RoadClassificationState> {

	private final Counts counts;

	public RoadClassificationObjectiveFunction(final Counts counts) {
		this.counts = counts;
	}

	@Override
	public double evaluateState(final RoadClassificationState state) {
		double result = 0.0;
		for (Map.Entry<Id<Link>, int[]> linkId2simulatedCountsEntry : state
				.getLinkId2simulatedCounts().entrySet()) {
			for (int h = 0; h < linkId2simulatedCountsEntry.getValue().length; h++) {
				final double measuredCount = this.counts
						.getCount(linkId2simulatedCountsEntry.getKey())
						.getVolume(h).getValue();
				final double simulatedCount = linkId2simulatedCountsEntry
						.getValue()[h];
				final double residual = measuredCount - simulatedCount;
				result += residual * residual;
			}
		}
		return result;
	}

}
