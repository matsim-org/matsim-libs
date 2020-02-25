package org.matsim.contrib.osm.networkReader;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import static org.junit.Assert.fail;

public class OsmSignalsParserTest {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void parse() {

		Path file = Paths.get(utils.getPackageInputDirectory()).resolve("andorra-latest.osm.pbf");

		OsmSignalsParser parser = new OsmSignalsParser(coordinateTransformation,
				LinkProperties.createLinkProperties(),
				(coord, id) -> true, Executors.newSingleThreadExecutor());

		parser.parse(file);

		fail();

	}
}