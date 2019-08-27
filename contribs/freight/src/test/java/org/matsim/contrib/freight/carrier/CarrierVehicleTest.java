package org.matsim.contrib.freight.carrier;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

public class CarrierVehicleTest {
	@Test
	public void getSkills() {
		CarrierVehicle vehicle = CarrierVehicle.Builder.newInstance(Id.createVehicleId("1"), Id.createLinkId("1"))
				.build();
		Assert.assertTrue("There should be no skills set.", vehicle.getSkills().values().isEmpty());

		CarrierVehicle vehicleWithSkills = CarrierVehicle.Builder.newInstance(Id.createVehicleId("2"), Id.createLinkId("1"))
				.addSkill("tailLift")
				.build();
		Assert.assertEquals("Vehicle should have one skill.", 1L, vehicleWithSkills.getSkills().values().size());
		Assert.assertTrue("Cannot find skill 'tailLift'.", vehicleWithSkills.getSkills().containsSkill("tailLift"));
	}


}