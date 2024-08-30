package org.matsim.contrib.sumo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SumoNetworkFeatureExtractorTest {

	@Test
	void twoCoordsBackAndForth() {
		String[] coords = {"0,0", "0,100", "0,0"};
		int lentgh = 200;

		SumoNetworkHandler.Edge edge = new SumoNetworkHandler.Edge("1", "2", "3", "highway", 0, "name", coords);
		edge.lanes.add(new SumoNetworkHandler.Lane("1", 0, lentgh, 50 / 3.6, null, null));

		double ku = SumoNetworkFeatureExtractor.calcCurvature(edge);

		// Length: 0.2 km
		// Gon: 200
		// Curvature: 200 / 0.2 = 1000

		Assertions.assertEquals(1000, ku, 0.000001);

		System.out.println(ku);
	}


	@Test
	void nintyDegreeCorner() {

		String[] coords = {"0,0", "0,100", "100,100"};
		int lentgh = 200;

		SumoNetworkHandler.Edge edge = new SumoNetworkHandler.Edge("1", "2", "3", "highway", 0, "name", coords);
		edge.lanes.add(new SumoNetworkHandler.Lane("1", 0, lentgh, 50 / 3.6, null, null));

		double ku = SumoNetworkFeatureExtractor.calcCurvature(edge);

		// Length: 0.2 km
		// Gon: 100
		// Curvature: 100 / 0.2 = 500

		Assertions.assertEquals(500, ku, 0.000001);
	}

	@Test
	void twoCorners() {

		String[] coords = {"0,0", "0,100", "100,100", "100,200"};
		int lentgh = 300;

		SumoNetworkHandler.Edge edge = new SumoNetworkHandler.Edge("1", "2", "3", "highway", 0, "name", coords);
		edge.lanes.add(new SumoNetworkHandler.Lane("1", 0, lentgh, 50 / 3.6, null, null));

		double ku = SumoNetworkFeatureExtractor.calcCurvature(edge);

		// Length: 0.3 km
		// Gon: 100 + 100 = 200
		// Curvature: 200 / 0.3 = 666.6666666666666

		Assertions.assertEquals((200 / 0.3), ku, 0.000001);
	}

	@Test
	void rectangle() {

		String[] coords = {"0,0", "0,100", "100,100", "100,0", "0,0"};
		int lentgh = 400;

		SumoNetworkHandler.Edge edge = new SumoNetworkHandler.Edge("1", "2", "3", "highway", 0, "name", coords);
		edge.lanes.add(new SumoNetworkHandler.Lane("1", 0, lentgh, 50 / 3.6, null, null));

		double ku = SumoNetworkFeatureExtractor.calcCurvature(edge);

		// Length: 0.4 km
		// Gon: 100 + 100 + 100 = 300
		// Curvature: 300 / 0.4 = 666.6666666666666

		Assertions.assertEquals((300 / 0.4), ku, 0.000001);
	}
}
