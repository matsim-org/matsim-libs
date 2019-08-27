package org.matsim.contrib.freight.carrier;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

import static org.junit.Assert.*;

public class CarrierServiceTest {

	@Test
	public void getskills() {
		CarrierService service = CarrierService.Builder.newInstance(Id.create("1", CarrierService.class), Id.createLinkId("1"))
				.build();
		Assert.assertTrue("Should not have any skills.", service.getskills().values().isEmpty());

		CarrierService serviceWithSkill = CarrierService.Builder.newInstance(Id.create("1", CarrierService.class), Id.createLinkId("1"))
				.addSkill("Crane")
				.build();
		Assert.assertEquals("Should have one skill.", 1L, serviceWithSkill.getskills().values().size());
		Assert.assertTrue("Cannot find skill 'Crane'.", serviceWithSkill.getskills().containsSkill("Crane"));
	}
}