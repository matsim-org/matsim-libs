package org.matsim.application.options;

import org.assertj.core.data.Offset;
import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.testcases.MatsimTestUtils;

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
	void get() {

		Path input = Path.of(utils.getClassInputDirectory()
				.replace("ShpOptionsTest", "CreateLandUseShpTest")
				.replace("options", "prepare"))
			.resolve("andorra-latest-free.shp.zip");

		Assumptions.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);

		ShpOptions.Index index = shp.createIndex(shp.getShapeCrs(), "name");

		SimpleFeature result = index.queryFeature(new Coord(1.5333461, 42.555388));

		assertThat(result)
			.isNotNull();

		String name = index.query(new Coord(1.5333461, 42.555388));
		assertThat(name)
			.isEqualTo("Museu de la Miniatura");
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

		List<SimpleFeature> ft = index.getAllFeatures();

		assertThat(ft)
			.hasSize(4906)
			.hasSize(Set.copyOf(ft).size());

		assertThat(shp.readFeatures())
			.hasSameElementsAs(ft);

	}

	@Test
	void testGetGeometry() {

		Path input = Path.of(utils.getClassInputDirectory()
				.replace("ShpOptionsTest", "CreateLandUseShpTest")
				.replace("options", "prepare"))
			.resolve("andorra-latest-free.shp.zip");

		Assumptions.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);
		Geometry geometry = shp.getGeometry();

		assertThat(geometry.getArea())
			.isCloseTo(1.9847543618489646E-4, Offset.offset(1e-8));

	}

	@Test
	void gpkg() {

		Path path = Path.of(utils.getPackageInputDirectory(), "example.gpkg");

		ShpOptions shp = ShpOptions.ofLayer(path.toString(), null);

		List<SimpleFeature> features = shp.readFeatures();

		assertThat(features)
			.hasSize(3);

		ShpOptions.Index index = shp.createIndex("_");

		assertThat(index.size())
			.isEqualTo(3);

	}
}
