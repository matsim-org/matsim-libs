package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;

public class ParkingGeneralLibTest extends TestCase {

	public void testGetAllParkingFacilityIds() {
		ScenarioImpl sc = new ScenarioImpl();

		BaseNonControlerScenario.loadNetwork(sc);

		LinkedList<Id> parkingFacilityIds = ParkingGeneralLib.getAllParkingFacilityIds(sc.getPopulation().getPersons()
				.get(new IdImpl(1)).getSelectedPlan());

		assertEquals(2, parkingFacilityIds.size());
		assertEquals("36", parkingFacilityIds.get(0).toString());
		assertEquals("1", parkingFacilityIds.get(1).toString());
	}
}
