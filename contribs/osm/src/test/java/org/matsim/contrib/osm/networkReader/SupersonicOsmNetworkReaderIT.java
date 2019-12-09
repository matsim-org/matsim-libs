package org.matsim.contrib.osm.networkReader;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class SupersonicOsmNetworkReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test_andorra() {

		Network network = SupersonicOsmNetworkReader.builder()
				.coordinateTransformation(coordinateTransformation)
				.build()
				.read(Paths.get(utils.getInputDirectory()).resolve("andorra-latest.osm.pbf"));

		Network expectedResult = NetworkUtils.readNetwork(Paths.get(utils.getInputDirectory()).resolve("expected-result.xml.gz").toString());

		assertTrue(NetworkUtils.compare(expectedResult, network));
	}
}
