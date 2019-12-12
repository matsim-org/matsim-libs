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

public class SuperSonicBicycleOsmNetworkReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void read() {

		final Path inputFile = Paths.get(matsimTestUtils.getPackageInputDirectory()).resolve("andorra_latest.osm.pbf");

		Network network = SupersonicBicycleOsmNetworkReader.builder()
				.coordinateTransformation(coordinateTransformation)
				.build()
				.read(inputFile);

		NetworkUtils.writeNetwork(network, "C:/Users/Janek/Desktop/bike-network-test.xml.gz");

	}
}
