package org.matsim.contrib.roadpricing.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class RunRoadPricingFromCodeIT {
	final private static Logger LOG = LogManager.getLogger(RunRoadPricingFromCodeIT.class);
	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	void testRunRoadPricingFromCode(){
		try{
			LOG.info("Run context: " + new File("./").getAbsolutePath());
			String[] args = new String[]{TEST_CONFIG};
			RunRoadPricingFromCode.main(args);
		} catch (Exception e){
			e.printStackTrace();
			Assertions.fail("Example should run without exception.");
		}
	}
}