package org.matsim.contrib.osm.networkReader;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;

public class SupersonicOsmNetworkReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");
	private static final Logger log = Logger.getLogger(SupersonicOsmNetworkReaderIT.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test_andorra() {

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.build()
				.read(Paths.get(utils.getPackageInputDirectory()).resolve("andorra-latest.osm.pbf"));

		NetworkUtils.writeNetwork(network, "expected.xml.gz");

		Network expectedResult = NetworkUtils.readNetwork(Paths.get(utils.getInputDirectory()).resolve("expected-result.xml.gz").toString());

		log.info("expected result contains: " + expectedResult.getLinks().size() + " links and " + expectedResult.getNodes().size() + " nodes");
		log.info("result contains: " + network.getLinks().size() + " links and " + network.getNodes().size() + " nodes");

		Utils.assertEquals(expectedResult, network);
	}
}
