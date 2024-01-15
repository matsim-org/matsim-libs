package org.matsim.contrib.osm.networkReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OsmBicycleReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");

	@RegisterExtension
	private MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	void test_andorra() {

		final Path inputFile = Paths.get(matsimTestUtils.getPackageInputDirectory()).resolve("andorra-latest.osm.pbf");

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.build()
				.read(inputFile);

		Network expectedResult = NetworkUtils.readNetwork(Paths.get(matsimTestUtils.getInputDirectory()).resolve("expected-result.xml.gz").toString());

		Utils.assertEquals(expectedResult, network);
	}
}
