package org.matsim.contrib.vsp.pt.fare;

import org.matsim.api.core.v01.Coord;

import java.util.Optional;

public interface PtFareCalculator {
	Optional<FareResult> calculateFare(Coord from, Coord to);

	record FareResult(Double fare, String transactionPartner) {
	}
}
