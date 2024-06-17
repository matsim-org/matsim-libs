package org.matsim.application.prepare.freight.tripGeneration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.options.LanduseOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

class DefaultLocationCalculatorTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testDefaultLocationCalculator(){
		String regionShpString = utils.getPackageInputDirectory() + "testRegion.shp";
		String networkXmlString = utils.getPackageInputDirectory() + "testNetwork.xml";
		String lookupTableString = "file:" + utils.getPackageInputDirectory() + "testLookup.csv";

		DefaultLocationCalculator locationCalculator;
		try{
			Network network = NetworkUtils.readNetwork(networkXmlString);
			locationCalculator = new DefaultLocationCalculator(network, Path.of(regionShpString), lookupTableString, new LanduseOptions()); //LanduseOptions-class is not tested here
		} catch (Exception e){
			Assertions.fail(e); // If this Assertion fails, there is an external problem in another class
			return;
		}

		//Test the getLocationOnNetwork() method
		List<String> referenceLinksCell1 = Arrays.asList("cd", "de"); // All links-names in Cell 1
		List<String> referenceLinksCell5 = Arrays.asList("ab", "bc"); // All links-names in Cell 5
		List<String> referenceLinksCell6 = Arrays.asList("ef"); // All links-names in Cell 6 (backup-link-test)

		// Check 5 times if the links are in the correct cell
		// Since this function is non-deterministic, it is impossible to make a 100% accurate test
		for(int i = 0; i < 5; i++){
			String testId1 = locationCalculator.getLocationOnNetwork("1").toString();
			Assertions.assertTrue(referenceLinksCell1.contains(testId1));
			String testId5 = locationCalculator.getLocationOnNetwork("5").toString();
			Assertions.assertTrue(referenceLinksCell5.contains(testId5));
			String testId6 = locationCalculator.getLocationOnNetwork("6").toString();
			Assertions.assertTrue(referenceLinksCell6.contains(testId6));
		}

		//Test the getVerkehrszelleOfLink() method
		Assertions.assertEquals("5", locationCalculator.getVerkehrszelleOfLink(Id.createLinkId("ab")));
		Assertions.assertEquals("5", locationCalculator.getVerkehrszelleOfLink(Id.createLinkId("bc")));
		Assertions.assertEquals("1", locationCalculator.getVerkehrszelleOfLink(Id.createLinkId("cd")));
		Assertions.assertEquals("1", locationCalculator.getVerkehrszelleOfLink(Id.createLinkId("de")));
		Assertions.assertEquals("6", locationCalculator.getVerkehrszelleOfLink(Id.createLinkId("ef")));
	}
}
