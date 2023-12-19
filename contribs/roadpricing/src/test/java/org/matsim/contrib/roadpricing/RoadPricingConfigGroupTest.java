package org.matsim.contrib.roadpricing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;


public class RoadPricingConfigGroupTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void getTollLinksFile() {
		RoadPricingConfigGroup cg = new RoadPricingConfigGroup();
		Assertions.assertNull(cg.getTollLinksFile(), "Default roadpricing file is not set.");
	}

	@Test
	void setTollLinksFile() {
		String file = "./test.xml.gz";
		RoadPricingConfigGroup cg = new RoadPricingConfigGroup();
		cg.setTollLinksFile(file);
		Assertions.assertEquals(file, cg.getTollLinksFile(), "Wrong input file.");
	}

	@Test
	void getEnforcementProbability() {
		RoadPricingConfigGroup cg = new RoadPricingConfigGroup();
		Assertions.assertEquals(1.0, cg.getEnforcementProbability(), MatsimTestUtils.EPSILON, "Default probability should be 1.0");

		double prob = 0.9;
		cg.setEnforcementProbability(prob);
		Assertions.assertEquals(prob, cg.getEnforcementProbability(), MatsimTestUtils.EPSILON, "Didn't get the adjusted probability.");
	}

	@Test
	void setEnforcementProbability() {
		RoadPricingConfigGroup cg = new RoadPricingConfigGroup();
		try{
			cg.setEnforcementProbability(1.2);
			Assertions.fail("Should not accept probability > 1.0");
		} catch (Exception e){
			e.printStackTrace();
		}

		cg.setEnforcementProbability(0.95);
	}
}
