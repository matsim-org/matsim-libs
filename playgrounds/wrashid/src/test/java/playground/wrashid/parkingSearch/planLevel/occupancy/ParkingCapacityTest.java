package playground.wrashid.parkingSearch.planLevel.occupancy;

import junit.framework.TestCase;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;

public class ParkingCapacityTest extends TestCase {

	public void testBasic() {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		BaseNonControlerScenario.loadNetwork(sc);

		ParkingCapacity pc = new ParkingCapacity(sc.getActivityFacilities());

		assertEquals(1, pc.getParkingCapacity(new IdImpl(8)));
		assertEquals(5, pc.getParkingCapacity(new IdImpl(9)));

	}

}
