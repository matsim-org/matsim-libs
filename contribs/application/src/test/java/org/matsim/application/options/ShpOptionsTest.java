package org.matsim.application.options;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
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
		Geometry geometry1 = shp.getGeometry() ;
		Geometry geometry2 = null ;
		Geometry geometry3 = null ;

		List<SimpleFeature> features = shp.readFeatures();

		geometry3 = (Geometry) features.get(0).getDefaultGeometry();

		for(SimpleFeature feature : features) {
			if(geometry2 == null) {
				geometry2 = (Geometry) feature.getDefaultGeometry();
			} else {
				geometry2 = geometry2.union((Geometry) feature.getDefaultGeometry());
			}
		}

		Assert.assertFalse(geometry1.equals(geometry3));
		Assert.assertTrue(geometry1.equals(geometry2));
	}
}