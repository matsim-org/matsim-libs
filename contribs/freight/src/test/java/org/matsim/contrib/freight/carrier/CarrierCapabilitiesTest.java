package org.matsim.contrib.freight.carrier;

import com.graphhopper.jsprit.core.problem.Skills;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class CarrierCapabilitiesTest {

	@Test
	public void getSkills() {
		CarrierCapabilities carrierCapabilities = CarrierCapabilities.Builder.newInstance().build();
		Assert.assertTrue("There should be no skills set.", carrierCapabilities.getSkills().isEmpty());

		CarrierCapabilities carrierCapabilitiesWithSkill = CarrierCapabilities.Builder.newInstance().addSkill("tailLift").build();
		Assert.assertEquals("Should have one skill.", 1L, carrierCapabilitiesWithSkill.getSkills().size());
	}

}