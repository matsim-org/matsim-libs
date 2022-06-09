package org.matsim.application.options;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
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

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void readZip() {

		// use same shape file as for land-use
		Path input = Path.of(utils.getClassInputDirectory()
				.replace("ShpOptionsTest", "CreateLandUseShpTest")
				.replace("options", "prepare"))
				.resolve("andorra-latest-free.shp.zip");

		Assume.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);

		List<SimpleFeature> ft = shp.readFeatures();

		assertThat(ft)
				.isNotEmpty();

	}

	@Test
	public void all() {

		// use same shape file as for land-use
		Path input = Path.of(utils.getClassInputDirectory()
				.replace("ShpOptionsTest", "CreateLandUseShpTest")
				.replace("options", "prepare"))
				.resolve("andorra-latest-free.shp.zip");

		Assume.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);

		ShpOptions.Index index = shp.createIndex(shp.getShapeCrs(), "_");

		List<SimpleFeature> ft = index.getAll();

		assertThat(ft)
				.hasSize(578)
				.hasSize(Set.copyOf(ft).size());

	}

	@Test
	public void testGetGeometry() {

		Path input = Path.of(utils.getClassInputDirectory()
						.replace("ShpOptionsTest", "CreateLandUseShpTest")
						.replace("options", "prepare"))
				.resolve("andorra-latest-free.shp.zip");

		Assume.assumeTrue(Files.exists(input));

		ShpOptions shp = new ShpOptions(input, null, null);
		Geometry geometry = shp.getGeometry() ;
		Geometry expectedGeometry = new GeometryFactory().createEmpty(2);

		List<SimpleFeature> features = shp.readFeatures();

		for(SimpleFeature feature : features) {
			Geometry geometryToJoin = (Geometry) feature.getDefaultGeometry();
			expectedGeometry = expectedGeometry.union(geometryToJoin);
		}

		Assert.assertTrue(geometry.equals(expectedGeometry));
	}
}