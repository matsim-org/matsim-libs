package org.matsim.contrib.roadpricing.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class RunRoadPricingFromCodeIT {
	final private static Logger LOG = Logger.getLogger(RunRoadPricingFromCodeIT.class);
	private static final String TEST_CONFIG = "./test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	@Test
	public void testRunRoadPricingFromCode(){
		try{
			LOG.info("Run context: " + new File("./").getAbsolutePath());
			String[] args = new String[]{TEST_CONFIG};
			RunRoadPricingFromCode.main(args);
		} catch (Exception e){
			e.printStackTrace();
			Assert.fail("Example should run without exception.");
		}
	}
}