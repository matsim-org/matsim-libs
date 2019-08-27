package org.matsim.contrib.freight.carrier;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

import static org.junit.Assert.*;

public class CarrierShipmentTest {

	@Test
	public void getSkills() {
		CarrierShipment shipment = CarrierShipment.Builder.newInstance(
				Id.create("1", CarrierShipment.class), Id.createLinkId("1"), Id.createLinkId("2"), 1)
				.build();
		Assert.assertTrue("Should not have any skills.", shipment.getSkills().values().isEmpty());

		CarrierShipment shipmentWithSkill = CarrierShipment.Builder.newInstance(
				Id.create("1", CarrierShipment.class), Id.createLinkId("1"), Id.createLinkId("2"), 1)
				.addSkill("Crane")
				.build();
		Assert.assertEquals("Should have one skill.", 1L, shipmentWithSkill.getSkills().values().size());
		Assert.assertTrue("Cannot find skill 'Crane'.", shipmentWithSkill.getSkills().containsSkill("Crane"));
	}
}