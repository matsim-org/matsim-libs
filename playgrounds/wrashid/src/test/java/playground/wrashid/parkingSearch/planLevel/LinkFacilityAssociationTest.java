package playground.wrashid.parkingSearch.planLevel;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;

public class LinkFacilityAssociationTest extends MatsimTestCase {

	public static NetworkLayer loadNetwork(ScenarioImpl sc) {
		String facilitiesPath = "test/input/playground/wrashid/parkingSearch/planLevel/chessFacilities.xml";
		String networkFile = "test/input/playground/wrashid/parkingSearch/planLevel/network.xml";
		String inputPlansFile = "test/input/playground/wrashid/parkingSearch/planLevel/chessPlans2.xml";

		new MatsimFacilitiesReader(sc).readFile(facilitiesPath);

		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inputPlansFile);

		new MatsimNetworkReader(sc).readFile(networkFile);

		return sc.getNetwork();
	}

	public void testGeneralFacility() {
		ScenarioImpl sc = new ScenarioImpl();

		NetworkLayer net = loadNetwork(sc);

		LinkFacilityAssociation lfa = new LinkFacilityAssociation(sc.getActivityFacilities(), net);

		// parking is possible at facility 19

		assertEquals("19", lfa.getFacilities(new IdImpl("1")).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(new IdImpl("1")).size());

		// only leisure is possible at facility 42 => used in the parking test
		// afterwards for verification
		assertEquals("42", lfa.getFacilities(new IdImpl("50")).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(new IdImpl("50")).size());
	}

	public void testParkingFacility() {
		ScenarioImpl sc = new ScenarioImpl();

		NetworkLayer net = loadNetwork(sc);

		LinkParkingFacilityAssociation lpfa = new LinkParkingFacilityAssociation(sc.getActivityFacilities(), net);

		// parking is possible at facility 19

		assertEquals("19", lpfa.getFacilities(new IdImpl("1")).get(0).getId().toString());
		assertEquals(1, lpfa.getFacilities(new IdImpl("1")).size());

		// only shopping is possible at facility 42 => no parking available at
		// link 50
		assertEquals(0, lpfa.getFacilities(new IdImpl("50")).size());
	}

}
