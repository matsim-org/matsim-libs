package org.matsim.contrib.sumo;

import org.junit.jupiter.api.Test;

class SumoNetworkFeatureExtractorTest {

	@Test
	void curvature() throws Exception {

		String[] coords = {"0,0", "0,100", "0,0"};
		SumoNetworkHandler.Edge edge = new SumoNetworkHandler.Edge("1", "2", "3", "highway", 0, "name", coords);
		edge.lanes.add(new SumoNetworkHandler.Lane("1", 0, 200, 50/3.6, null, null));

		double ku = SumoNetworkFeatureExtractor.calcCurvature(edge);

		//TODO should be around 400 for 200m road

		System.out.println(ku);

	}
}
