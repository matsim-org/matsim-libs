package org.matsim.contrib.vsp.pt.fare;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import java.net.URL;
import java.util.Optional;

class DistanceBasedPtFareCalculatorTest {
	private static final String TRANSACTION_PARTNER = "TP";

	@Test
	void testNormalDistance() {
		//100m -> 1.1 EUR
		calculateAndCheck(null, new Coord(0, 0), new Coord(0, 100), 1.1);
	}

	@Test
	void testLongDistance() {
		//3000m -> 4.5 EUR
		calculateAndCheck(null, new Coord(0, 0), new Coord(0, 3000), 4.5);
	}

	@Test
	void testThreshold() {
		//2000m -> 3.0 EUR
		calculateAndCheck(null, new Coord(0, 0), new Coord(0, 2000), 3.0);
	}

	@Test
	void testNotInShapeFile() {
		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		String shape = IOUtils.extendUrl(context, "ptTestArea/pt-area.shp").toString();
		Coord a = new Coord(726634.40, 5433508.07);
		Coord b = new Coord(736634.40, 5533508.07);
		DistanceBasedPtFareCalculator calculator = getCalculator(shape);
		Assertions.assertEquals(Optional.empty(), calculator.calculateFare(a, b));
	}

	@Test
	void testInShapeFile() {
		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		String shape = IOUtils.extendUrl(context, "ptTestArea/pt-area.shp").toString();
		Coord a = new Coord(710300.624, 5422165.737);
		Coord b = new Coord(714940.65, 5420707.78);
		double distance = CoordUtils.calcEuclideanDistance(a, b);
		calculateAndCheck(shape, a, b, distance * 0.0005 + 3.);
	}

	private void calculateAndCheck(String shape, Coord a, Coord b, double fare) {
		DistanceBasedPtFareCalculator distanceBasedPtFareCalculator = getCalculator(shape);
		PtFareCalculator.FareResult fareResult = distanceBasedPtFareCalculator.calculateFare(a, b).orElseThrow();
		Assertions.assertEquals(new PtFareCalculator.FareResult(fare, TRANSACTION_PARTNER), fareResult);
	}

	private DistanceBasedPtFareCalculator getCalculator(String shapeFile) {
		var params = new DistanceBasedPtFareParams();
		params.setTransactionPartner(TRANSACTION_PARTNER);
		//0-2000m: 1EUR + 1EUR/km
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClass2kmFareParams =
			params.getOrCreateDistanceClassFareParams(2000.0);
		distanceClass2kmFareParams.setFareIntercept(1.0);
		distanceClass2kmFareParams.setFareSlope(0.001);

		//2000m+: 3EUR + 0.5EUR/km
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClassLongFareParams =
			params.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
		distanceClassLongFareParams.setFareIntercept(3.0);
		distanceClassLongFareParams.setFareSlope(0.0005);

		params.setMinFare(1.0);
		params.setFareZoneShp(shapeFile);
		return new DistanceBasedPtFareCalculator(params);
	}
}
