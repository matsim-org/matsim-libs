package org.matsim.utils.gis;

import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / senozon
 */
public class ShapeFileReaderTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils(); 
	
	/**
	 * Based on message on users-mailing list from 20Dec2012)
	 * @throws IOException
	 */
	@Test
	public void testPlusInFilename() throws IOException {
		String filename = "src/test/resources/" + utils.getInputDirectory() + "test+test.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(filename);
		Assert.assertEquals(3, fs.getFeatures().size());
	}
}
