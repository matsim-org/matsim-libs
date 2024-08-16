package org.matsim.contrib.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;

import java.util.Optional;
import java.util.Set;

/**
 * This class is a {@link PtFareCalculator} that chains multiple {@link PtFareCalculator}s together. As soon as one of the chained calculators
 * returns a fare, this calculator returns that fare. In {@link PtFareModule} all available {@link PtFareCalculator}s are bound to this class. The
 * order in which the calculators are bound is determined by the priority of the {@link PtFareParams} they are created from.
 */
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
