package org.matsim.application.options;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.testcases.MatsimTestUtils;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class ShpOptionsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void readZip() {

		// use same shape file as for land-use
		Path input = Path.of(utils.getClassInputDirectory()
				.replace("ShpOptionsTest", "CreateLandUseShpTest")
				.replace("options", "prepare"))
				.resolve("andorra-latest-free.shp.zip");

		Assumptions.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);

		List<SimpleFeature> ft = shp.readFeatures();

		assertThat(ft)
				.isNotEmpty();

	}

	@Test
	void all() {

		// use same shape file as for land-use
		Path input = Path.of(utils.getClassInputDirectory()
				.replace("ShpOptionsTest", "CreateLandUseShpTest")
				.replace("options", "prepare"))
				.resolve("andorra-latest-free.shp.zip");

		Assumptions.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);

		ShpOptions.Index index = shp.createIndex(shp.getShapeCrs(), "_");

		List<SimpleFeature> ft = index.getAll();

		assertThat(ft)
				.hasSize(4906)
				.hasSize(Set.copyOf(ft).size());

	}

	@Test
	void testGetGeometry() {

		Path input = Path.of(utils.getClassInputDirectory()
						.replace("ShpOptionsTest", "CreateLandUseShpTest")
						.replace("options", "prepare"))
				.resolve("andorra-latest-free.shp.zip");

		Assumptions.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);
		Geometry geometry = shp.getGeometry() ;

		assertThat(geometry.getArea())
			.isCloseTo(1.9847543618489646E-4, Offset.offset(1e-8));

	}
}
