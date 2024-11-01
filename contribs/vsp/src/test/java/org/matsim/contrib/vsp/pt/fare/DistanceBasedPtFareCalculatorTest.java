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

	private static final URL context = ExamplesUtils.getTestScenarioURL("kelheim");

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
		String shape = IOUtils.extendUrl(context, "ptTestArea/pt-area.shp").toString();
		Coord a = new Coord(726634.40, 5433508.07);
		Coord b = new Coord(736634.40, 5533508.07);
		DistanceBasedPtFareCalculator calculator = getCalculator(shape);
		Assertions.assertEquals(Optional.empty(), calculator.calculateFare(a, b));
	}

	@Test
	void testInShapeFile() {
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
		return new DistanceBasedPtFareCalculator(params, context);
	}

	@Test
	void testModifyParams() {
		var params = new DistanceBasedPtFareParams();
		params.setTransactionPartner(TRANSACTION_PARTNER);

		//2000m+: 3EUR + 0.5EUR/km
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClassLongFareParams =
			params.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
		distanceClassLongFareParams.setFareIntercept(3.0);
		distanceClassLongFareParams.setFareSlope(0.0005);

		//0-2000m: 1EUR + 1EUR/km
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClass2kmFareParams =
			params.getOrCreateDistanceClassFareParams(2000.0);
		distanceClass2kmFareParams.setFareIntercept(1.0);
		distanceClass2kmFareParams.setFareSlope(0.001);

		// get params for max distance 2km and modify
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClass2kmDuplicateFareParams =
			params.getOrCreateDistanceClassFareParams(2000.0);
		distanceClass2kmDuplicateFareParams.setFareIntercept(2.0);
		distanceClass2kmDuplicateFareParams.setFareSlope(0.002);

		params.setMinFare(1.0);

		DistanceBasedPtFareCalculator distanceBasedPtFareCalculator = new DistanceBasedPtFareCalculator(params, context);
		PtFareCalculator.FareResult fareResult = distanceBasedPtFareCalculator.calculateFare(new Coord(0, 0), new Coord(0, 100)).orElseThrow();
		Assertions.assertEquals(new PtFareCalculator.FareResult(2.2, TRANSACTION_PARTNER), fareResult);
	}

	@Test
	void testMultipleParamsWithSameMaxDistance() {
		var params = new DistanceBasedPtFareParams();
		params.setTransactionPartner(TRANSACTION_PARTNER);
		//0-2000m: 1EUR + 1EUR/km
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClass2kmFareParams =
			params.getOrCreateDistanceClassFareParams(2000.0);
		distanceClass2kmFareParams.setFareIntercept(1.0);
		distanceClass2kmFareParams.setFareSlope(0.001);

		// add second distance class with same max distance
		DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams distanceClass2kmDuplicateFareParams =
			new DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams();
		distanceClass2kmDuplicateFareParams.setMaxDistance(2000.0);
		distanceClass2kmDuplicateFareParams.setFareIntercept(2.0);
		distanceClass2kmDuplicateFareParams.setFareSlope(0.002);
		// add in a different way so no automatic overwrite
		params.addParameterSet(distanceClass2kmDuplicateFareParams);

		Assertions.assertThrows(RuntimeException.class, () -> new DistanceBasedPtFareCalculator(params, context),
			"DistanceBasedPtFareCalculator should crash if multiple DistanceClassLinearFareFunctionParams with the same max distance " +
				" are present, because it is unclear which of them should be applied at that distance.");
	}
}
