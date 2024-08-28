package org.matsim.contrib.vsp.pt.fare;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FareZoneBasedPtFareCalculatorTest {
	private static final double COST_IN_SHP_FILE = 1.5;
	private static final String TRANSACTION_PARTNER = "TP";
	private static final URL context = ExamplesUtils.getTestScenarioURL("kelheim");

	@Test
	void testCalculateFareInShape() {
		FareZoneBasedPtFareCalculator fareZoneBasedPtFareCalculator = new FareZoneBasedPtFareCalculator(getParams(), context);

		Coord inShape = new Coord(710300.624, 5422165.737);
		Coord inShape2 = new Coord(714940.65, 5420707.78);

		assertEquals(Optional.of(new PtFareCalculator.FareResult(COST_IN_SHP_FILE, TRANSACTION_PARTNER)),
			fareZoneBasedPtFareCalculator.calculateFare(inShape,
				inShape2));
		assertEquals(Optional.of(new PtFareCalculator.FareResult(COST_IN_SHP_FILE, TRANSACTION_PARTNER)),
			fareZoneBasedPtFareCalculator.calculateFare(inShape2,
				inShape));
	}

	@Test
	void testCalculateFareOutShape() {
		FareZoneBasedPtFareCalculator fareZoneBasedPtFareCalculator = new FareZoneBasedPtFareCalculator(getParams(), context);

		Coord inShape = new Coord(710300.624, 5422165.737);
		Coord inShape2 = new Coord(714940.65, 5420707.78);
		Coord outShape = new Coord(726634.40, 5433508.07);
		Coord outShape2 = new Coord(736634.40, 5533508.07);

		assertEquals(Optional.empty(), fareZoneBasedPtFareCalculator.calculateFare(inShape, outShape));
		assertEquals(Optional.empty(), fareZoneBasedPtFareCalculator.calculateFare(inShape, outShape2));
		assertEquals(Optional.empty(), fareZoneBasedPtFareCalculator.calculateFare(inShape2, outShape));
		assertEquals(Optional.empty(), fareZoneBasedPtFareCalculator.calculateFare(inShape2, outShape));
		assertEquals(Optional.empty(), fareZoneBasedPtFareCalculator.calculateFare(outShape, outShape2));
	}

	private FareZoneBasedPtFareParams getParams() {
		FareZoneBasedPtFareParams fareZoneBasedPtFareParams = new FareZoneBasedPtFareParams();
		fareZoneBasedPtFareParams.setFareZoneShp("ptTestArea/pt-area.shp");
		fareZoneBasedPtFareParams.setTransactionPartner(TRANSACTION_PARTNER);
		return fareZoneBasedPtFareParams;
	}

}
