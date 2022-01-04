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
