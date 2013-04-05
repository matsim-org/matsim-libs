package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class CarrierCapabilitiesTest extends MatsimTestCase {
	
	public void testAssignTypeToDepot(){
		CarrierCapabilities capabilities = CarrierCapabilities.newInstance();
		capabilities.assignTypeToDepot(makeId("depot"), CarrierVehicleType.Builder.newInstance(makeId("type")).build());
		assertEquals(1,capabilities.getVehicleTypes().size());
		assertEquals(1, capabilities.getDepots().size());
		assertEquals(1, capabilities.getDepotToTypes().get(makeId("depot")).size());
	}

	private Id makeId(String string) {
		return new IdImpl(string);
	}

}
