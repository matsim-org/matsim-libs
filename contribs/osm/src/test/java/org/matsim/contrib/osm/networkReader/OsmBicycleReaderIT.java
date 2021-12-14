package org.matsim.contrib.osm.networkReader;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OsmBicycleReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void test_andorra() {

		final Path inputFile = Paths.get(matsimTestUtils.getPackageInputDirectory()).resolve("andorra-latest.osm.pbf");

		Network network = new OsmBicycleReader.Builder()
				.setCoordinateTransformation(coordinateTransformation)
				.build()
				.read(inputFile);

		Network expectedResult = NetworkUtils.readTimeInvariantNetwork(Paths.get(matsimTestUtils.getInputDirectory()).resolve("expected-result.xml.gz").toString());

		Utils.assertEquals(expectedResult, network);
	}
}
