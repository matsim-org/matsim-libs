package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingFacilityAttributes;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;

public class LinkFacilityAssociationTest extends MatsimTestCase {

	public void testGeneralFacility() {
		ScenarioImpl sc = new ScenarioImpl();

		NetworkImpl net = BaseNonControlerScenario.loadNetwork(sc);

		LinkFacilityAssociation lfa = new LinkFacilityAssociation(sc.getActivityFacilities(), net);

		// parking is possible at facility 19

		assertEquals("19", lfa.getFacilities(new IdImpl("1")).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(new IdImpl("1")).size());

		// only leisure is possible at facility 42 => used in the parking test
		// afterwards for verification
		assertEquals("42", lfa.getFacilities(new IdImpl("50")).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(new IdImpl("50")).size());
		
		// save state of static variable
		ParkingFacilityAttributes tempParkingFacilityAttributes = ParkingRoot.getParkingFacilityAttributes();
		
		ParkingRoot.setParkingFacilityAttributes(new ParkingFacilityAttributes() {
			public LinkedList<ParkingAttribute> getParkingFacilityAttributes(Id facilityId) {
				LinkedList<ParkingAttribute> result=new LinkedList<ParkingAttribute>();
				result.add(ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG);
				return result;
			}
		});
		
		assertEquals(1, lfa.getFacilitiesHavingParkingAttribute(new IdImpl("50"), ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG).size());
		
		// reset static variable
		ParkingRoot.setParkingFacilityAttributes(tempParkingFacilityAttributes);
	}

	public void testParkingFacility() {
		ScenarioImpl sc = new ScenarioImpl();

		NetworkImpl net = BaseNonControlerScenario.loadNetwork(sc);

		LinkParkingFacilityAssociation lpfa = new LinkParkingFacilityAssociation(sc.getActivityFacilities(), net);

		// parking is possible at facility 19

		assertEquals("19", lpfa.getFacilities(new IdImpl("1")).get(0).getId().toString());
		assertEquals(1, lpfa.getFacilities(new IdImpl("1")).size());

		// only shopping is possible at facility 42 => no parking available at
		// link 50
		assertEquals(0, lpfa.getFacilities(new IdImpl("50")).size());
	}

}
