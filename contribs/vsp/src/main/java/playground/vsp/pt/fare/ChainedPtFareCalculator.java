package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;

import java.util.Optional;
import java.util.Set;

public class ChainedPtFareCalculator implements PtFareCalculator {
	@Inject
	private Set<PtFareCalculator> fareCalculators;

	@Override
	public Optional<FareResult> calculateFare(Coord from, Coord to) {
		for (PtFareCalculator fareCalculator : fareCalculators) {
			Optional<FareResult> fareResult = fareCalculator.calculateFare(from, to);
			if (fareResult.isPresent()) {
				return fareResult;
			}
		}
		return Optional.empty();
	}
}
