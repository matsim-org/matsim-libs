package org.matsim.contrib.osm.networkReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;

public class SupersonicOsmNetworkReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");
	private static final Logger log = LogManager.getLogger(SupersonicOsmNetworkReaderIT.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void test_andorra() {

		Network network = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.setFreeSpeedFactor(LinkProperties.DEFAULT_FREESPEED_FACTOR)
				.build()
				.read(Paths.get(utils.getPackageInputDirectory()).resolve("andorra-latest.osm.pbf"));

		Network expectedResult = NetworkUtils.readNetwork(Paths.get(utils.getInputDirectory()).resolve("expected-result.xml.gz").toString());

		log.info("expected result contains: " + expectedResult.getLinks().size() + " links and " + expectedResult.getNodes().size() + " nodes");
		log.info("result contains: " + network.getLinks().size() + " links and " + network.getNodes().size() + " nodes");

		Utils.assertEquals(expectedResult, network);

		// Alternative expression with functional API that should do the same
		Network alternative = new SupersonicOsmNetworkReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.setFreeSpeedFactor(1.0)
				.setAfterLinkCreated(SupersonicOsmNetworkReader.adjustFreespeed(LinkProperties.DEFAULT_FREESPEED_FACTOR))
				.build()
				.read(Paths.get(utils.getPackageInputDirectory()).resolve("andorra-latest.osm.pbf"));

		Utils.assertEquals(alternative, network);
	}

}
