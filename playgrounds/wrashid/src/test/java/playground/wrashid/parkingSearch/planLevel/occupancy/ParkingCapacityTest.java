package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;
import junit.framework.TestCase;

public class ParkingCapacityTest extends TestCase {

	public void testBasic() {
		ScenarioImpl sc = new ScenarioImpl();
		BaseNonControlerScenario.loadNetwork(sc);

		ParkingCapacity pc = new ParkingCapacity(sc.getActivityFacilities());

		assertEquals(1, pc.getParkingCapacity(new IdImpl(8)));
		assertEquals(5, pc.getParkingCapacity(new IdImpl(9)));

	}

}
